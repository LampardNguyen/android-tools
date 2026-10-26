package com.techvibe.androidtools.presentation.ui.menu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.techvibe.androidtools.R
import com.techvibe.androidtools.databinding.ActivityMenuBinding
import com.techvibe.androidtools.MainActivity
import com.techvibe.androidtools.presentation.ui.floatvolume.FloatVolumeSettingsActivity
import com.techvibe.androidtools.presentation.ui.caffeine.CaffeineSettingsActivity
import com.techvibe.androidtools.utils.AppUpdateManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Menu Activity - Main entry point of the application
 * Displays a menu with available options for the user
 */
class MenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuBinding
    private var isCheckingUpdate = false
    private var pendingUpdate: AppUpdateManager.UpdateInfo? = null
    
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
        binding.tvCheckUpdate.text =
            getString(R.string.update_check_button, AppUpdateManager.currentVersion)
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
        
        // Caffeine menu item click listener
        binding.cardCaffeine.setOnClickListener {
            startActivity(Intent(this, CaffeineSettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Manual update check
        binding.tvCheckUpdate.setOnClickListener {
            checkForUpdate(manual = true)
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
        
        val update = pendingUpdate
        if (update != null && AppUpdateManager.canInstallPackages(this)) {
            // Returned from "install unknown apps" settings with permission granted
            pendingUpdate = null
            downloadAndInstall(update)
        } else if (AppUpdateManager.shouldAutoCheck(this)) {
            checkForUpdate(manual = false)
        }
    }
    
    /**
     * Check GitHub for a newer release. Auto check stays silent unless an update exists.
     */
    private fun checkForUpdate(manual: Boolean) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        if (manual) binding.tvCheckUpdate.setText(R.string.update_checking)
        
        lifecycleScope.launch {
            val info = AppUpdateManager.fetchLatest()
            isCheckingUpdate = false
            setupUI()
            when {
                info != null && AppUpdateManager.isNewer(info.version) -> showUpdateDialog(info)
                manual && info != null -> toast(R.string.update_latest)
                manual -> toast(R.string.update_check_failed)
            }
        }
    }
    
    private fun showUpdateDialog(info: AppUpdateManager.UpdateInfo) {
        val message = buildString {
            append(getString(R.string.update_available_message, AppUpdateManager.currentVersion))
            if (info.notes.isNotEmpty()) append("\n\n").append(info.notes)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_available_title, info.version))
            .setMessage(message)
            .setPositiveButton(R.string.update_now) { _, _ -> startUpdate(info) }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }
    
    private fun startUpdate(info: AppUpdateManager.UpdateInfo) {
        if (!AppUpdateManager.canInstallPackages(this)) {
            pendingUpdate = info
            toast(R.string.update_install_permission)
            startActivity(AppUpdateManager.installPermissionIntent(this))
            return
        }
        downloadAndInstall(info)
    }
    
    private fun downloadAndInstall(info: AppUpdateManager.UpdateInfo) {
        val progress = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_downloading)
            .setView(progress)
            .setCancelable(false)
            .show()
        
        lifecycleScope.launch {
            try {
                val apk = AppUpdateManager.downloadApk(this@MenuActivity, info) { percent ->
                    if (percent >= 0) {
                        progress.isIndeterminate = false
                        progress.setProgressCompat(percent, true)
                    }
                }
                dialog.dismiss()
                AppUpdateManager.install(this@MenuActivity, apk)
            } catch (e: Exception) {
                dialog.dismiss()
                toast(R.string.update_download_failed)
            }
        }
    }
    
    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}
