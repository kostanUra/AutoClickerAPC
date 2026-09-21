package com.example.autoclicker

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var view: FloatingButtonView
    private lateinit var params: WindowManager.LayoutParams
    private var clicking = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addView()
    }

    private fun addView() {
        view = FloatingButtonView(this)
        val size = (72 * resources.displayMetrics.density).toInt()

        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        var dragging = false
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!dragging && (abs(dx) > 12 || abs(dy) > 12)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        wm.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) toggle()
                    true
                }
                else -> false
            }
        }

        wm.addView(view, params)
    }

    private fun toggle() {
        clicking = !clicking
        view.isClicking = clicking
        if (clicking) startClicking() else stopClicking()
    }

    private fun startClicking() {
        job = scope.launch {
            while (isActive && clicking) {
                val x = params.x + params.width / 2f
                val y = params.y + params.height / 2f
                withContext(Dispatchers.Main) {
                    AutoClickAccessibilityService.instance?.click(x, y)
                }
                delay(60L)
            }
        }
    }

    private fun stopClicking() {
        job?.cancel(); job = null
    }

    override fun onDestroy() {
        scope.cancel()
        if (::view.isInitialized) wm.removeView(view)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "ac_ch"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(channelId, "AutoClicker",
                NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AutoClicker работает")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setOngoing(true)
            .build()
    }
}