package com.techvibe.androidtools.presentation.ui.floatvolume

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.databinding.ActivityFloatVolumeSettingsBinding
import com.techvibe.androidtools.presentation.ui.menu.MenuActivity
import com.techvibe.androidtools.service.FloatingVolumeService
import com.techvibe.androidtools.data.preferences.FloatVolumePreferences

/**
 * Float Volume Settings Activity
 * Configure floating volume buttons overlay
 */
class FloatVolumeSettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFloatVolumeSettingsBinding
    private lateinit var preferences: FloatVolumePreferences
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloatVolumeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = FloatVolumePreferences(this)
        
        setupStatusBar()
        setupUI()
        setupClickListeners()
        loadSettings()
    }
    
    /**
     * Setup status bar to be transparent with light icons
     */
    private fun setupStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0, // Clear the light status bars flag to show light icons on dark background
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        
        // Set status bar color to transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }
    
    /**
     * Setup initial UI state
     */
    private fun setupUI() {
        // Additional UI setup can be added here if needed
    }
    
    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            navigateBackToMenu()
        }
        
        // Enable float buttons switch listener
        binding.switchEnableFloat.setOnCheckedChangeListener { _, isChecked ->
            updateFloatButtonsEnabled(isChecked)
        }
        
        // Button size seekbar listener
        binding.seekBarButtonSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val size = progress + 40 // Minimum size 40dp, range 40-60dp
                    binding.tvButtonSizeValue.text = "${size}dp"
                    
                    // Save to preferences and update floating button real-time
                    preferences.buttonSize = size
                    FloatingVolumeService.updateSettings(this@FloatVolumeSettingsActivity)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // Button opacity seekbar listener
        binding.seekBarButtonOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val opacity = progress + 10 // Minimum opacity 10%, range 10-70%
                    binding.tvButtonOpacityValue.text = "${opacity}%"
                    
                    // Save to preferences and update floating button real-time
                    preferences.buttonOpacity = opacity
                    FloatingVolumeService.updateSettings(this@FloatVolumeSettingsActivity)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
    }
    
    /**
     * Update UI when float buttons are enabled/disabled
     */
    private fun updateFloatButtonsEnabled(isEnabled: Boolean) {
        binding.seekBarButtonSize.isEnabled = isEnabled
        binding.seekBarButtonOpacity.isEnabled = isEnabled
        
        preferences.isEnabled = isEnabled
        
        if (isEnabled) {
            if (checkOverlayPermission()) {
                startFloatingVolumeService()
                showToast(getString(R.string.float_buttons_enabled))
            } else {
                // Reset switch and request permission
                binding.switchEnableFloat.isChecked = false
                preferences.isEnabled = false
                requestOverlayPermission()
            }
        } else {
            stopFloatingVolumeService()
            showToast(getString(R.string.float_buttons_disabled))
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Permission not required for older versions
        }
    }
    
    /**
     * Request overlay permission from user
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.overlay_permission_title))
                .setMessage(getString(R.string.overlay_permission_message))
                .setPositiveButton(getString(R.string.grant_overlay_permission)) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }
    
    /**
     * Start floating volume service
     */
    private fun startFloatingVolumeService() {
        FloatingVolumeService.startFloating(this)
    }
    
    /**
     * Stop floating volume service
     */
    private fun stopFloatingVolumeService() {
        FloatingVolumeService.stopFloating(this)
    }
    
    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Load settings from preferences
     */
    private fun loadSettings() {
        // Load switch state
        binding.switchEnableFloat.isChecked = preferences.isEnabled
        
        // Load button size
        val buttonSize = preferences.buttonSize
        binding.seekBarButtonSize.progress = buttonSize - 40 // Subtract minimum size (40dp)
        binding.tvButtonSizeValue.text = "${buttonSize}dp"
        
        // Load button opacity
        val buttonOpacity = preferences.buttonOpacity
        binding.seekBarButtonOpacity.progress = buttonOpacity - 10 // Subtract minimum opacity (10%)
        binding.tvButtonOpacityValue.text = "${buttonOpacity}%"
        
        // Update UI based on enabled state
        binding.seekBarButtonSize.isEnabled = preferences.isEnabled
        binding.seekBarButtonOpacity.isEnabled = preferences.isEnabled
    }
    
    
    /**
     * Navigate back to Menu Activity
     */
    private fun navigateBackToMenu() {
        finish() // This will go back to MenuActivity since it's the parent
    }
    
    /**
     * Handle activity result for overlay permission
     */
    @Deprecated("Deprecated in API level 30")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                // Permission granted, enable float buttons
                binding.switchEnableFloat.isChecked = true
                preferences.isEnabled = true
                startFloatingVolumeService()
                showToast(getString(R.string.float_buttons_enabled))
            } else {
                // Permission denied
                showToast("Overlay permission denied")
            }
        }
    }
}
