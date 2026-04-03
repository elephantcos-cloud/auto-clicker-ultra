package com.autoclicker.ultra

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView

class AutoClickerService : AccessibilityService() {

    companion object {
        var instance: AutoClickerService? = null
        var isRunning = false
        var targetText = ""
        var intervalMs = 1000L
        var useTextClick = true
        var slideFromX = 500f
        var slideFromY = 1200f
        var slideToX = 500f
        var slideToY = 400f
        var slideDurationMs = 300L
        var onClickCallback: ((String) -> Unit)? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var clickCount = 0
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private lateinit var floatBtn: TextView
    private lateinit var floatCount: TextView

    private val runnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (useTextClick) performTextClick() else performSlide()
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { isRunning = false }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopClicker()
        removeFloatingWindow()
    }

    // ── FLOATING WINDOW ──────────────────────────────────────
    fun showFloatingWindow() {
        if (floatingView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Root layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC1A1A2E"))
                cornerRadius = 16f
                setStroke(1, Color.parseColor("#44FFFFFF"))
            }
        }

        // Click count
        floatCount = TextView(this).apply {
            text = "0 ক্লিক"
            textSize = 11f
            setTextColor(Color.parseColor("#F9D423"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 6)
        }
        root.addView(floatCount)

        // Play/Stop button
        floatBtn = TextView(this).apply {
            text = "▶"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(18, 14, 18, 14)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#43A047"))
            }
            setOnClickListener { toggleFromFloat() }
        }
        root.addView(floatBtn)

        // Status text
        val statusTxt = TextView(this).apply {
            text = "\"$targetText\""
            textSize = 10f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
            maxWidth = 200
            isSingleLine = false
        }
        root.addView(statusTxt)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        // Drag support
        var lastX = 0; var lastY = 0; var initX = 0; var initY = 0
        root.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt(); lastY = event.rawY.toInt()
                    initX = params.x; initY = params.y
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (event.rawX.toInt() - lastX)
                    params.y = initY + (event.rawY.toInt() - lastY)
                    windowManager?.updateViewLayout(root, params)
                }
            }
            false
        }

        floatingView = root
        windowManager?.addView(root, params)
    }

    fun removeFloatingWindow() {
        floatingView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        floatingView = null
    }

    private fun toggleFromFloat() {
        if (isRunning) {
            stopClicker()
            floatBtn.text = "▶"
            floatBtn.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#43A047"))
            }
        } else {
            startClicker()
            floatBtn.text = "⏹"
            floatBtn.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E53935"))
            }
        }
    }

    fun startClicker() {
        isRunning = true
        clickCount = 0
        handler.post(runnable)
    }

    fun stopClicker() {
        isRunning = false
        handler.removeCallbacks(runnable)
    }

    // ── TEXT CLICK ───────────────────────────────────────────
    private fun performTextClick() {
        val root = rootInActiveWindow ?: return

        // নিজের app-এ ক্লিক করবে না
        val pkg = root.packageName?.toString() ?: ""
        if (pkg == "com.autoclicker.ultra") {
            root.recycle()
            onClickCallback?.invoke("⏭ নিজের app — skip")
            return
        }

        val found = findNodeByText(root, targetText.trim())
        if (found != null) {
            val clicked = found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) found.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            clickCount++
            handler.post {
                if (::floatCount.isInitialized) floatCount.text = "$clickCount ক্লিক"
            }
            onClickCallback?.invoke("✅ ক্লিক #$clickCount — \"$targetText\" — $pkg")
            found.recycle()
        } else {
            onClickCallback?.invoke("🔍 \"$targetText\" খুঁজছি... ($pkg)")
        }
        root.recycle()
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (text.isEmpty()) return null
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""
        if (nodeText.contains(text, ignoreCase = true) ||
            nodeDesc.contains(text, ignoreCase = true)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    // ── SLIDE ────────────────────────────────────────────────
    private fun performSlide() {
        val path = Path()
        path.moveTo(slideFromX, slideFromY)
        path.lineTo(slideToX, slideToY)
        val stroke = GestureDescription.StrokeDescription(path, 0, slideDurationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                clickCount++
                handler.post {
                    if (::floatCount.isInitialized) floatCount.text = "$clickCount ক্লিক"
                }
                onClickCallback?.invoke("↕️ স্লাইড #$clickCount সম্পন্ন!")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onClickCallback?.invoke("⚠️ স্লাইড বাতিল")
            }
        }, null)
    }
}
