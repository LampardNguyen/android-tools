package com.techvibe.androidtools.presentation.ui.menu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.databinding.ActivityMenuBinding
import com.techvibe.androidtools.MainActivity
import com.techvibe.androidtools.presentation.ui.floatvolume.FloatVolumeSettingsActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Menu Activity - Main entry point of the application
 * Displays a menu with available options for the user
 */
class MenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupStatusBar()
        setupUI()
        setupClickListeners()
        updateTime()
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
        // The UI is set up in the layout file
        // Additional setup can be added here if needed
    }
    
    /**
     * Setup click listeners for menu items
     */
    private fun setupClickListeners() {
        // Battery Alert menu item click listener
        binding.cardBatteryAlert.setOnClickListener {
            navigateToBatterySettings()
        }
        
        // Float Volume menu item click listener
        binding.cardFloatVolume.setOnClickListener {
            navigateToFloatVolumeSettings()
        }
    }
    
    /**
     * Update the time display in the header (removed time display as requested)
     */
    private fun updateTime() {
        // Time display removed from layout as per user request
    }
    
    /**
     * Navigate to Battery Settings Activity
     */
    private fun navigateToBatterySettings() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        
        // Add slide transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    /**
     * Navigate to Float Volume Settings Activity
     */
    private fun navigateToFloatVolumeSettings() {
        val intent = Intent(this, FloatVolumeSettingsActivity::class.java)
        startActivity(intent)
        
        // Add slide transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    override fun onResume() {
        super.onResume()
        // Update time when returning to this activity
        updateTime()
    }
}
