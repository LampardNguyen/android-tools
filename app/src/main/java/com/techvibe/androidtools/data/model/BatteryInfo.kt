package com.techvibe.androidtools.data.model

/**
 * Data class representing battery information
 */
data class BatteryInfo(
    val level: Int, // Battery level percentage (0-100)
    val isCharging: Boolean, // Whether device is currently charging
    val chargingType: ChargingType = ChargingType.UNKNOWN,
    val temperature: Float = 0f, // Battery temperature in Celsius
    val voltage: Int = 0, // Battery voltage in millivolts
    val health: BatteryHealth = BatteryHealth.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if battery level has reached the specified threshold
     */
    fun hasReachedThreshold(threshold: Int): Boolean {
        return level >= threshold && isCharging
    }
    
    /**
     * Get battery level as formatted string with percentage
     */
    fun getLevelString(): String = "$level%"
    
    /**
     * Get charging status as human-readable string
     */
    fun getChargingStatusString(): String {
        return when {
            isCharging -> "Đang sạc"
            level == 100 -> "Đã đầy"
            else -> "Không sạc"
        }
    }
    
    /**
     * Check if battery info is valid
     */
    fun isValid(): Boolean {
        return level in 0..100
    }
}

/**
 * Enum representing different charging types
 */
enum class ChargingType {
    UNKNOWN,
    AC,     // Wall charger
    USB,    // USB charging
    WIRELESS // Wireless charging
}

/**
 * Enum representing battery health status
 */
enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD
}
