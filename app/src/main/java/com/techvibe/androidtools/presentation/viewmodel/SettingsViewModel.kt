package com.techvibe.androidtools.presentation.viewmodel

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvibe.androidtools.R
import com.techvibe.androidtools.domain.repository.BatteryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity
 * Handles settings logic and UI state management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryRepository: BatteryRepository
) : ViewModel() {
    
    private val _uiState = MutableLiveData<SettingsUiState>()
    val uiState: LiveData<SettingsUiState> = _uiState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    init {
        _uiState.value = SettingsUiState()
    }
    
    /**
     * Load initial settings from repository
     */
    suspend fun loadInitialSettings() {
        try {
            val currentState = _uiState.value ?: SettingsUiState()
            
            val ringtoneUri = batteryRepository.getNotificationSoundUri()
            val ringtoneName = getRingtoneName(ringtoneUri)
            
            val newState = currentState.copy(
                selectedRingtoneUri = ringtoneUri,
                selectedRingtoneName = ringtoneName,
                notificationVolume = batteryRepository.getNotificationVolume(),
                isVibrationEnabled = batteryRepository.isVibrationEnabled(),
                repeatIntervalSeconds = batteryRepository.getRepeatIntervalSeconds()
            )
            
            _uiState.value = newState
            
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi tải cài đặt: ${e.message}"
        }
    }
    
    /**
     * Update notification volume (temporary, not saved yet)
     */
    fun updateNotificationVolume(volume: Int) {
        val currentState = _uiState.value ?: SettingsUiState()
        _uiState.value = currentState.copy(notificationVolume = volume)
    }
    
    /**
     * Save notification volume to repository
     */
    suspend fun saveNotificationVolume(volume: Int) {
        try {
            batteryRepository.saveNotificationVolume(volume)
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu âm lượng: ${e.message}"
        }
    }
    
    /**
     * Test current volume and ringtone
     */
    fun testVolume() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value ?: return@launch
                
                // TODO: Implement actual ringtone testing with current volume
                // For now, just show feedback
                _successMessage.value = "Âm lượng: ${currentState.notificationVolume}%"
                
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi thử âm lượng: ${e.message}"
            }
        }
    }
    
    /**
     * Set vibration enabled state
     */
    suspend fun setVibrationEnabled(enabled: Boolean) {
        try {
            batteryRepository.setVibrationEnabled(enabled)
            val currentState = _uiState.value ?: SettingsUiState()
            _uiState.value = currentState.copy(isVibrationEnabled = enabled)
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu cài đặt rung: ${e.message}"
        }
    }
    
    /**
     * Save ringtone selection
     */
    suspend fun saveRingtoneSelection(uri: String?, name: String) {
        try {
            batteryRepository.saveNotificationSoundUri(uri)
            val currentState = _uiState.value ?: SettingsUiState()
            _uiState.value = currentState.copy(
                selectedRingtoneUri = uri,
                selectedRingtoneName = name
            )
            _successMessage.value = "Đã chọn nhạc chuông: $name"
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu nhạc chuông: ${e.message}"
        }
    }
    
    /**
     * Save repeat interval
     */
    suspend fun saveRepeatInterval(seconds: Int) {
        try {
            batteryRepository.saveRepeatIntervalSeconds(seconds)
            val currentState = _uiState.value ?: SettingsUiState()
            _uiState.value = currentState.copy(repeatIntervalSeconds = seconds)
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu khoảng thời gian: ${e.message}"
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        try {
            // Reset to default values
            batteryRepository.saveNotificationSoundUri(null)
            batteryRepository.saveNotificationVolume(80)
            batteryRepository.setVibrationEnabled(true)
            batteryRepository.saveRepeatIntervalSeconds(30)
            
            // Update UI state
            val newState = SettingsUiState(
                selectedRingtoneUri = null,
                selectedRingtoneName = "Default notification",
                notificationVolume = 80,
                isVibrationEnabled = true,
                repeatIntervalSeconds = 30
            )
            
            _uiState.value = newState
            _successMessage.value = context.getString(R.string.settings_saved)
            
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi khôi phục cài đặt: ${e.message}"
        }
    }
    
    /**
     * Save all current settings
     */
    suspend fun saveAllSettings() {
        try {
            val currentState = _uiState.value ?: return
            
            batteryRepository.saveNotificationSoundUri(currentState.selectedRingtoneUri)
            batteryRepository.saveNotificationVolume(currentState.notificationVolume)
            batteryRepository.setVibrationEnabled(currentState.isVibrationEnabled)
            batteryRepository.saveRepeatIntervalSeconds(currentState.repeatIntervalSeconds)
            
            _successMessage.value = context.getString(R.string.settings_saved)
            
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu cài đặt: ${e.message}"
        }
    }
    
    /**
     * Get current ringtone URI for picker
     */
    fun getCurrentRingtoneUri(): Uri? {
        val uriString = _uiState.value?.selectedRingtoneUri
        return if (uriString != null) Uri.parse(uriString) else null
    }
    
    /**
     * Get human-readable ringtone name from URI
     */
    private fun getRingtoneName(uriString: String?): String {
        return try {
            if (uriString == null) {
                "Default notification"
            } else {
                val uri = Uri.parse(uriString)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: "Custom ringtone"
            }
        } catch (e: Exception) {
            "Default notification"
        }
    }
}

/**
 * Data class representing UI state for SettingsActivity
 */
data class SettingsUiState(
    val selectedRingtoneUri: String? = null,
    val selectedRingtoneName: String? = "Default notification",
    val notificationVolume: Int = 80,
    val isVibrationEnabled: Boolean = true,
    val repeatIntervalSeconds: Int = 30,
    val isLoading: Boolean = false
)
