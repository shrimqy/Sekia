package komu.seki.domain.repository

import komu.seki.domain.models.PlaybackData
import kotlinx.coroutines.flow.StateFlow

interface PlaybackRepository {
     fun updatePlaybackData(data: PlaybackData)
     fun readPlaybackData(): StateFlow<PlaybackData?>
}