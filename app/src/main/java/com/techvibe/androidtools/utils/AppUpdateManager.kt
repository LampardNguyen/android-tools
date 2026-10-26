package com.techvibe.androidtools.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.techvibe.androidtools.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * AppUpdateManager - Check, download and install the latest APK from GitHub Releases.
 * Release convention: tag `vX.Y.Z`, body = release notes, first `.apk` asset = download.
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/LampardNguyen/android-tools/releases/latest"
    private const val PREFS_NAME = "app_update_preferences"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val AUTO_CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour

    data class UpdateInfo(
        val version: String,
        val notes: String,
        val apkUrl: String
    )

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /**
     * Auto check is throttled to once per hour; manual check is not.
     */
    fun shouldAutoCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) < AUTO_CHECK_INTERVAL_MS) return false
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        return true
    }

    /**
     * Fetch the latest release. Returns null on any failure (network, 404, invalid tag, no APK).
     */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Latest release HTTP ${connection.responseCode}")
                    return@withContext null
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                parseRelease(json)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch latest release", e)
            null
        }
    }

    private fun parseRelease(json: JSONObject): UpdateInfo? {
        val version = json.optString("tag_name").removePrefix("v")
        if (!Regex("""^\d+\.\d+\.\d+$""").matches(version)) return null

        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").lowercase().endsWith(".apk")) {
                return UpdateInfo(
                    version = version,
                    notes = json.optString("body").trim(),
                    apkUrl = asset.optString("browser_download_url")
                )
            }
        }
        return null
    }

    fun isNewer(latest: String, current: String = currentVersion): Boolean {
        val a = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    /**
     * Download the APK into the cache dir, reporting progress (0-100, or -1 if unknown).
     */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(dir, "androidtools-${info.version}.apk")
        val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastPercent = -2
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            withContext(Dispatchers.Main) { onProgress(percent) }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        file
    }

    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
