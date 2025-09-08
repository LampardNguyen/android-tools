package com.techvibe.androidtools.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.utils.Constants.TAG_BATTERY_SERVICE

/**
 * Helper class to handle battery optimization and Doze mode compatibility
 */
object BatteryOptimizationHelper {
    
    /**
     * Check if the app is whitelisted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            // Pre-Marshmallow devices don't have Doze mode
            true
        }
    }
    
    /**
     * Request to be whitelisted from battery optimizations
     */
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    android.util.Log.e(TAG_BATTERY_SERVICE, "Failed to request battery optimization whitelist", e)
                    false
                }
            } else {
                true
            }
        } else {
            true
        }
    }
    
    /**
     * Check if device is in Doze mode
     */
    fun isDeviceInDozeMode(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
            powerManager?.isDeviceIdleMode ?: false
        } else {
            false
        }
    }
    
    /**
     * Check if app standby mode is enabled
     */
    fun isAppInStandbyMode(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val usageStatsManager = ContextCompat.getSystemService(context, android.app.usage.UsageStatsManager::class.java)
                usageStatsManager?.isAppInactive(context.packageName) ?: false
            } catch (e: Exception) {
                android.util.Log.w(TAG_BATTERY_SERVICE, "Unable to check app standby mode", e)
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Check if the device is in power save mode
     */
    fun isInPowerSaveMode(context: Context): Boolean {
        val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        return powerManager?.isPowerSaveMode ?: false
    }
    
    /**
     * Get optimization status summary
     */
    fun getBatteryOptimizationStatus(context: Context): BatteryOptimizationStatus {
        return BatteryOptimizationStatus(
            isIgnoringOptimizations = isIgnoringBatteryOptimizations(context),
            isInDozeMode = isDeviceInDozeMode(context),
            isInStandbyMode = isAppInStandbyMode(context),
            isInPowerSaveMode = isInPowerSaveMode(context)
        )
    }
    
    /**
     * Check if the battery monitoring service should continue running
     */
    fun shouldServiceContinueRunning(context: Context): ServiceOptimizationDecision {
        val status = getBatteryOptimizationStatus(context)
        
        return when {
            // If not whitelisted, service might be killed
            !status.isIgnoringOptimizations -> ServiceOptimizationDecision.MIGHT_BE_KILLED
            
            // If in Doze mode but whitelisted, can continue with limitations
            status.isInDozeMode && status.isIgnoringOptimizations -> ServiceOptimizationDecision.CONTINUE_WITH_LIMITATIONS
            
            // If in app standby but whitelisted, continue with caution
            status.isInStandbyMode && status.isIgnoringOptimizations -> ServiceOptimizationDecision.CONTINUE_WITH_CAUTION
            
            // If in power save mode, reduce activity
            status.isInPowerSaveMode -> ServiceOptimizationDecision.REDUCE_ACTIVITY
            
            // Normal operation
            else -> ServiceOptimizationDecision.CONTINUE_NORMAL
        }
    }
    
    /**
     * Get recommended monitoring interval based on optimization status
     */
    fun getRecommendedMonitoringInterval(context: Context): Long {
        val status = getBatteryOptimizationStatus(context)
        
        return when {
            status.isInDozeMode -> 60_000L // 1 minute in Doze mode
            status.isInStandbyMode -> 30_000L // 30 seconds in standby
            status.isInPowerSaveMode -> 45_000L // 45 seconds in power save mode
            else -> 15_000L // Normal 15 seconds
        }
    }
    
    /**
     * Check if service is running in background restrictions
     */
    fun isRunningUnderBackgroundRestrictions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val activityManager = ContextCompat.getSystemService(context, ActivityManager::class.java)
                val restriction = activityManager?.isBackgroundRestricted ?: false
                restriction
            } catch (e: Exception) {
                android.util.Log.w(TAG_BATTERY_SERVICE, "Unable to check background restrictions", e)
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Get wake lock flags appropriate for current optimization state
     */
    fun getWakeLockFlags(context: Context): Int {
        val status = getBatteryOptimizationStatus(context)
        
        return when {
            status.isInDozeMode -> {
                // In Doze mode, use partial wake lock with acquire causes wakeup
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            }
            status.isInPowerSaveMode -> {
                // In power save mode, use minimal wake lock
                PowerManager.PARTIAL_WAKE_LOCK
            }
            else -> {
                // Normal operation
                PowerManager.PARTIAL_WAKE_LOCK
            }
        }
    }
    
    /**
     * Get wake lock timeout based on optimization status
     */
    fun getWakeLockTimeout(context: Context): Long {
        val status = getBatteryOptimizationStatus(context)
        
        return when {
            status.isInDozeMode -> 30_000L // 30 seconds in Doze mode
            status.isInPowerSaveMode -> 20_000L // 20 seconds in power save mode
            else -> 60_000L // 1 minute normal
        }
    }
    
    /**
     * Check if high-priority notifications should be used
     */
    fun shouldUseHighPriorityNotification(context: Context): Boolean {
        val status = getBatteryOptimizationStatus(context)
        
        // Use high priority if not in restrictive modes and whitelisted
        return status.isIgnoringOptimizations && 
               !status.isInDozeMode && 
               !status.isInPowerSaveMode
    }
    
    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG_BATTERY_SERVICE, "Failed to open battery optimization settings", e)
            try {
                // Fallback to general settings
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            } catch (fallbackException: Exception) {
                android.util.Log.e(TAG_BATTERY_SERVICE, "Failed to open settings", fallbackException)
                false
            }
        }
    }
}

/**
 * Data class representing battery optimization status
 */
data class BatteryOptimizationStatus(
    val isIgnoringOptimizations: Boolean,
    val isInDozeMode: Boolean,
    val isInStandbyMode: Boolean,
    val isInPowerSaveMode: Boolean
) {
    val hasAnyRestrictions: Boolean
        get() = isInDozeMode || isInStandbyMode || isInPowerSaveMode || !isIgnoringOptimizations
        
    val canRunNormally: Boolean
        get() = isIgnoringOptimizations && !isInDozeMode && !isInStandbyMode && !isInPowerSaveMode
}

/**
 * Enum representing service optimization decisions
 */
enum class ServiceOptimizationDecision {
    CONTINUE_NORMAL,
    CONTINUE_WITH_CAUTION,
    CONTINUE_WITH_LIMITATIONS,
    REDUCE_ACTIVITY,
    MIGHT_BE_KILLED
}
