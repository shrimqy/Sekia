package komu.seki.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveSyncStatus()
    fun readSyncStatus(): Flow<Boolean>
    suspend fun saveServiceDetails(serviceName: String, hostAddress: String, port: Int)
    fun readServiceDetails(): Flow<ServiceDetails>
}

data class ServiceDetails(
    val serviceName: String,
    val hostAddress: String,
    val port: Int
)