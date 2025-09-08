package com.techvibe.androidtools.presentation.ui.settings

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.techvibe.androidtools.R
import com.techvibe.androidtools.databinding.ActivitySettingsBinding
import com.techvibe.androidtools.presentation.viewmodel.SettingsViewModel
import com.techvibe.androidtools.presentation.viewmodel.SettingsUiState
import com.techvibe.androidtools.utils.Constants.RINGTONE_PICKER_REQUEST_CODE
import com.techvibe.androidtools.utils.Constants.TAG_SETTINGS_ACTIVITY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Settings Activity for Battery Alert application
 * Handles notification settings, ringtone selection, and general preferences
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    
    // Local media player for previewing notification sound
    private var previewPlayer: MediaPlayer? = null
    
    /**
     * Activity result launcher for ringtone picker
     */
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            handleRingtoneSelection(uri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupStatusBar()
        setupToolbar()
        setupUI()
        setupObservers()
        loadInitialSettings()
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
        val statusBarHeight = getStatusBarHeight()
        binding.toolbar.setPadding(
            binding.toolbar.paddingLeft,
            binding.toolbar.paddingTop + statusBarHeight,
            binding.toolbar.paddingRight,
            binding.toolbar.paddingBottom
        )
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
     * Setup toolbar with back button
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        // Handle back button click
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    /**
     * Setup UI components and event listeners
     */
    private fun setupUI() {
        with(binding) {
            // Ringtone selection button
            btnSelectRingtone.setOnClickListener {
                openRingtonePicker()
            }
            
            // Volume SeekBar listener
            seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        tvVolumeValue.text = "$progress%"
                        viewModel.updateNotificationVolume(progress)
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val volume = seekBar?.progress ?: 80
                    lifecycleScope.launch {
                        viewModel.saveNotificationVolume(volume)
                    }
                }
            })
            
            // Test volume button: play current notification sound at selected volume
            btnTestVolume.setOnClickListener {
                testNotificationSound()
            }
            
            // Vibration switch
            switchVibration.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    viewModel.setVibrationEnabled(isChecked)
                }
            }
            
            // Repeat interval text change listener
            etRepeatInterval.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val intervalText = etRepeatInterval.text?.toString() ?: "30"
                    val interval = intervalText.toIntOrNull() ?: 30
                    validateAndSaveRepeatInterval(interval)
                }
            }
            
            // Reset settings button
            btnResetSettings.setOnClickListener {
                showResetConfirmationDialog()
            }
            
            // About button
            btnAbout.setOnClickListener {
                showAboutDialog()
            }
            
            // Save settings button
            btnSaveSettings.setOnClickListener {
                saveAllSettings()
            }
        }
    }
    
    /**
     * Setup observers for ViewModel LiveData
     */
    private fun setupObservers() {
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
        
        // Observe success messages
        viewModel.successMessage.observe(this) { successMessage ->
            if (successMessage.isNotEmpty()) {
                showToast(successMessage)
            }
        }
    }
    
    /**
     * Load initial settings from preferences
     */
    private fun loadInitialSettings() {
        lifecycleScope.launch {
            viewModel.loadInitialSettings()
        }
    }
    
    /**
     * Update UI with current settings state
     */
    private fun updateUI(uiState: SettingsUiState) {
        with(binding) {
            // Update ringtone display
            tvSelectedRingtone.text = uiState.selectedRingtoneName ?: "Default notification"
            
            // Update volume
            seekBarVolume.progress = uiState.notificationVolume
            tvVolumeValue.text = "${uiState.notificationVolume}%"
            
            // Update vibration
            switchVibration.isChecked = uiState.isVibrationEnabled
            
            // Update repeat interval
            etRepeatInterval.setText(uiState.repeatIntervalSeconds.toString())
        }
    }
    
    /**
     * Open system ringtone picker
     */
    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            // Restrict to system notification sounds only
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            // Allow selecting Silent from system list
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            // Current selection (if any)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, viewModel.getCurrentRingtoneUri())
        }
        
        try {
            ringtonePickerLauncher.launch(intent)
        } catch (e: Exception) {
            showToast(getString(R.string.error_ringtone_not_found))
        }
    }
    
    /**
     * Handle ringtone selection result
     */
    private fun handleRingtoneSelection(uri: Uri?) {
        lifecycleScope.launch {
            try {
                val ringtoneName = if (uri != null) {
                    RingtoneManager.getRingtone(this@SettingsActivity, uri)?.getTitle(this@SettingsActivity)
                        ?: "Custom ringtone"
                } else {
                    "Silent"
                }
                
                viewModel.saveRingtoneSelection(uri?.toString(), ringtoneName)
                
            } catch (e: Exception) {
                showToast(getString(R.string.error_ringtone_not_found))
            }
        }
    }
    
    /**
     * Validate and save repeat interval
     */
    private fun validateAndSaveRepeatInterval(interval: Int) {
        if (interval < 5 || interval > 300) {
            binding.tilRepeatInterval.error = getString(R.string.invalid_interval)
            binding.etRepeatInterval.setText("30") // Reset to default
        } else {
            binding.tilRepeatInterval.error = null
            lifecycleScope.launch {
                viewModel.saveRepeatInterval(interval)
            }
        }
    }
    
    /**
     * Show reset confirmation dialog
     */
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_reset_title)
            .setMessage(R.string.confirm_reset_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch {
                    viewModel.resetToDefaults()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    /**
     * Show about dialog
     */
    private fun showAboutDialog() {
        val message = """
            ${getString(R.string.app_description)}
            
            ${getString(R.string.version)}: 1.0
            ${getString(R.string.developer)}: TechVibe Team
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    /**
     * Play a short preview of the currently selected notification sound
     */
    private fun testNotificationSound() {
        val selectedUri: Uri? = viewModel.getCurrentRingtoneUri()
        val effectiveUri: Uri = selectedUri
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val volumePercent = viewModel.uiState.value?.notificationVolume ?: 80

        // Stop any previous preview
        stopPreview()

        try {
            previewPlayer = MediaPlayer().apply {
                setDataSource(this@SettingsActivity, effectiveUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                }
                val vol = (volumePercent.coerceIn(0, 100) / 100f)
                setVolume(vol, vol)
                isLooping = false
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stopPreview() }
                setOnErrorListener { _, _, _ ->
                    // If playback fails, fallback to system ringtone play
                    stopPreview()
                    try {
                        val fallback = RingtoneManager.getRingtone(
                            this@SettingsActivity,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        )
                        fallback?.play()
                    } catch (_: Exception) { }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            // Fallback to default system sound if MediaPlayer fails
            stopPreview()
            try {
                val fallback = RingtoneManager.getRingtone(
                    this,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
                fallback?.play()
            } catch (_: Exception) {
                showToast(getString(R.string.error_ringtone_not_found))
            }
        }
    }

    private fun stopPreview() {
        try {
            previewPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Exception) { }
        previewPlayer = null
    }
    
    /**
     * Save all current settings
     */
    private fun saveAllSettings() {
        // Validate repeat interval before saving
        val intervalText = binding.etRepeatInterval.text?.toString() ?: "30"
        val interval = intervalText.toIntOrNull() ?: 30
        
        if (interval < 5 || interval > 300) {
            binding.tilRepeatInterval.error = getString(R.string.invalid_interval)
            return
        }
        
        binding.tilRepeatInterval.error = null
        
        lifecycleScope.launch {
            viewModel.saveAllSettings()
        }
    }
    
    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }
}
