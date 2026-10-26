package com.techvibe.androidtools.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.techvibe.androidtools.utils.CaffeineManager

/**
 * CaffeineTileService - Quick Settings tile to toggle caffeine without opening the app
 */
@RequiresApi(Build.VERSION_CODES.N)
class CaffeineTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!CaffeineManager.isEnabled(this) && !CaffeineManager.canWriteSettings(this)) {
            openPermissionSettings()
            return
        }
        CaffeineManager.toggle(this)
        updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openPermissionSettings() {
        val intent = CaffeineManager.permissionIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (CaffeineManager.isEnabled(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
