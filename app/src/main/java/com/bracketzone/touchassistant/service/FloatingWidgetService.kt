package com.bracketzone.touchassistant.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.DisplayCutout
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import com.bracketzone.touchassistant.core.ActionExecutor
import com.bracketzone.touchassistant.core.AssistantAction
import com.bracketzone.touchassistant.preferences.GesturePreferences
import com.bracketzone.touchassistant.ui.RelativeMenuManager

private const val IDLE_DELAY_MS = 3_000L
private const val IDLE_ALPHA = 0.3f
private const val IDLE_ANIMATION_DURATION_MS = 250L

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var relativeMenuManager: RelativeMenuManager
    private lateinit var floatingView: ImageView
    private lateinit var windowParams: WindowManager.LayoutParams
    private lateinit var gesturePreferences: GesturePreferences
    private lateinit var touchSlop: ViewConfiguration

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isDragging = false

    private var edgeAnimator: ValueAnimator? = null
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { animateToIdle() }
    private var idleAnimator: AnimatorSet? = null
    private var idleTranslationX = 0f

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate() {
        super.onCreate()

        gesturePreferences = GesturePreferences(this)
        touchSlop = ViewConfiguration.get(this)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        relativeMenuManager = RelativeMenuManager(this, windowManager) { action ->
            ActionExecutor(this).execute(action)
        }

        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    executeGestureAction(gesturePreferences.getSingleTapAction())
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    executeGestureAction(gesturePreferences.getDoubleTapAction())
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    executeGestureAction(gesturePreferences.getLongPressAction())
                }
            }
        )

        startForegroundServiceNotification()
        createFloatingView()
    }

    private fun createFloatingView() {
        floatingView = ImageView(this).apply {
            // Load the app's official launcher icon directly
            val appIconDrawable = packageManager.getApplicationIcon(packageName)
            setImageDrawable(appIconDrawable)

            // Ensure no background shape or color filter is applied
            background = null
            clearColorFilter()

            contentDescription = "Touch Assistant"
            scaleType = ImageView.ScaleType.FIT_CENTER

            alpha = 0f
            scaleX = 0f
            scaleY = 0f

            setOnTouchListener { _, event -> handleTouch(event) }
        }

        val size = dp(56)

        val defaultX = dp(16)
        val defaultY = dp(220)
        val savedX = gesturePreferences.getSavedX(defaultX)
        val savedY = gesturePreferences.getSavedY(defaultY)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            size,
            size,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(floatingView, windowParams)

        floatingView.post {
            clampFloatingViewPosition()
            snapToNearestEdge()
        }

        startEnterAnimation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        relativeMenuManager.dismiss()

        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            restoreFromIdle()
            edgeAnimator?.cancel()

            floatingView.post {
                clampFloatingViewPosition()
                snapToNearestEdge()
            }

            scheduleIdleHide()
        }
    }

    private fun startEnterAnimation() {
        val targetAlpha = gesturePreferences.getBaseAlpha()
        val scaleX = ObjectAnimator.ofFloat(floatingView, View.SCALE_X, 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(floatingView, View.SCALE_Y, 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(floatingView, View.ALPHA, 0f, targetAlpha)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300L
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scheduleIdleHide()
                }
            })
            start()
        }
    }

    private fun scheduleIdleHide() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, IDLE_DELAY_MS)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "touch_assistant_service"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Touch Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Touch Assistant floating overlay"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Touch Assistant")
                .setContentText("Floating assistant is active")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Touch Assistant")
                .setContentText("Floating assistant is active")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop immediately if accessibility service is not active
        if (!TouchAccessibilityService.isEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    override fun onDestroy() {
        relativeMenuManager.dismiss()
        idleHandler.removeCallbacks(idleRunnable)
        idleAnimator?.cancel()
        edgeAnimator?.cancel()

        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            val scaleX = ObjectAnimator.ofFloat(floatingView, View.SCALE_X, floatingView.scaleX, 0f)
            val scaleY = ObjectAnimator.ofFloat(floatingView, View.SCALE_Y, floatingView.scaleY, 0f)
            val alpha = ObjectAnimator.ofFloat(floatingView, View.ALPHA, floatingView.alpha, 0f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 200L
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (floatingView.isAttachedToWindow) {
                            windowManager.removeView(floatingView)
                        }
                    }
                })
                start()
            }
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CUTOUT_NONE = 0
        private const val CUTOUT_LEFT = 1
        private const val CUTOUT_RIGHT = 2
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelIdleHide()
                edgeAnimator?.cancel()
                floatingView.translationX = 0f

                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialWindowX = windowParams.x
                initialWindowY = windowParams.y
                isDragging = false

                gestureDetector.onTouchEvent(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                if (!isDragging && distance > touchSlop.scaledTouchSlop) {
                    isDragging = true
                }

                gestureDetector.onTouchEvent(event)

                if (isDragging) {
                    windowParams.x = initialWindowX + deltaX.toInt()
                    windowParams.y = initialWindowY + deltaY.toInt()
                    clampFloatingViewPosition()
                    windowManager.updateViewLayout(floatingView, windowParams)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                gestureDetector.onTouchEvent(event)

                if (isDragging) {
                    snapToNearestEdge()
                } else {
                    gesturePreferences.savePosition(windowParams.x, windowParams.y)
                }

                scheduleIdleHide()
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                gestureDetector.onTouchEvent(event)
                isDragging = false
                scheduleIdleHide()
                return true
            }
        }
        return false
    }

    private fun getSafeInsets(): Rect {
        val rect = Rect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            rect.set(insets.left, insets.top, insets.right, insets.bottom)
        } else {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                rect.top = resources.getDimensionPixelSize(resourceId)
            }
        }
        return rect
    }

    private fun getCameraCutoutSide(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout: DisplayCutout? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.windowInsets.displayCutout
            } else if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
                floatingView.rootWindowInsets?.displayCutout
            } else {
                null
            }

            if (cutout != null) {
                if (cutout.safeInsetLeft > 0) return CUTOUT_LEFT
                if (cutout.safeInsetRight > 0) return CUTOUT_RIGHT
            }
        }
        return CUTOUT_NONE
    }

    private fun clampFloatingViewPosition() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val insets = getSafeInsets()

        val viewWidth = if (floatingView.width > 0) floatingView.width else dp(56)
        val viewHeight = if (floatingView.height > 0) floatingView.height else dp(56)

        val minX = insets.left
        val maxX = (screenWidth - viewWidth - insets.right).coerceAtLeast(minX)
        val minY = insets.top
        val maxY = (screenHeight - viewHeight - insets.bottom).coerceAtLeast(minY)

        windowParams.x = windowParams.x.coerceIn(minX, maxX)
        windowParams.y = windowParams.y.coerceIn(minY, maxY)
    }

    private fun snapToNearestEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val insets = getSafeInsets()

        val viewWidth = if (floatingView.width > 0) floatingView.width else dp(56)
        val currentX = windowParams.x

        val minX = insets.left
        val maxX = (screenWidth - viewWidth - insets.right).coerceAtLeast(minX)

        val cutoutSide = getCameraCutoutSide()

        val targetX = when (cutoutSide) {
            CUTOUT_LEFT -> maxX
            CUTOUT_RIGHT -> minX
            else -> {
                if (currentX + viewWidth / 2 < screenWidth / 2) minX else maxX
            }
        }

        edgeAnimator?.cancel()
        edgeAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 250L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                windowParams.x = animator.animatedValue as Int
                floatingView.translationX = 0f
                if (floatingView.isAttachedToWindow) {
                    windowManager.updateViewLayout(floatingView, windowParams)
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gesturePreferences.savePosition(windowParams.x, windowParams.y)
                }
            })
            start()
        }
    }

    private fun cancelIdleHide() {
        idleHandler.removeCallbacks(idleRunnable)
        idleAnimator?.cancel()
        idleAnimator = null
        restoreFromIdle()
    }

    private fun animateToIdle() {
        if (!::floatingView.isInitialized) return
        val width = floatingView.width
        if (width <= 0) return

        val direction = if (windowParams.x < resources.displayMetrics.widthPixels / 2) -1f else 1f
        idleTranslationX = width * 0.60f * direction

        idleAnimator?.cancel()

        val alphaAnimator = ObjectAnimator.ofFloat(
            floatingView,
            View.ALPHA,
            floatingView.alpha,
            IDLE_ALPHA
        )

        val translationAnimator = ObjectAnimator.ofFloat(
            floatingView,
            View.TRANSLATION_X,
            floatingView.translationX,
            idleTranslationX
        )

        idleAnimator = AnimatorSet().apply {
            duration = IDLE_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            playTogether(alphaAnimator, translationAnimator)
            start()
        }
    }

    private fun restoreFromIdle() {
        if (!::floatingView.isInitialized) return
        floatingView.animate().cancel()
        floatingView.alpha = gesturePreferences.getBaseAlpha()
        floatingView.translationX = 0f
    }

    private fun executeGestureAction(action: AssistantAction) {
        if (action == AssistantAction.MAIN) {
            showMainMenu()
        } else {
            ActionExecutor(this).execute(action)
        }
    }

    private fun showMainMenu() {
        relativeMenuManager.dismiss()

        val viewWidth = if (floatingView.width > 0) floatingView.width else dp(56)
        val viewHeight = if (floatingView.height > 0) floatingView.height else dp(56)

        relativeMenuManager.showMenu(
            anchorView = floatingView,
            anchorX = windowParams.x,
            anchorY = windowParams.y,
            anchorWidth = viewWidth,
            anchorHeight = viewHeight
        )
    }
}