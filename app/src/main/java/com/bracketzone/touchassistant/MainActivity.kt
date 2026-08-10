package com.bracketzone.touchassistant

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.bracketzone.touchassistant.service.FloatingWidgetService
import com.bracketzone.touchassistant.service.TouchAccessibilityService
import com.bracketzone.touchassistant.ui.ActionsActivity

class MainActivity : Activity() {

    private lateinit var overlaySetupCard: LinearLayout
    private lateinit var accessibilitySetupCard: LinearLayout
    private lateinit var mainControlCard: LinearLayout

    private lateinit var overlayButton: Button
    private lateinit var accessibilityButton: Button

    private lateinit var serviceStatusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var actionsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlaySetupCard = findViewById(R.id.overlaySetupCard)
        accessibilitySetupCard = findViewById(R.id.accessibilitySetupCard)
        mainControlCard = findViewById(R.id.mainControlCard)

        overlayButton = findViewById(R.id.overlayButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)

        serviceStatusText = findViewById(R.id.serviceStatusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        actionsButton = findViewById(R.id.actionsButton)

        overlayButton.setOnClickListener { openOverlaySettings() }
        accessibilityButton.setOnClickListener { openAccessibilitySettings() }

        startButton.setOnClickListener {
            startFloatingWidgetService()
            updateUIState()
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
    }

    private fun updateUIState() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityServiceEnabled()

        if (!hasOverlay) {
            overlaySetupCard.visibility = View.VISIBLE
            accessibilitySetupCard.visibility = View.GONE
            mainControlCard.visibility = View.GONE
        } else if (!hasAccessibility) {
            overlaySetupCard.visibility = View.GONE
            accessibilitySetupCard.visibility = View.VISIBLE
            mainControlCard.visibility = View.GONE
        } else {
            overlaySetupCard.visibility = View.GONE
            accessibilitySetupCard.visibility = View.GONE
            mainControlCard.visibility = View.VISIBLE

            val isRunning = isServiceRunning(FloatingWidgetService::class.java)
            if (isRunning) {
                serviceStatusText.text = "✓ Touch Assistant is Running"
                startButton.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
            } else {
                serviceStatusText.text = "Touch Assistant is Stopped"
                startButton.visibility = View.VISIBLE
                stopButton.visibility = View.GONE
            }
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