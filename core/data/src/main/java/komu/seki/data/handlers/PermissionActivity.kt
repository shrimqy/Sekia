package komu.seki.data.handlers

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import komu.seki.data.services.ScreenMirrorService
import komu.seki.data.services.ScreenMirrorService.Companion.ACTION_START_SCREEN_CAPTURE

class PermissionRequestActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Pass the result to the service
            val intent = Intent(this, ScreenMirrorService::class.java)
            intent.putExtra("resultCode", resultCode)
            intent.putExtra("data", data)
            intent.action = ACTION_START_SCREEN_CAPTURE
            startService(intent)
        }
        finish() // Close the activity
    }

    companion object {
        const val REQUEST_CODE = 1000
    }
}