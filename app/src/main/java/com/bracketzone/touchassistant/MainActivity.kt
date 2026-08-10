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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceStatusText = findViewById(R.id.serviceStatusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        actionsButton = findViewById(R.id.actionsButton)

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
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow Display Overlay permission", Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable Touch Assistant in Accessibility Settings", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        startFloatingWidgetService()
        updateUIState()
    }

    private fun updateUIState() {
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