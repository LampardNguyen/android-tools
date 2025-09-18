package com.techvibe.androidtools.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * FloatVolumePreferences - Manage floating volume button settings
 */
class FloatVolumePreferences(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "float_volume_preferences"
        private const val KEY_ENABLED = "float_volume_enabled"
        private const val KEY_SIZE = "float_volume_size"
        private const val KEY_OPACITY = "float_volume_opacity"
        private const val KEY_POSITION_X = "float_volume_position_x"
        private const val KEY_POSITION_Y = "float_volume_position_y"
        
        // Default values  
        const val DEFAULT_SIZE = 50 // 40dp + 10 from seekbar (middle of 40-60 range)
        const val DEFAULT_OPACITY = 40 // 40% (middle of 10-70 range)
    }
    
    var isEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_ENABLED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_ENABLED, value).apply()
    
    var buttonSize: Int
        get() = sharedPreferences.getInt(KEY_SIZE, DEFAULT_SIZE)
        set(value) = sharedPreferences.edit().putInt(KEY_SIZE, value).apply()
    
    var buttonOpacity: Int
        get() = sharedPreferences.getInt(KEY_OPACITY, DEFAULT_OPACITY)
        set(value) = sharedPreferences.edit().putInt(KEY_OPACITY, value).apply()
    
    var positionX: Int
        get() = sharedPreferences.getInt(KEY_POSITION_X, 0)
        set(value) = sharedPreferences.edit().putInt(KEY_POSITION_X, value).apply()
    
    var positionY: Int
        get() = sharedPreferences.getInt(KEY_POSITION_Y, 100)
        set(value) = sharedPreferences.edit().putInt(KEY_POSITION_Y, value).apply()
    
    /**
     * Register listener for preferences changes
     */
    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }
    
    /**
     * Unregister listener for preferences changes
     */
    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
