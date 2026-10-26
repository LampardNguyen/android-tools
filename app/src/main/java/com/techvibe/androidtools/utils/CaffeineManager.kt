package com.techvibe.androidtools.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.data.preferences.CaffeinePreferences
import com.techvibe.androidtools.service.CaffeineService
import com.techvibe.androidtools.service.CaffeineTileService

/**
 * CaffeineManager - Keep the screen on by raising the system screen-off timeout.
 * Requires WRITE_SETTINGS ("Modify system settings") permission.
 */
object CaffeineManager {

    private const val TAG = "CaffeineManager"
    private const val KEEP_ON_TIMEOUT = Int.MAX_VALUE

    fun canWriteSettings(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)

    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isEnabled(context: Context): Boolean = CaffeinePreferences(context).isEnabled

    /**
     * Enable caffeine. Returns false if permission is missing.
     */
    fun enable(context: Context): Boolean {
        if (!canWriteSettings(context)) return false
        val prefs = CaffeinePreferences(context)
        if (!prefs.isEnabled) {
            // Only save the original timeout once, never overwrite it with our own value
            prefs.originalTimeout = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                CaffeinePreferences.DEFAULT_TIMEOUT
            )
        }
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, KEEP_ON_TIMEOUT)
        prefs.isEnabled = true
        ContextCompat.startForegroundService(context, Intent(context, CaffeineService::class.java))
        requestTileUpdate(context)
        return true
    }

    /**
     * Disable caffeine and restore the original screen-off timeout.
     */
    fun disable(context: Context) {
        val prefs = CaffeinePreferences(context)
        if (prefs.isEnabled && canWriteSettings(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    prefs.originalTimeout
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore screen timeout", e)
            }
        }
        prefs.isEnabled = false
        context.stopService(Intent(context, CaffeineService::class.java))
        requestTileUpdate(context)
    }

    fun toggle(context: Context): Boolean =
        if (isEnabled(context)) {
            disable(context)
            true
        } else {
            enable(context)
        }

    fun requestTileUpdate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(
                context,
                ComponentName(context, CaffeineTileService::class.java)
            )
        }
    }
}
