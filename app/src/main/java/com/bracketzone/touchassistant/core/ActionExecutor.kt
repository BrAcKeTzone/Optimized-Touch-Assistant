package com.bracketzone.touchassistant.core

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.bracketzone.touchassistant.service.TouchAccessibilityService
import com.bracketzone.touchassistant.ui.VolumeSliderManager

class ActionExecutor(
    private val context: Context,
    private val volumeSliderManager: VolumeSliderManager = VolumeSliderManager(context)
) {

    private val audioManager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    fun execute(
        action: AssistantAction,
        anchorView: View? = null
    ) {

        // First, check whether this is an
        // Accessibility Service action.
        if (executeAccessibilityAction(action)) {
            return
        }

        when (action) {

            // ------------------------------------------
            // Audio
            // ------------------------------------------

            AssistantAction.VOLUME_UP -> {
                volumeUp(anchorView)
            }

            AssistantAction.VOLUME_DOWN -> {
                volumeDown(anchorView)
            }

            AssistantAction.MUTE_UNMUTE -> {
                toggleMute()
            }


            // ------------------------------------------
            // Hardware
            // ------------------------------------------

            AssistantAction.FLASHLIGHT -> {
                toggleFlashlight()
            }

            // ------------------------------------------
            // Display
            // ------------------------------------------

            AssistantAction.AUTO_ROTATE -> {
                toggleAutoRotate()
            }

            AssistantAction.MAX_BRIGHTNESS -> {
                setMaximumBrightness()
            }

            // ------------------------------------------
            // Connectivity
            // ------------------------------------------

            AssistantAction.WI_FI -> {
                openSettings(
                    Settings.ACTION_WIFI_SETTINGS
                )
            }

            AssistantAction.BLUETOOTH -> {
                openSettings(
                    Settings.ACTION_BLUETOOTH_SETTINGS
                )
            }

            AssistantAction.LOCATION -> {
                openSettings(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
            }


            AssistantAction.MOBILE_DATA -> {
                openMobileNetworkSettings()
            }

            // ------------------------------------------
            // No direct action
            // ------------------------------------------

            AssistantAction.NONE,
            AssistantAction.MAIN -> {
                // These are handled by the UI/service.
            }

            else -> {
                // Accessibility actions were already
                // handled above.
            }
        }
    }

    // ==================================================
    // ACCESSIBILITY ACTIONS
    // ==================================================

    private fun executeAccessibilityAction(
        action: AssistantAction
    ): Boolean {

        return when (action) {

            AssistantAction.HOME,
            AssistantAction.BACK,
            AssistantAction.RECENTS,
            AssistantAction.NOTIFICATIONS,
            AssistantAction.QUICK_SETTINGS,
            AssistantAction.POWER,
            AssistantAction.LOCK,
            AssistantAction.SCREENSHOT -> {

                val service =
                    TouchAccessibilityService
                        .getInstance()

                if (service == null) {

                    showToast(
                        "Enable Touch Assistant Accessibility Service"
                    )

                    false

                } else {

                    service.performAssistantAction(
                        action
                    )
                }
            }

            else -> {
                false
            }
        }
    }

    // ==================================================
    // VOLUME
    // ==================================================

    private fun volumeUp(anchorView: View?) {
        volumeSliderManager.adjustVolume(AudioManager.ADJUST_RAISE, anchorView)
    }

    private fun volumeDown(anchorView: View?) {
        volumeSliderManager.adjustVolume(AudioManager.ADJUST_LOWER, anchorView)
    }

    // ==================================================
    // MUTE
    // ==================================================

    private fun toggleMute() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_RING,
                AudioManager.ADJUST_TOGGLE_MUTE,
                0
            )

        } else {

            @Suppress("DEPRECATION")
            audioManager.setStreamMute(
                AudioManager.STREAM_RING,
                !isRingStreamMuted()
            )
        }
    }

    private fun isRingStreamMuted(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            audioManager.isStreamMute(
                AudioManager.STREAM_RING
            )

        } else {

            false
        }
    }


    // ==================================================
    // FLASHLIGHT
    // ==================================================

    private fun toggleFlashlight() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M
        ) {

            showToast(
                "Flashlight requires Android 6.0+"
            )

            return
        }

        try {

            val cameraManager =
                context.getSystemService(
                    Context.CAMERA_SERVICE
                ) as android.hardware.camera2.CameraManager

            val cameraId =
                cameraManager.cameraIdList
                    .firstOrNull { id ->

                        val characteristics =
                            cameraManager
                                .getCameraCharacteristics(id)

                        characteristics.get(
                            android.hardware.camera2
                                .CameraCharacteristics
                                .FLASH_INFO_AVAILABLE
                        ) == true
                    }

            if (cameraId == null) {

                showToast(
                    "No flashlight available"
                )

                return
            }

            val preferences =
                context.getSharedPreferences(
                    FLASHLIGHT_PREFS,
                    Context.MODE_PRIVATE
                )

            val currentState =
                preferences.getBoolean(
                    KEY_FLASHLIGHT,
                    false
                )

            val newState =
                !currentState

            cameraManager.setTorchMode(
                cameraId,
                newState
            )

            preferences.edit()
                .putBoolean(
                    KEY_FLASHLIGHT,
                    newState
                )
                .apply()

        } catch (
            _: SecurityException
        ) {

            showToast(
                "Camera permission is required"
            )

        } catch (
            _: Exception
        ) {

            showToast(
                "Unable to control flashlight"
            )
        }
    }

    // ==================================================
    // AUTO ROTATE
    // ==================================================

    private fun toggleAutoRotate() {

        if (
            !Settings.System.canWrite(context)
        ) {

            openWriteSettingsPermission()

            return
        }

        try {

            val current =
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    1
                )

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (current == 1) 0 else 1
            )

        } catch (
            _: Exception
        ) {

            showToast(
                "Unable to change auto-rotate"
            )
        }
    }

    // ==================================================
    // MAXIMUM BRIGHTNESS
    // ==================================================

    private fun setMaximumBrightness() {

        if (
            !Settings.System.canWrite(context)
        ) {

            openWriteSettingsPermission()

            return
        }

        try {

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                255
            )

        } catch (
            _: Exception
        ) {

            showToast(
                "Unable to change brightness"
            )
        }
    }

    // ==================================================
    // SETTINGS
    // ==================================================

    private fun openSettings(
        action: String
    ) {

        try {

            val intent =
                Intent(action).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

            context.startActivity(intent)

        } catch (
            _: Exception
        ) {

            openGeneralSettings()
        }
    }

    private fun openMobileNetworkSettings() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_NETWORK_OPERATOR_SETTINGS
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

            context.startActivity(intent)

        } catch (
            _: Exception
        ) {

            openSettings(
                Settings.ACTION_WIRELESS_SETTINGS
            )
        }
    }

    private fun openGeneralSettings() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_SETTINGS
                ).apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

            context.startActivity(intent)

        } catch (
            _: Exception
        ) {

            showToast(
                "Unable to open Settings"
            )
        }
    }

    // ==================================================
    // WRITE SETTINGS
    // ==================================================

    private fun openWriteSettingsPermission() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS
                ).apply {

                    data = Uri.parse(
                        "package:${context.packageName}"
                    )

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }

            context.startActivity(intent)

        } catch (
            _: Exception
        ) {

            showToast(
                "Allow Modify System Settings"
            )
        }
    }

    // ==================================================
    // TOAST
    // ==================================================

    private fun showToast(
        message: String
    ) {

        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {

        private const val FLASHLIGHT_PREFS =
            "touch_assistant_hardware"

        private const val KEY_FLASHLIGHT =
            "flashlight_enabled"
    }
}