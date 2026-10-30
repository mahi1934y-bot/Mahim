package com.mahim.aisignalpro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {
    companion object {
        const val ACTION_START = "com.mahim.aisignalpro.START_CAPTURE"
        const val ACTION_STOP = "com.mahim.aisignalpro.STOP_CAPTURE"
        const val ACTION_CAPTURE = "com.mahim.aisignalpro.CAPTURE_FRAME"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 4102
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CAPTURE) captureOneFrame()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter(ACTION_CAPTURE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(captureReceiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(captureReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProjection(intent)
            ACTION_STOP -> stopCapture()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(intent: Intent) {
        if (projection != null) return
        startForeground(NOTIFICATION_ID, buildNotification())
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent

        if (resultCode == 0 || data == null) { stopSelf(); return }

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

        thread = HandlerThread("MahimScreenCapture").also { it.start() }
        handler = Handler(thread!!.looper)
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels,
            PixelFormat.RGBA_8888, 3)

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data).also { mp ->
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopCapture() }
            }, handler)
        }

        virtualDisplay = projection!!.createVirtualDisplay(
            "MahimAIProjection",
            metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )
    }

    private fun captureOneFrame() {
        val reader = imageReader ?: return
        try {
            val image: Image = reader.acquireLatestImage() ?: run {
                broadcastStatus("FRAME_NOT_READY")
                return
            }
            image.use {
                val plane = it.planes[0]
                val buffer: ByteBuffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * it.width
                val paddedWidth = it.width + rowPadding / pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, it.height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(padded, 0, 0, it.width, it.height)
                padded.recycle()
                // Stage 1 validation only: real bitmap is obtained on each bubble tap.
                // Stage 2 will pass this bitmap to chart-quality checks and Gemini Vision.
                cropped.recycle()
                broadcastStatus("FRAME_CAPTURED")
            }
        } catch (_: Exception) {
            broadcastStatus("CAPTURE_FAILED")
        }
    }

    private fun broadcastStatus(status: String) {
        sendBroadcast(Intent("com.mahim.aisignalpro.CAPTURE_STATUS")
            .setPackage(packageName).putExtra("status", status))
    }

    private fun stopCapture() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        thread?.quitSafely()
        thread = null; handler = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Screen capture",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAHIM AI SIGNAL PRO")
            .setContentText("Screen sharing is active. Tap STOP in the app to end.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        try { unregisterReceiver(captureReceiver) } catch (_: Exception) {}
        if (projection != null || virtualDisplay != null) stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
