package com.clickflowpro.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Ekranda sürüklenebilir bir nişangah (crosshair) gösterir. Kullanıcı parmağıyla
 * istediği noktaya sürükler, sonra "Onayla" butonuna basar; o anki ekran
 * koordinatları (gerçek piksel, tıklama için kullanılacak nokta) callback ile döner.
 */
class PointPickerOverlay(
    private val context: Context,
    initialX: Int,
    initialY: Int,
    private val onConfirm: (Int, Int) -> Unit,
    private val onCancel: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).toInt()
    private var isAttached = false

    // Nişangahın merkezinin ekran koordinatları (mutlak, tıklama için kullanılacak)
    private var pointX = initialX
    private var pointY = initialY

    private val crosshairSize = dp(56)
    private val crosshair: View = createCrosshair()
    private val toolbar: LinearLayout = createToolbar()

    private val crosshairParams = WindowManager.LayoutParams(
        crosshairSize,
        crosshairSize,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = pointX - crosshairSize / 2
        y = pointY - crosshairSize / 2
    }

    private val toolbarParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 80
    }

    init {
        show()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createCrosshair(): View {
        val view = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(140, 255, 64, 129))
            setOnTouchListener(CrosshairDragListener())
        }
        val ring = View(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        view.addView(ring, FrameLayout.LayoutParams(crosshairSize, crosshairSize))
        return view
    }

    private fun createToolbar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.rgb(28, 30, 45))
        }
        val label = TextView(context).apply {
            text = context.getString(com.clickflowpro.app.R.string.overlay_drag_confirm_hint)
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, dp(10), dp(20), 0)
        }
        val confirm = ImageButton(context).apply {
            setImageDrawable(context.getDrawable(android.R.drawable.checkbox_on_background))
            setColorFilter(Color.parseColor("#19B6A5"))
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = context.getString(com.clickflowpro.app.R.string.overlay_confirm_desc)
            setOnClickListener { onConfirm(pointX, pointY) }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        val cancel = ImageButton(context).apply {
            setImageDrawable(context.getDrawable(android.R.drawable.ic_menu_close_clear_cancel))
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = context.getString(com.clickflowpro.app.R.string.overlay_cancel_desc)
            setOnClickListener { onCancel() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        bar.addView(label)
        bar.addView(confirm)
        bar.addView(cancel)
        return bar
    }

    private fun show() {
        if (isAttached) return
        runCatching {
            windowManager.addView(crosshair, crosshairParams)
            windowManager.addView(toolbar, toolbarParams)
            isAttached = true
        }
    }

    fun remove() {
        if (!isAttached) return
        runCatching { windowManager.removeView(crosshair) }
        runCatching { windowManager.removeView(toolbar) }
        isAttached = false
    }

    private inner class CrosshairDragListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startWinX = 0
        private var startWinY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startWinX = crosshairParams.x
                    startWinY = crosshairParams.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    crosshairParams.x = startWinX + dx
                    crosshairParams.y = startWinY + dy
                    if (isAttached) windowManager.updateViewLayout(crosshair, crosshairParams)
                    // Nişangahın merkezi = gerçek tıklama noktası
                    pointX = crosshairParams.x + crosshairSize / 2
                    pointY = crosshairParams.y + crosshairSize / 2
                    return true
                }
            }
            return false
        }
    }
}
