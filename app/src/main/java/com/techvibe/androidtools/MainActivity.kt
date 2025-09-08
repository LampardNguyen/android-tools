package com.techvibe.androidtools

import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.techvibe.androidtools.databinding.ActivityMainBinding
import com.techvibe.androidtools.presentation.viewmodel.MainViewModel
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.data.repository.BatteryRepositoryImpl
import com.techvibe.androidtools.receiver.BatteryBroadcastReceiver
import com.techvibe.androidtools.utils.Constants.TAG_MAIN_ACTIVITY
import com.techvibe.androidtools.utils.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for Battery Alert application
 * Displays battery status and allows user to configure monitoring settings
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var batteryReceiver: BatteryBroadcastReceiver
    
    @Inject
    lateinit var preferencesHelper: PreferencesHelper
    
    @Inject
    lateinit var batteryRepository: BatteryRepositoryImpl
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupStatusBar()
        setupUI()
        setupObservers()
        registerBatteryReceiver()
        loadInitialData()
        checkAndRequestPermissions()
    }
    
    /**
     * Setup status bar appearance based on system theme
     */
    private fun setupStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        // Add top padding to account for status bar
        binding.root.setPadding(0, getStatusBarHeight(), 0, 0)
    }
    
    /**
     * Get status bar height
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * Setup UI components and event listeners
     */
    private fun setupUI() {
        with(binding) {
            // Setup SeekBar listener for threshold selection
            seekBarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        tvThresholdValue.text = "$progress%"
                        viewModel.updateBatteryThreshold(progress)
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Not needed for this implementation
                }
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Save threshold when user stops dragging
                    val threshold = seekBar?.progress ?: 80
                    lifecycleScope.launch {
                        viewModel.saveBatteryThreshold(threshold)
                    }
                }
            })
            
            // Setup monitoring switch listener
            switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setMonitoringEnabled(isChecked)
                updateMonitoringStatus(isChecked)
            }
            
            // Setup test notification button
            btnTestNotification.setOnClickListener {
                viewModel.testNotification()
                showToast(getString(R.string.test_notification_sent))
            }
            
            // Setup debug button (long click on test notification)
            btnTestNotification.setOnLongClickListener {
                showDebugInfo()
                true
            }
            
            // Setup settings button
            btnSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }
    
    /**
     * Setup observers for ViewModel LiveData
     */
    private fun setupObservers() {
        // Observe battery info changes
        viewModel.batteryInfo.observe(this) { batteryInfo ->
            updateBatteryDisplay(batteryInfo)
        }
        
        // Observe UI state changes
        viewModel.uiState.observe(this) { uiState ->
            updateUI(uiState)
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showToast(errorMessage)
            }
        }
    }
    
    /**
     * Register battery broadcast receiver
     */
    private fun registerBatteryReceiver() {
        batteryReceiver = BatteryBroadcastReceiver { batteryInfo ->
            viewModel.updateBatteryInfo(batteryInfo)
        }
        
        // Inject dependencies after creating receiver
        batteryReceiver.injectDependencies(preferencesHelper, batteryRepository)
        
        val filter = IntentFilter().apply {
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            addAction(android.content.Intent.ACTION_POWER_CONNECTED)
            addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
        }
        
        registerReceiver(batteryReceiver, filter)
    }
    
    /**
     * Load initial data from preferences
     */
    private fun loadInitialData() {
        lifecycleScope.launch {
            viewModel.loadInitialData()
        }
    }
    
    /**
     * Update battery display with current information
     */
    private fun updateBatteryDisplay(batteryInfo: com.techvibe.androidtools.data.model.BatteryInfo) {
        with(binding) {
            tvBatteryLevel.text = batteryInfo.getLevelString()
            tvChargingStatus.text = batteryInfo.getChargingStatusString()
            progressBattery.progress = batteryInfo.level
        }
    }
    
    /**
     * Update UI state based on ViewModel state
     */
    private fun updateUI(uiState: com.techvibe.androidtools.presentation.viewmodel.MainUiState) {
        with(binding) {
            switchMonitoring.isChecked = uiState.isMonitoringEnabled
            seekBarThreshold.progress = uiState.batteryThreshold
            tvThresholdValue.text = "${uiState.batteryThreshold}%"
            
            // Enable/disable UI components based on monitoring state
            seekBarThreshold.isEnabled = uiState.isMonitoringEnabled
            btnTestNotification.isEnabled = true
        }
    }
    
    /**
     * Update monitoring status display
     */
    private fun updateMonitoringStatus(isEnabled: Boolean) {
        val message = if (isEnabled) {
            getString(R.string.monitoring_enabled)
        } else {
            getString(R.string.monitoring_disabled)
        }
        showToast(message)
    }
    
    /**
     * Navigate to Settings Activity
     */
    private fun navigateToSettings() {
        val intent = android.content.Intent(this, com.techvibe.androidtools.presentation.ui.settings.SettingsActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show debug information dialog
     */
    private fun showDebugInfo() {
        val debugInfo = buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("Monitoring enabled: ${preferencesHelper.isMonitoringEnabled}")
            appendLine("Battery threshold: ${preferencesHelper.batteryThreshold}%")
            appendLine("Vibration enabled: ${preferencesHelper.isVibrationEnabled}")
            appendLine("Notification volume: ${preferencesHelper.notificationVolume}")
            appendLine("Repeat interval: ${preferencesHelper.repeatIntervalSeconds}s")
            appendLine()
            appendLine("=== PERMISSIONS ===")
            appendLine("All critical permissions: ${PermissionHelper.areAllCriticalPermissionsGranted(this@MainActivity)}")
            appendLine("Battery optimization ignored: ${PermissionHelper.isBatteryOptimizationIgnored(this@MainActivity)}")
            appendLine()
            appendLine("=== BATTERY INFO ===")
            val batteryInfo = viewModel.batteryInfo.value
            if (batteryInfo != null) {
                appendLine("Level: ${batteryInfo.level}%")
                appendLine("Charging: ${batteryInfo.isCharging}")
                appendLine("Charging type: ${batteryInfo.chargingType}")
                appendLine("Temperature: ${batteryInfo.temperature}°C")
                appendLine("Health: ${batteryInfo.health}")
            } else {
                appendLine("No battery info available")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Debug Information")
            .setMessage(debugInfo)
            .setPositiveButton("Copy to Clipboard") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo)
                clipboard.setPrimaryClip(clip)
                showToast("Debug info copied to clipboard")
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    /**
     * Check and request required permissions
     */
    private fun checkAndRequestPermissions() {
        if (!PermissionHelper.areAllCriticalPermissionsGranted(this)) {
            showPermissionDialog()
        }
    }
    
    /**
     * Show permission explanation dialog
     */
    private fun showPermissionDialog() {
        val missingPermissions = PermissionHelper.getMissingPermissions(this)
        val message = getString(R.string.permission_required_message, missingPermissions.joinToString(", "))
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.grant_permissions)) { _, _ ->
                PermissionHelper.requestMissingPermissions(this)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                showToast(getString(R.string.permission_denied_warning))
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        val result = PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
        
        when (result) {
            com.techvibe.androidtools.utils.PermissionResult.GRANTED -> {
                showToast(getString(R.string.permission_granted))
                // Check if there are more permissions to request
                if (!PermissionHelper.areAllCriticalPermissionsGranted(this)) {
                    PermissionHelper.requestMissingPermissions(this)
                }
            }
            com.techvibe.androidtools.utils.PermissionResult.DENIED -> {
                val rationaleMessage = PermissionHelper.getPermissionRationaleMessage(requestCode, this)
                showPermissionDeniedDialog(rationaleMessage)
            }
            com.techvibe.androidtools.utils.PermissionResult.UNKNOWN -> {
                // Do nothing
            }
        }
    }
    
    /**
     * Handle activity results (for battery optimization permission)
     */
    @Deprecated("Deprecated in API level 30")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == com.techvibe.androidtools.utils.Constants.PERMISSION_REQUEST_IGNORE_BATTERY_OPTIMIZATION) {
            if (PermissionHelper.isBatteryOptimizationIgnored(this)) {
                showToast(getString(R.string.battery_optimization_disabled))
            } else {
                showToast(getString(R.string.battery_optimization_warning))
            }
        }
    }
    
    /**
     * Show permission denied dialog with rationale
     */
    private fun showPermissionDeniedDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                // Open app settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::batteryReceiver.isInitialized) {
            unregisterReceiver(batteryReceiver)
        }
    }
}