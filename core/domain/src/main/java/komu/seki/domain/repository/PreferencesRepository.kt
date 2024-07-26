package komu.seki.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveSyncStatus()
    fun readSyncStatus(): Flow<Boolean>
    suspend fun saveDeviceDetails(serviceName: String, hostAddress: String, port: Int)
    fun readDeviceDetails(): Flow<DeviceDetails>
}

data class DeviceDetails(
    val deviceName: String,
    val hostAddress: String,
    val port: Int
)