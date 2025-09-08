package com.techvibe.androidtools.domain.repository

import com.techvibe.androidtools.data.model.BatteryInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for battery-related operations
 */
interface BatteryRepository {
    
    /**
     * Get current battery information
     */
    suspend fun getCurrentBatteryInfo(): BatteryInfo
    
    /**
     * Observe battery changes as a Flow
     */
    fun observeBatteryChanges(): Flow<BatteryInfo>
    
    /**
     * Check if device is currently charging
     */
    suspend fun isCharging(): Boolean
    
    /**
     * Get current battery level (0-100)
     */
    suspend fun getBatteryLevel(): Int
    
    /**
     * Get battery threshold from preferences
     */
    suspend fun getBatteryThreshold(): Int
    
    /**
     * Save battery threshold to preferences
     */
    suspend fun saveBatteryThreshold(threshold: Int)
    
    /**
     * Check if monitoring is enabled
     */
    suspend fun isMonitoringEnabled(): Boolean
    
    /**
     * Enable or disable battery monitoring
     */
    suspend fun setMonitoringEnabled(enabled: Boolean)
    
    /**
     * Get notification sound URI
     */
    suspend fun getNotificationSoundUri(): String?
    
    /**
     * Save notification sound URI
     */
    suspend fun saveNotificationSoundUri(uri: String?)
    
    /**
     * Check if vibration is enabled for notifications
     */
    suspend fun isVibrationEnabled(): Boolean
    
    /**
     * Enable or disable vibration for notifications
     */
    suspend fun setVibrationEnabled(enabled: Boolean)
    
    /**
     * Get notification volume (0-100)
     */
    suspend fun getNotificationVolume(): Int
    
    /**
     * Save notification volume
     */
    suspend fun saveNotificationVolume(volume: Int)
    
    /**
     * Get repeat interval for notifications in seconds
     */
    suspend fun getRepeatIntervalSeconds(): Int
    
    /**
     * Save repeat interval for notifications in seconds
     */
    suspend fun saveRepeatIntervalSeconds(seconds: Int)
}
