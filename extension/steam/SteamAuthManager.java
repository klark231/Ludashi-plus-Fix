package com.winlator.cmod.store;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

import in.dragonbra.javasteam.enums.EAuthTokenPlatformType;
import in.dragonbra.javasteam.enums.EOSType;
import in.dragonbra.javasteam.steam.authentication.AuthPollResult;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.authentication.CredentialsAuthSession;
import in.dragonbra.javasteam.steam.authentication.IAuthenticator;
import in.dragonbra.javasteam.steam.authentication.SteamAuthentication;

/**
 * Handles the Steam credential authentication flow.
 *
 * Written in Java (not Kotlin) to avoid Kotlin 2.2.0 metadata incompatibility
 * with the base APK's kotlinc 1.9.x.
 *
 * Flow:
 *   startCredentialLogin(username, password, listener)
 *     → beginAuthSessionViaCredentials()
 *     → pollingWaitForResult() [blocks auth thread]
 *       → IAuthenticator.getEmailCode() / getTotpCode() if Steam Guard needed
 *         → posts event to UI; blocks on codeQueue.take()
 *         → UI calls submitGuardCode(code) → codeQueue.offer(code) → unblocks
 *     → onSuccess(username, refreshToken) posted to main thread
 *
 * Cancellation: cancelAuth() offers a sentinel to unblock the queue.
 */
public final class SteamAuthManager {

    private static final String TAG = "SteamAuth";
    private static final String CANCEL_SENTINEL = "\u0000__cancel__\u0000";

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static final SteamAuthManager INSTANCE = new SteamAuthManager();
    public static SteamAuthManager getInstance() { return INSTANCE; }
    private SteamAuthManager() {}

    // -------------------------------------------------------------------------
    // Callback interface (delivered on main thread)
    // -------------------------------------------------------------------------

    public interface AuthListener {
        void onSteamGuardEmailRequired(String emailDomain, boolean codeWrong);
        void onSteamGuardTotpRequired(boolean codeWrong);
        void onDeviceConfirmationRequired();
        void onSuccess(String username, String refreshToken);
        void onFailure(String reason);
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** Capacity-1 queue: auth thread blocks on take(); UI offers the code. */
    private final LinkedBlockingQueue<String> codeQueue = new LinkedBlockingQueue<>(1);
    private volatile boolean cancelled = false;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Begin credential login on a dedicated background thread.
     * The SteamClient must already be connected (SteamForegroundService does this).
     */
    public void startCredentialLogin(String username, String password, AuthListener listener) {
        cancelled = false;
        codeQueue.clear();

        new Thread(() -> {
            try {
                SteamAuthentication auth =
                        new SteamAuthentication(SteamRepository.getInstance().getSteamClient());

                AuthSessionDetails details = new AuthSessionDetails();
                details.username         = username;
                details.password         = password;
                details.clientOsType     = EOSType.AndroidUnknown;
                details.deviceFriendlyName = "Android Device";
                details.platformType     = EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp;
                details.persistentSession = true;
                details.authenticator    = buildAuthenticator(listener);

                CredentialsAuthSession session =
                        auth.beginAuthSessionViaCredentials(details);
                AuthPollResult result = session.pollingWaitForResult();

                if (cancelled) return;

                String refreshToken = result.getRefreshToken();
                String accountName  = result.getAccountName();
                String finalName    = (accountName != null && !accountName.isEmpty())
                                      ? accountName : username;

                SteamRepository.getInstance().saveSession(finalName, refreshToken);
                mainHandler.post(() -> listener.onSuccess(finalName, refreshToken));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Auth interrupted");
            } catch (Exception e) {
                if (cancelled) return;
                Log.e(TAG, "Login error", e);
                String msg = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
                mainHandler.post(() -> listener.onFailure(msg));
            }
        }, "SteamCredentialLogin").start();
    }

    /**
     * Deliver a Steam Guard code (email or TOTP) to the waiting auth thread.
     * Call from the UI after the user types the code.
     */
    public void submitGuardCode(String code) {
        codeQueue.offer(code);
    }

    /** Cancel any pending auth and unblock the auth thread. */
    public void cancelAuth() {
        cancelled = true;
        codeQueue.offer(CANCEL_SENTINEL);
    }

    // -------------------------------------------------------------------------
    // IAuthenticator implementation
    // -------------------------------------------------------------------------

    private IAuthenticator buildAuthenticator(AuthListener listener) {
        return new IAuthenticator() {

            /** Called when Steam wants device-confirmation (approve in mobile app). */
            @Override
            public void acceptDeviceConfirmation() {
                mainHandler.post(listener::onDeviceConfirmationRequired);
            }

            /**
             * Called when Steam Guard email code is required.
             * Blocks the auth thread until submitGuardCode() is called from the UI.
             */
            @Override
            public String getEmailCode(String email, boolean previousCodeWrong) throws Exception {
                mainHandler.post(() -> listener.onSteamGuardEmailRequired(email, previousCodeWrong));
                String code = codeQueue.take();
                if (CANCEL_SENTINEL.equals(code)) throw new InterruptedException("Cancelled");
                return code;
            }

            /**
             * Called when Steam Guard TOTP code is required (mobile authenticator).
             * Blocks the auth thread until submitGuardCode() is called from the UI.
             */
            @Override
            public String getTotpCode(boolean previousCodeWrong) throws Exception {
                mainHandler.post(() -> listener.onSteamGuardTotpRequired(previousCodeWrong));
                String code = codeQueue.take();
                if (CANCEL_SENTINEL.equals(code)) throw new InterruptedException("Cancelled");
                return code;
            }
        };
    }
}
