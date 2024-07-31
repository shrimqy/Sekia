package komu.seki.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import komu.seki.domain.models.DeviceDetails
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appPreferences")

class PreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object DataPreferencesKeys{
        val DEVICE_NAME = stringPreferencesKey("serviceName")
        val HOST_ADDRESS = stringPreferencesKey("hostAddress")
        val SYNC_STATUS = booleanPreferencesKey("syncStatus")
    }

    private val datastore = context.dataStore


    override suspend fun saveDeviceDetails(deviceName: String, hostAddress: String) {
        datastore.edit { settings ->
            settings[DataPreferencesKeys.DEVICE_NAME] = deviceName
            settings[DataPreferencesKeys.HOST_ADDRESS] = hostAddress
        }
    }

    override fun readDeviceDetails(): Flow<DeviceDetails> {
        return datastore.data.map { preferences ->
            val deviceName = preferences[DataPreferencesKeys.DEVICE_NAME]
            val host = preferences[DataPreferencesKeys.HOST_ADDRESS]
            DeviceDetails(deviceName, host)
        }
    }

    override suspend fun saveSynStatus(syncStatus: Boolean) {
        datastore.edit { status->
            status[DataPreferencesKeys.SYNC_STATUS] = syncStatus
        }
    }

    override fun readSyncStatus(): Flow<Boolean> {
        return datastore.data.map { status ->
            status[DataPreferencesKeys.SYNC_STATUS] ?: false
        }
    }
}