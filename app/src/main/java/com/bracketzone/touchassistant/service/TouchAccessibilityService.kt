package com.bracketzone.touchassistant.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.bracketzone.touchassistant.core.AssistantAction
import java.lang.ref.WeakReference

class TouchAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)

        // Automatically start the floating widget service when accessibility is enabled
        val intent = Intent(this, FloatingWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onDestroy() {
        if (instance?.get() === this) {
            instance?.clear()
            instance = null
        }

        // Automatically stop the floating widget service when accessibility is disabled
        stopService(Intent(this, FloatingWidgetService::class.java))

        super.onDestroy()
    }

    fun performAssistantAction(action: AssistantAction): Boolean {
        return when (action) {
            AssistantAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            AssistantAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            AssistantAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            AssistantAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            AssistantAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            AssistantAction.POWER -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            AssistantAction.LOCK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else false
            }
            AssistantAction.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else false
            }
            else -> false
        }
    }

    companion object {
        private var instance: WeakReference<TouchAccessibilityService>? = null

        fun getInstance(): TouchAccessibilityService? = instance?.get()

        fun isEnabled(): Boolean = instance?.get() != null
    }
}