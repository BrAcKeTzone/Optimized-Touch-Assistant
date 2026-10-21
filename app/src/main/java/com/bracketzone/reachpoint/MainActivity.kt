package com.bracketzone.reachpoint

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
import androidx.core.net.toUri
import com.bracketzone.reachpoint.core.ConfigManager
import com.bracketzone.reachpoint.service.FloatingWidgetService
import com.bracketzone.reachpoint.service.TouchAccessibilityService
import com.bracketzone.reachpoint.ui.ActionsActivity
import java.io.BufferedReader
import java.io.InputStreamReader

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
            iconHandler.postDelayed(this, 1000L)
        }
    }

    companion object {
        private const val REQUEST_CODE_EXPORT = 1001
        private const val REQUEST_CODE_IMPORT = 1002
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

        // Optional: If you have export/import buttons in your layout, wire them up here:
         findViewById<Button>(R.id.exportButton)?.setOnClickListener { exportConfig() }
         findViewById<Button>(R.id.importButton)?.setOnClickListener { importConfig() }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        iconHandler.post(iconRunnable)
    }

    override fun onPause() {
        super.onPause()
        iconHandler.removeCallbacks(iconRunnable)
    }

    // --- Export / Import triggers ---

    private fun exportConfig() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "touch_assistant_layout.json")
        }
        startActivityForResult(intent, REQUEST_CODE_EXPORT)
    }

    private fun importConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_CODE_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val uri: Uri = data.data ?: return
            try {
                when (requestCode) {
                    REQUEST_CODE_EXPORT -> {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val jsonString = ConfigManager.exportToJson(this)
                            outputStream.write(jsonString.toByteArray())
                            Toast.makeText(this, "Layout exported successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    REQUEST_CODE_IMPORT -> {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val reader = BufferedReader(InputStreamReader(inputStream))
                            val stringBuilder = StringBuilder()
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                            val success = ConfigManager.importFromJson(this, stringBuilder.toString())
                            if (success) {
                                Toast.makeText(this, "Layout imported successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to parse configuration file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
            "package:$packageName".toUri()
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