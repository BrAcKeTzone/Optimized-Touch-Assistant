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
        private const val REQUEST_ACTION_SLOT = 2000
        private const val REQUEST_APP_SLOT = 2001
    }

    private lateinit var gesturePreferences: GesturePreferences
    private lateinit var menuPreferences: MenuPreferences

    private lateinit var singleTapActionText: TextView
    private lateinit var doubleTapActionText: TextView
    private lateinit var longPressActionText: TextView
    private lateinit var pageIndicatorText: TextView
    private lateinit var menuViewPager: ViewPager2

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
            openActionPicker("Single Tap", gesturePreferences.getSingleTapAction(), REQUEST_SINGLE_TAP)
        }

        findViewById<View>(R.id.doubleTapButton).setOnClickListener {
            openActionPicker("Double Tap", gesturePreferences.getDoubleTapAction(), REQUEST_DOUBLE_TAP)
        }

        findViewById<View>(R.id.longPressButton).setOnClickListener {
            openActionPicker("Long Press", gesturePreferences.getLongPressAction(), REQUEST_LONG_PRESS)
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

    private fun openActionPicker(title: String, currentAction: AssistantAction, requestCode: Int) {
        val intent = Intent(this, ActionPickerActivity::class.java).apply {
            putExtra(ActionPickerActivity.EXTRA_GESTURE_NAME, title)
            putExtra(ActionPickerActivity.EXTRA_SELECTED_ACTION, currentAction.name)
        }
        startActivityForResult(intent, requestCode)
    }

    private fun openAppPicker(selectedPackage: String?, requestCode: Int) {
        val intent = Intent(this, AppPickerActivity::class.java).apply {
            putExtra(AppPickerActivity.EXTRA_SELECTED_PACKAGE, selectedPackage)
        }
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_SINGLE_TAP -> {
                    val actionName = data.getStringExtra(ActionPickerActivity.EXTRA_RESULT_ACTION) ?: return
                    val action = try { AssistantAction.valueOf(actionName) } catch (e: Exception) { return }
                    gesturePreferences.setSingleTapAction(action)
                    updateGestureLabels()
                }
                REQUEST_DOUBLE_TAP -> {
                    val actionName = data.getStringExtra(ActionPickerActivity.EXTRA_RESULT_ACTION) ?: return
                    val action = try { AssistantAction.valueOf(actionName) } catch (e: Exception) { return }
                    gesturePreferences.setDoubleTapAction(action)
                    updateGestureLabels()
                }
                REQUEST_LONG_PRESS -> {
                    val actionName = data.getStringExtra(ActionPickerActivity.EXTRA_RESULT_ACTION) ?: return
                    val action = try { AssistantAction.valueOf(actionName) } catch (e: Exception) { return }
                    gesturePreferences.setLongPressAction(action)
                    updateGestureLabels()
                }
                REQUEST_ACTION_SLOT -> {
                    val actionName = data.getStringExtra(ActionPickerActivity.EXTRA_RESULT_ACTION) ?: return
                    val action = try { AssistantAction.valueOf(actionName) } catch (e: Exception) { return }
                    if (pendingSlot != -1) {
                        menuPreferences.setSlotAction(0, pendingSlot, action)
                        setupMenuGridPages()
                    }
                }
                REQUEST_APP_SLOT -> {
                    val pkg = data.getStringExtra(AppPickerActivity.EXTRA_RESULT_PACKAGE)
                    if (pendingSlot != -1) {
                        menuPreferences.setSlotAppPackage(pendingSlot, pkg)
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

    private fun getFormattedActionTitle(action: AssistantAction): String {
        return when (action) {
            AssistantAction.HOME -> "HOME"
            AssistantAction.BACK -> "BACK"
            AssistantAction.RECENTS -> "RECENTS"
            AssistantAction.NOTIFICATIONS -> "NOTIFICATIONS"
            AssistantAction.QUICK_SETTINGS -> "QUICK SETTINGS"
            AssistantAction.LOCK -> "LOCK"
            AssistantAction.SCREENSHOT -> "SCREENSHOT"
            AssistantAction.FLASHLIGHT -> "FLASHLIGHT"
            AssistantAction.AUTO_ROTATE -> "AUTO-ROTATE"
            AssistantAction.VOLUME_UP -> "VOLUME UP"
            AssistantAction.VOLUME_DOWN -> "VOLUME DOWN"
            AssistantAction.MUTE_UNMUTE -> "MUTE/UNMUTE"
            AssistantAction.POWER -> "POWER"
            AssistantAction.MAIN -> "MAIN"
            AssistantAction.NONE -> "NONE"
            else -> action.name.replace("_", " ")
        }
    }

    private inner class MenuGridPagesAdapter : RecyclerView.Adapter<MenuGridPagesAdapter.PageViewHolder>() {

        override fun getItemCount(): Int = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_menu_grid_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val gridLayout: GridLayout = itemView.findViewById(R.id.gridPageLayout)

            fun bind(pageIndex: Int) {
                gridLayout.removeAllViews()

                if (pageIndex == 0) {
                    val actions = menuPreferences.getPageActions(0)
                    for ((slotIndex, action) in actions.withIndex()) {
                        val cellView = createActionGridCell(slotIndex, action)
                        gridLayout.addView(cellView)
                    }
                } else {
                    val appPackages = menuPreferences.getPageAppPackages()
                    for ((slotIndex, pkg) in appPackages.withIndex()) {
                        val cellView = createAppGridCell(slotIndex, pkg)
                        gridLayout.addView(cellView)
                    }
                }
            }

            private fun createActionGridCell(slotIndex: Int, action: AssistantAction): View {
                return LinearLayout(this@ActionsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isClickable = true
                    isFocusable = true

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }

                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(Color.parseColor("#F5F5F7"))
                    }

                    val icon = ImageView(this@ActionsActivity).apply {
                        setImageResource(ActionIconProvider.getIconResId(action))
                        setColorFilter(if (action == AssistantAction.NONE) Color.LTGRAY else Color.BLACK)
                    }

                    val text = TextView(this@ActionsActivity).apply {
                        text = if (action == AssistantAction.NONE) "None" else getFormattedActionTitle(action)
                        setTextColor(if (action == AssistantAction.NONE) Color.GRAY else Color.DKGRAY)
                        textSize = 9f
                        gravity = Gravity.CENTER
                    }

                    addView(icon, LinearLayout.LayoutParams(dp(24), dp(24)))
                    addView(text)

                    setOnClickListener {
                        pendingSlot = slotIndex
                        openActionPicker("Slot ${slotIndex + 1}", action, REQUEST_ACTION_SLOT)
                    }
                }
            }

            private fun createAppGridCell(slotIndex: Int, packageName: String?): View {
                val pm = packageManager

                return LinearLayout(this@ActionsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isClickable = true
                    isFocusable = true

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }

                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(Color.parseColor("#F5F5F7"))
                    }

                    val iconView = ImageView(this@ActionsActivity)
                    val textView = TextView(this@ActionsActivity).apply {
                        textSize = 9f
                        gravity = Gravity.CENTER
                        maxLines = 1
                    }

                    if (!packageName.isNullOrEmpty()) {
                        try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            iconView.setImageDrawable(pm.getApplicationIcon(appInfo))
                            textView.text = pm.getApplicationLabel(appInfo)
                            textView.setTextColor(Color.DKGRAY)
                        } catch (e: Exception) {
                            iconView.setImageResource(android.R.drawable.ic_menu_help)
                            iconView.setColorFilter(Color.LTGRAY)
                            textView.text = "Missing"
                            textView.setTextColor(Color.GRAY)
                        }
                    } else {
                        iconView.setImageResource(android.R.drawable.ic_menu_add)
                        iconView.setColorFilter(Color.LTGRAY)
                        textView.text = "Empty"
                        textView.setTextColor(Color.GRAY)
                    }

                    addView(iconView, LinearLayout.LayoutParams(dp(24), dp(24)))
                    addView(textView)

                    setOnClickListener {
                        pendingSlot = slotIndex
                        openAppPicker(packageName, REQUEST_APP_SLOT)
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}