package com.bracketzone.touchassistant.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bracketzone.touchassistant.R
import com.bracketzone.touchassistant.core.AssistantAction

class ActionPickerActivity : Activity() {

    companion object {
        const val EXTRA_GESTURE_NAME = "extra_gesture_name"
        const val EXTRA_SELECTED_ACTION = "extra_selected_action"
        const val EXTRA_RESULT_ACTION = "extra_result_action"
    }

    private var currentSelectedAction: AssistantAction = AssistantAction.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_picker)

        val gestureName = intent.getStringExtra(EXTRA_GESTURE_NAME) ?: "Gesture"
        val selectedName = intent.getStringExtra(EXTRA_SELECTED_ACTION) ?: AssistantAction.NONE.name

        currentSelectedAction = try {
            AssistantAction.valueOf(selectedName)
        } catch (e: Exception) {
            AssistantAction.NONE
        }

        findViewById<TextView>(R.id.pickerTitle).text = "$gestureName Action"
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.actionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ActionsAdapter(AssistantAction.values().toList())
    }

    private inner class ActionsAdapter(
        private val actionsList: List<AssistantAction>
    ) : RecyclerView.Adapter<ActionsAdapter.ActionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_action_picker, parent, false)
            return ActionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            holder.bind(actionsList[position])
        }

        override fun getItemCount(): Int = actionsList.size

        inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.actionIcon)
            private val title: TextView = itemView.findViewById(R.id.actionName)
            private val desc: TextView = itemView.findViewById(R.id.actionDescription)
            private val checkmark: ImageView = itemView.findViewById(R.id.selectedCheckmark)

            fun bind(action: AssistantAction) {
                // Set name and description
                title.text = action.name.replace("_", " ")
                desc.text = getActionDescription(action)

                // Set icon
                icon.setImageResource(getIconForAction(action))

                // Display selected state (Mark + Highlight)
                val isSelected = (action == currentSelectedAction)
                checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE

                if (isSelected) {
                    itemView.setBackgroundColor(Color.parseColor("#EBF5FF"))
                } else {
                    itemView.setBackgroundColor(Color.WHITE)
                }

                itemView.setOnClickListener {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RESULT_ACTION, action.name)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    private fun getActionDescription(action: AssistantAction): String {
        return when (action) {
            AssistantAction.HOME -> "Navigate to device home screen"
            AssistantAction.BACK -> "Go back to previous screen"
            AssistantAction.RECENTS -> "Open recent app switcher"
            AssistantAction.NOTIFICATIONS -> "Expand notification drawer"
            AssistantAction.QUICK_SETTINGS -> "Expand quick settings panel"
            AssistantAction.LOCK -> "Turn off screen and lock device"
            AssistantAction.SCREENSHOT -> "Capture current screen image"
            AssistantAction.FLASHLIGHT -> "Toggle device torch flashlight"
            AssistantAction.AUTO_ROTATE -> "Toggle auto-rotation setting"
            AssistantAction.VOLUME_UP -> "Increase media volume level"
            AssistantAction.VOLUME_DOWN -> "Decrease media volume level"
            AssistantAction.SPLIT_SCREEN -> "Toggle split-screen mode"
            AssistantAction.NONE -> "No action assigned"
            else -> "Execute ${action.name.replace("_", " ").lowercase()} action"
        }
    }

    private fun getIconForAction(action: AssistantAction): Int {
        return when (action) {
            AssistantAction.HOME -> android.R.drawable.ic_menu_compass
            AssistantAction.BACK -> android.R.drawable.ic_menu_revert
            AssistantAction.RECENTS -> android.R.drawable.ic_menu_recent_history
            AssistantAction.NOTIFICATIONS -> android.R.drawable.ic_popup_reminder
            AssistantAction.QUICK_SETTINGS -> android.R.drawable.ic_menu_preferences
            AssistantAction.LOCK -> android.R.drawable.ic_lock_lock
            AssistantAction.SCREENSHOT -> android.R.drawable.ic_menu_camera
            AssistantAction.FLASHLIGHT -> android.R.drawable.ic_menu_day
            AssistantAction.AUTO_ROTATE -> android.R.drawable.ic_menu_always_landscape_portrait
            AssistantAction.VOLUME_UP -> android.R.drawable.ic_lock_idle_alarm
            AssistantAction.VOLUME_DOWN -> android.R.drawable.ic_lock_silent_mode
            else -> android.R.drawable.ic_menu_info_details
        }
    }
}