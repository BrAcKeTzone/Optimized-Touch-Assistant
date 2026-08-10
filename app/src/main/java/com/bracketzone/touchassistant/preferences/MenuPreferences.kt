package com.bracketzone.touchassistant.preferences

import android.content.Context
import android.content.SharedPreferences
import com.bracketzone.touchassistant.core.AssistantAction

class MenuPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("menu_preferences", Context.MODE_PRIVATE)

    companion object {
        val PAGE_1_DEFAULTS = listOf(
            AssistantAction.HOME,
            AssistantAction.BACK,
            AssistantAction.RECENTS,
            AssistantAction.NOTIFICATIONS,
            AssistantAction.QUICK_SETTINGS,
            AssistantAction.LOCK,
            AssistantAction.SCREENSHOT,
            AssistantAction.FLASHLIGHT,
            AssistantAction.AUTO_ROTATE
        )
        // Page 2 defaults to NONE for all slots
        val PAGE_2_DEFAULTS = List(9) { AssistantAction.NONE }
    }

    fun getPageActions(pageIndex: Int): List<AssistantAction> {
        val defaults = if (pageIndex == 0) PAGE_1_DEFAULTS else PAGE_2_DEFAULTS
        return defaults.indices.map { slotIndex ->
            getSlotAction(pageIndex, slotIndex, defaults[slotIndex])
        }
    }

    fun getSlotAction(pageIndex: Int, slotIndex: Int, defaultAction: AssistantAction): AssistantAction {
        val savedName = prefs.getString("menu_p${pageIndex}_slot_$slotIndex", null) ?: return defaultAction
        return try {
            AssistantAction.valueOf(savedName)
        } catch (e: Exception) {
            defaultAction
        }
    }

    fun setSlotAction(pageIndex: Int, slotIndex: Int, action: AssistantAction) {
        prefs.edit().putString("menu_p${pageIndex}_slot_$slotIndex", action.name).apply()
    }
}