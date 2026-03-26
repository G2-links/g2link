package com.g2link.connect.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.g2link.connect.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

// ─── Data models ─────────────────────────────────────────
@Serializable
data class UpdateInfo(
    val latestVersion: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false,
    val publishedAt: String = ""
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * UpdateManager — Zero-cost update system.
 *
 * Primary:  GitHub Releases API (free, no server needed)
 * Fallback: Mesh-propagated update packet (works offline)
 *
 * Flow:
 * 1. On launch, hit GitHub API → compare versionCode
 * 2. If update exists → show banner in app
 * 3. Download links direct to GitHub Releases (free CDN)
 * 4. Also listens for update packets propagated via mesh
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_API =
            "https://api.github.com/repos/G2-links/g2link/releases/latest"
        private const val CURRENT_VERSION_CODE = BuildConfig.VERSION_CODE
        private const val CONNECT_TIMEOUT = 8000
        private const val READ_TIMEOUT = 8000
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // ═══════════════════════════════════════════════════════
    // CHECK FOR UPDATES VIA GITHUB API
    // ═══════════════════════════════════════════════════════

    /**
     * Check GitHub Releases for a newer version.
     * Completely free — GitHub API allows 60 unauthenticated calls/hour/IP.
     * Called once on app launch.
     */
    suspend fun checkForUpdates() {
        if (!BuildConfig.UPDATE_CHECK_ENABLED) return
        _updateState.value = UpdateState.Checking

        try {
            val releaseInfo = withContext(Dispatchers.IO) {
                fetchGithubRelease()
            } ?: run {
                _updateState.value = UpdateState.UpToDate
                return
            }

            val latestCode = parseVersionCode(releaseInfo.tagName)
            val downloadUrl = releaseInfo.assets
                .firstOrNull { it.name.endsWith(".apk") }
                ?.browserDownloadUrl
                ?: releaseInfo.htmlUrl // fallback to release page

            if (latestCode > CURRENT_VERSION_CODE) {
                val updateInfo = UpdateInfo(
                    latestVersion = releaseInfo.tagName.trimStart('v'),
                    versionCode = latestCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseInfo.body
                        ?.take(300)
                        ?: "Bug fixes and improvements",
                    publishedAt = releaseInfo.publishedAt ?: ""
                )
                _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                Log.d(TAG, "Update available: ${updateInfo.latestVersion}")
            } else {
                _updateState.value = UpdateState.UpToDate
                Log.d(TAG, "App is up to date (v${BuildConfig.VERSION_NAME})")
            }

        } catch (e: Exception) {
            // Silently fail — update check is non-critical
            _updateState.value = UpdateState.Idle
            Log.w(TAG, "Update check failed (non-critical): ${e.message}")
        }
    }

    /**
     * Receive an update notice from the mesh network.
     * When a new version is injected into the mesh, it propagates
     * device-to-device and eventually reaches all users — even offline.
     */
    fun receiveMeshUpdateNotice(packet: MeshUpdatePacket) {
        if (packet.versionCode > CURRENT_VERSION_CODE) {
            val updateInfo = UpdateInfo(
                latestVersion = packet.version,
                versionCode = packet.versionCode,
                downloadUrl = packet.downloadUrl,
                releaseNotes = packet.releaseNotes,
                isForceUpdate = packet.forceUpdate
            )
            _updateState.value = UpdateState.UpdateAvailable(updateInfo)
            Log.d(TAG, "Mesh update notice received: v${packet.version}")
        }
    }

    /**
     * Open the APK download URL in browser.
     * Points to GitHub Releases — free CDN globally.
     */
    fun openDownloadPage(downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    // ═══════════════════════════════════════════════════════
    // GITHUB API FETCH
    // ═══════════════════════════════════════════════════════

    private fun fetchGithubRelease(): GithubRelease? {
        val url = URL(GITHUB_API)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "G2Link-Android/${BuildConfig.VERSION_NAME}")
            }
            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().readText()
                json.decodeFromString<GithubRelease>(body)
            } else null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVersionCode(tagName: String): Int {
        // Supports tags like "v1.2.3" → 10203 or "v2.0.0" → 20000
        return try {
            val clean = tagName.trimStart('v')
            val parts = clean.split(".").map { it.toInt() }
            (parts.getOrElse(0) { 0 } * 10000) +
            (parts.getOrElse(1) { 0 } * 100) +
             parts.getOrElse(2) { 0 }
        } catch (e: Exception) { 0 }
    }
}

// ─── GitHub API response models ───────────────────────────
@Serializable
private data class GithubRelease(
    val tag_name: String = "",
    val html_url: String = "",
    val body: String? = null,
    val published_at: String? = null,
    val assets: List<GithubAsset> = emptyList()
) {
    val tagName get() = tag_name
    val htmlUrl get() = html_url
    val publishedAt get() = published_at
}

@Serializable
private data class GithubAsset(
    val name: String = "",
    val browser_download_url: String = ""
) {
    val browserDownloadUrl get() = browser_download_url
}

// ─── Mesh update packet model ─────────────────────────────
@Serializable
data class MeshUpdatePacket(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis()
)
