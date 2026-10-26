package com.techvibe.androidtools.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * CaffeinePreferences - Store caffeine (keep screen on) state
 */
class CaffeinePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "caffeine_preferences"
        private const val KEY_ENABLED = "caffeine_enabled"
        private const val KEY_ORIGINAL_TIMEOUT = "caffeine_original_timeout"

        const val DEFAULT_TIMEOUT = 30_000 // 30 seconds
    }

    var isEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_ENABLED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_ENABLED, value).apply()

    var originalTimeout: Int
        get() = sharedPreferences.getInt(KEY_ORIGINAL_TIMEOUT, DEFAULT_TIMEOUT)
        set(value) = sharedPreferences.edit().putInt(KEY_ORIGINAL_TIMEOUT, value).apply()
}
