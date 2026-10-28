package com.bracketzone.reachpoint.core

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

object ConfigManager {

    fun exportToJson(context: Context): String {
        val json = JSONObject()

        // Export menu preferences
        val menuPrefs = context.getSharedPreferences("menu_preferences", Context.MODE_PRIVATE)
        val menuJson = JSONObject()
        menuPrefs.all.forEach { (key, value) -> menuJson.put(key, value) }
        json.put("menu_preferences", menuJson)

        // Export gesture/general preferences
        val gesturePrefs = context.getSharedPreferences("touch_assistant_preferences", Context.MODE_PRIVATE)
        val gestureJson = JSONObject()
        gesturePrefs.all.forEach { (key, value) -> gestureJson.put(key, value) }
        json.put("touch_assistant_preferences", gestureJson)

        return json.toString(4) // Pretty print JSON
    }

    fun importFromJson(context: Context, jsonString: String): Boolean {
        try {
            val json = JSONObject(jsonString)

            // Import menu preferences
            if (json.has("menu_preferences")) {
                val menuJson = json.getJSONObject("menu_preferences")
                context.getSharedPreferences("menu_preferences", Context.MODE_PRIVATE).edit {
                    clear()
                    menuJson.keys().forEach { key ->
                        when (val value = menuJson.get(key)) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                            is Long -> putLong(key, value)
                        }
                    }
                }
            }

            // Import gesture preferences
            if (json.has("touch_assistant_preferences")) {
                val gestureJson = json.getJSONObject("touch_assistant_preferences")
                context.getSharedPreferences("touch_assistant_preferences", Context.MODE_PRIVATE).edit {
                    clear()
                    gestureJson.keys().forEach { key ->
                        when (val value = gestureJson.get(key)) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                            is Long -> putLong(key, value)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}