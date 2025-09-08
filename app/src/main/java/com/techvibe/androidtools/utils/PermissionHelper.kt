package com.techvibe.androidtools.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.utils.Constants.PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION
import com.techvibe.androidtools.utils.Constants.PERMISSION_REQUEST_POST_NOTIFICATIONS
import com.techvibe.androidtools.utils.Constants.PERMISSION_REQUEST_VIBRATE
import com.techvibe.androidtools.utils.Constants.PERMISSION_REQUEST_WAKE_LOCK

/**
 * Helper class for handling runtime permissions required by Battery Alert app
 */
object PermissionHelper {
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 13 devices don't need this permission
            true
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }
    
    /**
     * Check if vibration permission is granted
     */
    fun isVibrationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request vibration permission
     */
    fun requestVibrationPermission(activity: Activity) {
        if (!isVibrationPermissionGranted(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.VIBRATE),
                PERMISSION_REQUEST_VIBRATE
            )
        }
    }
    
    /**
     * Check if wake lock permission is granted
     */
    fun isWakeLockPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WAKE_LOCK
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request wake lock permission
     */
    fun requestWakeLockPermission(activity: Activity) {
        if (!isWakeLockPermissionGranted(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WAKE_LOCK),
                PERMISSION_REQUEST_WAKE_LOCK
            )
        }
    }
    
    /**
     * Check if app is whitelisted from battery optimizations
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            // Pre-Marshmallow devices don't have battery optimization
            true
        }
    }
    
    /**
     * Request to ignore battery optimizations
     */
    fun requestIgnoreBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isBatteryOptimizationIgnored(activity)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION)
                } catch (e: Exception) {
                    // If direct intent fails, open battery optimization settings
                    openBatteryOptimizationSettings(activity)
                }
            }
        }
    }
    
    /**
     * Open battery optimization settings page
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }
    
    /**
     * Check all critical permissions for the app
     */
    fun checkAllCriticalPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            notificationPermission = isNotificationPermissionGranted(context),
            vibrationPermission = isVibrationPermissionGranted(context),
            wakeLockPermission = isWakeLockPermissionGranted(context),
            batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        )
    }
    
    /**
     * Request all missing critical permissions
     */
    fun requestMissingPermissions(activity: Activity) {
        val status = checkAllCriticalPermissions(activity)
        
        if (!status.notificationPermission) {
            requestNotificationPermission(activity)
        } else if (!status.vibrationPermission) {
            requestVibrationPermission(activity)
        } else if (!status.wakeLockPermission) {
            requestWakeLockPermission(activity)
        } else if (!status.batteryOptimizationIgnored) {
            requestIgnoreBatteryOptimization(activity)
        }
    }
    
    /**
     * Check if all critical permissions are granted
     */
    fun areAllCriticalPermissionsGranted(context: Context): Boolean {
        val status = checkAllCriticalPermissions(context)
        return status.notificationPermission && 
               status.vibrationPermission && 
               status.wakeLockPermission && 
               status.batteryOptimizationIgnored
    }
    
    /**
     * Get list of missing permissions for display
     */
    fun getMissingPermissions(context: Context): List<String> {
        val status = checkAllCriticalPermissions(context)
        val missing = mutableListOf<String>()
        
        if (!status.notificationPermission) {
            missing.add("Thông báo")
        }
        if (!status.vibrationPermission) {
            missing.add("Rung")
        }
        if (!status.wakeLockPermission) {
            missing.add("Wake Lock")
        }
        if (!status.batteryOptimizationIgnored) {
            missing.add("Tối ưu hóa pin")
        }
        
        return missing
    }
    
    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        if (permissions.isEmpty() || grantResults.isEmpty()) {
            return PermissionResult.DENIED
        }
        
        return when (requestCode) {
            PERMISSION_REQUEST_POST_NOTIFICATIONS,
            PERMISSION_REQUEST_VIBRATE,
            PERMISSION_REQUEST_WAKE_LOCK -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionResult.GRANTED
                } else {
                    PermissionResult.DENIED
                }
            }
            PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION -> {
                // This will be handled separately as it's not a standard permission
                PermissionResult.GRANTED
            }
            else -> PermissionResult.UNKNOWN
        }
    }
    
    /**
     * Show permission rationale message
     */
    fun getPermissionRationaleMessage(requestCode: Int, context: Context): String {
        return when (requestCode) {
            PERMISSION_REQUEST_POST_NOTIFICATIONS -> 
                "Ứng dụng cần quyền thông báo để cảnh báo khi pin đạt ngưỡng mong muốn."
            PERMISSION_REQUEST_VIBRATE -> 
                "Ứng dụng cần quyền rung để rung thiết bị khi có thông báo pin."
            PERMISSION_REQUEST_WAKE_LOCK -> 
                "Ứng dụng cần quyền Wake Lock để hoạt động chính xác trong background."
            PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION -> 
                "Để ứng dụng hoạt động tốt nhất, hãy tắt tối ưu hóa pin cho ứng dụng này."
            else -> "Ứng dụng cần quyền này để hoạt động bình thường."
        }
    }
}

/**
 * Data class representing current permission status
 */
data class PermissionStatus(
    val notificationPermission: Boolean,
    val vibrationPermission: Boolean,
    val wakeLockPermission: Boolean,
    val batteryOptimizationIgnored: Boolean
) {
    val allGranted: Boolean
        get() = notificationPermission && vibrationPermission && wakeLockPermission && batteryOptimizationIgnored
}

/**
 * Enum representing permission request results
 */
enum class PermissionResult {
    GRANTED,
    DENIED,
    UNKNOWN
}
