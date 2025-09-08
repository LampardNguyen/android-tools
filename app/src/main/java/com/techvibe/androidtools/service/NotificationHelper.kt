package com.techvibe.androidtools.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.MainActivity
import com.techvibe.androidtools.R
import com.techvibe.androidtools.utils.BatteryOptimizationHelper
import com.techvibe.androidtools.utils.Constants.BATTERY_ALERT_NOTIFICATION_ID
import com.techvibe.androidtools.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.techvibe.androidtools.utils.Constants.SERVICE_NOTIFICATION_ID
import com.techvibe.androidtools.utils.Constants.TAG_NOTIFICATION_HELPER
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for creating and managing notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    
    private val notificationManager = ContextCompat.getSystemService(
        context,
        NotificationManager::class.java
    ) ?: throw IllegalStateException("NotificationManager not available")
    
    companion object {
        private const val SERVICE_CHANNEL_ID = "battery_service_channel"
        private const val ACTION_DISMISS = "com.techvibe.androidtools.ACTION_DISMISS"
        private const val ACTION_SNOOZE = "com.techvibe.androidtools.ACTION_SNOOZE"
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Battery Alert Channel (High Priority)
            val batteryChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for battery threshold alerts"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            // Service Channel (Low Priority)
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Battery Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for battery monitoring service"
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            
            notificationManager.createNotificationChannel(batteryChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
    
    /**
     * Create battery threshold alert notification
     */
    fun createBatteryAlertNotification(
        batteryLevel: Int,
        threshold: Int
    ): android.app.Notification {
        
        val title = context.getString(R.string.battery_alert_title)
        val message = context.getString(R.string.battery_alert_message, batteryLevel)
        
        // Intent to open main activity when notification is tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Dismiss action
        val dismissIntent = Intent(context, BatteryMonitorService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Snooze action
        val snoozeIntent = Intent(context, BatteryMonitorService::class.java).apply {
            action = ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getService(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_test)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nPin đã đạt $batteryLevel% (ngưỡng: $threshold%). Hãy rút sạc để bảo vệ tuổi thọ pin.")
            )
            .setPriority(
                if (BatteryOptimizationHelper.shouldUseHighPriorityNotification(context)) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_HIGH
                }
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_refresh,
                context.getString(R.string.dismiss),
                dismissPendingIntent
            )
            .addAction(
                R.drawable.ic_snooze,
                context.getString(R.string.snooze),
                snoozePendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()
    }
    
    /**
     * Create foreground service notification
     */
    fun createServiceNotification(): android.app.Notification {
        val title = context.getString(R.string.service_notification_title)
        val message = context.getString(R.string.service_notification_message)
        
        // Intent to open main activity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_settings)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setShowWhen(false)
            .build()
    }
    
    /**
     * Show battery alert notification
     */
    fun showBatteryAlert(batteryLevel: Int, threshold: Int) {
        Log.d(TAG_NOTIFICATION_HELPER, "Creating battery alert notification for $batteryLevel% (threshold: $threshold%)")
        
        // Check if notifications are enabled
        if (!areNotificationsEnabled()) {
            Log.w(TAG_NOTIFICATION_HELPER, "Notifications are disabled globally")
            return
        }
        
        if (!isBatteryAlertChannelEnabled()) {
            Log.w(TAG_NOTIFICATION_HELPER, "Battery alert channel is disabled")
            return
        }
        
        try {
            val notification = createBatteryAlertNotification(batteryLevel, threshold)
            Log.d(TAG_NOTIFICATION_HELPER, "Showing notification with ID: $BATTERY_ALERT_NOTIFICATION_ID")
            notificationManager.notify(BATTERY_ALERT_NOTIFICATION_ID, notification)
            Log.d(TAG_NOTIFICATION_HELPER, "Battery alert notification displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATION_HELPER, "Failed to show battery alert notification", e)
        }
    }
    
    /**
     * Cancel battery alert notification
     */
    fun cancelBatteryAlert() {
        notificationManager.cancel(BATTERY_ALERT_NOTIFICATION_ID)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Assume enabled on older versions
        }
    }
    
    /**
     * Check if battery alert channel is enabled
     */
    fun isBatteryAlertChannelEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }
}
