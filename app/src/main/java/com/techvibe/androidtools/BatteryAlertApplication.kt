package com.techvibe.androidtools

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.utils.Constants
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Battery Alert app
 * Handles app-wide initialization including Hilt setup and notification channels
 */
@HiltAndroidApp
class BatteryAlertApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channels for Android O+
        createNotificationChannels()
        
        // Initialize any other app-wide components if needed
        initializeApp()
    }
    
    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = ContextCompat.getSystemService(
                this, 
                NotificationManager::class.java
            )
            
            // Battery Alert notification channel
            val batteryAlertChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Constants.NOTIFICATION_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            // Service notification channel (for foreground service)
            val serviceChannel = NotificationChannel(
                "battery_service_channel",
                "Battery Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for battery monitoring service"
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            
            // Create the channels
            notificationManager?.createNotificationChannel(batteryAlertChannel)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }
    
    /**
     * Initialize any app-wide components
     */
    private fun initializeApp() {
        // Any other initialization code can go here
        // For example: crash reporting, analytics, etc.
    }
}
