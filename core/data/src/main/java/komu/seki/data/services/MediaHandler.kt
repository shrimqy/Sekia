package komu.seki.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import komu.seki.common.util.base64ToBitmap
import komu.seki.domain.models.MediaAction
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun mediaController(
    context: Context,
    playbackData: PlaybackData,
    sendMessage: suspend (SocketMessage) -> Unit
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationId = 5941

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

    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            handleMediaAction(playbackData, MediaAction.RESUME, sendMessage)
        }

        override fun onPause() {
            handleMediaAction(playbackData, MediaAction.PAUSE, sendMessage)
        }

        override fun onSkipToNext() {
            handleMediaAction(playbackData, MediaAction.NEXT_QUEUE, sendMessage)
        }

        override fun onSkipToPrevious() {
            handleMediaAction(playbackData, MediaAction.PREV_QUEUE, sendMessage)
        }
    })


    val channel = NotificationChannel(
        "playback_channel",
        "Playback Notifications",
        NotificationManager.IMPORTANCE_LOW
    )
    notificationManager.createNotificationChannel(channel)

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
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true)
        .build()

    // Notify (update) the existing notification
    notificationManager.notify(notificationId, notification)

    Log.d("PlaybackData", "Notification updated for playback: ${playbackData.trackTitle}")
}



fun handleMediaAction(
    playbackData: PlaybackData,
    action: MediaAction,
    sendMessage: suspend (SocketMessage) -> Unit
) {
    playbackData.mediaAction = action
    CoroutineScope(Dispatchers.IO).launch {
        sendMessage(playbackData)
    }
    Log.d("MediaSession", "Action received: $action" + playbackData.trackTitle)
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


