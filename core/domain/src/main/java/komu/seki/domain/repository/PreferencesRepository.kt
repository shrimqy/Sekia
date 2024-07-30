package komu.seki.domain.repository

import komu.seki.domain.models.DeviceDetails
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveDeviceDetails(deviceName: String, hostAddress: String)
    fun readDeviceDetails(): Flow<DeviceDetails>?
}

