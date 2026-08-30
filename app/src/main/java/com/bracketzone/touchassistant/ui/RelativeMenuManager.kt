package com.bracketzone.touchassistant.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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
import com.bracketzone.touchassistant.core.ActionExecutor
import com.bracketzone.touchassistant.core.AssistantAction
import com.bracketzone.touchassistant.preferences.MenuPreferences

class RelativeMenuManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onActionSelected: (AssistantAction) -> Unit
) {

    private var rootOverlayView: FrameLayout? = null
    var menuContainer: View? = null
        private set

    private var anchorFloatingButton: View? = null
    private val menuPreferences = MenuPreferences(context)
    private var isDismissing = false

    // Initialize VolumeSliderManager to coordinate closing together
    val volumeSliderManager = VolumeSliderManager(context)

    private fun getSafeInsets(): Rect {
        val rect = Rect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            rect.set(insets.left, insets.top, insets.right, insets.bottom)
        } else {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                rect.top = context.resources.getDimensionPixelSize(resourceId)
            }
        }
        return rect
    }

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
        val insets = getSafeInsets()
        val margin = dp(8)

        val targetCenterX = anchorX + (anchorWidth / 2)
        val targetCenterY = anchorY + (anchorHeight / 2)

        val minX = insets.left + margin
        val maxX = (screenWidth - menuWidth - insets.right - margin).coerceAtLeast(minX)

        val minY = insets.top + dp(16)
        val maxY = (screenHeight - menuHeight - insets.bottom - dp(16)).coerceAtLeast(minY)

        val menuX = (targetCenterX - (menuWidth / 2)).coerceIn(minX, maxX)
        val menuY = (targetCenterY - (menuHeight / 2)).coerceIn(minY, maxY)

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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(overlay, windowParams)

        anchorFloatingButton?.visibility = View.INVISIBLE
        animateExpandIn(container)
    }

    fun dismiss() {
        if (isDismissing) return

        // Ensure volume slider manager closes when menu is dismissed
        volumeSliderManager.dismissSlider()

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
        // Ensure volume slider manager closes immediately as well
        volumeSliderManager.dismissSlider()

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

    private var menuViewPager: ViewPager2? = null

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
        menuViewPager = viewPager
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
            setPadding(dp(2), dp(2), dp(2), dp(2))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            val icon = ImageView(context).apply {
                setImageResource(ActionIconProvider.getIconResId(action))
                setColorFilter(if (action == AssistantAction.NONE) Color.GRAY else Color.WHITE)
            }

            val text = TextView(context).apply {
                text = getLabelForAction(action)
                setTextColor(if (action == AssistantAction.NONE) Color.GRAY else Color.WHITE)
                textSize = 8f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            addView(icon, LinearLayout.LayoutParams(dp(24), dp(24)))
            addView(text)

            setOnClickListener {
                when (action) {
                    AssistantAction.FAVORITE_APPS -> {
                        menuViewPager?.setCurrentItem(1, true)
                    }
                    AssistantAction.VOLUME_UP, AssistantAction.VOLUME_DOWN, AssistantAction.MUTE_UNMUTE -> {
                        ActionExecutor(context, volumeSliderManager).execute(action, menuContainer)
                    }
                    AssistantAction.NONE -> {}
                    else -> {
                        dismiss()
                        onActionSelected(action)
                    }
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

    private fun getLabelForAction(action: AssistantAction): String {
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
            AssistantAction.FAVORITE_APPS -> "FAVORITE APPS"
            AssistantAction.NONE -> "NONE"
            else -> action.name.replace("_", " ")
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}