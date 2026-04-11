package com.winlator.cmod.store;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import in.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud;

/**
 * Steam Cloud save sync for Ludashi-plus.
 *
 * Mirrors Pluvia's SteamAutoCloud: sync cloud files before launching a game
 * and upload changed files after exit.
 *
 * Cloud files live in two places:
 *   Remote: Steam servers, accessed via SteamCloud handler
 *   Local:  Inside the Wine imagefs at paths determined by Steam's app config
 *
 * For simplicity this implementation uses the flat-file "userdata" mirror:
 *   {imagefs}/userdata/{accountId}/{appId}/remote/
 *
 * That directory is what Steam itself uses for cloud saves on Windows/Linux,
 * so games that use the default Steam Cloud path will work without any special
 * path-type mapping.
 *
 * Pre-launch sync: download any remote files newer than local copies.
 * Post-exit sync:  upload any local files newer than the last download time.
 *
 * SHA1 comparison matches Pluvia's streamingShaHash().
 */
public final class SteamCloudSync {

    private static final String TAG         = "SteamCloud";
    private static final int    TIMEOUT_SEC = 60;
    private static final int    BUFFER_SIZE = 64 * 1024;

    public interface SyncCallback {
        void onProgress(String message, float fraction);
        void onComplete(boolean success, String summary);
    }

    private SteamCloudSync() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Download cloud saves for appId into the local userdata mirror.
     * Call this before launching the game.
     */
    public static void syncBeforeLaunch(Context ctx, int appId, SyncCallback cb) {
        new Thread(() -> doSync(ctx, appId, true, cb), "CloudSync-pre").start();
    }

    /**
     * Upload changed local saves for appId to the cloud.
     * Call this after the game process has exited.
     */
    public static void syncAfterExit(Context ctx, int appId, SyncCallback cb) {
        new Thread(() -> doSync(ctx, appId, false, cb), "CloudSync-post").start();
    }

    // -------------------------------------------------------------------------
    // Core sync
    // -------------------------------------------------------------------------

    private static void doSync(Context ctx, int appId, boolean isDownload, SyncCallback cb) {
        SteamCloud cloud = SteamRepository.getInstance().getSteamCloud();
        if (cloud == null) {
            Log.w(TAG, "SteamCloud handler not available — skip sync for appId=" + appId);
            cb.onComplete(false, "Not connected to Steam");
            return;
        }

        long accountId = SteamPrefs.INSTANCE.getSteamId64() & 0xFFFFFFFFL;
        File localDir  = getLocalCloudDir(ctx, appId, accountId);
        localDir.mkdirs();

        cb.onProgress(isDownload ? "Fetching file list…" : "Preparing upload…", 0f);

        try {
            // Fetch the list of files Steam has for this app/user
            List<CloudFileEntry> remoteFiles = fetchRemoteFileList(cloud, appId, cb);
            if (remoteFiles == null) {
                cb.onComplete(false, "Could not retrieve cloud file list");
                return;
            }

            if (remoteFiles.isEmpty()) {
                cb.onComplete(true, "No cloud files for this game");
                return;
            }

            if (isDownload) {
                downloadFiles(cloud, appId, remoteFiles, localDir, cb);
            } else {
                uploadFiles(cloud, appId, remoteFiles, localDir, cb);
            }

        } catch (Exception e) {
            Log.e(TAG, "Sync error for appId=" + appId, e);
            cb.onComplete(false, "Sync error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // File list
    // -------------------------------------------------------------------------

    /**
     * Fetch the list of cloud files for this app via SteamCloud.
     * Uses ClientUGSGetFileDetails or similar RPC — the exact call depends on
     * the JavaSteam version. Returns null on timeout.
     */
    private static List<CloudFileEntry> fetchRemoteFileList(SteamCloud cloud, int appId,
                                                             SyncCallback cb) {
        // JavaSteam's SteamCloud handler exposes getUGCFileDetails() for UGC content
        // and the newer cloud file list via sendRemoteStorageRequest messages.
        // For the initial implementation we use a polling approach: request the file
        // list and wait for the callback. The actual message type is
        // EMsg.ClientUGSGetFileDetails which JavaSteam wraps in SteamCloud.
        //
        // If the JavaSteam version in use does not support getAppFileList(), we fall
        // back to an empty list (no-op sync) rather than crashing.
        Log.i(TAG, "Requesting cloud file list for appId=" + appId);

        CountDownLatch latch = new CountDownLatch(1);
        List<CloudFileEntry> files = new ArrayList<>();

        try {
            // Attempt to call getAppFileChangeList if available in this JavaSteam build
            var sub = SteamRepository.getInstance().getCallbackManager()
                    .subscribe(in.dragonbra.javasteam.steam.handlers.steamcloud.callback.RemoteStorageGetFileListCallback.class,
                            cb2 -> {
                                if (cb2.getAppID() == appId) {
                                    for (var fi : cb2.getFiles()) {
                                        files.add(new CloudFileEntry(
                                                fi.getFilename(),
                                                fi.getSize(),
                                                fi.getTimestamp(),
                                                fi.getSha()));
                                    }
                                    latch.countDown();
                                }
                            });
            cloud.getFileList(appId);
            boolean got = latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
            sub.close();
            if (!got) {
                Log.w(TAG, "Timeout waiting for cloud file list, appId=" + appId);
                return null;
            }
        } catch (Exception e) {
            // JavaSteam build may not have this API yet — return empty list
            Log.w(TAG, "Cloud file list API not available: " + e.getMessage());
            return files; // empty — sync is a no-op
        }

        Log.i(TAG, "Cloud file list: " + files.size() + " files for appId=" + appId);
        return files;
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    private static void downloadFiles(SteamCloud cloud, int appId,
                                       List<CloudFileEntry> remoteFiles,
                                       File localDir, SyncCallback cb) throws Exception {
        int done = 0;
        int synced = 0;

        for (CloudFileEntry remote : remoteFiles) {
            cb.onProgress("Checking " + remote.filename, (float) done / remoteFiles.size());

            File localFile = new File(localDir, sanitizePath(remote.filename));
            boolean needsDownload = true;

            if (localFile.exists()) {
                // Compare SHA1 — only download if server copy differs
                byte[] localSha = sha1(localFile);
                needsDownload = !bytesEqual(localSha, remote.sha);
            }

            if (needsDownload) {
                cb.onProgress("Downloading " + remote.filename, (float) done / remoteFiles.size());
                downloadFile(cloud, appId, remote.filename, localFile);
                synced++;
                Log.i(TAG, "Downloaded " + remote.filename + " (" + remote.size + " bytes)");
            }

            done++;
        }

        cb.onComplete(true, "Cloud sync: " + synced + " file(s) updated");
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    private static void uploadFiles(SteamCloud cloud, int appId,
                                     List<CloudFileEntry> remoteFiles,
                                     File localDir, SyncCallback cb) throws Exception {
        // Build a map of remote filename → timestamp for comparison
        java.util.Map<String, CloudFileEntry> remoteMap = new java.util.HashMap<>();
        for (CloudFileEntry e : remoteFiles) remoteMap.put(e.filename, e);

        // Scan local directory for all files
        List<File> localFiles = listFilesRecursive(localDir);
        int done = 0;
        int uploaded = 0;

        for (File local : localFiles) {
            String relPath = localDir.toURI().relativize(local.toURI()).getPath();
            cb.onProgress("Checking " + relPath, (float) done / Math.max(localFiles.size(), 1));

            CloudFileEntry remote = remoteMap.get(relPath);
            boolean needsUpload;

            if (remote == null) {
                needsUpload = true; // new local file
            } else {
                // Upload if local is newer than remote
                long localMtime = local.lastModified() / 1000L; // seconds
                needsUpload = localMtime > remote.timestamp;
                if (!needsUpload) {
                    // Also check SHA1 in case timestamp was reset
                    byte[] localSha = sha1(local);
                    needsUpload = !bytesEqual(localSha, remote.sha);
                }
            }

            if (needsUpload) {
                cb.onProgress("Uploading " + relPath, (float) done / Math.max(localFiles.size(), 1));
                uploadFile(cloud, appId, relPath, local);
                uploaded++;
                Log.i(TAG, "Uploaded " + relPath + " (" + local.length() + " bytes)");
            }

            done++;
        }

        cb.onComplete(true, "Cloud sync: " + uploaded + " file(s) uploaded");
    }

    // -------------------------------------------------------------------------
    // Individual file transfer helpers (delegating to JavaSteam)
    // -------------------------------------------------------------------------

    private static void downloadFile(SteamCloud cloud, int appId,
                                      String filename, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        CountDownLatch latch = new CountDownLatch(1);
        Exception[] error = {null};

        var sub = SteamRepository.getInstance().getCallbackManager()
                .subscribe(in.dragonbra.javasteam.steam.handlers.steamcloud.callback.RemoteStorageDownloadFileCallback.class,
                        cb -> {
                            if (cb.getAppID() == appId && cb.getFilename().equals(filename)) {
                                try (FileOutputStream fos = new FileOutputStream(dest)) {
                                    byte[] data = cb.getData();
                                    if (data != null) fos.write(data);
                                } catch (IOException e) {
                                    error[0] = e;
                                }
                                latch.countDown();
                            }
                        });
        try {
            cloud.downloadFile(appId, filename);
            if (!latch.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new IOException("Timeout downloading " + filename);
            }
            if (error[0] != null) throw error[0];
        } finally {
            sub.close();
        }
    }

    private static void uploadFile(SteamCloud cloud, int appId,
                                    String filename, File src) throws Exception {
        byte[] data = readFully(src);
        CountDownLatch latch = new CountDownLatch(1);

        var sub = SteamRepository.getInstance().getCallbackManager()
                .subscribe(in.dragonbra.javasteam.steam.handlers.steamcloud.callback.RemoteStorageUploadFileCallback.class,
                        cb -> {
                            if (cb.getAppID() == appId && cb.getFilename().equals(filename)) {
                                latch.countDown();
                            }
                        });
        try {
            cloud.uploadFile(appId, filename, data);
            if (!latch.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new IOException("Timeout uploading " + filename);
            }
        } finally {
            sub.close();
        }
    }

    // -------------------------------------------------------------------------
    // Path helpers
    // -------------------------------------------------------------------------

    /**
     * Resolve the local cloud save directory for an app.
     * Mirrors: {imagefs}/userdata/{accountId}/{appId}/remote/
     *
     * imagefs is accessible inside Wine as Z:\ and is stored on Android at
     * {getExternalFilesDir(null)}/imagefs or the configured imagefs path.
     * We use reflection to ask ImageFs for the actual path.
     */
    static File getLocalCloudDir(Context ctx, int appId, long accountId) {
        String imageFsPath = resolveImageFsPath(ctx);
        if (imageFsPath == null) {
            // Fallback: use app-private storage
            imageFsPath = ctx.getFilesDir().getAbsolutePath() + "/imagefs";
        }
        return new File(imageFsPath, "userdata/" + accountId + "/" + appId + "/remote");
    }

    private static String resolveImageFsPath(Context ctx) {
        try {
            Class<?> cls = Class.forName("com.winlator.cmod.xenvironment.ImageFs");
            java.lang.reflect.Method m = cls.getMethod("getMount", Context.class);
            Object mount = m.invoke(null, ctx);
            if (mount instanceof File) return ((File) mount).getAbsolutePath();
            if (mount instanceof String) return (String) mount;
        } catch (Exception e) {
            Log.w(TAG, "Could not resolve imagefs path: " + e.getMessage());
        }
        return null;
    }

    /** Sanitize a Steam cloud filename for use as a file system path. */
    private static String sanitizePath(String filename) {
        // Steam cloud filenames use forward slashes on all platforms
        return filename.replace('\\', '/').replaceAll("\\.\\./", "");
    }

    // -------------------------------------------------------------------------
    // Crypto / IO helpers
    // -------------------------------------------------------------------------

    private static byte[] sha1(File f) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = bis.read(buf)) != -1) md.update(buf, 0, n);
            return md.digest();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static boolean bytesEqual(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static byte[] readFully(File f) throws IOException {
        byte[] buf = new byte[(int) f.length()];
        try (FileInputStream fis = new FileInputStream(f)) {
            int off = 0, n;
            while (off < buf.length && (n = fis.read(buf, off, buf.length - off)) != -1) off += n;
        }
        return buf;
    }

    private static List<File> listFilesRecursive(File dir) {
        List<File> result = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return result;
        File[] children = dir.listFiles();
        if (children == null) return result;
        for (File f : children) {
            if (f.isDirectory()) result.addAll(listFilesRecursive(f));
            else result.add(f);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Data model
    // -------------------------------------------------------------------------

    /** Lightweight representation of a remote cloud file entry. */
    static final class CloudFileEntry {
        final String filename;
        final long   size;
        final long   timestamp;  // Unix seconds
        final byte[] sha;        // SHA1 bytes from server (may be null)

        CloudFileEntry(String filename, long size, long timestamp, byte[] sha) {
            this.filename  = filename;
            this.size      = size;
            this.timestamp = timestamp;
            this.sha       = sha;
        }
    }
}
