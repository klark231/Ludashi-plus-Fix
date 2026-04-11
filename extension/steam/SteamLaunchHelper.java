package com.winlator.cmod.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Steam-aware launcher for Ludashi-plus.
 *
 * Extends LudashiLaunchBridge with:
 *  - steam_appid.txt written to the game directory (makes the game recognise
 *    itself as a Steam game and loads steamapi.dll / steamapi64.dll correctly)
 *  - SteamAppId + SteamGameId env vars in the .desktop shortcut
 *  - STEAM_COMPAT_DATA_PATH pointing to the userdata directory
 *  - steamclient_loader_x64.exe support when the Steam runtime component is
 *    installed inside the Wine container
 *
 * Shortcut Exec line format:
 *   env SteamAppId=12345 SteamGameId=12345 \
 *       STEAM_COMPAT_DATA_PATH=Z:\userdata wine Z:\steamapps\common\Game\game.exe
 *
 * Usage:
 *   SteamLaunchHelper.launch(activity, appId, gameName, exePath, installDir)
 */
public final class SteamLaunchHelper {

    private static final String STEAM_LOADER_WIN_PATH =
            "C:\\Program Files (x86)\\Steam\\steamclient_loader_x64.exe";

    private SteamLaunchHelper() {}

    /**
     * Show a container picker, then write a Steam-aware .desktop shortcut.
     *
     * @param activity    current Activity
     * @param appId       Steam application ID
     * @param gameName    display name
     * @param exePath     absolute Android path to the .exe inside imagefs
     * @param installDir  game install directory (Android path) for steam_appid.txt
     */
    public static void launch(Activity activity, int appId, String gameName,
                               String exePath, String installDir) {
        new Thread(() -> {
            Handler h = new Handler(Looper.getMainLooper());
            try {
                Class<?> cmClass = Class.forName("com.winlator.cmod.container.ContainerManager");
                Object manager = cmClass.getConstructor(Context.class).newInstance(activity);
                Method getContainers = cmClass.getMethod("getContainers");
                List<?> containers = (List<?>) getContainers.invoke(manager);

                if (containers == null || containers.isEmpty()) {
                    h.post(() -> Toast.makeText(activity,
                            "No Wine container found. Create one first.",
                            Toast.LENGTH_LONG).show());
                    return;
                }

                String[] names = new String[containers.size()];
                for (int i = 0; i < containers.size(); i++) {
                    Object c = containers.get(i);
                    try {
                        names[i] = (String) c.getClass().getMethod("getName").invoke(c);
                    } catch (Exception ignored) {}
                    if (names[i] == null || names[i].isEmpty()) names[i] = "Container " + i;
                }

                h.post(() -> new AlertDialog.Builder(activity)
                        .setTitle("Select container for \"" + gameName + "\"")
                        .setItems(names, (dialog, which) ->
                                writeShortcut(activity, containers.get(which),
                                        appId, gameName, exePath, installDir, h))
                        .setNegativeButton("Cancel", null)
                        .show());

            } catch (Exception e) {
                h.post(() -> Toast.makeText(activity,
                        "Error loading containers: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static void writeShortcut(Activity activity, Object container,
                                       int appId, String gameName, String exePath,
                                       String installDir, Handler h) {
        new Thread(() -> {
            try {
                Method getDesktopDir = container.getClass().getMethod("getDesktopDir");
                File desktopDir = (File) getDesktopDir.invoke(container);

                if (desktopDir == null) {
                    h.post(() -> Toast.makeText(activity,
                            "Container desktop directory not found.",
                            Toast.LENGTH_LONG).show());
                    return;
                }

                desktopDir.mkdirs();

                // Write steam_appid.txt to the game's install directory.
                // Steamapi.dll reads this file to identify which game is running.
                if (installDir != null && !installDir.isEmpty()) {
                    writeSteamAppId(installDir, appId);
                }

                // Convert Android install path → Wine Z: path for STEAM_COMPAT_DATA_PATH.
                // imagefs root is accessible as Z:\ inside Wine. We point compat data at
                // the per-user directory: Z:\userdata\{accountId}\{appId}\
                String userdataWinPath = buildUserdataWinPath(activity, appId);

                // Resolve the game exe as a Wine Z: path
                String exeWinPath = GogInstallPath.toWinePath(activity, exePath);
                String escapedExeWinPath = exeWinPath.replace("\\", "\\\\\\\\");

                // Env vars written into the Exec line so Wine sets them before launching.
                // SteamAppId / SteamGameId — steamapi.dll reads these to initialise Steam.
                // STEAM_COMPAT_DATA_PATH — used by Proton-based tools, harmless otherwise.
                String execLine = buildExecLine(appId, exeWinPath, userdataWinPath,
                        desktopDir, container);

                String safeName = gameName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                if (safeName.isEmpty()) safeName = "steam_" + appId;

                File shortcutFile = new File(desktopDir, safeName + ".desktop");
                try (FileWriter fw = new FileWriter(shortcutFile)) {
                    fw.write("[Desktop Entry]\n");
                    fw.write("Name=" + gameName + "\n");
                    fw.write("Exec=" + execLine + "\n");
                    fw.write("Icon=\n");
                    fw.write("Type=Application\n");
                    fw.write("StartupWMClass=explorer\n");
                    fw.write("\n");
                    fw.write("[Extra Data]\n");
                    fw.write("SteamAppId=" + appId + "\n");
                }

                h.post(() -> Toast.makeText(activity,
                        "\"" + gameName + "\" added to Shortcuts.",
                        Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                h.post(() -> Toast.makeText(activity,
                        "Failed to add shortcut: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Build the Exec line. If the Steam runtime loader is present inside the
     * container, use it (enables DRM and Steam overlay). Otherwise fall back to
     * a direct wine invocation with env vars so steamapi.dll can still init.
     */
    private static String buildExecLine(int appId, String exeWinPath,
                                         String userdataWinPath,
                                         File desktopDir, Object container) {
        // Check whether steamclient_loader_x64.exe is installed in this container.
        boolean hasLoader = false;
        try {
            // Container root is the parent of the desktop directory.
            File containerRoot = desktopDir.getParentFile();
            if (containerRoot != null) {
                // Map the Win path to an Android path: C:\ → drive_c/ inside container root.
                // Loader lives at: {containerRoot}/drive_c/Program Files (x86)/Steam/
                File loaderFile = new File(containerRoot,
                        "drive_c/Program Files (x86)/Steam/steamclient_loader_x64.exe");
                hasLoader = loaderFile.exists();
            }
        } catch (Exception ignored) {}

        // Env vars prefix injected via wine's env-setting mechanism.
        // Shortcut.java unescapes \\\\ → \\, so we need to double-escape backslashes.
        String escapedExe = exeWinPath.replace("\\", "\\\\\\\\");
        String escapedUserdata = userdataWinPath.replace("\\", "\\\\\\\\");

        if (hasLoader) {
            String loaderWin = STEAM_LOADER_WIN_PATH.replace("\\", "\\\\\\\\");
            // Use loader — it sets up the Steam environment and then launches the game.
            // Env vars tell the loader which game to launch.
            return "env SteamAppId=" + appId
                    + " SteamGameId=" + appId
                    + " STEAM_COMPAT_DATA_PATH=" + escapedUserdata
                    + " wine " + loaderWin;
        } else {
            // Direct wine launch with Steam env vars.
            return "env SteamAppId=" + appId
                    + " SteamGameId=" + appId
                    + " STEAM_COMPAT_DATA_PATH=" + escapedUserdata
                    + " wine " + escapedExe;
        }
    }

    /** Write steam_appid.txt into the game install directory. */
    private static void writeSteamAppId(String installDir, int appId) {
        try {
            File dir = new File(installDir);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "steam_appid.txt");
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(String.valueOf(appId));
            }
        } catch (Exception e) {
            android.util.Log.w("SteamLaunchHelper", "Could not write steam_appid.txt: " + e);
        }
    }

    /**
     * Build the Wine-side path for STEAM_COMPAT_DATA_PATH.
     * Points to: Z:\userdata\{accountId}\{appId}
     * If no accountId is stored, falls back to Z:\userdata\0\{appId}.
     */
    private static String buildUserdataWinPath(Context ctx, int appId) {
        long steamId64 = SteamPrefs.INSTANCE.getSteamId64();
        // Lower 32 bits of SteamID64 minus the Steam universe offset = accountId
        long accountId = steamId64 == 0 ? 0 : (steamId64 & 0xFFFFFFFFL);
        return "Z:\\userdata\\" + accountId + "\\" + appId;
    }
}
