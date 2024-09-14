package komu.seki.data.handlers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import komu.seki.common.models.FileMetadata
import komu.seki.domain.models.DataTransferType
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.TransferType
import java.io.IOException
import java.io.OutputStream


private var bytesReceived = 0L // Track the total number of bytes received

private var pendingCompletion = false // Flag to indicate completion message waiting

private var currentFileOutputStream: OutputStream? = null
private var currentFileMetadata: FileMetadata? = null

var uri: Uri? = null

fun receivingFileHandler(context: Context, message: FileTransfer){
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationId = 4159

    val totalFileSize = currentFileMetadata?.fileSize ?: 0L // Total file size for progress
    var progress = 0 // To track percentage

    when (message.dataTransferType) {
        DataTransferType.METADATA -> {
            // Start receiving file
            currentFileMetadata = message.metadata
            currentFileMetadata?.let { metadata ->
                val contentResolver = context.contentResolver

                // Prepare the content values
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, metadata.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, metadata.mimeType ?: "*/*") // Use MIME type if available
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
                }

                // Insert the file entry and get the Uri
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    try {
                        // Open an output stream to write the file
                        currentFileOutputStream = contentResolver.openOutputStream(uri!!)
                        Log.d("FileTransfer", "Receiving file: ${metadata.fileName}")
                        bytesReceived = 0L // Reset bytes received counter
                        // Initialize the notification with 0 progress
                        val notification = createProgressNotification(context, metadata.fileName, 0, 100)
                        notificationManager.notify(notificationId, notification)
                    } catch (e: IOException) {
                        Log.e("FileTransfer", "Error opening output stream: ${e.message}")
                    }
                } ?: Log.e("FileTransfer", "Failed to create file entry")
            }
        }
        DataTransferType.CHUNK -> {
            // Receive a chunk of file data
            currentFileOutputStream?.let { outputStream ->
                message.chunkData?.let { chunkBase64 ->
                    try {
                        val chunk = Base64.decode(chunkBase64, Base64.DEFAULT)
                        outputStream.write(chunk)
                        bytesReceived += chunk.size // Update total bytes received

                        // Update progress percentage
                        progress = if (totalFileSize > 0) ((bytesReceived * 100) / totalFileSize).toInt() else 0
                        Log.d("FileTransfer", "Received chunk of ${chunk.size} bytes (Total: $bytesReceived bytes)")

                        // Update the notification with progress
                        val notification = createProgressNotification(context, currentFileMetadata?.fileName ?: "File", progress, 100)
                        notificationManager.notify(notificationId, notification)

                        // If there is a pending completion message, check if the file is fully received
                        checkAndCompleteFileTransfer(context)
                    } catch (e: IOException) {
                        Log.e("FileTransfer", "Error writing chunk: ${e.message}")
                    }
                } ?: Log.e("FileTransfer", "Chunk data is null")
            } ?: Log.e("FileTransfer", "Output stream is null")
        }
        else -> { return }
    }
}

fun createProgressNotification(context: Context, fileName: String, progress: Int, maxProgress: Int): Notification {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create notification channel if not exists
    val channelId = "fileTransfer_channel"
    val channel = NotificationChannel(
        channelId,
        "File Transfer",
        NotificationManager.IMPORTANCE_LOW
    )
    notificationManager.createNotificationChannel(channel)

    return NotificationCompat.Builder(context, channelId)
        .setContentTitle("Receiving file: $fileName")
        .setSmallIcon(com.komu.seki.core.common.R.drawable.ic_splash)
        .setProgress(maxProgress, progress, false) // Update progress
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setOngoing(true)

        .setOnlyAlertOnce(true)
        .setSilent(true)
        .build()
}

// Check if file transfer is complete and finalize
private fun checkAndCompleteFileTransfer(context: Context) {
    currentFileMetadata?.let { metadata ->
        if (pendingCompletion && bytesReceived == metadata.fileSize) {
            // Finalize the file transfer if all bytes have been received
            currentFileOutputStream?.let {
                try {
                    it.close()
                    val mimeType = metadata.mimeType ?: "*/*" // Use actual MIME type or fallback

                    // Create an intent to view the file using a file manager or other apps
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Build the notification to show file transfer completion
                    val completedNotification = NotificationCompat.Builder(context, "fileTransfer_channel")
                        .setContentTitle("File transfer completed")
                        .setContentText("Tap to view the file.")
                        .setSmallIcon(com.komu.seki.core.common.R.drawable.ic_splash)
                        .setContentIntent(pendingIntent) // Intent to open file location
                        .setAutoCancel(true) // Remove the notification once tapped
                        .build()

                    // Display the notification
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(4159, completedNotification)

                    Log.d("FileTransfer", "File transfer completed successfully: ${metadata.fileName}")
                } catch (e: IOException) {
                    Log.e("FileTransfer", "Error closing stream: ${e.message}")
                } finally {
                    currentFileOutputStream = null
                    bytesReceived = 0L
                    pendingCompletion = false

                    if (metadata.mimeType?.startsWith("image/") == true) {
                        copyImageToClipboard(context, metadata.fileName)
                    }
                }
            } ?: Log.e("FileTransfer", "Output stream already closed")
        } else if (pendingCompletion) {
            Log.d("FileTransfer", "Waiting for more chunks to complete file transfer.")
        } else {

        }
    } ?: Log.e("FileTransfer", "Metadata is null, unable to finalize file transfer")
}

// Helper function to copy the image to clipboard
private fun copyImageToClipboard(context: Context, fileName: String) {
    val contentResolver = context.contentResolver

    // Query the file from the Downloads directory
    val fileUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val query = contentResolver.query(
        fileUri,
        null,
        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
        arrayOf(fileName),
        null
    )

    query?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
            val uri = ContentUris.withAppendedId(fileUri, id)

            // Get the clipboard manager
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(contentResolver, "Image", uri)

            // Set the clipboard content
            clipboard.setPrimaryClip(clip)
            Log.d("FileTransfer", "Image copied to clipboard: $fileName")
        } else {
            Log.e("FileTransfer", "Failed to find image file in downloads for clipboard")
        }
    }
}