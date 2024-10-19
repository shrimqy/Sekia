package komu.seki.data.handlers

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import komu.seki.data.services.ScreenMirrorService
import komu.seki.data.services.ScreenMirrorService.Companion.ACTION_START_SCREEN_CAPTURE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionRequestActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Start screen capture intent
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    // Register the callback for screen capture result
    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // Pass the result to the service
            val intent = Intent(this, ScreenMirrorService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
                action = ACTION_START_SCREEN_CAPTURE
            }
            startForegroundService(intent)
        }
        finish() // Close the activity
    }

    companion object {
        const val REQUEST_CODE = 1000
    }
}