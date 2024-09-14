package komu.seki.domain.repository

import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.PreferencesSettings
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveSynStatus(syncStatus: Boolean)
    fun readSyncStatus(): Flow<Boolean>

    suspend fun saveLastConnected(hostAddress: String)
    fun readLastConnected(): Flow<String?>

    suspend fun saveAutoDiscoverySettings(discoverySettings: Boolean)
    suspend fun saveImageClipboardSettings(clipboardSettings: Boolean)
    suspend fun updateStorageLocation(uri: String)
    fun preferenceSettings(): Flow<PreferencesSettings>
}

