package com.techvibe.androidtools.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.techvibe.androidtools.data.model.BatteryInfo
import com.techvibe.androidtools.data.model.BatteryHealth
import com.techvibe.androidtools.data.model.ChargingType
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.domain.repository.BatteryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BatteryRepository interface
 */
@Singleton
class BatteryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesHelper: PreferencesHelper
) : BatteryRepository {
    
    private val _batteryFlow = MutableSharedFlow<BatteryInfo>(replay = 1)
    
    override suspend fun getCurrentBatteryInfo(): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        
        return batteryStatus?.let { intent ->
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
            
            BatteryInfo(
                level = batteryLevel,
                isCharging = isCharging,
                chargingType = chargingType,
                temperature = temperature,
                voltage = voltage,
                health = health
            )
        } ?: BatteryInfo(level = 0, isCharging = false)
    }
    
    override fun observeBatteryChanges(): Flow<BatteryInfo> {
        return _batteryFlow.asSharedFlow()
    }
    
    /**
     * Update battery info for observers (called from BroadcastReceiver)
     */
    fun updateBatteryInfo(batteryInfo: BatteryInfo) {
        _batteryFlow.tryEmit(batteryInfo)
    }
    
    override suspend fun isCharging(): Boolean {
        return getCurrentBatteryInfo().isCharging
    }
    
    override suspend fun getBatteryLevel(): Int {
        return getCurrentBatteryInfo().level
    }
    
    override suspend fun getBatteryThreshold(): Int {
        return preferencesHelper.batteryThreshold
    }
    
    override suspend fun saveBatteryThreshold(threshold: Int) {
        preferencesHelper.batteryThreshold = threshold
    }
    
    override suspend fun isMonitoringEnabled(): Boolean {
        return preferencesHelper.isMonitoringEnabled
    }
    
    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        preferencesHelper.isMonitoringEnabled = enabled
    }
    
    override suspend fun getNotificationSoundUri(): String? {
        return preferencesHelper.notificationSoundUri
    }
    
    override suspend fun saveNotificationSoundUri(uri: String?) {
        preferencesHelper.notificationSoundUri = uri
    }
    
    override suspend fun isVibrationEnabled(): Boolean {
        return preferencesHelper.isVibrationEnabled
    }
    
    override suspend fun setVibrationEnabled(enabled: Boolean) {
        preferencesHelper.isVibrationEnabled = enabled
    }
    
    override suspend fun getNotificationVolume(): Int {
        return preferencesHelper.notificationVolume
    }
    
    override suspend fun saveNotificationVolume(volume: Int) {
        preferencesHelper.notificationVolume = volume
    }
    
    override suspend fun getRepeatIntervalSeconds(): Int {
        return preferencesHelper.repeatIntervalSeconds
    }
    
    override suspend fun saveRepeatIntervalSeconds(seconds: Int) {
        preferencesHelper.repeatIntervalSeconds = seconds
    }
}
