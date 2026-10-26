package com.techvibe.androidtools.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.presentation.ui.caffeine.CaffeineSettingsActivity
import com.techvibe.androidtools.utils.CaffeineManager

/**
 * CaffeineService - Foreground service that keeps caffeine alive and
 * turns it off automatically when the screen is turned off (device locked).
 */
class CaffeineService : Service() {

    companion object {
        private const val CHANNEL_ID = "caffeine_channel"
        private const val NOTIFICATION_ID = 3001
        const val ACTION_STOP = "com.techvibe.androidtools.action.CAFFEINE_STOP"
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                CaffeineManager.disable(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()
        if (intent?.action == ACTION_STOP) {
            CaffeineManager.disable(this)
            return START_NOT_STICKY
        }
        if (!CaffeineManager.isEnabled(this)) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.caffeine_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, CaffeineSettingsActivity::class.java), pendingFlags
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, CaffeineService::class.java).setAction(ACTION_STOP), pendingFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_coffee)
            .setContentTitle(getString(R.string.caffeine_notification_title))
            .setContentText(getString(R.string.caffeine_notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.caffeine_turn_off), stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
