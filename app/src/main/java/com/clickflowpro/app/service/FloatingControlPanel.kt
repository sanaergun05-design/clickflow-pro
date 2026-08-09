package com.clickflowpro.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Kompakt, suruklenebilir "baloncuk" kontrol paneli.
 *
 * Onceki versiyondan farkli olarak:
 * - Servis baglandigi an (onServiceConnected) OTOMATIK gorunmez artik.
 *   Sadece bot Start ile baslatildiginda ekrana eklenir (show()) - "botu
 *   baslatmadan onume cikiyor" sikayetini cozer. Stop basilinca ekrandan
 *   KALDIRILMAZ artik; sadece "oynat" durumuna doner (bkz. setRunning) ki
 *   kullanici Stop'a bastiktan hemen sonra baloncugun kaybolmasindan
 *   sikayet etmesin ve isterse ayni baloncuktan tekrar Start'a basabilsin.
 *   Baloncuk yalnizca servis tamamen kapanirken (remove()/hide()) kalkar.
 * - Buyuk kart yerine tek kucuk daire (52dp) + gerektiginde beliren
 *   minik bir uyari rozeti var. Baslik, istatistik metni ve ayri
 *   surukleme tutamaci kaldirildi - ekranin cok az yerini kaplar.
 * - Surukleme ve tek-dokunma (tap) ayni View uzerinde, hareket esigine
 *   (touch slop) gore ayirt ediliyor; boylece surukleme yanlislikla
 *   start/stop'u tetiklemiyor.
 */
class FloatingControlPanel(
    private val context: Context,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
    private val onClose: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private fun dp(value: Int): Int = (value * density).toInt()

    private var isAttached = false
    private var isRunning = false
    private var failCount = 0

    private lateinit var circleButton: TextView
    private lateinit var warningText: TextView

    private val root: LinearLayout = createView()

    private val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(16)
        y = dp(140)
    }

    private fun createView(): LinearLayout {
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        circleButton = TextView(context).apply {
            text = "\u23F9"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(214, 69, 69))
            }
            elevation = dp(6).toFloat()
            isClickable = true
            setOnTouchListener(DragOrTapListener())
        }
        val size = dp(52)
        wrapper.addView(circleButton, LinearLayout.LayoutParams(size, size))

        warningText = TextView(context).apply {
            text = ""
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.rgb(150, 40, 40))
                cornerRadius = dp(10).toFloat()
            }
            visibility = View.GONE
        }
        val warningParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(6) }
        wrapper.addView(warningText, warningParams)

        return wrapper
    }

    private fun toggle() {
        if (isRunning) onStop() else onStart()
    }

    /** Bot Start ile baslatildiginda servis bunu cagirir - baloncuk simdi belirir. */
    fun show() {
        if (!isAttached) {
            runCatching {
                windowManager.addView(root, windowParams)
                isAttached = true
            }
        }
    }

    /** Bot durdugunda servis bunu cagirir - baloncuk ekrandan tamamen kalkar. */
    fun hide() {
        if (isAttached) {
            runCatching { windowManager.removeView(root) }
            isAttached = false
        }
    }

    fun setRunning(running: Boolean) {
        isRunning = running
        if (running) failCount = 0
        circleButton.text = if (running) "\u23F9" else "\u25B6"
        (circleButton.background as? GradientDrawable)?.setColor(
            if (running) Color.rgb(214, 69, 69) else Color.rgb(34, 160, 107),
        )
        refreshWarning()
    }

    // Kompakt tasarimda tik sayaci ve nokta sayisi artik baloncukta gosterilmiyor
    // (uygulama ekranindaki kartlarda zaten gorunuyor); metotlar API uyumlulugu icin duruyor.
    fun updateClickCount(newCount: Long) = Unit

    fun setPointCount(newCount: Int) = Unit

    /** Bir dokunma (gesture) sistem tarafindan reddedildiginde servis bunu cagirir. */
    fun reportGestureFailed() {
        failCount++
        mainHandler.post { refreshWarning() }
    }

    private fun refreshWarning() {
        if (isRunning && failCount > 0) {
            warningText.visibility = View.VISIBLE
            val rejectedLabel = context.getString(com.clickflowpro.app.R.string.overlay_rejected_suffix)
            warningText.text = "\u26A0 $failCount $rejectedLabel"
        } else {
            warningText.visibility = View.GONE
        }
    }

    /** Servis tamamen kapanirken (onDestroy/onUnbind) cagrilir. */
    fun remove() = hide()

    private inner class DragOrTapListener : View.OnTouchListener {
        private var downX = 0
        private var downY = 0
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX.toInt()
                    downY = event.rawY.toInt()
                    startX = windowParams.x
                    startY = windowParams.y
                    dragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - downX
                    val dy = event.rawY.toInt() - downY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        windowParams.x = startX + dx
                        windowParams.y = startY + dy
                        if (isAttached) windowManager.updateViewLayout(root, windowParams)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        view.performClick()
                        // toggle() cagirilinca (Stop durumunda) panel kendi kendini
                        // WindowManager'dan kaldirabiliyor (hide()); bunu hala AYNI
                        // dokunus olayinin (ACTION_UP) icindeyken senkron yapmak
                        // bazi cihazlarda "tikladim ama hicbir sey olmadi" hissi
                        // yaratiyordu. Bir sonraki main-thread donguesune erteliyoruz.
                        mainHandler.post { toggle() }
                    }
                    return true
                }
            }
            return false
        }
    }
}
