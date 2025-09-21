package com.techvibe.androidtools.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.data.preferences.FloatVolumePreferences
import kotlin.math.abs

/**
 * FloatingVolumeService - Service to display floating volume buttons overlay
 */
class FloatingVolumeService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var volumeButton: ImageButton? = null
    private var audioManager: AudioManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var preferences: FloatVolumePreferences? = null
    private var displayMetrics: DisplayMetrics? = null
    private var snapHandler = Handler(Looper.getMainLooper())
    private var snapRunnable: Runnable? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val ACTION_START_FLOATING = "START_FLOATING"
        const val ACTION_STOP_FLOATING = "STOP_FLOATING"
        const val ACTION_UPDATE_SETTINGS = "UPDATE_SETTINGS"
        private const val SNAP_DELAY = 1500L // Delay before snapping to edge (1.5 seconds)
        private const val TAG = "FloatingVolumeService"
        
        fun startFloating(context: Context) {
            val intent = Intent(context, FloatingVolumeService::class.java)
            intent.action = ACTION_START_FLOATING
            context.startService(intent)
        }
        
        fun stopFloating(context: Context) {
            val intent = Intent(context, FloatingVolumeService::class.java)
            intent.action = ACTION_STOP_FLOATING
            context.startService(intent)
        }
        
        fun updateSettings(context: Context) {
            val intent = Intent(context, FloatingVolumeService::class.java)
            intent.action = ACTION_UPDATE_SETTINGS
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        preferences = FloatVolumePreferences(this)
        preferences?.registerOnChangeListener(this)
        
        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        updateDisplayMetrics()
    }
    
    private fun updateDisplayMetrics() {
        displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        Log.d(TAG, "Updated display metrics - W: ${displayMetrics?.widthPixels}, H: ${displayMetrics?.heightPixels}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FLOATING -> {
                if (floatingView == null) {
                    createFloatingButton()
                }
            }
            ACTION_STOP_FLOATING -> {
                removeFloatingButton()
                stopSelf()
            }
            ACTION_UPDATE_SETTINGS -> {
                updateFloatingButtonAppearance()
            }
        }
        return START_STICKY
    }

    private fun createFloatingButton() {
        try {
            Log.d(TAG, "Creating floating button")
            
            // Inflate the floating layout
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_volume, null)
            volumeButton = floatingView?.findViewById(R.id.btnVolume)

            // Set up window layout parameters
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = preferences?.positionX ?: 0
                y = preferences?.positionY ?: 100
            }

            // Set up volume button
            setupVolumeButton()
            
            // Update appearance based on settings
            updateFloatingButtonAppearance()

            // Add the view to window manager
            windowManager?.addView(floatingView, layoutParams)
            Log.d(TAG, "Added floating view to window manager")

            // Make the view draggable with snap to edge
            makeViewDraggableWithSnap(floatingView!!, layoutParams!!)
            Log.d(TAG, "Set up drag functionality")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating button", e)
            e.printStackTrace()
        }
    }

    private fun setupVolumeButton() {
        // Disable onClick listener to let OnTouchListener handle everything
        volumeButton?.setOnClickListener(null)
        
        // Ensure button can receive touch events but don't consume them
        volumeButton?.isClickable = false
        volumeButton?.isFocusable = false
        volumeButton?.isLongClickable = false
        
        Log.d(TAG, "Volume button setup - clickable: ${volumeButton?.isClickable}, focusable: ${volumeButton?.isFocusable}")
    }

    private fun showSystemVolumeControl() {
        // Trigger light vibration before showing volume control
        performLightVibration()
        
        // Show system volume control by adjusting volume with 0 change but show UI
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }
    
    /**
     * Perform light vibration when floating volume button is clicked
     */
    private fun performLightVibration() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Use VibrationEffect for API 26+
                    val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(vibrationEffect)
                } else {
                    // Use deprecated method for older versions
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during vibration", e)
        }
    }

    private fun makeViewDraggableWithSnap(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            Log.d(TAG, "Touch event: ${event.action}, rawX: ${event.rawX}, rawY: ${event.rawY}")
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "ACTION_DOWN - Starting touch")
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    
                    Log.d(TAG, "Initial position - X: $initialX, Y: $initialY")
                    
                    // Cancel pending snap
                    snapRunnable?.let { snapHandler.removeCallbacks(it) }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    
                    Log.d(TAG, "ACTION_MOVE - deltaX: $deltaX, deltaY: $deltaY, isDragging: $isDragging")
                    
                    if (!isDragging && (deltaX > 5 || deltaY > 5)) {
                        isDragging = true
                        Log.d(TAG, "Started dragging!")
                    }
                    
                    if (isDragging) {
                        val newX = initialX + (event.rawX - initialTouchX).toInt()
                        val newY = initialY + (event.rawY - initialTouchY).toInt()
                        
                        Log.d(TAG, "Dragging to - newX: $newX, newY: $newY")
                        
                        params.x = newX
                        params.y = newY
                        
                        // Ensure button stays within screen bounds during drag
                        displayMetrics?.let { metrics ->
                            val buttonWidth = 60.dpToPx()
                            val buttonHeight = 60.dpToPx()
                            val maxX = metrics.widthPixels - buttonWidth
                            val maxY = metrics.heightPixels - buttonHeight - 100
                            
                            params.x = params.x.coerceIn(0, maxX)
                            params.y = params.y.coerceIn(0, maxY)
                            
                            Log.d(TAG, "Bounded position - X: ${params.x}, Y: ${params.y}, screenW: ${metrics.widthPixels}, screenH: ${metrics.heightPixels}")
                        }
                        
                        try {
                            windowManager?.updateViewLayout(view, params)
                            Log.d(TAG, "Updated view layout successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating view layout", e)
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "ACTION_UP/CANCEL - isDragging: $isDragging")
                    
                    if (isDragging) {
                        Log.d(TAG, "Finished dragging - final position X: ${params.x}, Y: ${params.y}")
                        
                        // Save current position
                        preferences?.positionX = params.x
                        preferences?.positionY = params.y
                        
                        // Schedule snap to edge after delay (user can still position anywhere)
                        scheduleSnapToEdge(params)
                        isDragging = false
                        true
                    } else {
                        Log.d(TAG, "Was a click, not drag - showing volume control")
                        // It was a click, not a drag - handle volume control directly
                        showSystemVolumeControl()
                        true
                    }
                }
                else -> {
                    Log.d(TAG, "Other touch event: ${event.action}")
                    false
                }
            }
        }
    }
    
    private fun scheduleSnapToEdge(params: WindowManager.LayoutParams) {
        snapRunnable?.let { 
            snapHandler.removeCallbacks(it)
            Log.d(TAG, "Cancelled previous snap")
        }
        
        Log.d(TAG, "Scheduling snap to edge in ${SNAP_DELAY}ms")
        
        snapRunnable = Runnable {
            Log.d(TAG, "Executing snap to edge")
            snapToEdge(params)
        }
        
        snapHandler.postDelayed(snapRunnable!!, SNAP_DELAY)
    }
    
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        displayMetrics?.let { metrics ->
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val buttonWidth = volumeButton?.width ?: 60.dpToPx()
            val buttonHeight = volumeButton?.height ?: 60.dpToPx()
            
            // Ensure button stays within screen bounds
            val maxX = screenWidth - buttonWidth
            val maxY = screenHeight - buttonHeight - 100 // Leave space for navigation bar
            
            // Keep button within bounds
            params.x = params.x.coerceIn(0, maxX)
            params.y = params.y.coerceIn(0, maxY)
            
            // Determine which edge is closer
            val distanceToLeft = params.x
            val distanceToRight = screenWidth - params.x - buttonWidth
            
            // Only snap to edge if button is not already at edge
            Log.d(TAG, "Snap check - distanceToLeft: $distanceToLeft, distanceToRight: $distanceToRight")
            
            if (distanceToLeft > 10 && distanceToRight > 10) {
                // Snap to the closer edge with smooth animation
                val targetX = if (distanceToLeft < distanceToRight) {
                    Log.d(TAG, "Snapping to left edge")
                    0 // Snap to left edge
                } else {
                    Log.d(TAG, "Snapping to right edge")
                    screenWidth - buttonWidth // Snap to right edge
                }
                
                Log.d(TAG, "Animating from X:${params.x} to X:$targetX")
                // Animate to edge
                animateToPosition(params, targetX, params.y)
            } else {
                Log.d(TAG, "Button already at edge, no snap needed")
            }
        }
    }
    
    private fun animateToPosition(params: WindowManager.LayoutParams, targetX: Int, targetY: Int) {
        val startX = params.x
        val startY = params.y
        val duration = 300L
        val startTime = System.currentTimeMillis()
        
        val animateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                
                if (progress < 1f) {
                    // Interpolate position
                    params.x = (startX + (targetX - startX) * progress).toInt()
                    params.y = (startY + (targetY - startY) * progress).toInt()
                    
                    floatingView?.let { view ->
                        windowManager?.updateViewLayout(view, params)
                    }
                    
                    // Continue animation
                    snapHandler.postDelayed(this, 16) // ~60fps
                } else {
                    // Animation complete
                    params.x = targetX
                    params.y = targetY
                    
                    floatingView?.let { view ->
                        windowManager?.updateViewLayout(view, params)
                        preferences?.positionX = params.x
                        preferences?.positionY = params.y
                    }
                }
            }
        }
        
        snapHandler.post(animateRunnable)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * (displayMetrics?.density ?: 1f)).toInt()
    }

    private fun updateFloatingButtonAppearance() {
        preferences?.let { prefs ->
            volumeButton?.let { button ->
                // Update size
                val newSize = prefs.buttonSize.dpToPx()
                val layoutParams = button.layoutParams
                layoutParams.width = newSize
                layoutParams.height = newSize
                button.layoutParams = layoutParams
                
                // Update opacity
                val opacity = prefs.buttonOpacity / 100f
                button.alpha = opacity
            }
        }
    }
    
    private fun removeFloatingButton() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingView = null
        volumeButton = null
        layoutParams = null
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences?.unregisterOnChangeListener(this)
        snapRunnable?.let { snapHandler.removeCallbacks(it) }
        removeFloatingButton()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed - orientation: ${newConfig.orientation}")
        
        // Update display metrics for new screen dimensions
        updateDisplayMetrics()
        
        // Immediately adjust button position for new screen size
        layoutParams?.let { params ->
            floatingView?.let { view ->
                adjustPositionForNewScreen(params)
                windowManager?.updateViewLayout(view, params)
            }
        }
    }
    
    private fun adjustPositionForNewScreen(params: WindowManager.LayoutParams) {
        displayMetrics?.let { metrics ->
            val buttonWidth = 60.dpToPx()
            val buttonHeight = 60.dpToPx()
            val maxX = metrics.widthPixels - buttonWidth
            val maxY = metrics.heightPixels - buttonHeight - 100
            
            // Keep button within new screen bounds
            params.x = params.x.coerceIn(0, maxX)
            params.y = params.y.coerceIn(0, maxY)
            
            // If button was on right edge before rotation, keep it on right edge
            val wasOnRightEdge = params.x > (metrics.widthPixels * 0.6) // If was on right 40% of screen
            if (wasOnRightEdge) {
                params.x = maxX // Snap to new right edge
                Log.d(TAG, "Adjusted to right edge after rotation - new X: ${params.x}")
            }
            
            // Save adjusted position
            preferences?.positionX = params.x
            preferences?.positionY = params.y
            
            Log.d(TAG, "Adjusted position for new screen - X: ${params.x}, Y: ${params.y}, maxX: $maxX")
        }
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "float_volume_size", "float_volume_opacity" -> {
                updateFloatingButtonAppearance()
            }
        }
    }
}
