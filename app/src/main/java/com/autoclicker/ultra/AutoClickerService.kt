package com.autoclicker.ultra

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickerService : AccessibilityService() {

    companion object {
        var instance: AutoClickerService? = null

        var isRunning = false
        var targetText = ""
        var intervalMs = 1000L
        var useTextClick = true

        // Slide settings
        var slideFromX = 500f
        var slideFromY = 1200f
        var slideToX = 500f
        var slideToY = 400f
        var slideDurationMs = 300L

        var onClickCallback: ((String) -> Unit)? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var clickCount = 0

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

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopService()
    }

    fun startService() {
        isRunning = true
        clickCount = 0
        handler.post(runnable)
    }

    fun stopService() {
        isRunning = false
        handler.removeCallbacks(runnable)
    }

    // ── TEXT CLICK ──────────────────────────────────────────
    private fun performTextClick() {
        val root = rootInActiveWindow ?: return
        val found = findNodeByText(root, targetText.trim())
        if (found != null) {
            val clicked = found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) found.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            clickCount++
            onClickCallback?.invoke("✅ ক্লিক #$clickCount — \"$targetText\" পেয়েছি!")
            found.recycle()
        } else {
            onClickCallback?.invoke("🔍 \"$targetText\" খুঁজছি...")
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

    // ── SLIDE / SWIPE ───────────────────────────────────────
    private fun performSlide() {
        val path = Path()
        path.moveTo(slideFromX, slideFromY)
        path.lineTo(slideToX, slideToY)

        val stroke = GestureDescription.StrokeDescription(path, 0, slideDurationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                clickCount++
                onClickCallback?.invoke("↕️ স্লাইড #$clickCount সম্পন্ন!")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onClickCallback?.invoke("⚠️ স্লাইড বাতিল হয়েছে")
            }
        }, null)
    }
}
