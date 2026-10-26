package com.techvibe.androidtools.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.data.preferences.CaffeinePreferences
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.service.BatteryMonitorService
import com.techvibe.androidtools.utils.BatteryOptimizationHelper
import com.techvibe.androidtools.utils.CaffeineManager
import com.techvibe.androidtools.utils.Constants.ACTION_BATTERY_THRESHOLD_REACHED
import com.techvibe.androidtools.utils.Constants.EXTRA_BATTERY_LEVEL
import com.techvibe.androidtools.utils.Constants.EXTRA_IS_CHARGING
import com.techvibe.androidtools.utils.Constants.TAG_BOOT_RECEIVER

/**
 * BroadcastReceiver to handle device boot completion
 * Automatically starts battery monitoring if it was enabled before reboot
 */
class BootCompletedReceiver : BroadcastReceiver() {
    
    private lateinit var preferencesHelper: PreferencesHelper
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG_BOOT_RECEIVER, "Boot completed broadcast received: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handleBootOrUpdate(context, intent)
            }
            else -> {
                Log.w(TAG_BOOT_RECEIVER, "Unknown action received: ${intent.action}")
            }
        }
    }
    
    /**
     * Handle boot completion or package update
     */
    private fun handleBootOrUpdate(context: Context, intent: Intent) {
        try {
            Log.i(TAG_BOOT_RECEIVER, "Device booted or app updated - checking monitoring status")
            
            // Initialize PreferencesHelper manually since this receiver is not managed by Hilt
            preferencesHelper = PreferencesHelper(context)
            
            // Caffeine never survives a reboot: restore the original screen timeout
            if (CaffeinePreferences(context).isEnabled) {
                CaffeineManager.disable(context)
            }
            
            // Check if monitoring was enabled before boot/update
            if (preferencesHelper.isMonitoringEnabled) {
                Log.i(TAG_BOOT_RECEIVER, "Battery monitoring was enabled - attempting to restart")
                
                // Check battery optimization status
                val optimizationStatus = BatteryOptimizationHelper.getBatteryOptimizationStatus(context)
                Log.d(TAG_BOOT_RECEIVER, "Battery optimization status: $optimizationStatus")
                
                if (!optimizationStatus.isIgnoringOptimizations) {
                    Log.w(TAG_BOOT_RECEIVER, "App not whitelisted from battery optimizations - monitoring might not work reliably")
                }
                
                // Get current battery status to determine if we should start monitoring
                val currentBatteryInfo = getBatteryInfo(context)
                
                if (currentBatteryInfo.isCharging) {
                    val batteryLevel = currentBatteryInfo.level
                    val threshold = preferencesHelper.batteryThreshold
                    
                    Log.d(TAG_BOOT_RECEIVER, "Device is charging: $batteryLevel%, threshold: $threshold%")
                    
                    if (batteryLevel >= threshold) {
                        Log.i(TAG_BOOT_RECEIVER, "Battery level already at/above threshold - starting alert service")
                        startBatteryMonitorService(context, batteryLevel, true)
                    } else {
                        Log.d(TAG_BOOT_RECEIVER, "Battery level below threshold - monitoring will start when threshold is reached")
                        // The BatteryBroadcastReceiver will handle future battery changes
                    }
                } else {
                    Log.d(TAG_BOOT_RECEIVER, "Device not charging - monitoring will start when charging begins")
                }
                
                // Log restoration success
                Log.i(TAG_BOOT_RECEIVER, "Battery monitoring setup completed after boot/update")
                
            } else {
                Log.d(TAG_BOOT_RECEIVER, "Battery monitoring was disabled - not starting service")
            }
            
        } catch (e: Exception) {
            Log.e(TAG_BOOT_RECEIVER, "Error handling boot completion", e)
        }
    }
    
    /**
     * Start battery monitor service with current battery level
     */
    private fun startBatteryMonitorService(context: Context, batteryLevel: Int, isCharging: Boolean) {
        try {
            val intent = Intent(context, BatteryMonitorService::class.java).apply {
                action = ACTION_BATTERY_THRESHOLD_REACHED
                putExtra(EXTRA_BATTERY_LEVEL, batteryLevel)
                putExtra(EXTRA_IS_CHARGING, isCharging)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG_BOOT_RECEIVER, "Started battery monitor service as foreground service")
            } else {
                context.startService(intent)
                Log.d(TAG_BOOT_RECEIVER, "Started battery monitor service")
            }
            
        } catch (e: Exception) {
            Log.e(TAG_BOOT_RECEIVER, "Failed to start battery monitor service", e)
        }
    }
    
    /**
     * Get current battery information
     */
    private fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0)
                val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                
                val status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == android.os.BatteryManager.BATTERY_STATUS_FULL
                
                val plugged = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
                val chargingType = when (plugged) {
                    android.os.BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    android.os.BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Unknown"
                }
                
                val temperature = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f
                val voltage = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0f
                val health = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)
                
                BatteryInfo(
                    level = batteryPct,
                    isCharging = isCharging,
                    chargingType = chargingType,
                    temperature = temperature,
                    voltage = voltage,
                    health = health
                )
            } else {
                Log.w(TAG_BOOT_RECEIVER, "Unable to get battery status")
                BatteryInfo(0, false, "Unknown", 0f, 0f, -1)
            }
            
        } catch (e: Exception) {
            Log.e(TAG_BOOT_RECEIVER, "Error getting battery info", e)
            BatteryInfo(0, false, "Unknown", 0f, 0f, -1)
        }
    }
    
    /**
     * Data class to hold battery information
     */
    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val chargingType: String,
        val temperature: Float,
        val voltage: Float,
        val health: Int
    )
}
