package com.techvibe.androidtools.utils

/**
 * Constants used throughout the Battery Alert application
 */
object Constants {
    
    // Battery monitoring constants
    const val MIN_BATTERY_THRESHOLD = 0
    const val MAX_BATTERY_THRESHOLD = 100
    const val DEFAULT_BATTERY_THRESHOLD = 80
    
    // Notification constants
    const val NOTIFICATION_CHANNEL_ID = "battery_alert_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Battery Alert Notifications"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for battery charging alerts"
    const val BATTERY_ALERT_NOTIFICATION_ID = 1001
    const val SERVICE_NOTIFICATION_ID = 1002
    
    // Service constants
    const val BATTERY_MONITOR_SERVICE_NAME = "BatteryMonitorService"
    const val SERVICE_ACTION_START = "ACTION_START_MONITORING"
    const val SERVICE_ACTION_STOP = "ACTION_STOP_MONITORING"
    
    // Intent extras
    const val EXTRA_BATTERY_LEVEL = "extra_battery_level"
    const val EXTRA_IS_CHARGING = "extra_is_charging"
    const val EXTRA_BATTERY_THRESHOLD = "extra_battery_threshold"
    
    // Broadcast actions
    const val ACTION_BATTERY_THRESHOLD_REACHED = "com.techvibe.androidtools.BATTERY_THRESHOLD_REACHED"
    const val ACTION_UPDATE_UI = "com.techvibe.androidtools.UPDATE_UI"
    
    // Audio constants
    const val DEFAULT_NOTIFICATION_VOLUME = 80
    const val MIN_NOTIFICATION_VOLUME = 0
    const val MAX_NOTIFICATION_VOLUME = 100
    
    // Timing constants (in seconds)
    const val DEFAULT_REPEAT_INTERVAL_SECONDS = 30
    const val MIN_REPEAT_INTERVAL_SECONDS = 5
    const val MAX_REPEAT_INTERVAL_SECONDS = 300 // 5 minutes
    
    // Permission request codes
    const val PERMISSION_REQUEST_POST_NOTIFICATIONS = 2001
    const val PERMISSION_REQUEST_VIBRATE = 2002
    const val PERMISSION_REQUEST_WAKE_LOCK = 2003
    const val PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION = 2004
    
    // Worker constants
    const val BATTERY_CHECK_WORKER_TAG = "battery_check_worker"
    const val BATTERY_CHECK_WORK_NAME = "periodic_battery_check"
    const val BATTERY_CHECK_INTERVAL_MINUTES = 15L
    
    // Preferences keys (additional backup)
    const val PREF_KEY_LAST_BATTERY_LEVEL = "last_battery_level"
    const val PREF_KEY_LAST_CHARGING_STATE = "last_charging_state"
    const val PREF_KEY_NOTIFICATION_DISMISSED_TIME = "notification_dismissed_time"
    
    // UI constants
    const val BATTERY_UPDATE_DELAY_MS = 1000L
    const val UI_UPDATE_THROTTLE_MS = 500L
    
    // Error messages
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_SERVICE_NOT_AVAILABLE = "Battery monitoring service not available"
    const val ERROR_INVALID_THRESHOLD = "Invalid battery threshold value"
    
    // Log tags (max 23 characters for Android)
    const val TAG_MAIN_ACTIVITY = "MainActivity"
    const val TAG_SETTINGS_ACTIVITY = "SettingsActivity"
    const val TAG_BATTERY_SERVICE = "BatteryService"
    const val TAG_BATTERY_RECEIVER = "BatteryReceiver"
    const val TAG_NOTIFICATION_HELPER = "NotificationHelper"
    const val TAG_BOOT_RECEIVER = "BootReceiver"
    
    // File and URI constants
    const val RINGTONE_PICKER_REQUEST_CODE = 3001
    const val DEFAULT_RINGTONE_TYPE = "notification"
}
