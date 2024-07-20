package komu.seki.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import komu.seki.common.util.Constants
import komu.seki.domain.PreferencesRepository
import komu.seki.domain.ServiceDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appPreferences")

class PreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object DataPreferencesKeys{
        val SYNC_STATUS = booleanPreferencesKey("syncStatus")
        val SERVICE_NAME = stringPreferencesKey("serviceName")
        val HOST_ADDRESS = stringPreferencesKey("hostAddress")
        val PORT = intPreferencesKey("port")
    }

    private val datastore = context.dataStore

    override suspend fun saveSyncStatus() {
        datastore.edit { settings ->
            settings[DataPreferencesKeys.SYNC_STATUS] = true
        }
    }
    override fun readSyncStatus(): Flow<Boolean> {
        return datastore.data.map { preferences ->
            preferences[DataPreferencesKeys.SYNC_STATUS] ?: false
        }
    }

    override suspend fun saveServiceDetails(serviceName: String, hostAddress: String, port: Int) {
        datastore.edit { settings ->
            settings[DataPreferencesKeys.SERVICE_NAME] = serviceName
            settings[DataPreferencesKeys.HOST_ADDRESS] = hostAddress
            settings[DataPreferencesKeys.PORT] = port
        }
    }

    override fun readServiceDetails(): Flow<ServiceDetails> {
        return datastore.data.map { preferences ->
            val serviceName = preferences[DataPreferencesKeys.SERVICE_NAME] ?: ""
            val host = preferences[DataPreferencesKeys.HOST_ADDRESS] ?: ""
            val port = preferences[DataPreferencesKeys.PORT] ?: 0
            ServiceDetails(serviceName, host, port)
        }
    }
}