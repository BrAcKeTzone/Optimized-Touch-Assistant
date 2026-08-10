package com.bracketzone.touchassistant.preferences

import android.content.Context
import com.bracketzone.touchassistant.core.AssistantAction

class GesturePreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    // --------------------------------------------------
    // Position Persistence
    // --------------------------------------------------

    fun getSavedX(defaultX: Int): Int {
        return preferences.getInt(KEY_POSITION_X, defaultX)
    }

    fun getSavedY(defaultY: Int): Int {
        return preferences.getInt(KEY_POSITION_Y, defaultY)
    }

    fun savePosition(x: Int, y: Int) {
        preferences.edit()
            .putInt(KEY_POSITION_X, x)
            .putInt(KEY_POSITION_Y, y)
            .apply()
    }

    // --------------------------------------------------
    // Single Tap
    // --------------------------------------------------

    fun getSingleTapAction(): AssistantAction {
        return getAction(
            KEY_SINGLE_TAP,
            AssistantAction.MAIN
        )
    }

    fun setSingleTapAction(
        action: AssistantAction
    ) {
        setAction(
            KEY_SINGLE_TAP,
            action
        )
    }

    // --------------------------------------------------
    // Double Tap
    // --------------------------------------------------

    fun getDoubleTapAction(): AssistantAction {
        return getAction(
            KEY_DOUBLE_TAP,
            AssistantAction.SCREENSHOT
        )
    }

    fun setDoubleTapAction(
        action: AssistantAction
    ) {
        setAction(
            KEY_DOUBLE_TAP,
            action
        )
    }

    // --------------------------------------------------
    // Long Press
    // --------------------------------------------------

    fun getLongPressAction(): AssistantAction {
        return getAction(
            KEY_LONG_PRESS,
            AssistantAction.NOTIFICATIONS
        )
    }

    fun setLongPressAction(
        action: AssistantAction
    ) {
        setAction(
            KEY_LONG_PRESS,
            action
        )
    }

    // --------------------------------------------------
    // Floating Button Transparency
    // --------------------------------------------------

    fun getBaseAlpha(): Float {
        return preferences
            .getFloat(
                KEY_BASE_ALPHA,
                DEFAULT_ALPHA
            )
            .coerceIn(
                MIN_ALPHA,
                MAX_ALPHA
            )
    }

    fun setBaseAlpha(
        alpha: Float
    ) {
        preferences.edit()
            .putFloat(
                KEY_BASE_ALPHA,
                alpha.coerceIn(
                    MIN_ALPHA,
                    MAX_ALPHA
                )
            )
            .apply()
    }

    // --------------------------------------------------
    // Internal Helpers
    // --------------------------------------------------

    private fun getAction(
        key: String,
        default: AssistantAction
    ): AssistantAction {
        val storedValue =
            preferences.getString(
                key,
                default.name
            )

        return try {
            AssistantAction.valueOf(
                storedValue ?: default.name
            )
        } catch (
            _: IllegalArgumentException
        ) {
            default
        }
    }

    private fun setAction(
        key: String,
        action: AssistantAction
    ) {
        preferences.edit()
            .putString(
                key,
                action.name
            )
            .apply()
    }

    companion object {
        private const val PREF_NAME = "touch_assistant_preferences"
        private const val KEY_SINGLE_TAP = "single_tap_action"
        private const val KEY_DOUBLE_TAP = "double_tap_action"
        private const val KEY_LONG_PRESS = "long_press_action"
        private const val KEY_BASE_ALPHA = "base_alpha"
        private const val KEY_POSITION_X = "position_x"
        private const val KEY_POSITION_Y = "position_y"

        private const val DEFAULT_ALPHA = 1.0f
        private const val MIN_ALPHA = 0.1f
        private const val MAX_ALPHA = 1.0f
    }
}