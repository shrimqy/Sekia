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
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import komu.seki.common.models.FileMetadata
import komu.seki.domain.models.DataTransferType
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.PreferencesSettings
import java.io.File
import java.io.IOException
import java.io.OutputStream


private var bytesReceived = 0L // Track the total number of bytes received

private var pendingCompletion = false // Flag to indicate completion message waiting

private var currentFileOutputStream: OutputStream? = null
private var currentFileMetadata: FileMetadata? = null

var uri: Uri? = null

fun receivingFileHandler(context: Context, preferencesSettings: PreferencesSettings, message: FileTransfer) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationId = 4159

    val totalFileSize = currentFileMetadata?.fileSize ?: 0L // Total file size for progress
    var progress = 0 // To track percentage

    when (message.dataTransferType) {
        DataTransferType.METADATA -> {
            currentFileMetadata = message.metadata
            currentFileMetadata?.let { metadata ->
                val contentResolver = context.contentResolver
                var storageUri: Uri? = null
                if (preferencesSettings.storageLocation.isNotEmpty()) {
                    storageUri = preferencesSettings.storageLocation.toUri()
                    val directory = DocumentFile.fromTreeUri(context, storageUri)
                    val newFile = directory?.createFile(metadata.mimeType ?: "*/*", metadata.fileName)

                    uri = newFile?.uri
                    currentFileOutputStream = uri?.let { contentResolver.openOutputStream(it) }
                    Log.d("FileTransfer", "uri used: $uri")
                } else {
                    // Use MediaStore to insert a file in the Downloads folder
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, metadata.fileName) // File name
                        put(MediaStore.Downloads.MIME_TYPE, metadata.mimeType ?: "*/*") // MIME type
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Path
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10 (Q) and above
                        uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    } else {
                        // For Android 9 (Pie) and below
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, metadata.fileName)

                        // Create the file on the file system
                        if (!file.exists()) {
                            file.createNewFile()
                        }

                        // Uri for the file
                        uri = Uri.fromFile(file)
                    }
                    currentFileOutputStream = uri?.let { contentResolver.openOutputStream(it) }
                }
                Log.d("FileTransfer", "uri: $uri")
                try {
                    if (currentFileOutputStream != null) {
                        Log.d("FileTransfer", "Receiving file: ${metadata.fileName}")
                        bytesReceived = 0L // Reset bytes received counter

                        // Initialize the notification with 0 progress
                        val notification = createProgressNotification(context, metadata.fileName, 0, 100)
                        notificationManager.notify(notificationId, notification)
                    } else {
                        Log.e("FileTransfer", "Failed to open output stream for URI: $storageUri")
                    }
                } catch (e: Exception) {
                    Log.e("FileTransfer", "Error during file creation or output stream opening: ${e.message}")
                }
            }
        }

        DataTransferType.CHUNK -> {
            currentFileOutputStream?.let { outputStream ->
                message.chunkData?.let { chunkBase64 ->
                    try {
                        val chunk = Base64.decode(chunkBase64, Base64.DEFAULT)
                        outputStream.write(chunk)
                        bytesReceived += chunk.size // Update total bytes received

                        // Update progress percentage
                        progress = if (totalFileSize > 0) ((bytesReceived * 100) / totalFileSize).toInt() else 0

                        // Log the progress percentage
                        Log.d("FileTransfer", "Progress: $progress%")

                        // Update the notification with progress
                        val notification = createProgressNotification(
                            context, currentFileMetadata?.fileName ?: "File", progress, 100
                        )
                        notificationManager.notify(notificationId, notification)

                        // If the file transfer is complete, finalize the transfer
                        if (bytesReceived >= totalFileSize) {
                            outputStream.flush()
                            // Cancel the progress notification
                            notificationManager.cancel(notificationId)

                            // Show completion notification
                            showCompletionNotification(context, currentFileMetadata?.fileName ?: "File", uri)

                            // Handle completion logic
                            checkAndCompleteFileTransfer(context, preferencesSettings)
                        } else {
                            return
                        }
                    } catch (e: IOException) {
                        Log.e("FileTransfer", "Error writing chunk: ${e.message}")
                    }
                } ?: Log.e("FileTransfer", "Chunk data is null")
            } ?: Log.e("FileTransfer", "Output stream is null")
        }

        else -> {
            return
        }
    }
}

fun showCompletionNotification(context: Context, fileName: String, uri: Uri?) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create an intent to view the file using a file manager or other apps
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "*/*") // Use MIME type if available
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

    // Display the completion notification
    notificationManager.notify(4951, completedNotification)
    Log.d("FileTransfer", "File transfer completed successfully: $fileName")
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
private fun checkAndCompleteFileTransfer(context: Context, preferencesSettings: PreferencesSettings) {
    currentFileMetadata?.let { metadata ->
        if (pendingCompletion && bytesReceived == metadata.fileSize) {
            // Finalize the file transfer if all bytes have been received
            currentFileOutputStream?.let {
                try {
                    it.close()
                    currentFileOutputStream = null
                    bytesReceived = 0L
                    pendingCompletion = false
                    if (preferencesSettings.imageClipboard) {
                        copyImageToClipboard(context)
                    } else return

                } catch (e: IOException) {
                    Log.e("FileTransfer", "Error closing stream: ${e.message}")
                } finally {
                    currentFileOutputStream = null
                    bytesReceived = 0L
                    pendingCompletion = false

                    if (metadata.mimeType.startsWith("image/")) {
                        copyImageToClipboard(context)
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
private fun copyImageToClipboard(context: Context) {
    uri?.let {
        val contentResolver = context.contentResolver

        // Get the clipboard manager
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(contentResolver, "Image", uri)

        // Set the clipboard content
        clipboard.setPrimaryClip(clip)
        Log.d("FileTransfer", "Image copied to clipboard: ${uri!!.lastPathSegment}")
    } ?: Log.e("FileTransfer", "Uri is null, cannot copy image to clipboard")
}

fun createUniqueFile(directory: DocumentFile, fileName: String, mimeType: String?): DocumentFile? {
    var baseName = fileName
    var extension = ""

    // Separate file name and extension
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex != -1) {
        baseName = fileName.substring(0, dotIndex)
        extension = fileName.substring(dotIndex)
    }

    var newFile = directory.findFile(fileName)
    var index = 1

    // Check for duplicate files and create a unique name
    while (newFile != null) {
        val newName = "$baseName ($index)$extension"
        newFile = directory.findFile(newName)
        index++
    }

    // Create the file with the unique name
    return directory.createFile(mimeType ?: "*/*", "$baseName ($index)$extension")
}