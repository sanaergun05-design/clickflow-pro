package com.clickflowpro.app.service

import android.animation.ValueAnimator
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.clickflowpro.app.data.ProfileStore
import com.clickflowpro.app.model.ClickPoint
import com.clickflowpro.app.model.MarkerShape
import com.clickflowpro.app.model.MarkerStyle
import com.clickflowpro.app.util.LocaleHelper
import kotlin.math.max

class AutoClickAccessibilityService : AccessibilityService() {

    companion object {
        // MainActivity, bu statik referans üzerinden servise DOĞRUDAN erişir.
        // startService()/Intent kullanmıyoruz çünkü servis BIND_ACCESSIBILITY_SERVICE
        // izni istiyor ve bu izin hiçbir uygulamaya (kendi appimiz dahil) verilmez -
        // Intent ile komut göndermek SecurityException'a sebep olur ve sessizce başarısız olur.
        var instance: AutoClickAccessibilityService? = null
            private set
    }

    // Kayan panel/nokta seçici gibi overlay View'lerin bu context'ten (getString
    // vb.) doğru dili alabilmesi için servis Context'ini de sarmalıyoruz.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private val handler = Handler(Looper.getMainLooper())
    private val points = mutableListOf<ClickPoint>()
    private var intervalMs = 125L
    private var running = false
    private var totalClicks = 0L
    private var currentPointIndex = 0
    private var overlayController: FloatingControlPanel? = null
    private var pointPicker: PointPickerOverlay? = null
    private var markerStyle: MarkerStyle = MarkerStyle()

    // Tiklama isareti (pulse) icin TEK bir view - eskiden her tikta yeni bir
    // pencere ekleyip kaldiriyorduk, bu yuksek CPS'te WindowManager'i saniyede
    // onlarca kez zorluyordu; ayni pencere yoneticisini paylasan baloncugun
    // (Stop butonu) dokunuslara gec/hic cevap vermemesine sebep oluyordu.
    // Simdi pencere SADECE BIR KERE eklenip konumu/animasyonu guncelleniyor.
    private var pulseView: MarkerPulseView? = null
    private var pulseParams: WindowManager.LayoutParams? = null
    private var pulseAnimator: ValueAnimator? = null

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (!running || points.isEmpty()) return
            val point = points[currentPointIndex % points.size]
            currentPointIndex = (currentPointIndex + 1) % points.size
            dispatchTap(point)
            handler.postDelayed(this, max(20L, intervalMs))
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Kaydedilmis isaretleyici (pulse) sekli/rengi varsa yukle - ayarlar
        // ekraninda degistirilen tercih boylece servis yeniden baglaninca da kalici olur.
        markerStyle = ProfileStore(this).loadMarkerStyle()
        overlayController = FloatingControlPanel(
            context = this,
            onStart = { startClicking() },
            onStop = { stopClicking() },
            onClose = { stopClicking(); removeOverlay() },
        )
        overlayController?.setPointCount(points.size)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        stopClicking()
    }

    override fun onDestroy() {
        stopClicking()
        removeOverlay()
        pointPicker?.remove()
        pointPicker = null
        if (instance == this) instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopClicking()
        removeOverlay()
        pointPicker?.remove()
        pointPicker = null
        if (instance == this) instance = null
        return super.onUnbind(intent)
    }

    // --- MainActivity'nin doğrudan çağırdığı public API (IPC yok, çökme riski yok) ---

    /** Ana ekrandaki Start butonu bunu çağırır. */
    fun applyAndStart(newPoints: List<ClickPoint>, newIntervalMs: Long) {
        intervalMs = newIntervalMs
        points.clear()
        points.addAll(newPoints)
        overlayController?.setPointCount(points.size)
        startClicking()
    }

    /** Ana ekrandaki Stop butonu bunu çağırır. */
    fun requestStop() {
        stopClicking()
    }

    fun isCurrentlyRunning(): Boolean = running

    /**
     * Ayarlar ekranindan secilen isaretleyici sekli/rengini aninda uygular
     * (kaydetme islemi ProfileStore uzerinden MainActivity tarafinda yapilir,
     * burada sadece calisan servis ornegine yansitiyoruz).
     */
    fun updateMarkerStyle(style: MarkerStyle) {
        markerStyle = style
    }

    /**
     * Ekranda sürüklenebilir bir nişangah (crosshair) gösterir. Kullanıcı sürükleyip
     * bırakınca ya da onayla'ya basınca onPicked(x, y) çağrılır ve overlay kaldırılır.
     */
    fun startPointPicker(
        initialX: Int,
        initialY: Int,
        onPicked: (Int, Int) -> Unit,
        onCancelled: () -> Unit = {},
    ) {
        pointPicker?.remove()
        pointPicker = PointPickerOverlay(
            context = this,
            initialX = initialX,
            initialY = initialY,
            onConfirm = { x, y ->
                pointPicker?.remove()
                pointPicker = null
                onPicked(x, y)
            },
            onCancel = {
                pointPicker?.remove()
                pointPicker = null
                onCancelled()
            },
        )
    }

    private fun dispatchTap(point: ClickPoint) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply {
            moveTo(point.x.toFloat(), point.y.toFloat())
        }
        // ÖNEMLİ: Önceki sürümde stroke süresi 1ms'ydi. Bazı OEM Android sürümleri
        // (özellikle MIUI) bu kadar kısa süreli gesture'ları sessizce reddediyor,
        // dispatchGesture() true dönse bile onCompleted hiç tetiklenmiyor - "0 taps"
        // sorununun asıl sebeplerinden biri bu. Süreyi en az 16ms'ye çıkarıyoruz,
        // ama interval'i aşmayacak şekilde sınırlıyoruz.
        val strokeDuration = intervalMs.coerceIn(16L, 60L)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, strokeDuration))
            .build()
        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    totalClicks++
                    overlayController?.updateClickCount(totalClicks)
                    showTapPulse(point.x, point.y, success = true)
                    sendBroadcast(
                        Intent(ClickerContract.ACTION_CLICK_COUNT).setPackage(packageName)
                            .putExtra(ClickerContract.EXTRA_TOTAL_CLICKS, totalClicks),
                    )
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    // Gesture reddedildi (MIUI gibi OEM kısıtlamaları, ya da başka bir
                    // gesture çakışması). Panelde görünür kılmak için durumu işaretle.
                    overlayController?.reportGestureFailed()
                    showTapPulse(point.x, point.y, success = false)
                }
            },
            handler,
        )
        if (!dispatched) {
            overlayController?.reportGestureFailed()
            showTapPulse(point.x, point.y, success = false)
        }
    }

    /**
     * Gercek tiklamanin EKRANDA HANGI NOKTAYA indigini gormek icin, o
     * koordinatta kisa sureli (yesil=basarili, kirmizi=reddedildi) bir
     * halka gosterir.
     *
     * ONEMLI: pencere SADECE ILK cagrida eklenir (ensurePulseView); sonraki
     * her tikta ayni view'in konumu ve rengi guncellenir, animasyon yeniden
     * baslatilir. Eskiden her tikta addView/removeView yapiliyordu - bu hem
     * pulse'in gorunmemesine (view daha layout almadan kaldiriliyordu) hem de
     * yuksek CPS'te WindowManager'in tikanip baloncugun (Stop) dokunuslara
     * gec cevap vermesine sebep oluyordu.
     */
    private fun showTapPulse(x: Int, y: Int, success: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val view = ensurePulseView() ?: return
        val params = pulseParams ?: return
        val density = resources.displayMetrics.density
        val size = (26 * density).toInt()

        pulseAnimator?.cancel()
        // Basarili tiklamada kullanicinin ayarlar ekranindan sectigi sekil/renk
        // kullanilir; reddedilen (cancelled) gesture'larda farkin hemen fark
        // edilmesi icin sabit kirmizi renkte kalir.
        view.applyStyle(
            shape = markerStyle.shape,
            colorArgb = if (success) markerStyle.colorArgb else Color.rgb(255, 69, 58),
        )

        params.x = x - size / 2
        params.y = y - size / 2
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        runCatching { wm.updateViewLayout(view, params) }

        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
        view.visibility = View.VISIBLE

        pulseAnimator = ValueAnimator.ofFloat(1f, 2.4f).apply {
            duration = 320
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                view.scaleX = t
                view.scaleY = t
                view.alpha = 1f - (t - 1f) / 1.4f
            }
            start()
        }
    }

    /** Pulse penceresini bir kere olusturup ekler; sonraki cagrilarda ayni view dondurulur. */
    private fun ensurePulseView(): MarkerPulseView? {
        pulseView?.let { return it }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val size = (26 * density).toInt()
        val view = MarkerPulseView(this).apply {
            visibility = View.GONE
        }
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        val attached = runCatching { wm.addView(view, params) }.isSuccess
        if (!attached) return null
        pulseView = view
        pulseParams = params
        return view
    }

    /**
     * Sekli (halka/nokta/arti/kare/elmas) ve rengi ayarlar ekranindan
     * secilebilen basit bir isaretleyici View'i. GradientDrawable yerine
     * dogrudan Canvas'a ciziyoruz cunku "arti" ve "elmas" gibi sekiller
     * standart drawable seklleriyle ifade edilemiyor.
     */
    private class MarkerPulseView(context: android.content.Context) : View(context) {
        private var shape: MarkerShape = MarkerShape.RING
        private val density = context.resources.displayMetrics.density
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            color = Color.rgb(76, 217, 100)
        }

        fun applyStyle(shape: MarkerShape, colorArgb: Int) {
            this.shape = shape
            paint.color = colorArgb
            paint.style = if (shape == MarkerShape.DOT) Paint.Style.FILL else Paint.Style.STROKE
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = (width.coerceAtMost(height) / 2f) - paint.strokeWidth
            when (shape) {
                MarkerShape.RING -> canvas.drawCircle(cx, cy, r, paint)
                MarkerShape.DOT -> canvas.drawCircle(cx, cy, r, paint)
                MarkerShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
                MarkerShape.DIAMOND -> {
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
                MarkerShape.CROSS -> {
                    canvas.drawLine(cx - r, cy, cx + r, cy, paint)
                    canvas.drawLine(cx, cy - r, cx, cy + r, paint)
                }
            }
        }
    }

    /** Bot tamamen durunca (Stop) pulse penceresini de kaldiriyoruz. */
    private fun removePulseView() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseView?.let { view ->
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            runCatching { wm.removeView(view) }
        }
        pulseView = null
        pulseParams = null
    }

    private fun startClicking() {
        if (points.isEmpty()) return
        running = true
        currentPointIndex = 0
        // Baloncuk sadece bot fiilen calisirken ekrana eklenir - "baslatmadan
        // onume cikiyor" sikayetini cozuyor.
        overlayController?.show()
        overlayController?.setRunning(true)
        handler.removeCallbacks(clickRunnable)
        handler.post(clickRunnable)
    }

    private fun stopClicking() {
        running = false
        handler.removeCallbacks(clickRunnable)
        // ONEMLI: Daha once burada overlayController?.hide() cagirilip baloncuk
        // Stop'a basilir basilmaz tamamen kaldiriliyordu. Kullanici "durdurma
        // calisiyor ama baloncuk kayboluyor" diye sikayet etti - Stop'tan hemen
        // sonra "kac tik reddedildi" uyarisini gormek ya da baloncuktan tekrar
        // Start'a basmak istiyor. Artik baloncuk kalici: setRunning(false) onu
        // yesil/oynatma simgesine cevirir ama ekrandan kaldirmaz. Sadece servis
        // tamamen kapanirken (onDestroy/onUnbind, bkz. removeOverlay) kaldirilir.
        overlayController?.setRunning(false)
        removePulseView()
    }

    private fun removeOverlay() {
        overlayController?.remove()
        overlayController = null
    }

}