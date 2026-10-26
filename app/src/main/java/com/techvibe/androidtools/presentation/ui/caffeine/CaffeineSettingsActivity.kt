package com.techvibe.androidtools.presentation.ui.caffeine

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.techvibe.androidtools.R
import com.techvibe.androidtools.databinding.ActivityCaffeineSettingsBinding
import com.techvibe.androidtools.service.CaffeineTileService
import com.techvibe.androidtools.utils.CaffeineManager

/**
 * Caffeine Settings Activity
 * Keep the screen always on, toggle from app or Quick Settings tile
 */
class CaffeineSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaffeineSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaffeineSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh state: permission may have been granted, or caffeine toggled from the tile
        refreshUI()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnGrantPermission.setOnClickListener {
            startActivity(CaffeineManager.permissionIntent(this))
        }

        binding.switchCaffeine.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            if (isChecked) {
                if (!CaffeineManager.enable(this)) {
                    Toast.makeText(this, R.string.caffeine_permission_message, Toast.LENGTH_SHORT).show()
                }
            } else {
                CaffeineManager.disable(this)
            }
            refreshUI()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.btnAddTile.setOnClickListener { requestAddTile() }
        } else {
            binding.btnAddTile.visibility = View.GONE
        }
    }

    private fun refreshUI() {
        val canWrite = CaffeineManager.canWriteSettings(this)
        binding.switchCaffeine.isChecked = CaffeineManager.isEnabled(this)
        binding.switchCaffeine.isEnabled = canWrite
        binding.tvPermissionStatus.visibility = if (canWrite) View.GONE else View.VISIBLE
        binding.btnGrantPermission.visibility = if (canWrite) View.GONE else View.VISIBLE
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, CaffeineTileService::class.java),
            getString(R.string.caffeine_tile_label),
            Icon.createWithResource(this, R.drawable.ic_coffee),
            mainExecutor
        ) {}
    }
}
