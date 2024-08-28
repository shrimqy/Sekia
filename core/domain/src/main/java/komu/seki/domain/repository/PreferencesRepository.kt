package komu.seki.domain.repository

import komu.seki.domain.models.DeviceInfo
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveSynStatus(syncStatus: Boolean)
    fun readSyncStatus(): Flow<Boolean>

    suspend fun saveLastConnected(hostAddress: String)
    fun readLastConnected(): Flow<String?>
}

