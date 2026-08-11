package com.bracketzone.touchassistant.ui

import com.bracketzone.touchassistant.R
import com.bracketzone.touchassistant.core.AssistantAction

object ActionIconProvider {
    fun getIconResId(action: AssistantAction): Int {
        return when (action) {
            AssistantAction.HOME -> R.drawable.ic_home
            AssistantAction.BACK -> R.drawable.ic_arrow_back
            AssistantAction.RECENTS -> R.drawable.ic_apps
            AssistantAction.NOTIFICATIONS -> R.drawable.ic_notifications
            AssistantAction.QUICK_SETTINGS -> R.drawable.ic_settings
            AssistantAction.LOCK -> R.drawable.ic_lock
            AssistantAction.SCREENSHOT -> R.drawable.ic_screenshot
            AssistantAction.FLASHLIGHT -> R.drawable.ic_flashlight
            AssistantAction.AUTO_ROTATE -> R.drawable.ic_screen_rotation
            AssistantAction.VOLUME_UP -> R.drawable.ic_volume_up
            AssistantAction.VOLUME_DOWN -> R.drawable.ic_volume_down
            AssistantAction.SPLIT_SCREEN -> R.drawable.ic_view_stream
            AssistantAction.MAIN -> R.drawable.ic_widgets
            AssistantAction.NONE -> R.drawable.ic_block
            AssistantAction.POWER -> R.drawable.ic_power
            AssistantAction.MUTE_UNMUTE -> R.drawable.ic_mute
            AssistantAction.TOGGLE_RINGER -> R.drawable.ic_ringer
            AssistantAction.MAX_BRIGHTNESS -> R.drawable.ic_brightness
            AssistantAction.TOGGLE_SYNC -> R.drawable.ic_sync
            AssistantAction.WI_FI -> R.drawable.ic_wifi
            AssistantAction.BLUETOOTH -> R.drawable.ic_bluetooth
            AssistantAction.LOCATION -> R.drawable.ic_location
            AssistantAction.AIRPLANE_MODE -> R.drawable.ic_airplane
            AssistantAction.MOBILE_DATA -> R.drawable.ic_mobile_data

            // Fallback for any unexpected actions
            else -> R.drawable.ic_block
        }
    }
}