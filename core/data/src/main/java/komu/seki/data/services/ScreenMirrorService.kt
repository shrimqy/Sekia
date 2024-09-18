package komu.seki.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.data.R
import komu.seki.domain.models.ScreenMirrorData
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ScreenMirrorService : Service(){

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d("ScreenMirrorService", "MediaProjection stopped")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SCREEN_CAPTURE -> {
                stopScreenCapture()
                return START_NOT_STICKY
            }
        }
        try {
            // Create the notification and start the service in the foreground
            startForeground(345, createNotification())
            Log.d("ScreenMirrorService", "Service started in foreground")

            // Retrieve the extras from the Intent
            val resultCode = intent?.getIntExtra("resultCode", 0)
            val data = intent?.getParcelableExtra<Intent>("data")

            if (resultCode == null || resultCode == 0 || data == null) {
                Log.e("ScreenMirrorService", "Invalid intent extras: resultCode=$resultCode, data=$data")
                return START_NOT_STICKY
            }

            Log.d("ScreenMirrorService", "Intent extras retrieved successfully")

            val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            mediaProjection.registerCallback(mediaProjectionCallback, null)
            Log.d("ScreenMirrorService", "MediaProjection created")

            startScreenCapture()
        } catch (e: Exception) {
            Log.e("ScreenMirrorService", "Error in onStartCommand", e)
        }

        return START_STICKY
    }

    private fun startScreenCapture() {
        Log.d("ScreenMirrorService", "Starting screen capture")
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        Log.d("ScreenMirrorService", "Created ImageReader and VirtualDisplay")

        CoroutineScope(Dispatchers.Main + job).launch {
            while (isActive) {
                val image = imageReader.acquireLatestImage()

                image?.use { img ->
                    val planes = img.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth

                    // Create bitmap
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride, screenHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    // Compress bitmap to JPEG
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
                    val byteArray = outputStream.toByteArray()

                    // Convert byteArray to Base64 string
                    val base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)

                    webSocketRepository.sendMessage(ScreenMirrorData(base64String, System.currentTimeMillis()))
                }
                delay(33) // ~30 FPS
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun stopScreenCapture() {
        virtualDisplay.release()
        mediaProjection.unregisterCallback(mediaProjectionCallback)
        mediaProjection.stop()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d("ScreenMirrorService", "Service onDestroy called")
        job.cancel()
        try {
            virtualDisplay.release()
            mediaProjection.unregisterCallback(mediaProjectionCallback)
            mediaProjection.stop()
        } catch (e: Exception) {
            Log.e("ScreenMirrorService", "Error during service destruction", e)
        }
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "SCREEN_MIRROR_SERVICE_CHANNEL"

        val channel = NotificationChannel(
            notificationChannelId,
            "Screen Mirror Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        // Create an Intent for the stop action
        val stopIntent = Intent(this, ScreenMirrorService::class.java).apply {
            action = ACTION_STOP_SCREEN_CAPTURE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Screen Mirroring")
            .setContentText("Screen mirroring is active")
            .setSmallIcon(com.komu.seki.core.common.R.drawable.ic_splash)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return notificationBuilder.build()
    }

    companion object {
        const val ACTION_STOP_SCREEN_CAPTURE = "com.komu.seki.ACTION_STOP_SCREEN_CAPTURE"
    }

}