package com.g2link.connect.sharing

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApkShareManager — Share the G2-Link APK directly to nearby devices.
 *
 * Three sharing methods:
 * 1. Nearby Connections (Bluetooth/WiFi Direct) — no internet, no cost
 * 2. System share sheet (Bluetooth, WhatsApp, etc.)
 * 3. QR code with GitHub download link
 *
 * Why this matters:
 * - In disaster zones, new users can install G2-Link from a neighbor's phone
 * - No internet required
 * - Zero distribution cost — users become the CDN
 */
@Singleton
class ApkShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ApkShareManager"
        private const val APK_CACHE_NAME = "g2link-share.apk"
        // Your GitHub releases page — update to your actual repo
        const val GITHUB_DOWNLOAD_URL =
            "https://github.com/G2-links/g2link/releases/latest"
        const val DIRECT_DOWNLOAD_URL =
            "https://github.com/G2-links/g2link/releases/latest/download/app-release.apk"
    }

    // ═══════════════════════════════════════════════════════
    // SHARE VIA SYSTEM SHARE SHEET
    // Works via Bluetooth, WhatsApp, Telegram, email, etc.
    // ═══════════════════════════════════════════════════════

    /**
     * Copy APK from installed package and share via system share sheet.
     * The recipient can install directly without internet.
     */
    fun shareApkViaBluetoothOrAny(): Boolean {
        return try {
            val apkFile = extractApkToCache() ?: return false
            val apkUri = getApkUri(apkFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "G2-Link — Offline Emergency Communication")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Install G2-Link for offline mesh messaging — works without internet or signal.\n\n" +
                    "Download: $GITHUB_DOWNLOAD_URL"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share G2-Link via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to share APK: ${e.message}")
            false
        }
    }

    // ═══════════════════════════════════════════════════════
    // SHARE VIA LINK (when internet available)
    // ═══════════════════════════════════════════════════════

    /**
     * Share the GitHub download link via any app (SMS, WhatsApp, etc.)
     * Recipient downloads directly from GitHub — free CDN.
     */
    fun shareDownloadLink() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "G2-Link — Offline Emergency Communication App")
            putExtra(
                Intent.EXTRA_TEXT,
                """
🔗 G2-Link — Communicate without internet, SIM or signal

Works in disasters, blackouts & remote areas using mesh networking between phones.

📱 Download: $GITHUB_DOWNLOAD_URL

• No internet needed
• No account required
• Encrypted & private
• Free forever
                """.trimIndent()
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share G2-Link").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ═══════════════════════════════════════════════════════
    // QR CODE CONTENT FOR DOWNLOAD LINK
    // ═══════════════════════════════════════════════════════

    /**
     * Returns QR content string for displaying a download QR.
     * Anyone who scans this QR gets taken to the GitHub download page.
     */
    fun getDownloadQrContent(): String = GITHUB_DOWNLOAD_URL

    // ═══════════════════════════════════════════════════════
    // APK EXTRACTION HELPERS
    // ═══════════════════════════════════════════════════════

    /**
     * Extract the installed APK to a cache file for sharing.
     * The APK is already on the device — just copying it.
     */
    private fun extractApkToCache(): File? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, 0)
            }

            val sourceApk = File(appInfo.sourceDir)
            val cacheDir = File(context.cacheDir, "apk_share").apply { mkdirs() }
            val destApk = File(cacheDir, APK_CACHE_NAME)

            // Only re-copy if cache is stale
            if (!destApk.exists() || destApk.length() != sourceApk.length()) {
                FileInputStream(sourceApk).use { input ->
                    FileOutputStream(destApk).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
            }

            Log.d(TAG, "APK extracted to cache: ${destApk.length() / 1024}KB")
            destApk

        } catch (e: Exception) {
            Log.e(TAG, "APK extraction failed: ${e.message}")
            null
        }
    }

    private fun getApkUri(apkFile: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }
    }

    /**
     * Get the size of the APK in MB for display in UI.
     */
    fun getApkSizeMb(): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, 0)
            }
            val sizeBytes = File(appInfo.sourceDir).length()
            "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
        } catch (e: Exception) { "~5 MB" }
    }
}
