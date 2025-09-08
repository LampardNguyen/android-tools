package com.techvibe.androidtools.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.data.model.BatteryInfo
import com.techvibe.androidtools.data.model.BatteryHealth
import com.techvibe.androidtools.data.model.ChargingType
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.data.repository.BatteryRepositoryImpl
import com.techvibe.androidtools.service.BatteryMonitorService
import com.techvibe.androidtools.utils.Constants.ACTION_BATTERY_THRESHOLD_REACHED
import com.techvibe.androidtools.utils.Constants.ACTION_UPDATE_UI
import com.techvibe.androidtools.utils.Constants.EXTRA_BATTERY_LEVEL
import com.techvibe.androidtools.utils.Constants.EXTRA_IS_CHARGING
import com.techvibe.androidtools.utils.Constants.TAG_BATTERY_RECEIVER

/**
 * Enhanced BroadcastReceiver for comprehensive battery monitoring
 * Handles battery level changes, charging state, and threshold notifications
 */
class BatteryBroadcastReceiver : BroadcastReceiver {
    
    private var preferencesHelper: PreferencesHelper? = null
    private var batteryRepository: BatteryRepositoryImpl? = null
    
    // Callback for UI updates (used when registered dynamically)
    private var onBatteryChanged: ((BatteryInfo) -> Unit)? = null
    
    // Default constructor for manifest registration
    constructor() : super()
    
    // Constructor for dynamic registration with callback
    constructor(onBatteryChanged: (BatteryInfo) -> Unit) : super() {
        this.onBatteryChanged = onBatteryChanged
    }
    
    // Constructor for manual dependency injection
    constructor(
        preferencesHelper: PreferencesHelper,
        batteryRepository: BatteryRepositoryImpl
    ) : super() {
        this.preferencesHelper = preferencesHelper
        this.batteryRepository = batteryRepository
    }
    
    /**
     * Inject dependencies after receiver creation (for MainActivity registration)
     */
    fun injectDependencies(
        preferencesHelper: PreferencesHelper,
        batteryRepository: BatteryRepositoryImpl
    ) {
        Log.d(TAG_BATTERY_RECEIVER, "Injecting dependencies into BatteryBroadcastReceiver")
        this.preferencesHelper = preferencesHelper
        this.batteryRepository = batteryRepository
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG_BATTERY_RECEIVER, "Received broadcast: ${intent.action}")
        Log.d(TAG_BATTERY_RECEIVER, "Dependencies status - preferencesHelper: ${preferencesHelper != null}, batteryRepository: ${batteryRepository != null}")
        
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChanged(context, intent)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG_BATTERY_RECEIVER, "Power connected")
                handlePowerConnection(context, intent, true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG_BATTERY_RECEIVER, "Power disconnected")
                handlePowerConnection(context, intent, false)
                // Stop any ongoing notification when charger is removed
                stopBatteryAlert(context)
            }
            Intent.ACTION_BATTERY_LOW -> {
                Log.d(TAG_BATTERY_RECEIVER, "Battery low warning")
                handleBatteryLow(context)
            }
            Intent.ACTION_BATTERY_OKAY -> {
                Log.d(TAG_BATTERY_RECEIVER, "Battery okay")
                handleBatteryOkay(context)
            }
        }
    }
    
    /**
     * Handle battery level and status changes
     */
    private fun handleBatteryChanged(context: Context, intent: Intent) {
        val batteryInfo = extractBatteryInfo(intent)
        
        Log.d(TAG_BATTERY_RECEIVER, "Battery changed: ${batteryInfo.level}%, charging: ${batteryInfo.isCharging}")
        
        // Update repository with new battery info
        batteryRepository?.updateBatteryInfo(batteryInfo)
        
        // Notify UI callback if registered
        onBatteryChanged?.invoke(batteryInfo)
        
        // Check if monitoring is enabled (skip if dependencies not injected)
        if (preferencesHelper?.isMonitoringEnabled != true) {
            Log.w(TAG_BATTERY_RECEIVER, "Battery monitoring is disabled or dependencies not initialized")
            Log.w(TAG_BATTERY_RECEIVER, "preferencesHelper: ${preferencesHelper != null}, isMonitoringEnabled: ${preferencesHelper?.isMonitoringEnabled}")
            return
        }
        
        // Check threshold only when charging
        if (batteryInfo.isCharging) {
            checkBatteryThreshold(context, batteryInfo)
        }
        
        // Broadcast update for other components
        broadcastBatteryUpdate(context, batteryInfo)
    }
    
    /**
     * Handle power connection/disconnection
     */
    private fun handlePowerConnection(context: Context, intent: Intent, isConnected: Boolean) {
        // Get current battery info when power state changes
        val batteryInfo = extractBatteryInfo(intent)
        
        Log.d(TAG_BATTERY_RECEIVER, "Power ${if (isConnected) "connected" else "disconnected"}, battery: ${batteryInfo.level}%")
        
        // Update repository
        batteryRepository?.updateBatteryInfo(batteryInfo)
        
        // Notify UI callback
        onBatteryChanged?.invoke(batteryInfo)
        
        if (isConnected && preferencesHelper?.isMonitoringEnabled == true) {
            // Start monitoring when charger is connected
            checkBatteryThreshold(context, batteryInfo)
        } else if (!isConnected) {
            // Stop notifications when charger is disconnected
            stopBatteryAlert(context)
        }
        
        // Broadcast update
        broadcastBatteryUpdate(context, batteryInfo)
    }
    
    /**
     * Handle system battery low warning
     */
    private fun handleBatteryLow(context: Context) {
        Log.w(TAG_BATTERY_RECEIVER, "System battery low warning received")
        // Could implement additional low battery handling here
    }
    
    /**
     * Handle system battery okay status
     */
    private fun handleBatteryOkay(context: Context) {
        Log.d(TAG_BATTERY_RECEIVER, "System battery okay status received")
        // Could implement additional recovery handling here
    }
    
    /**
     * Check if battery has reached the configured threshold
     */
    private fun checkBatteryThreshold(context: Context, batteryInfo: BatteryInfo) {
        val threshold = preferencesHelper?.batteryThreshold ?: 80
        
        Log.d(TAG_BATTERY_RECEIVER, "Checking threshold: ${batteryInfo.level}% >= $threshold%")
        
        if (batteryInfo.hasReachedThreshold(threshold)) {
            Log.i(TAG_BATTERY_RECEIVER, "Battery threshold reached: ${batteryInfo.level}% >= $threshold%")
            triggerThresholdAlert(context, batteryInfo, threshold)
        }
    }
    
    /**
     * Trigger battery threshold alert
     */
    private fun triggerThresholdAlert(context: Context, batteryInfo: BatteryInfo, threshold: Int) {
        Log.i(TAG_BATTERY_RECEIVER, "Triggering threshold alert for ${batteryInfo.level}%")
        
        // Start notification service
        val serviceIntent = Intent(context, BatteryMonitorService::class.java).apply {
            action = ACTION_BATTERY_THRESHOLD_REACHED
            putExtra(EXTRA_BATTERY_LEVEL, batteryInfo.level)
            putExtra(EXTRA_IS_CHARGING, batteryInfo.isCharging)
        }
        
        try {
            // Use appropriate method based on API level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG_BATTERY_RECEIVER, "Started battery alert service")
        } catch (e: Exception) {
            Log.e(TAG_BATTERY_RECEIVER, "Failed to start battery alert service", e)
        }
        
        // Broadcast threshold reached event
        val thresholdIntent = Intent(ACTION_BATTERY_THRESHOLD_REACHED).apply {
            putExtra(EXTRA_BATTERY_LEVEL, batteryInfo.level)
            putExtra(EXTRA_IS_CHARGING, batteryInfo.isCharging)
        }
        context.sendBroadcast(thresholdIntent)
    }
    
    /**
     * Stop battery alert notifications
     */
    private fun stopBatteryAlert(context: Context) {
        Log.d(TAG_BATTERY_RECEIVER, "Stopping battery alert")
        
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        context.stopService(serviceIntent)
    }
    
    /**
     * Broadcast battery update for UI and other components
     */
    private fun broadcastBatteryUpdate(context: Context, batteryInfo: BatteryInfo) {
        val updateIntent = Intent(ACTION_UPDATE_UI).apply {
            putExtra(EXTRA_BATTERY_LEVEL, batteryInfo.level)
            putExtra(EXTRA_IS_CHARGING, batteryInfo.isCharging)
        }
        context.sendBroadcast(updateIntent)
    }
    
    /**
     * Extract comprehensive battery information from intent
     */
    private fun extractBatteryInfo(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryLevel = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargingType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
            else -> ChargingType.UNKNOWN
        }
        
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        
        val healthStatus = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthStatus) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }
        
        return BatteryInfo(
            level = batteryLevel,
            isCharging = isCharging,
            chargingType = chargingType,
            temperature = temperature,
            voltage = voltage,
            health = health
        )
    }
}
