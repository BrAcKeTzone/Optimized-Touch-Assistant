package com.bracketzone.touchassistant.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.bracketzone.touchassistant.core.AssistantAction
import java.lang.ref.WeakReference

class TouchAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        // We don't need to inspect the screen.
        //
        // The service exists primarily so that we can
        // execute Android global accessibility actions.
    }

    override fun onInterrupt() {
        // Nothing to interrupt.
    }

    override fun onDestroy() {

        if (instance?.get() === this) {
            instance?.clear()
            instance = null
        }

        super.onDestroy()
    }

    fun performAssistantAction(
        action: AssistantAction
    ): Boolean {

        return when (action) {

            // ------------------------------------------
            // Navigation
            // ------------------------------------------

            AssistantAction.HOME -> {
                performGlobalAction(
                    GLOBAL_ACTION_HOME
                )
            }

            AssistantAction.BACK -> {
                performGlobalAction(
                    GLOBAL_ACTION_BACK
                )
            }

            AssistantAction.RECENTS -> {
                performGlobalAction(
                    GLOBAL_ACTION_RECENTS
                )
            }

            // ------------------------------------------
            // System panels
            // ------------------------------------------

            AssistantAction.NOTIFICATIONS -> {
                performGlobalAction(
                    GLOBAL_ACTION_NOTIFICATIONS
                )
            }

            AssistantAction.QUICK_SETTINGS -> {
                performGlobalAction(
                    GLOBAL_ACTION_QUICK_SETTINGS
                )
            }

            AssistantAction.POWER -> {
                performGlobalAction(
                    GLOBAL_ACTION_POWER_DIALOG
                )
            }

            // ------------------------------------------
            // Split screen
            // ------------------------------------------

            AssistantAction.SPLIT_SCREEN -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.N
                ) {

                    performGlobalAction(
                        GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
                    )

                } else {

                    false
                }
            }

            // ------------------------------------------
            // Lock screen
            // ------------------------------------------

            AssistantAction.LOCK -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.P
                ) {

                    performGlobalAction(
                        GLOBAL_ACTION_LOCK_SCREEN
                    )

                } else {

                    false
                }
            }

            // ------------------------------------------
            // Screenshot
            // ------------------------------------------

            AssistantAction.SCREENSHOT -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R
                ) {

                    performGlobalAction(
                        GLOBAL_ACTION_TAKE_SCREENSHOT
                    )

                } else {

                    false
                }
            }

            else -> {
                false
            }
        }
    }

    companion object {

        private var instance:
                WeakReference<TouchAccessibilityService>? = null

        fun getInstance():
                TouchAccessibilityService? {

            return instance?.get()
        }

        fun isEnabled(): Boolean {
            return instance?.get() != null
        }
    }
}