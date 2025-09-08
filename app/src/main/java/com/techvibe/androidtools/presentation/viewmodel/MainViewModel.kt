package com.techvibe.androidtools.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvibe.androidtools.data.model.BatteryInfo
import com.techvibe.androidtools.domain.repository.BatteryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 * Handles battery monitoring logic and UI state management
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val batteryRepository: BatteryRepository
) : ViewModel() {
    
    private val _batteryInfo = MutableLiveData<BatteryInfo>()
    val batteryInfo: LiveData<BatteryInfo> = _batteryInfo
    
    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> = _uiState
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    init {
        // Initialize with default UI state
        _uiState.value = MainUiState()
        loadCurrentBatteryInfo()
    }
    
    /**
     * Load initial data from repository
     */
    fun loadInitialData() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value ?: MainUiState()
                val newState = currentState.copy(
                    isMonitoringEnabled = batteryRepository.isMonitoringEnabled(),
                    batteryThreshold = batteryRepository.getBatteryThreshold()
                )
                _uiState.value = newState
                
                // Load current battery info
                loadCurrentBatteryInfo()
                
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi tải dữ liệu: ${e.message}"
            }
        }
    }
    
    /**
     * Load current battery information
     */
    private fun loadCurrentBatteryInfo() {
        viewModelScope.launch {
            try {
                val batteryInfo = batteryRepository.getCurrentBatteryInfo()
                _batteryInfo.value = batteryInfo
            } catch (e: Exception) {
                _errorMessage.value = "Không thể lấy thông tin pin: ${e.message}"
            }
        }
    }
    
    /**
     * Update battery info from BroadcastReceiver
     */
    fun updateBatteryInfo(batteryInfo: BatteryInfo) {
        _batteryInfo.value = batteryInfo
    }
    
    /**
     * Update battery threshold (temporary, not saved yet)
     */
    fun updateBatteryThreshold(threshold: Int) {
        val currentState = _uiState.value ?: MainUiState()
        _uiState.value = currentState.copy(batteryThreshold = threshold)
    }
    
    /**
     * Save battery threshold to repository
     */
    suspend fun saveBatteryThreshold(threshold: Int) {
        try {
            batteryRepository.saveBatteryThreshold(threshold)
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi lưu ngưỡng pin: ${e.message}"
        }
    }
    
    /**
     * Enable or disable battery monitoring
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                batteryRepository.setMonitoringEnabled(enabled)
                val currentState = _uiState.value ?: MainUiState()
                _uiState.value = currentState.copy(isMonitoringEnabled = enabled)
                
                // TODO: Start/stop battery monitoring service based on enabled state
                
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi thay đổi trạng thái theo dõi: ${e.message}"
            }
        }
    }
    
    /**
     * Test notification functionality
     */
    fun testNotification() {
        viewModelScope.launch {
            try {
                val currentBattery = _batteryInfo.value ?: return@launch
                val threshold = _uiState.value?.batteryThreshold ?: 80
                
                // TODO: Implement notification testing
                // For now, just show that it's working
                
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi thử nghiệm thông báo: ${e.message}"
            }
        }
    }
}

/**
 * Data class representing UI state for MainActivity
 */
data class MainUiState(
    val isMonitoringEnabled: Boolean = false,
    val batteryThreshold: Int = 80,
    val isLoading: Boolean = false
)
