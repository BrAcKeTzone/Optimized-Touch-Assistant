package com.bracketzone.touchassistant.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bracketzone.touchassistant.core.AssistantAction
import com.bracketzone.touchassistant.preferences.MenuPreferences

class RelativeMenuManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onActionSelected: (AssistantAction) -> Unit
) {

    private var rootOverlayView: FrameLayout? = null
    private var menuContainer: View? = null
    private var anchorFloatingButton: View? = null
    private val menuPreferences = MenuPreferences(context)
    private var isDismissing = false

    fun showMenu(anchorView: View, anchorX: Int, anchorY: Int, anchorWidth: Int, anchorHeight: Int) {
        if (isShowing()) {
            dismissImmediate()
        }

        isDismissing = false
        anchorFloatingButton = anchorView

        val menuWidth = dp(240)
        val menuHeight = dp(240)

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val margin = dp(8)

        val targetCenterX = anchorX + (anchorWidth / 2)
        val targetCenterY = anchorY + (anchorHeight / 2)

        val menuX = (targetCenterX - (menuWidth / 2))
            .coerceIn(margin, (screenWidth - menuWidth - margin).coerceAtLeast(margin))
        val menuY = (targetCenterY - (menuHeight / 2))
            .coerceIn(dp(16), (screenHeight - menuHeight - dp(16)).coerceAtLeast(dp(16)))

        val overlay = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !isDismissing) {
                    dismiss()
                    true
                } else {
                    false
                }
            }
        }
        rootOverlayView = overlay

        val container = createMenuView()
        menuContainer = container

        val containerParams = FrameLayout.LayoutParams(menuWidth, menuHeight).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = menuX
            topMargin = menuY
        }

        container.setOnTouchListener { _, _ -> true }
        overlay.addView(container, containerParams)

        val pivotXInMenu = (targetCenterX - menuX).toFloat().coerceIn(0f, menuWidth.toFloat())
        val pivotYInMenu = (targetCenterY - menuY).toFloat().coerceIn(0f, menuHeight.toFloat())

        container.pivotX = pivotXInMenu
        container.pivotY = pivotYInMenu

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlay, windowParams)

        anchorFloatingButton?.visibility = View.INVISIBLE
        animateExpandIn(container)
    }

    fun dismiss() {
        if (isDismissing) return
        val container = menuContainer ?: return

        if (rootOverlayView?.isAttachedToWindow == true) {
            isDismissing = true
            animateCollapseOut(container) {
                dismissImmediate()
            }
        } else {
            dismissImmediate()
        }
    }

    private fun dismissImmediate() {
        rootOverlayView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }

        anchorFloatingButton?.apply {
            visibility = View.VISIBLE
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
        }

        anchorFloatingButton = null
        menuContainer = null
        rootOverlayView = null
        isDismissing = false
    }

    fun isShowing(): Boolean = rootOverlayView != null && rootOverlayView?.isAttachedToWindow == true

    private fun animateExpandIn(view: View) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f

        val scaleXAnim = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f)
        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim)
            duration = 200L
            interpolator = OvershootInterpolator(0.8f)
            start()
        }
    }

    private fun animateCollapseOut(view: View, onComplete: () -> Unit) {
        val scaleXAnim = ObjectAnimator.ofFloat(view, View.SCALE_X, view.scaleX, 0f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, View.SCALE_Y, view.scaleY, 0f)
        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f)

        AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim)
            duration = 180L
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun createMenuView(): View {
        val viewPager = ViewPager2(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(230, 30, 30, 30))
            }
        }

        viewPager.adapter = OverlayMenuPagerAdapter()
        return viewPager
    }

    private inner class OverlayMenuPagerAdapter :
        RecyclerView.Adapter<OverlayMenuPagerAdapter.PageViewHolder>() {

        override fun getItemCount(): Int = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val grid = GridLayout(context).apply {
                columnCount = 3
                rowCount = 3
                useDefaultMargins = true
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return PageViewHolder(grid)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bindPage(position)
        }

        inner class PageViewHolder(private val grid: GridLayout) : RecyclerView.ViewHolder(grid) {
            fun bindPage(pageIndex: Int) {
                grid.removeAllViews()

                if (pageIndex == 0) {
                    val actions = menuPreferences.getPageActions(0)
                    for (action in actions) {
                        val itemView = createActionItem(action)
                        grid.addView(itemView)
                    }
                } else {
                    val appPackages = menuPreferences.getPageAppPackages()
                    for (packageName in appPackages) {
                        val itemView = createAppItem(packageName)
                        grid.addView(itemView)
                    }
                }
            }
        }
    }

    private fun createActionItem(action: AssistantAction): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            val icon = ImageView(context).apply {
                setImageResource(getIconForAction(action))
                setColorFilter(if (action == AssistantAction.NONE) Color.GRAY else Color.WHITE)
            }
            val text = TextView(context).apply {
                text = getLabelForAction(action)
                setTextColor(if (action == AssistantAction.NONE) Color.GRAY else Color.WHITE)
                textSize = 9f
                gravity = Gravity.CENTER
            }

            addView(icon, LinearLayout.LayoutParams(dp(24), dp(24)))
            addView(text)

            setOnClickListener {
                if (action != AssistantAction.NONE) {
                    dismiss()
                    onActionSelected(action)
                }
            }
        }
    }

    private fun createAppItem(packageName: String?): View {
        val pm = context.packageManager

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            val iconView = ImageView(context)
            val textView = TextView(context).apply {
                textSize = 9f
                gravity = Gravity.CENTER
                maxLines = 1
            }

            if (!packageName.isNullOrEmpty()) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    iconView.setImageDrawable(pm.getApplicationIcon(appInfo))
                    textView.text = pm.getApplicationLabel(appInfo)
                    textView.setTextColor(Color.WHITE)

                    setOnClickListener {
                        dismiss()
                        val launchIntent = pm.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        }
                    }
                } catch (e: Exception) {
                    iconView.setImageResource(android.R.drawable.ic_menu_help)
                    iconView.setColorFilter(Color.GRAY)
                    textView.text = "None"
                    textView.setTextColor(Color.GRAY)
                }
            } else {
                iconView.setImageResource(android.R.drawable.ic_menu_add)
                iconView.setColorFilter(Color.GRAY)
                textView.text = "Empty"
                textView.setTextColor(Color.GRAY)
            }

            addView(iconView, LinearLayout.LayoutParams(dp(24), dp(24)))
            addView(textView)
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

    private fun getLabelForAction(action: AssistantAction): String {
        return when (action) {
            AssistantAction.HOME -> "Home"
            AssistantAction.BACK -> "Back"
            AssistantAction.RECENTS -> "Recents"
            AssistantAction.NOTIFICATIONS -> "Notif"
            AssistantAction.QUICK_SETTINGS -> "Settings"
            AssistantAction.LOCK -> "Lock"
            AssistantAction.SCREENSHOT -> "Capture"
            AssistantAction.FLASHLIGHT -> "Flash"
            AssistantAction.AUTO_ROTATE -> "Rotate"
            AssistantAction.VOLUME_UP -> "Vol +"
            AssistantAction.VOLUME_DOWN -> "Vol -"
            AssistantAction.NONE -> "None"
            else -> action.name.take(7)
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}