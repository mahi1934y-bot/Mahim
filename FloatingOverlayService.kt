package com.mahim.aisignalpro

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var panel: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun layoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 220 }
    }

    private fun showBubble() {
        if (bubble != null) return
        val view = TextView(this).apply {
            text = "AI\\nPRO"
            textSize = 12f
            setTextColor(Color.rgb(90, 255, 80))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
                setStroke(3, Color.rgb(90, 255, 80))
            }
            elevation = 12f
        }
        val params = layoutParams(64, 64)
        var downX = 0
        var downY = 0
        var startX = 0
        var startY = 0
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX.toInt(); downY = event.rawY.toInt()
                    startX = params.x; startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + event.rawX.toInt() - downX
                    params.y = startY + event.rawY.toInt() - downY
                    windowManager.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.rawX.toInt() - downX)
                    val dy = kotlin.math.abs(event.rawY.toInt() - downY)
                    if (dx < 10 && dy < 10) requestScan()
                    true
                }
                else -> true
            }
        }
        bubble = view
        windowManager.addView(view, params)
    }

    private fun requestScan() {
        sendBroadcast(Intent(ScreenCaptureService.ACTION_CAPTURE).setPackage(packageName))
        showPanel("স্ক্রিন ফ্রেম নেওয়া হচ্ছে…")
    }

    private fun showPanel(message: String) {
        try {
            panel?.let { windowManager.removeView(it) }
            val v = TextView(this).apply {
                text = "MAHIM AI PRO\\n$message"
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(20, 16, 20, 16)
                background = GradientDrawable().apply {
                    cornerRadius = 22f
                    setColor(Color.rgb(24, 27, 31))
                    setStroke(2, Color.rgb(90, 255, 80))
                }
            }
            panel = v
            windowManager.addView(v, layoutParams(
                (resources.displayMetrics.widthPixels - 36).coerceAtLeast(240), 110
            ).apply { x = 18; y = 300 })
            v.postDelayed({
                try { if (panel === v) windowManager.removeView(v); if (panel === v) panel = null }
                catch (_: Exception) { }
            }, 3500)
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        try { bubble?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        try { panel?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        bubble = null; panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
