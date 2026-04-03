package com.autoclicker.ultra

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

private const val MATCH = -1
private const val WRAP  = -2

class MainActivity : AppCompatActivity() {

    private lateinit var statusTv: TextView
    private lateinit var logTv: TextView
    private lateinit var startStopBtn: TextView
    private lateinit var serviceStatusTv: TextView
    private lateinit var overlayStatusTv: TextView
    private lateinit var textClickSection: LinearLayout
    private lateinit var targetTextInput: EditText
    private lateinit var slideSection: LinearLayout
    private lateinit var fromXInput: EditText
    private lateinit var fromYInput: EditText
    private lateinit var toXInput: EditText
    private lateinit var toYInput: EditText
    private lateinit var slideDurInput: EditText
    private lateinit var tabTextClick: TextView
    private lateinit var tabSlide: TextView
    private lateinit var intervalInput: EditText

    private var isTextMode = true
    private val handler = Handler(Looper.getMainLooper())
    private val logLines = ArrayDeque<String>(20)

    private val serviceChecker = object : Runnable {
        override fun run() {
            updateServiceStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        supportActionBar?.hide()
        setContentView(buildUI())
        handler.post(serviceChecker)
        AutoClickerService.onClickCallback = { msg -> runOnUiThread { addLog(msg) } }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayStatus()
    }

    private fun hasOverlayPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(this) else true

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0C29"))
        }

        // ── Top Bar ─────────────────────────────────────────
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 40, 16, 12)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
        }
        topBar.addView(mkTv("⚡ Auto Clicker Ultra", 18f, "#F9D423", true).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        })
        serviceStatusTv = mkTv("● OFF", 12f, "#FF5252", true)
        topBar.addView(serviceStatusTv)
        root.addView(topBar)

        val scroll = ScrollView(this).apply { isFillViewport = false }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // ── Permission Cards ─────────────────────────────────
        // Accessibility
        val accCard = buildCard()
        accCard.addView(mkTv("🔧 ধাপ ১ — Accessibility Permission", 14f, "#A78BFA", true))
        accCard.addView(mkTv("Accessibility Service চালু করতে হবে", 12f, "#AAAAAA", false).apply {
            setPadding(0, 4, 0, 10)
        })
        accCard.addView(mkBtn("⚙️  Accessibility Settings", "#667EEA", "#764BA2") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        inner.addView(accCard, mpCard())

        // Overlay permission
        val overlayCard = buildCard()
        overlayCard.addView(mkTv("🪟 ধাপ ২ — Overlay Permission", 14f, "#A78BFA", true))
        overlayStatusTv = mkTv("❌ চালু নেই", 12f, "#FF5252", false).apply {
            setPadding(0, 4, 0, 10)
        }
        overlayCard.addView(overlayStatusTv)
        overlayCard.addView(mkBtn("🪟  Overlay Permission দাও", "#F9D423", "#FF6B35") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
        })
        inner.addView(overlayCard, mpCard())

        // ── Mode Tabs ────────────────────────────────────────
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 0)
        }
        tabTextClick = mkBtn("📝 টেক্সট ক্লিক", "#F9D423", "#FF6B35") { switchMode(true) }
        tabSlide     = mkBtn("↕️  স্লাইড", "#333355", "#333355") { switchMode(false) }
        tabRow.addView(tabTextClick, LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(0,0,6,0) })
        tabRow.addView(tabSlide,     LinearLayout.LayoutParams(0, WRAP, 1f))
        inner.addView(tabRow, LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0,0,0,8) })

        // ── Text Click Section ───────────────────────────────
        textClickSection = buildCard()
        textClickSection.addView(mkTv("🎯 যে লেখায় ক্লিক করবে:", 13f, "#CCCCCC", true).apply {
            setPadding(0, 0, 0, 8)
        })
        targetTextInput = EditText(this).apply {
            hint = "যেমন: Delete, OK, Skip, Next..."
            setHintTextColor(Color.parseColor("#666688"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(16, 14, 16, 14)
            background = inputBg()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    AutoClickerService.targetText = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }
        textClickSection.addView(targetTextInput, LinearLayout.LayoutParams(MATCH, WRAP))
        textClickSection.addView(mkTv("💡 Facebook → Friend Requests → Delete লিখো", 11f, "#8888AA", false).apply {
            setPadding(0, 8, 0, 0)
        })
        inner.addView(textClickSection, mpCard())

        // ── Slide Section ────────────────────────────────────
        slideSection = buildCard()
        slideSection.visibility = View.GONE
        slideSection.addView(mkTv("↕️ স্লাইড সেটিংস", 13f, "#CCCCCC", true).apply { setPadding(0,0,0,12) })
        slideSection.addView(mkTv("শুরুর অবস্থান (From):", 12f, "#AAAAAA", false).apply { setPadding(0,0,0,6) })
        val fromRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fromXInput = mkNumInput("X", "500"); fromYInput = mkNumInput("Y", "1200")
        fromRow.addView(fromXInput, LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(0,0,8,0) })
        fromRow.addView(fromYInput, LinearLayout.LayoutParams(0, WRAP, 1f))
        slideSection.addView(fromRow, LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0,0,0,12) })
        slideSection.addView(mkTv("শেষের অবস্থান (To):", 12f, "#AAAAAA", false).apply { setPadding(0,0,0,6) })
        val toRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toXInput = mkNumInput("X", "500"); toYInput = mkNumInput("Y", "400")
        toRow.addView(toXInput, LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(0,0,8,0) })
        toRow.addView(toYInput, LinearLayout.LayoutParams(0, WRAP, 1f))
        slideSection.addView(toRow, LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0,0,0,12) })
        slideSection.addView(mkTv("Duration (ms):", 12f, "#AAAAAA", false).apply { setPadding(0,0,0,6) })
        slideDurInput = mkNumInput("300", "300")
        slideSection.addView(slideDurInput, LinearLayout.LayoutParams(MATCH, WRAP))
        inner.addView(slideSection, mpCard())

        // ── Interval Card ────────────────────────────────────
        val intCard = buildCard()
        intCard.addView(mkTv("⏱️ বিরতি (Interval)", 13f, "#CCCCCC", true).apply { setPadding(0,0,0,8) })
        intervalInput = mkNumInput("Millisecond (1000 = ১ সেকেন্ড)", "1000")
        intCard.addView(intervalInput, LinearLayout.LayoutParams(MATCH, WRAP))
        val qRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,10,0,0) }
        for ((label, ms) in listOf("0.5s" to "500", "1s" to "1000", "2s" to "2000", "5s" to "5000")) {
            qRow.addView(mkSmallBtn(label) { intervalInput.setText(ms) },
                LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(2,0,2,0) })
        }
        intCard.addView(qRow)
        inner.addView(intCard, mpCard())

        // ── Status ───────────────────────────────────────────
        val statCard = buildCard()
        statusTv = mkTv("⏹ বন্ধ আছে", 15f, "#FF5252", true).apply { gravity = Gravity.CENTER }
        statCard.addView(statusTv)
        inner.addView(statCard, mpCard())

        // ── Start/Stop ───────────────────────────────────────
        startStopBtn = mkBtn("▶  শুরু করুন (Floating বাটন আসবে)", "#43A047", "#1B5E20") { toggleService() }
        startStopBtn.textSize = 16f
        startStopBtn.setPadding(0, 22, 0, 22)
        inner.addView(startStopBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0,0,0,12) })

        // ── Log ──────────────────────────────────────────────
        val logCard = buildCard()
        logCard.addView(mkTv("📋 লগ", 13f, "#CCCCCC", true).apply { setPadding(0,0,0,8) })
        logTv = mkTv("এখানে activity দেখাবে...", 11f, "#8888AA", false).apply {
            setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#11FFFFFF")); cornerRadius = 8f
            }
        }
        logCard.addView(logTv)
        inner.addView(logCard, mpCard())

        scroll.addView(inner)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        return root
    }

    private fun switchMode(textMode: Boolean) {
        isTextMode = textMode
        AutoClickerService.useTextClick = textMode
        if (textMode) {
            textClickSection.visibility = View.VISIBLE
            slideSection.visibility = View.GONE
            tabTextClick.background = gradBg("#F9D423", "#FF6B35")
            tabSlide.background = solidBg("#333355")
        } else {
            textClickSection.visibility = View.GONE
            slideSection.visibility = View.VISIBLE
            tabSlide.background = gradBg("#F9D423", "#FF6B35")
            tabTextClick.background = solidBg("#333355")
        }
    }

    private fun toggleService() {
        val svc = AutoClickerService.instance
        if (svc == null) {
            toast("⚠️ আগে Accessibility Service চালু করুন!")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (!hasOverlayPermission()) {
            toast("⚠️ Overlay Permission দিন!")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
            return
        }

        if (AutoClickerService.isRunning) {
            svc.stopClicker()
            svc.removeFloatingWindow()
            startStopBtn.text = "▶  শুরু করুন (Floating বাটন আসবে)"
            startStopBtn.background = gradBg("#43A047", "#1B5E20")
            statusTv.text = "⏹ বন্ধ"
            statusTv.setTextColor(Color.parseColor("#FF5252"))
            addLog("🛑 বন্ধ করা হয়েছে")
        } else {
            if (isTextMode && targetTextInput.text.toString().trim().isEmpty()) {
                toast("লেখা দিন যেটায় ক্লিক করবে!"); return
            }
            AutoClickerService.useTextClick   = isTextMode
            AutoClickerService.targetText     = targetTextInput.text.toString().trim()
            AutoClickerService.intervalMs     = intervalInput.text.toString().toLongOrNull() ?: 1000L
            if (!isTextMode) {
                AutoClickerService.slideFromX      = fromXInput.text.toString().toFloatOrNull() ?: 500f
                AutoClickerService.slideFromY      = fromYInput.text.toString().toFloatOrNull() ?: 1200f
                AutoClickerService.slideToX        = toXInput.text.toString().toFloatOrNull() ?: 500f
                AutoClickerService.slideToY        = toYInput.text.toString().toFloatOrNull() ?: 400f
                AutoClickerService.slideDurationMs = slideDurInput.text.toString().toLongOrNull() ?: 300L
            }
            svc.startClicker()
            svc.showFloatingWindow()

            startStopBtn.text = "⏹  বন্ধ করুন"
            startStopBtn.background = gradBg("#E53935", "#B71C1C")
            val modeStr = if (isTextMode) "\"${AutoClickerService.targetText}\"" else "স্লাইড"
            statusTv.text = "▶ চলছে — $modeStr তে ক্লিক\n📌 এখন Facebook এ যান!"
            statusTv.setTextColor(Color.parseColor("#69F0AE"))
            addLog("▶ শুরু — Floating button দেখাচ্ছে")
        }
    }

    private fun updateServiceStatus() {
        val active = AutoClickerService.instance != null
        serviceStatusTv.text = if (active) "● চালু" else "● OFF"
        serviceStatusTv.setTextColor(
            if (active) Color.parseColor("#69F0AE") else Color.parseColor("#FF5252"))
    }

    private fun updateOverlayStatus() {
        val has = hasOverlayPermission()
        overlayStatusTv.text = if (has) "✅ Permission আছে" else "❌ Permission নেই"
        overlayStatusTv.setTextColor(
            if (has) Color.parseColor("#69F0AE") else Color.parseColor("#FF5252"))
    }

    private fun addLog(msg: String) {
        logLines.addFirst(msg)
        if (logLines.size > 15) logLines.removeLast()
        logTv.text = logLines.joinToString("\n")
    }

    private fun buildCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20, 18, 20, 18)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1A1A2E"))
            cornerRadius = 18f
            setStroke(1, Color.parseColor("#33FFFFFF"))
        }
        elevation = 4f
    }

    private fun gradBg(c1: String, c2: String) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = 50f
        colors = intArrayOf(Color.parseColor(c1), Color.parseColor(c2))
        orientation = GradientDrawable.Orientation.LEFT_RIGHT
    }

    private fun solidBg(c: String) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = 50f
        setColor(Color.parseColor(c))
    }

    private fun inputBg() = GradientDrawable().apply {
        setColor(Color.parseColor("#22FFFFFF"))
        cornerRadius = 12f
        setStroke(1, Color.parseColor("#44FFFFFF"))
    }

    private fun mkTv(text: String, size: Float, color: String, bold: Boolean) =
        TextView(this).apply {
            this.text = text; textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun mkBtn(text: String, c1: String, c2: String, onClick: () -> Unit) =
        TextView(this).apply {
            this.text = text; textSize = 14f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 18, 24, 18); elevation = 6f
            background = gradBg(c1, c2)
            setOnClickListener { onClick() }
        }

    private fun mkSmallBtn(text: String, onClick: () -> Unit) =
        TextView(this).apply {
            this.text = text; textSize = 12f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFFFFF")); cornerRadius = 30f
            }
            setOnClickListener { onClick() }
        }

    private fun mkNumInput(hint: String, default: String) =
        EditText(this).apply {
            this.hint = hint; setText(default)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setHintTextColor(Color.parseColor("#666688"))
            setTextColor(Color.WHITE); textSize = 14f
            setPadding(16, 12, 16, 12)
            background = inputBg()
        }

    private fun mpCard() = LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0,0,0,12) }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(serviceChecker)
    }
}
