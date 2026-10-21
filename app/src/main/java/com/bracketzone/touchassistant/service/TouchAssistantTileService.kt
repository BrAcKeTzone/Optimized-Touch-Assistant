package com.bracketzone.touchassistant.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.bracketzone.touchassistant.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class TouchAssistantTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Check if mandatory permissions (Overlay & Accessibility) are granted
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val isAccessibilityEnabled = TouchAccessibilityService.isEnabled()

        if (!hasOverlayPermission || !isAccessibilityEnabled) {
            launchApp()
            return
        }

        // 2. Toggle Service State
        val isServiceRunning = FloatingWidgetService.isServiceRunning(this)

        if (isServiceRunning) {
            val intent = Intent(this, FloatingWidgetService::class.java)
            stopService(intent)
        } else {
            val intent = Intent(this, FloatingWidgetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        // 3. Post state update after a short delay
        handler.postDelayed({
            updateTileState()
        }, 300L)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        // Programmatically set a clean circular icon to prevent white-square/tint issues
        tile.icon = android.graphics.drawable.Icon.createWithResource(
            this,
            android.R.drawable.presence_online // Or use a custom circular drawable if you prefer
        )

        // Alternatively, use a custom shape drawable circle programmatically:
        // tile.icon = android.graphics.drawable.Icon.createWithBitmap(createCircleBitmap())

        val isRunning = TouchAccessibilityService.isEnabled() && FloatingWidgetService.isServiceRunning(this)

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.subtitle = "Enabled"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = "Disabled"
        }

        tile.updateTile()
    }

    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}