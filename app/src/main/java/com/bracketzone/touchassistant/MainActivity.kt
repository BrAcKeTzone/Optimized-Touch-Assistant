package com.bracketzone.touchassistant

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bracketzone.touchassistant.service.FloatingWidgetService
import com.bracketzone.touchassistant.service.TouchAccessibilityService
import com.bracketzone.touchassistant.ui.ActionsActivity

class MainActivity : Activity() {

    private lateinit var serviceStatusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var actionsButton: Button
    private lateinit var iconLoopView: ImageView

    // Array of system drawable icon resources to cycle through
    private val iconDrawables = intArrayOf(
        android.R.drawable.ic_menu_camera,
        android.R.drawable.ic_menu_gallery,
        android.R.drawable.ic_menu_manage,
        android.R.drawable.ic_menu_preferences,
        android.R.drawable.ic_menu_search,
        android.R.drawable.ic_menu_send,
        android.R.drawable.ic_menu_share,
        android.R.drawable.ic_menu_view
    )

    private var currentIconIndex = 0
    private val iconHandler = Handler(Looper.getMainLooper())
    private val iconRunnable = object : Runnable {
        override fun run() {
            if (iconDrawables.isNotEmpty()) {
                iconLoopView.setImageResource(iconDrawables[currentIconIndex])
                currentIconIndex = (currentIconIndex + 1) % iconDrawables.size
            }
            iconHandler.postDelayed(this, 1000L) // Switch icon every 1 second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceStatusText = findViewById(R.id.serviceStatusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        actionsButton = findViewById(R.id.actionsButton)
        iconLoopView = findViewById(R.id.iconLoopView)

        startButton.setOnClickListener {
            checkPermissionsAndStart()
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, FloatingWidgetService::class.java))
            updateUIState()
        }

        actionsButton.setOnClickListener {
            startActivity(Intent(this, ActionsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        iconHandler.post(iconRunnable) // Start loop when activity is visible
    }

    override fun onPause() {
        super.onPause()
        iconHandler.removeCallbacks(iconRunnable) // Stop loop to conserve memory
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.allow_overlay_permission), Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, getString(R.string.enable_accessibility), Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        startFloatingWidgetService()
        updateUIState()
    }

    private fun updateUIState() {
        val isRunning = isServiceRunning(FloatingWidgetService::class.java)
        if (isRunning) {
            serviceStatusText.text = getString(R.string.reachpoint_is_running)
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
        } else {
            serviceStatusText.text = getString(R.string.reachpoint_is_stopped)
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, TouchAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { service ->
            ComponentName.unflattenFromString(service) == expectedComponent
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startFloatingWidgetService() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}