package com.winlator.cmod.store;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import in.dragonbra.javasteam.steam.handlers.steamapps.SteamApps;
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.EncryptedAppTicketCallback;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;

/**
 * Generates an encrypted Steam app ticket for a given appId.
 *
 * The ticket proves to steamapi.dll (inside Wine) that this user owns the app.
 * Pluvia writes it via SteamService.getEncryptedAppTicket() and passes the
 * base64 string to SteamGameLauncher.writeGameSteamSettings().
 *
 * Usage:
 *   SteamAppTicket.request(appId, callback)  — async, calls back on background thread
 *
 * The resulting byte[] should be:
 *   1. Written to {gameDir}/encrypted_app_ticket.bin  (consumed by steamclient_loader)
 *   2. Also available as base64 string for debug logging
 */
public final class SteamAppTicket {

    private static final String TAG         = "SteamTicket";
    private static final int    TIMEOUT_SEC = 30;

    public interface Callback {
        void onTicket(byte[] ticketBytes);   // null on failure
    }

    private SteamAppTicket() {}

    /**
     * Request an encrypted app ticket for appId. The result is delivered
     * via callback on a background thread.
     *
     * Requires SteamRepository to be logged in.
     */
    public static void request(int appId, Callback callback) {
        new Thread(() -> {
            try {
                byte[] ticket = requestBlocking(appId);
                callback.onTicket(ticket);
            } catch (Exception e) {
                Log.e(TAG, "Ticket request failed for appId=" + appId, e);
                callback.onTicket(null);
            }
        }, "SteamTicketRequest").start();
    }

    /**
     * Blocking variant — call from a background thread only.
     * Returns null on timeout or error.
     */
    public static byte[] requestBlocking(int appId) throws InterruptedException {
        SteamApps steamApps = SteamRepository.getInstance().getSteamApps();
        CallbackManager callbackManager = SteamRepository.getInstance().getCallbackManager();

        if (steamApps == null || callbackManager == null) {
            Log.w(TAG, "Not connected — cannot request ticket for appId=" + appId);
            return null;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> result = new AtomicReference<>(null);

        // Subscribe to the one-shot callback
        var sub = callbackManager.subscribe(EncryptedAppTicketCallback.class, cb -> {
            if (cb.getAppID() == appId) {
                if (cb.getResult().code() == 1 /* EResult.OK */) {
                    byte[] ticket = cb.getEncryptedAppTicket();
                    result.set(ticket);
                    Log.i(TAG, "Ticket received for appId=" + appId
                            + " size=" + (ticket != null ? ticket.length : 0));
                } else {
                    Log.w(TAG, "Ticket request failed for appId=" + appId
                            + " result=" + cb.getResult());
                }
                latch.countDown();
            }
        });

        try {
            steamApps.getEncryptedAppTicket(appId, new byte[0]);
            Log.d(TAG, "Ticket request sent for appId=" + appId);
            latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        } finally {
            sub.close();
        }

        return result.get();
    }

    /**
     * Write a ticket to disk at {gameDir}/encrypted_app_ticket.bin.
     * Called by SteamGameDetailActivity after requesting a ticket before launch.
     */
    public static void writeToDisk(byte[] ticket, File gameDir) {
        if (ticket == null || ticket.length == 0) return;
        try {
            gameDir.mkdirs();
            File out = new File(gameDir, "encrypted_app_ticket.bin");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(ticket);
            }
            Log.i(TAG, "Ticket written to " + out.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Could not write ticket to disk", e);
        }
    }

    /** Base64-encode a ticket for debug logging. */
    public static String toBase64(byte[] ticket) {
        if (ticket == null) return "";
        return Base64.getEncoder().encodeToString(ticket);
    }
}
