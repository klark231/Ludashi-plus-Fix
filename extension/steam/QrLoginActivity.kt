package com.winlator.cmod.store

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QR code login screen.
 *
 * Displays a QR code that the user scans with the Steam mobile app on their phone.
 * The QR URL is refreshed every ~30 s by Steam; we poll for changes and redraw.
 *
 * Flow:
 *   onCreate → SteamQrAuthManager.startQrLogin()
 *     → onQrReady(url)    → display QR bitmap
 *     → onQrRefreshed(url) → redraw QR bitmap
 *     → onSuccess()       → navigate to SteamGamesActivity
 *     → onFailure()       → show error + retry button
 */
class QrLoginActivity : Activity(), SteamQrAuthManager.QrAuthListener {

    private lateinit var qrImage: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancel: Button
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        startQrAuth()
    }

    override fun onDestroy() {
        SteamQrAuthManager.getInstance().cancel()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // QR auth
    // -------------------------------------------------------------------------

    private fun startQrAuth() {
        setStatus("Connecting to Steam…", loading = true)
        qrImage.setImageBitmap(null)
        btnRetry.visibility = View.GONE
        SteamQrAuthManager.getInstance().startQrLogin(this)
    }

    // -------------------------------------------------------------------------
    // SteamQrAuthManager.QrAuthListener (all called on main thread)
    // -------------------------------------------------------------------------

    override fun onQrReady(challengeUrl: String) {
        setStatus("Scan with the Steam app on your phone", loading = false)
        showQr(challengeUrl)
    }

    override fun onQrRefreshed(newChallengeUrl: String) {
        showQr(newChallengeUrl)
    }

    override fun onSuccess(username: String, refreshToken: String) {
        SteamRepository.getInstance().loginWithToken(username, refreshToken)
        setStatus("Signed in as $username", loading = false)
        startActivity(Intent(this, SteamGamesActivity::class.java))
        finish()
    }

    override fun onFailure(reason: String) {
        setStatus("Failed: $reason", loading = false, isError = true)
        qrImage.setImageBitmap(null)
        btnRetry.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // QR bitmap generation (ZXing)
    // -------------------------------------------------------------------------

    private fun showQr(url: String) {
        try {
            val size = dp(260)
            val writer = QRCodeWriter()
            val matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            qrImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            // ZXing encode failure — unlikely; show URL as text fallback
            tvStatus.text = "QR error — open in browser:\n$url"
        }
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private fun buildUi(): View {
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(BG_DARK)
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        scroll.addView(ll)

        // Header
        ll.addView(TextView(this).apply {
            text = "Sign in via QR Code"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, wrapLp().also { it.bottomMargin = dp(8) })

        ll.addView(TextView(this).apply {
            text = "Open the Steam app → ☰ → Sign in via QR code"
            textSize = 13f
            setTextColor(GRAY_TEXT)
            gravity = Gravity.CENTER
        }, wrapLp().also { it.bottomMargin = dp(24) })

        // QR image (white background card)
        val card = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        qrImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        card.addView(qrImage, LinearLayout.LayoutParams(dp(260), dp(260)))
        ll.addView(card, wrapLp().also {
            it.gravity = Gravity.CENTER_HORIZONTAL
            it.bottomMargin = dp(16)
        })

        // Spinner
        progressBar = ProgressBar(this).apply { visibility = View.VISIBLE }
        ll.addView(progressBar, wrapLp().also { it.bottomMargin = dp(8) })

        // Status
        tvStatus = TextView(this).apply {
            text = "Connecting…"
            textSize = 13f
            setTextColor(GRAY_TEXT)
            gravity = Gravity.CENTER
        }
        ll.addView(tvStatus, wrapLp().also { it.bottomMargin = dp(20) })

        // Retry button (hidden until failure)
        btnRetry = Button(this).apply {
            text = "Retry"
            setTextColor(Color.WHITE)
            setBackgroundColor(STEAM_BLUE)
            visibility = View.GONE
            setOnClickListener { startQrAuth() }
        }
        ll.addView(btnRetry, fullLp(dp(48)).also { it.bottomMargin = dp(8) })

        // Cancel / back
        btnCancel = Button(this).apply {
            text = "← Back"
            setTextColor(GRAY_TEXT)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        ll.addView(btnCancel, fullLp(dp(44)))

        return scroll
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun setStatus(msg: String, loading: Boolean, isError: Boolean = false) {
        tvStatus.text = msg
        tvStatus.setTextColor(if (isError) Color.RED else GRAY_TEXT)
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun wrapLp() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun fullLp(h: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h)

    companion object {
        private val BG_DARK    = 0xFF1B1B1B.toInt()
        private val STEAM_BLUE = 0xFF1A3A5C.toInt()
        private val GRAY_TEXT  = 0xFFAAAAAA.toInt()
    }
}
