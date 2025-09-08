package com.techvibe.androidtools.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.data.preferences.PreferencesHelper
import com.techvibe.androidtools.utils.BatteryOptimizationHelper
import com.techvibe.androidtools.utils.Constants.ACTION_BATTERY_THRESHOLD_REACHED
import com.techvibe.androidtools.utils.Constants.EXTRA_BATTERY_LEVEL
import com.techvibe.androidtools.utils.Constants.EXTRA_IS_CHARGING
import com.techvibe.androidtools.utils.Constants.SERVICE_NOTIFICATION_ID
import com.techvibe.androidtools.utils.Constants.TAG_BATTERY_SERVICE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Service for Battery Monitoring and Alert Management
 * Handles battery threshold alerts with sound, vibration, and notifications
 */
@AndroidEntryPoint
class BatteryMonitorService : Service() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var preferencesHelper: PreferencesHelper
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ringtoneHandler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isAlertActive = false
    private var currentBatteryLevel = 0
    private var currentThreshold = 80
    
    // Ringtone repeat runnable
    private val ringtoneRepeatRunnable = object : Runnable {
        override fun run() {
            if (isAlertActive) {
                playRingtone()
                val repeatInterval = (preferencesHelper.repeatIntervalSeconds * 1000).toLong()
                ringtoneHandler.postDelayed(this, repeatInterval)
            }
        }
    }
    
    companion object {
        private const val ACTION_DISMISS = "com.techvibe.androidtools.ACTION_DISMISS"
        private const val ACTION_SNOOZE = "com.techvibe.androidtools.ACTION_SNOOZE"
        private const val SNOOZE_DURATION_MINUTES = 5
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG_BATTERY_SERVICE, "Service created")
        setupVibrator()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG_BATTERY_SERVICE, "Service started with action: ${intent?.action}")
        Log.d(TAG_BATTERY_SERVICE, "Service startId: $startId, flags: $flags")
        
        // Check battery optimization status
        val optimizationStatus = BatteryOptimizationHelper.getBatteryOptimizationStatus(this)
        Log.d(TAG_BATTERY_SERVICE, "Battery optimization status: $optimizationStatus")
        
        val decision = BatteryOptimizationHelper.shouldServiceContinueRunning(this)
        when (decision) {
            com.techvibe.androidtools.utils.ServiceOptimizationDecision.MIGHT_BE_KILLED -> {
                Log.w(TAG_BATTERY_SERVICE, "Service might be killed due to battery optimizations")
                showToast("Vui lòng tắt tối ưu hóa pin cho ứng dụng để hoạt động tốt nhất")
            }
            com.techvibe.androidtools.utils.ServiceOptimizationDecision.CONTINUE_WITH_LIMITATIONS -> {
                Log.d(TAG_BATTERY_SERVICE, "Service running with limitations (Doze mode)")
            }
            com.techvibe.androidtools.utils.ServiceOptimizationDecision.REDUCE_ACTIVITY -> {
                Log.d(TAG_BATTERY_SERVICE, "Reducing service activity (Power save mode)")
            }
            else -> {
                Log.d(TAG_BATTERY_SERVICE, "Service running normally")
            }
        }
        
        when (intent?.action) {
            ACTION_BATTERY_THRESHOLD_REACHED -> {
                handleBatteryThresholdReached(intent)
            }
            ACTION_DISMISS -> {
                handleDismissAction()
            }
            ACTION_SNOOZE -> {
                handleSnoozeAction()
            }
            else -> {
                Log.w(TAG_BATTERY_SERVICE, "Unknown action: ${intent?.action}")
            }
        }
        
        return START_STICKY
    }
    
    /**
     * Handle battery threshold reached event
     */
    private fun handleBatteryThresholdReached(intent: Intent) {
        currentBatteryLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, 0)
        val isCharging = intent.getBooleanExtra(EXTRA_IS_CHARGING, false)
        currentThreshold = preferencesHelper.batteryThreshold
        
        Log.i(TAG_BATTERY_SERVICE, "Battery threshold reached: $currentBatteryLevel% (charging: $isCharging)")
        
        // Only show alert if still charging
        if (isCharging) {
            startForegroundService()
            startBatteryAlert()
        } else {
            Log.d(TAG_BATTERY_SERVICE, "Not charging, stopping service")
            stopSelf()
        }
    }
    
    /**
     * Handle dismiss action from notification
     */
    private fun handleDismissAction() {
        Log.d(TAG_BATTERY_SERVICE, "Dismiss action received")
        stopBatteryAlert()
        stopSelf()
        
        // Show toast on main thread
        handler.post {
            Toast.makeText(
                this,
                getString(R.string.monitoring_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Handle snooze action from notification
     */
    private fun handleSnoozeAction() {
        Log.d(TAG_BATTERY_SERVICE, "Snooze action received")
        stopBatteryAlert()
        
        // Schedule re-alert after snooze duration
        handler.postDelayed({
            if (isAlertActive) {
                startBatteryAlert()
            }
        }, SNOOZE_DURATION_MINUTES * 60 * 1000L)
        
        // Show toast on main thread
        handler.post {
            Toast.makeText(
                this,
                getString(R.string.notification_snoozed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Start foreground service with persistent notification
     */
    private fun startForegroundService() {
        try {
            Log.d(TAG_BATTERY_SERVICE, "Creating service notification...")
            val notification = notificationHelper.createServiceNotification()
            Log.d(TAG_BATTERY_SERVICE, "Starting foreground with notification ID: $SERVICE_NOTIFICATION_ID")
            startForeground(SERVICE_NOTIFICATION_ID, notification)
            Log.d(TAG_BATTERY_SERVICE, "Started foreground service successfully")
        } catch (e: Exception) {
            Log.e(TAG_BATTERY_SERVICE, "Failed to start foreground service", e)
        }
    }
    
    /**
     * Start battery alert with sound, vibration, and notification
     */
    private fun startBatteryAlert() {
        if (isAlertActive) {
            Log.d(TAG_BATTERY_SERVICE, "Alert already active")
            return
        }
        
        isAlertActive = true
        Log.i(TAG_BATTERY_SERVICE, "Starting battery alert for $currentBatteryLevel%")
        
        // Acquire wake lock to ensure alert is delivered
        acquireWakeLock()
        
        // Show notification
        notificationHelper.showBatteryAlert(currentBatteryLevel, currentThreshold)
        
        // Start ringtone
        playRingtone()
        
        // Start vibration
        startVibration()
        
        // Schedule repeat
        val repeatInterval = (preferencesHelper.repeatIntervalSeconds * 1000).toLong()
        ringtoneHandler.postDelayed(ringtoneRepeatRunnable, repeatInterval)
    }
    
    /**
     * Stop battery alert
     */
    private fun stopBatteryAlert() {
        if (!isAlertActive) return
        
        Log.d(TAG_BATTERY_SERVICE, "Stopping battery alert")
        isAlertActive = false
        
        // Stop ringtone
        stopRingtone()
        
        // Stop vibration
        stopVibration()
        
        // Cancel notification
        notificationHelper.cancelBatteryAlert()
        
        // Remove scheduled repeats
        ringtoneHandler.removeCallbacks(ringtoneRepeatRunnable)
        
        // Release wake lock
        releaseWakeLock()
    }
    
    /**
     * Setup vibrator based on Android version
     */
    private fun setupVibrator() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = ContextCompat.getSystemService(this, VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(this, Vibrator::class.java)
        }
    }
    
    /**
     * Play ringtone based on user settings
     */
    private fun playRingtone() {
        try {
            stopRingtone() // Stop any existing playback
            
            val ringtoneUri = getRingtoneUri()
            val volume = preferencesHelper.notificationVolume / 100f
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@BatteryMonitorService, ringtoneUri)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }
                
                setVolume(volume, volume)
                isLooping = false // We handle looping with handler
                
                prepareAsync()
                setOnPreparedListener { player ->
                    if (isAlertActive) {
                        player.start()
                        Log.d(TAG_BATTERY_SERVICE, "Ringtone started playing")
                    }
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG_BATTERY_SERVICE, "MediaPlayer error: what=$what, extra=$extra")
                    true
                }
                
                setOnCompletionListener {
                    Log.d(TAG_BATTERY_SERVICE, "Ringtone playback completed")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG_BATTERY_SERVICE, "Failed to play ringtone", e)
            // Fallback to default sound
            playDefaultSound()
        }
    }
    
    /**
     * Stop ringtone playback
     */
    private fun stopRingtone() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d(TAG_BATTERY_SERVICE, "Ringtone stopped and released")
            } catch (e: Exception) {
                Log.w(TAG_BATTERY_SERVICE, "Error stopping ringtone", e)
            }
        }
        mediaPlayer = null
    }
    
    /**
     * Get ringtone URI from preferences or default
     */
    private fun getRingtoneUri(): Uri {
        val savedUri = preferencesHelper.notificationSoundUri
        return if (savedUri != null) {
            Uri.parse(savedUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
    
    /**
     * Play default system sound as fallback
     */
    private fun playDefaultSound() {
        try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, defaultUri)
            ringtone?.play()
            Log.d(TAG_BATTERY_SERVICE, "Playing default sound")
        } catch (e: Exception) {
            Log.e(TAG_BATTERY_SERVICE, "Failed to play default sound", e)
        }
    }
    
    /**
     * Start vibration if enabled
     */
    private fun startVibration() {
        if (!preferencesHelper.isVibrationEnabled) {
            Log.d(TAG_BATTERY_SERVICE, "Vibration disabled in settings")
            return
        }
        
        vibrator?.let { vib ->
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 250, 500, 250, 500),
                        -1 // Don't repeat
                    )
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 500, 250, 500, 250, 500), -1)
                }
                Log.d(TAG_BATTERY_SERVICE, "Vibration started")
            } catch (e: Exception) {
                Log.e(TAG_BATTERY_SERVICE, "Failed to start vibration", e)
            }
        }
    }
    
    /**
     * Stop vibration
     */
    private fun stopVibration() {
        vibrator?.let { vib ->
            try {
                vib.cancel()
                Log.d(TAG_BATTERY_SERVICE, "Vibration stopped")
            } catch (e: Exception) {
                Log.w(TAG_BATTERY_SERVICE, "Error stopping vibration", e)
            }
        }
    }
    
    /**
     * Acquire wake lock to ensure alert is delivered even in Doze mode
     */
    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = ContextCompat.getSystemService(this, PowerManager::class.java)
                val flags = BatteryOptimizationHelper.getWakeLockFlags(this)
                wakeLock = powerManager?.newWakeLock(flags, "BatteryAlert::AlertWakeLock")
            }
            
            wakeLock?.let { wl ->
                if (!wl.isHeld) {
                    val timeout = BatteryOptimizationHelper.getWakeLockTimeout(this)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.DONUT) {
                        wl.acquire(timeout)
                    } else {
                        @Suppress("DEPRECATION")
                        wl.acquire()
                    }
                    Log.d(TAG_BATTERY_SERVICE, "Wake lock acquired with timeout: ${timeout}ms")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_BATTERY_SERVICE, "Failed to acquire wake lock", e)
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let { wl ->
                if (wl.isHeld) {
                    wl.release()
                    Log.d(TAG_BATTERY_SERVICE, "Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG_BATTERY_SERVICE, "Error releasing wake lock", e)
        }
    }
    
    /**
     * Show toast message on main thread
     */
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG_BATTERY_SERVICE, "Service destroyed")
        
        // Clean up
        stopBatteryAlert()
        releaseWakeLock()
        handler.removeCallbacksAndMessages(null)
        ringtoneHandler.removeCallbacksAndMessages(null)
    }
}
