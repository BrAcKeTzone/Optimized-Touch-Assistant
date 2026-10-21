package com.bracketzone.reachpoint.preferences

import android.content.Context
import android.content.SharedPreferences
import com.bracketzone.reachpoint.core.AssistantAction

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
    }

    // --- Page 1: Actions ---
    fun getPageActions(pageIndex: Int): List<AssistantAction> {
        return PAGE_1_DEFAULTS.indices.map { slotIndex ->
            getSlotAction(pageIndex, slotIndex, PAGE_1_DEFAULTS[slotIndex])
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

    // --- Page 2: App Package Names ---
    fun getPageAppPackages(): List<String?> {
        return (0..8).map { slotIndex ->
            getSlotAppPackage(slotIndex)
        }
    }

    fun getSlotAppPackage(slotIndex: Int): String? {
        return prefs.getString("menu_p1_app_slot_$slotIndex", null)
    }

    fun setSlotAppPackage(slotIndex: Int, packageName: String?) {
        if (packageName == null) {
            prefs.edit().remove("menu_p1_app_slot_$slotIndex").apply()
        } else {
            prefs.edit().putString("menu_p1_app_slot_$slotIndex", packageName).apply()
        }
    }
}