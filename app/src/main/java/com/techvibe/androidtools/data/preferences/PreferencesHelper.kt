package com.techvibe.androidtools.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing SharedPreferences for Battery Alert app settings
 */
@Singleton
class PreferencesHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREF_NAME = "battery_alert_preferences"
        
        // Keys for different settings
        private const val KEY_IS_MONITORING_ENABLED = "is_monitoring_enabled"
        private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
        private const val KEY_NOTIFICATION_SOUND_URI = "notification_sound_uri"
        private const val KEY_IS_VIBRATION_ENABLED = "is_vibration_enabled"
        private const val KEY_NOTIFICATION_VOLUME = "notification_volume"
        private const val KEY_REPEAT_INTERVAL_SECONDS = "repeat_interval_seconds"
        private const val KEY_SOUND_REPEAT_COUNT = "sound_repeat_count"
        private const val KEY_IS_FIRST_TIME = "is_first_time"
        
        // Default values
        private const val DEFAULT_BATTERY_THRESHOLD = 80
        private const val DEFAULT_NOTIFICATION_VOLUME = 80
        private const val DEFAULT_REPEAT_INTERVAL = 30
        private const val DEFAULT_SOUND_REPEAT_COUNT = 3
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // Monitor enable/disable
    var isMonitoringEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_MONITORING_ENABLED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_MONITORING_ENABLED, value).apply()
    
    // Battery threshold (0-100)
    var batteryThreshold: Int
        get() = sharedPreferences.getInt(KEY_BATTERY_THRESHOLD, DEFAULT_BATTERY_THRESHOLD)
        set(value) = sharedPreferences.edit().putInt(KEY_BATTERY_THRESHOLD, value).apply()
    
    // Notification sound URI
    var notificationSoundUri: String?
        get() = sharedPreferences.getString(KEY_NOTIFICATION_SOUND_URI, null)
        set(value) = sharedPreferences.edit().putString(KEY_NOTIFICATION_SOUND_URI, value).apply()
    
    // Notification ringtone URI (non-null convenience property)
    var notificationRingtoneUri: String
        get() = sharedPreferences.getString(KEY_NOTIFICATION_SOUND_URI, "") ?: ""
        set(value) = sharedPreferences.edit().putString(KEY_NOTIFICATION_SOUND_URI, value).apply()
    
    // Vibration enable/disable
    var isVibrationEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_VIBRATION_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_VIBRATION_ENABLED, value).apply()
    
    // Notification volume (0-100)
    var notificationVolume: Int
        get() = sharedPreferences.getInt(KEY_NOTIFICATION_VOLUME, DEFAULT_NOTIFICATION_VOLUME)
        set(value) = sharedPreferences.edit().putInt(KEY_NOTIFICATION_VOLUME, value).apply()
    
    // Repeat interval in seconds
    var repeatIntervalSeconds: Int
        get() = sharedPreferences.getInt(KEY_REPEAT_INTERVAL_SECONDS, DEFAULT_REPEAT_INTERVAL)
        set(value) = sharedPreferences.edit().putInt(KEY_REPEAT_INTERVAL_SECONDS, value).apply()
    
    // Sound repeat count (1-10 times)
    var soundRepeatCount: Int
        get() = sharedPreferences.getInt(KEY_SOUND_REPEAT_COUNT, DEFAULT_SOUND_REPEAT_COUNT)
        set(value) = sharedPreferences.edit().putInt(KEY_SOUND_REPEAT_COUNT, value.coerceIn(1, 10)).apply()
    
    // First time app launch
    var isFirstTime: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_FIRST_TIME, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_FIRST_TIME, value).apply()
    
    /**
     * Clear all preferences (for testing or reset functionality)
     */
    fun clearAllPreferences() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Reset to default values
     */
    fun resetToDefaults() {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_MONITORING_ENABLED, false)
            putInt(KEY_BATTERY_THRESHOLD, DEFAULT_BATTERY_THRESHOLD)
            remove(KEY_NOTIFICATION_SOUND_URI)
            putBoolean(KEY_IS_VIBRATION_ENABLED, true)
            putInt(KEY_NOTIFICATION_VOLUME, DEFAULT_NOTIFICATION_VOLUME)
            putInt(KEY_REPEAT_INTERVAL_SECONDS, DEFAULT_REPEAT_INTERVAL)
            putInt(KEY_SOUND_REPEAT_COUNT, DEFAULT_SOUND_REPEAT_COUNT)
            putBoolean(KEY_IS_FIRST_TIME, false)
        }.apply()
    }
    
    /**
     * Export all settings as a map (for debugging or backup)
     */
    fun exportAllSettings(): Map<String, Any?> {
        return mapOf(
            "isMonitoringEnabled" to isMonitoringEnabled,
            "batteryThreshold" to batteryThreshold,
            "notificationSoundUri" to notificationSoundUri,
            "isVibrationEnabled" to isVibrationEnabled,
            "notificationVolume" to notificationVolume,
            "repeatIntervalSeconds" to repeatIntervalSeconds,
            "soundRepeatCount" to soundRepeatCount,
            "isFirstTime" to isFirstTime
        )
    }
}
