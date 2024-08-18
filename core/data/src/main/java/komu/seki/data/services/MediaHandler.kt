package komu.seki.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import komu.seki.common.util.base64ToBitmap
import komu.seki.domain.models.PlaybackData

fun mediaController(context: Context, playbackData: PlaybackData) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationId = 1

    // Get the existing or new MediaSession
    val mediaSession = MediaSessionManager.getMediaSession(context)

    // Update MediaSession metadata and playback state
    mediaSession.setMetadata(
        MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playbackData.trackTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playbackData.artist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, playbackData.thumbnail?.let { base64ToBitmap(it) })
            .build()
    )

    mediaSession.setPlaybackState(
        PlaybackStateCompat.Builder()
            .setState(
                if (playbackData.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
            )
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .build()
    )

    val channel = NotificationChannel(
        "playback_channel",
        "Playback Notifications",
        NotificationManager.IMPORTANCE_LOW
    )
    notificationManager.createNotificationChannel(channel)

    // Prepare the intents for media actions
    val playPauseIntent = Intent(context, PlaybackReceiver::class.java).apply {
        action = if (playbackData.isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
    }
    val playPausePendingIntent = PendingIntent.getBroadcast(context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val nextIntent = Intent(context, PlaybackReceiver::class.java).apply { action = "ACTION_NEXT" }
    val nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val prevIntent = Intent(context, PlaybackReceiver::class.java).apply { action = "ACTION_PREV" }
    val prevPendingIntent = PendingIntent.getBroadcast(context, 2, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    // Create or update the notification
    val notification = NotificationCompat.Builder(context, "playback_channel")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle(playbackData.trackTitle)
        .setContentText(playbackData.artist)
        .setLargeIcon(playbackData.thumbnail?.let { base64ToBitmap(it) })
        .setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        .addAction(
            NotificationCompat.Action(
                if (playbackData.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playbackData.isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
        )
        .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent))
        .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextPendingIntent))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(playbackData.isPlaying)
        .build()

    // Notify (update) the existing notification
    notificationManager.notify(notificationId, notification)

    Log.d("PlaybackData", "Notification updated for playback: ${playbackData.trackTitle}")
}

object MediaSessionManager {
    private var mediaSession: MediaSessionCompat? = null

    fun getMediaSession(context: Context): MediaSessionCompat {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context, "MediaSessionTag")
            mediaSession?.isActive = true
        }
        return mediaSession!!
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
    }
}


class PlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PLAY" -> {
                // Handle play action
                Log.d("PlaybackReceiver", "Play action received")
            }
            "ACTION_PAUSE" -> {
                // Handle pause action
                Log.d("PlaybackReceiver", "Pause action received")
            }
            "ACTION_NEXT" -> {
                // Handle next action
                Log.d("PlaybackReceiver", "Next action received")
            }
            "ACTION_PREV" -> {
                // Handle previous action
                Log.d("PlaybackReceiver", "Previous action received")
            }
        }
    }
}
