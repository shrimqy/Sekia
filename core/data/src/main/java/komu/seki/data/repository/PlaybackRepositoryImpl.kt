package komu.seki.data.repository

import android.util.Log
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlaybackRepositoryImpl @Inject constructor() : PlaybackRepository {
    private val _playbackData = MutableStateFlow<PlaybackData?>(null)
    private val playbackData: StateFlow<PlaybackData?> = _playbackData.also { flow ->
        Log.d("PlaybackRepository", "Initial PlaybackData: ${flow.value}")
    }

    init {
        Log.d("PlaybackRepository", "PlaybackRepositoryImpl initialized")
    }

    override fun updatePlaybackData(data: PlaybackData) {
        Log.d("PlaybackRepository", "Updating PlaybackData: $data")
        _playbackData.value = data
        Log.d("PlaybackRepository", "PlaybackData updated: ${_playbackData.value}")
    }

    override fun readPlaybackData(): StateFlow<PlaybackData?> {
        return playbackData;
    }
}