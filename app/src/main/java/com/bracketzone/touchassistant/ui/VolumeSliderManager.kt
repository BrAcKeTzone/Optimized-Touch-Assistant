package com.bracketzone.touchassistant.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar

class VolumeSliderManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var sliderView: SeekBar? = null
    private val handler = Handler(Looper.getMainLooper())

    private val hideRunnable = Runnable {
        dismissSlider()
    }

    fun adjustVolume(direction: Int, anchorView: View? = null) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            0
        )

        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        if (sliderView == null) {
            showSlider(currentVol, maxVol, anchorView)
        } else {
            sliderView?.progress = currentVol
        }

        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 2000L)
    }

    private fun showSlider(current: Int, max: Int, anchorView: View?) {
        val seekBar = SeekBar(context).apply {
            this.max = max
            this.progress = current
            setPadding(32, 16, 32, 16)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.argb(230, 30, 30, 30))
            }

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                        handler.removeCallbacks(hideRunnable)
                        handler.postDelayed(hideRunnable, 2000L)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sliderWidth = dp(220)
        val sliderHeight = dp(36)

        var posX = (context.resources.displayMetrics.widthPixels / 2) - (sliderWidth / 2)
        var posY = dp(60)

        if (anchorView != null && anchorView.isAttachedToWindow) {
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)

            posX = location[0] + (anchorView.width / 2) - (sliderWidth / 2)

            val screenWidth = context.resources.displayMetrics.widthPixels
            posX = posX.coerceIn(dp(12), screenWidth - sliderWidth - dp(12))

            val smallMargin = dp(6)
            posY = location[1] - sliderHeight - smallMargin

            if (posY < dp(36)) {
                posY = location[1] + anchorView.height + smallMargin
            }
        }

        val params = WindowManager.LayoutParams(
            sliderWidth,
            sliderHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = posX
            y = posY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        sliderView = seekBar
        try {
            windowManager.addView(seekBar, params)
        } catch (_: Exception) {}
    }

    fun dismissSlider() {
        handler.removeCallbacks(hideRunnable)
        sliderView?.let { view ->
            view.animate()
                .alpha(0f)
                .setDuration(180)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            windowManager.removeView(view)
                        } catch (_: Exception) {}
                        sliderView = null
                    }
                })
        } ?: run {
            sliderView = null
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}