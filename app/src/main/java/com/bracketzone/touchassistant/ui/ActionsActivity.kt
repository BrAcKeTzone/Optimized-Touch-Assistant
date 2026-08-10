package com.bracketzone.touchassistant.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bracketzone.touchassistant.R
import com.bracketzone.touchassistant.core.AssistantAction
import com.bracketzone.touchassistant.preferences.GesturePreferences
import com.bracketzone.touchassistant.preferences.MenuPreferences

class ActionsActivity : Activity() {

    companion object {
        private const val REQUEST_SINGLE_TAP = 1001
        private const val REQUEST_DOUBLE_TAP = 1002
        private const val REQUEST_LONG_PRESS = 1003
        private const val REQUEST_MENU_SLOT = 2000
    }

    private lateinit var gesturePreferences: GesturePreferences
    private lateinit var menuPreferences: MenuPreferences

    private lateinit var singleTapActionText: TextView
    private lateinit var doubleTapActionText: TextView
    private lateinit var longPressActionText: TextView
    private lateinit var pageIndicatorText: TextView
    private lateinit var menuViewPager: ViewPager2

    private var pendingPage: Int = -1
    private var pendingSlot: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actions)

        gesturePreferences = GesturePreferences(this)
        menuPreferences = MenuPreferences(this)

        singleTapActionText = findViewById(R.id.singleTapActionText)
        doubleTapActionText = findViewById(R.id.doubleTapActionText)
        longPressActionText = findViewById(R.id.longPressActionText)
        pageIndicatorText = findViewById(R.id.pageIndicatorText)
        menuViewPager = findViewById(R.id.menuViewPager)

        setupGestureButtons()
        updateGestureLabels()
        setupMenuGridPages()
    }

    private fun setupGestureButtons() {
        findViewById<View>(R.id.singleTapButton).setOnClickListener {
            openPicker("Single Tap", gesturePreferences.getSingleTapAction(), REQUEST_SINGLE_TAP)
        }

        findViewById<View>(R.id.doubleTapButton).setOnClickListener {
            openPicker("Double Tap", gesturePreferences.getDoubleTapAction(), REQUEST_DOUBLE_TAP)
        }

        findViewById<View>(R.id.longPressButton).setOnClickListener {
            openPicker("Long Press", gesturePreferences.getLongPressAction(), REQUEST_LONG_PRESS)
        }
    }

    private fun setupMenuGridPages() {
        menuViewPager.adapter = MenuGridPagesAdapter()

        menuViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pageIndicatorText.text = "Page ${position + 1} of 2"
            }
        })
    }

    private fun openPicker(title: String, currentAction: AssistantAction, requestCode: Int) {
        val intent = Intent(this, ActionPickerActivity::class.java).apply {
            putExtra(ActionPickerActivity.EXTRA_GESTURE_NAME, title)
            putExtra(ActionPickerActivity.EXTRA_SELECTED_ACTION, currentAction.name)
        }
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val actionName = data.getStringExtra(ActionPickerActivity.EXTRA_RESULT_ACTION) ?: return
            val action = try { AssistantAction.valueOf(actionName) } catch (e: Exception) { return }

            when (requestCode) {
                REQUEST_SINGLE_TAP -> {
                    gesturePreferences.setSingleTapAction(action)
                    updateGestureLabels()
                }
                REQUEST_DOUBLE_TAP -> {
                    gesturePreferences.setDoubleTapAction(action)
                    updateGestureLabels()
                }
                REQUEST_LONG_PRESS -> {
                    gesturePreferences.setLongPressAction(action)
                    updateGestureLabels()
                }
                REQUEST_MENU_SLOT -> {
                    if (pendingPage != -1 && pendingSlot != -1) {
                        menuPreferences.setSlotAction(pendingPage, pendingSlot, action)
                        setupMenuGridPages()
                    }
                }
            }
        }
    }

    private fun updateGestureLabels() {
        singleTapActionText.text = gesturePreferences.getSingleTapAction().name
        doubleTapActionText.text = gesturePreferences.getDoubleTapAction().name
        longPressActionText.text = gesturePreferences.getLongPressAction().name
    }

    private inner class MenuGridPagesAdapter : RecyclerView.Adapter<MenuGridPagesAdapter.PageViewHolder>() {

        override fun getItemCount(): Int = 2 // 2 scrollable pages

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_menu_grid_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(position, menuPreferences.getPageActions(position))
        }

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val gridLayout: GridLayout = itemView.findViewById(R.id.gridPageLayout)

            fun bind(pageIndex: Int, actions: List<AssistantAction>) {
                gridLayout.removeAllViews()

                for ((slotIndex, action) in actions.withIndex()) {
                    val cellView = createGridCell(pageIndex, slotIndex, action)
                    gridLayout.addView(cellView)
                }
            }

            private fun createGridCell(pageIndex: Int, slotIndex: Int, action: AssistantAction): View {
                return LinearLayout(this@ActionsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isClickable = true
                    isFocusable = true

                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    layoutParams = params

                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(Color.parseColor("#F5F5F7"))
                    }

                    val icon = ImageView(this@ActionsActivity).apply {
                        setImageResource(getIconForAction(action))
                        setColorFilter(if (action == AssistantAction.NONE) Color.LTGRAY else Color.BLACK)
                    }

                    val text = TextView(this@ActionsActivity).apply {
                        text = if (action == AssistantAction.NONE) "None" else action.name.replace("_", " ")
                        setTextColor(if (action == AssistantAction.NONE) Color.GRAY else Color.DKGRAY)
                        textSize = 9f
                        gravity = Gravity.CENTER
                    }

                    addView(icon, LinearLayout.LayoutParams(dp(24), dp(24)))
                    addView(text)

                    setOnClickListener {
                        pendingPage = pageIndex
                        pendingSlot = slotIndex
                        openPicker(
                            "Page ${pageIndex + 1} - Slot ${slotIndex + 1}",
                            action,
                            REQUEST_MENU_SLOT
                        )
                    }
                }
            }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}