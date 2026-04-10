package com.winlator.cmod.store;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import in.dragonbra.javasteam.protobufs.steamclient.ContentManifest;

/**
 * Steam depot download engine — Phase 6.
 *
 * Flow per app:
 *   1. Pick CDN server from Steam API
 *   2. For each depot: request decryption key → download manifest → enumerate files
 *   3. For each chunk: GET from CDN → decrypt (AES-ECB) → decompress (VZip/LZMA) → write
 *
 * Written in pure Java: uses HttpURLConnection + reflection for LZMAInputStream
 * (org.tukaani.xz is built into the base APK and available at runtime).
 */
public final class SteamDepotDownloader {

    private static final String TAG = "SteamDepot";

    // Steam CDN API — returns a JSON list of CDN servers for this client
    private static final String CDN_LIST_URL =
            "https://api.steampowered.com/IContentServerDirectoryService/GetServersForSteamPipe/v1/?cell_id=0&max_servers=5&format=json";

    // VZip magic header bytes
    private static final byte VZIP_MAGIC_0 = 'V';
    private static final byte VZIP_MAGIC_1 = 'Z';

    private static final SteamDepotDownloader INSTANCE = new SteamDepotDownloader();
    public static SteamDepotDownloader getInstance() { return INSTANCE; }
    private SteamDepotDownloader() {}

    private ExecutorService executor = Executors.newFixedThreadPool(3);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Begin installing an app: requests depot keys, then starts downloading.
     * Progress is emitted via SteamRepository events:
     *   DownloadProgress:<appId>:<bytesDownloaded>:<bytesTotal>
     *   DownloadComplete:<appId>
     *   DownloadFailed:<appId>:<reason>
     */
    public void installApp(int appId, Context ctx) {
        SteamDatabase db    = SteamRepository.getInstance().getDatabase();
        SteamDatabase.GameRow game = db.getGame(appId);
        if (game == null) {
            emitFailed(appId, "Game not found in database");
            return;
        }
        List<SteamDatabase.DepotManifestRow> depots = db.getDepotManifests(appId);
        if (depots.isEmpty()) {
            emitFailed(appId, "No depot manifests found — try re-syncing library");
            return;
        }

        File installBase = new File(ctx.getExternalFilesDir(null), "steam_games");
        String installName = sanitizeDirName(game.name.isEmpty() ? "app_" + appId : game.name);
        File installDir   = new File(installBase, installName);

        long totalSize = 0L;
        for (SteamDatabase.DepotManifestRow d : depots) totalSize += d.sizeBytes;
        db.queueDownload(appId, totalSize, installDir.getAbsolutePath());

        // Request depot keys first, then start download once keys arrive
        final int fAppId = appId;
        final List<SteamDatabase.DepotManifestRow> fDepots = depots;
        final File fInstallDir = installDir;
        final long fTotal = totalSize;

        executor.submit(() -> downloadAllDepots(fAppId, fDepots, fInstallDir, fTotal, ctx));
    }

    // -------------------------------------------------------------------------
    // Download orchestration
    // -------------------------------------------------------------------------

    private void downloadAllDepots(int appId, List<SteamDatabase.DepotManifestRow> depots,
                                   File installDir, long totalBytes, Context ctx) {
        try {
            installDir.mkdirs();
            String cdnHost = pickCdnServer();
            if (cdnHost == null) {
                emitFailed(appId, "Could not fetch CDN server list");
                return;
            }
            Log.i(TAG, "Using CDN: " + cdnHost);

            // Get CDN auth token for this host (required since ~2022 for chunk downloads)
            SteamRepository repo = SteamRepository.getInstance();
            String cdnToken = repo.getCdnAuthToken(cdnHost);
            if (cdnToken.isEmpty()) {
                repo.requestCdnAuthToken(appId, depots.get(0).depotId, cdnHost);
                for (int i = 0; i < 20 && cdnToken.isEmpty(); i++) {
                    Thread.sleep(500);
                    cdnToken = repo.getCdnAuthToken(cdnHost);
                }
                // Token may still be empty for some CDN configs — proceed anyway
                Log.i(TAG, "CDN auth token for " + cdnHost + ": " +
                      (cdnToken.isEmpty() ? "none (proceeding without)" : "acquired"));
            }

            SteamDatabase db = repo.getDatabase();
            long[] downloaded = {0L};
            final long fTotal = totalBytes > 0 ? totalBytes : 1L;

            for (SteamDatabase.DepotManifestRow depot : depots) {
                // Request depot key (non-blocking — we wait with a small poll)
                byte[] key = repo.getDepotKey(depot.depotId);
                if (key == null) {
                    repo.requestDepotKey(depot.depotId, appId);
                    for (int i = 0; i < 30 && key == null; i++) {
                        Thread.sleep(500);
                        key = repo.getDepotKey(depot.depotId);
                    }
                    // key may still be null (unencrypted depot) — that's fine
                }

                if (depot.manifestId == 0) {
                    Log.w(TAG, "Depot " + depot.depotId + " has no manifest ID — skipping");
                    continue;
                }

                // Request manifest code — required to authenticate CDN manifest URL
                long manifestCode = repo.getManifestCode(depot.depotId, depot.manifestId);
                if (manifestCode == 0L) {
                    repo.requestManifestCode(appId, depot.depotId, depot.manifestId);
                    for (int i = 0; i < 20 && manifestCode == 0L; i++) {
                        Thread.sleep(500);
                        manifestCode = repo.getManifestCode(depot.depotId, depot.manifestId);
                    }
                    Log.i(TAG, "Manifest code for depot " + depot.depotId + ": " + manifestCode);
                }

                Log.i(TAG, "Downloading manifest for depot " + depot.depotId +
                      " manifest " + depot.manifestId);
                byte[] manifestBytes = downloadManifest(cdnHost, depot.depotId,
                                                        depot.manifestId, manifestCode);
                if (manifestBytes == null) {
                    emitFailed(appId, "Failed to download manifest for depot " + depot.depotId);
                    return;
                }

                List<FileEntry> files = parseManifest(manifestBytes);
                Log.i(TAG, "Depot " + depot.depotId + ": " + files.size() + " files");

                for (FileEntry fe : files) {
                    File outFile = new File(installDir, fe.fileName.replace('\\', '/'));
                    outFile.getParentFile().mkdirs();

                    if (fe.chunks.isEmpty()) {
                        // Empty file
                        outFile.createNewFile();
                        continue;
                    }

                    try (RandomAccessFile raf = new RandomAccessFile(outFile, "rw")) {
                        raf.setLength(fe.uncompressedSize);
                        for (ChunkEntry chunk : fe.chunks) {
                            byte[] raw = downloadChunk(cdnHost, depot.depotId,
                                                       chunk.gidHex, cdnToken);
                            if (raw == null) {
                                emitFailed(appId, "Failed to download chunk " + chunk.gidHex);
                                return;
                            }
                            byte[] decrypted = decryptChunk(raw, key);
                            byte[] decompressed = decompressChunk(decrypted);
                            raf.seek(chunk.offset);
                            raf.write(decompressed, 0, Math.min(decompressed.length, (int)chunk.uncompressedLength));
                            downloaded[0] += chunk.compressedLength;
                            db.updateDownloadProgress(appId, downloaded[0]);
                            SteamRepository.getInstance().emit(
                                "DownloadProgress:" + appId + ":" + downloaded[0] + ":" + fTotal);
                        }
                    }
                }
            }

            // Mark installed
            db.markInstalled(appId, installDir.getAbsolutePath(), totalBytes);
            db.markDownloadComplete(appId);
            SteamRepository.getInstance().emit("DownloadComplete:" + appId);
            Log.i(TAG, "Install complete for app " + appId + " at " + installDir);

        } catch (Exception e) {
            Log.e(TAG, "Download failed for app " + appId, e);
            emitFailed(appId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    // -------------------------------------------------------------------------
    // CDN server selection
    // -------------------------------------------------------------------------

    private String pickCdnServer() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(CDN_LIST_URL).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "Valve/Steam HTTP Client 1.0");
            if (conn.getResponseCode() != 200) return fallbackCdn();
            byte[] body = readFully(conn.getInputStream());
            JSONObject root = new JSONObject(new String(body, "UTF-8"));
            JSONArray servers = root.getJSONObject("response").getJSONArray("servers");
            for (int i = 0; i < servers.length(); i++) {
                JSONObject s = servers.getJSONObject(i);
                String type   = s.optString("type", "");
                String https  = s.optString("https_support", "none");
                String host   = s.optString("host", "");
                if ((type.equals("CDN") || type.equals("SteamCache"))
                        && !https.equals("none") && !host.isEmpty()) {
                    return host;
                }
            }
            // Fallback: first server regardless of type
            if (servers.length() > 0) return servers.getJSONObject(0).optString("host");
        } catch (Exception e) {
            Log.w(TAG, "CDN list fetch failed: " + e.getMessage());
        }
        return fallbackCdn();
    }

    private static String fallbackCdn() {
        // Well-known public Steam CDN that serves depot content without auth
        return "lancache.steamcontent.com";
    }

    // -------------------------------------------------------------------------
    // Manifest download + parse
    // -------------------------------------------------------------------------

    private byte[] downloadManifest(String cdn, int depotId, long manifestId, long requestCode) {
        // Manifest URL format: https://{cdn}/depot/{depotId}/manifest/{manifestId}/5/{requestCode}
        // requestCode appended when non-zero (required since ~2022)
        String urlStr = "https://" + cdn + "/depot/" + depotId +
                        "/manifest/" + manifestId + "/5" +
                        (requestCode != 0L ? "/" + requestCode : "");
        try {
            HttpURLConnection conn = openGet(urlStr);
            if (conn.getResponseCode() != 200) {
                Log.w(TAG, "Manifest HTTP " + conn.getResponseCode() + " for " + urlStr);
                return null;
            }
            return readFully(conn.getInputStream());
        } catch (Exception e) {
            Log.e(TAG, "Manifest download failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a manifest response.
     * Steam manifests are returned as a VZip blob (LZMA-compressed protobuf)
     * or a plain zip in some cases.
     */
    private List<FileEntry> parseManifest(byte[] raw) throws Exception {
        byte[] protoBytes = decompressManifestBlob(raw);
        if (protoBytes == null || protoBytes.length == 0) return Collections.emptyList();

        ContentManifest.ContentManifestPayload payload =
                ContentManifest.ContentManifestPayload.parseFrom(protoBytes);

        List<FileEntry> files = new ArrayList<>();
        for (ContentManifest.ContentManifestPayload.FileMapping fm : payload.getMappingsList()) {
            // Skip directories (no chunks or flag bit 64 = directory)
            if (fm.getChunksCount() == 0 && fm.getSize() == 0) continue;

            FileEntry fe = new FileEntry();
            fe.fileName          = fm.getFilename();
            fe.uncompressedSize  = fm.getSize();
            for (ContentManifest.ContentManifestPayload.FileMapping.ChunkData cd : fm.getChunksList()) {
                ChunkEntry chunk = new ChunkEntry();
                // sha = chunk GID (20 bytes), hex-encode for URL
                chunk.gidHex           = bytesToHex(cd.getSha().toByteArray());
                chunk.offset           = cd.getOffset();
                chunk.compressedLength = cd.getCbCompressed();
                chunk.uncompressedLength = cd.getCbOriginal();
                fe.chunks.add(chunk);
            }
            files.add(fe);
        }
        return files;
    }

    private byte[] decompressManifestBlob(byte[] raw) throws Exception {
        if (raw.length < 2) return raw;
        // VZip format: starts with 'V','Z'
        if (raw[0] == VZIP_MAGIC_0 && raw[1] == VZIP_MAGIC_1) {
            return decompressVZip(raw);
        }
        // Try as zip (PKZIP)
        if (raw[0] == 0x50 && raw[1] == 0x4B) {
            return decompressZip(raw);
        }
        // Try as gzip
        if ((raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B) {
            return decompressGzip(raw);
        }
        // Assume raw protobuf
        return raw;
    }

    // -------------------------------------------------------------------------
    // Chunk download + decrypt + decompress
    // -------------------------------------------------------------------------

    private byte[] downloadChunk(String cdn, int depotId, String chunkGidHex, String cdnToken) {
        String urlStr = "https://" + cdn + "/depot/" + depotId + "/chunk/" + chunkGidHex;
        if (!cdnToken.isEmpty()) urlStr += "?token=" + cdnToken;
        try {
            HttpURLConnection conn = openGet(urlStr);
            if (conn.getResponseCode() != 200) {
                Log.w(TAG, "Chunk HTTP " + conn.getResponseCode() + " for " + chunkGidHex);
                return null;
            }
            return readFully(conn.getInputStream());
        } catch (Exception e) {
            Log.w(TAG, "Chunk download failed " + chunkGidHex + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypt a chunk with AES-256-ECB using the depot key.
     * If key is null (unencrypted depot), returns the input unchanged.
     */
    private byte[] decryptChunk(byte[] data, byte[] key) throws Exception {
        if (key == null || key.length == 0) return data;
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        // Data length must be multiple of 16; trim trailing bytes if needed
        int alignedLen = (data.length / 16) * 16;
        return cipher.doFinal(data, 0, alignedLen);
    }

    /**
     * Decompress a chunk.
     * VZip: 'V','Z',version,'.' → LZMA data → CRC32 → 'z','V'
     * Otherwise: try as raw deflate / gzip / uncompressed.
     */
    private byte[] decompressChunk(byte[] data) throws Exception {
        if (data.length < 2) return data;
        if (data[0] == VZIP_MAGIC_0 && data[1] == VZIP_MAGIC_1) {
            return decompressVZip(data);
        }
        if ((data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            return decompressGzip(data);
        }
        return data; // uncompressed
    }

    // -------------------------------------------------------------------------
    // Decompression helpers
    // -------------------------------------------------------------------------

    /**
     * VZip format:
     *   [0]  'V'
     *   [1]  'Z'
     *   [2]  version (0x61 = 'a')
     *   [3]  '.'
     *   [4+] LZMA-compressed data
     *   last 10 bytes: CRC32(4) + footer magic 'z','V' (6 byte footer varies)
     *
     * We skip the 4-byte header and strip the 10-byte footer, then LZMA-decompress.
     */
    private byte[] decompressVZip(byte[] data) throws Exception {
        // Header: 4 bytes (VZ + version + '.')
        // Footer: 10 bytes (CRC32 4 bytes + uncompressed size 4 bytes + 'zV' 2 bytes)
        int headerSize = 4;
        int footerSize = 10;
        if (data.length <= headerSize + footerSize) return data;
        byte[] lzmaData = Arrays.copyOfRange(data, headerSize, data.length - footerSize);
        return lzmaDecompress(lzmaData);
    }

    /** LZMA decompression via reflection (org.tukaani.xz is in the base APK). */
    private byte[] lzmaDecompress(byte[] data) throws Exception {
        // Detect XZ magic (0xFD 0x37) vs raw LZMA
        boolean isXz = data.length >= 2 && (data[0] & 0xFF) == 0xFD && (data[1] & 0xFF) == 0x37;
        String className = isXz ? "org.tukaani.xz.XZInputStream" : "org.tukaani.xz.LZMAInputStream";
        Class<?> cls = Class.forName(className);
        Constructor<?> ctor = cls.getConstructor(InputStream.class, int.class);
        InputStream is = (InputStream) ctor.newInstance(new ByteArrayInputStream(data), -1);
        return readFully(is);
    }

    private byte[] decompressGzip(byte[] data) throws Exception {
        try (java.util.zip.GZIPInputStream gzip =
                     new java.util.zip.GZIPInputStream(new ByteArrayInputStream(data))) {
            return readFully(gzip);
        }
    }

    private byte[] decompressZip(byte[] data) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            zis.getNextEntry();
            return readFully(zis);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private HttpURLConnection openGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("User-Agent", "Valve/Steam HTTP Client 1.0");
        return conn;
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }

    private static String sanitizeDirName(String name) {
        return name.replaceAll("[^a-zA-Z0-9 _.\\-]", "_").trim();
    }

    private void emitFailed(int appId, String reason) {
        SteamRepository.getInstance().getDatabase().markDownloadFailed(appId, reason);
        SteamRepository.getInstance().emit("DownloadFailed:" + appId + ":" + reason);
        Log.e(TAG, "DownloadFailed " + appId + ": " + reason);
    }

    // -------------------------------------------------------------------------
    // Data classes
    // -------------------------------------------------------------------------

    private static class FileEntry {
        String fileName = "";
        long   uncompressedSize = 0L;
        List<ChunkEntry> chunks = new ArrayList<>();
    }

    private static class ChunkEntry {
        String gidHex = "";
        long   offset = 0L;
        long   compressedLength   = 0L;
        long   uncompressedLength = 0L;
    }
}
