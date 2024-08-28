package komu.seki.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appPreferences")

class PreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object DataPreferencesKeys{
        val SYNC_STATUS = booleanPreferencesKey("syncStatus")
        val LAST_CONNECTED = stringPreferencesKey("lastConnected")
    }

    private val datastore = context.dataStore


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

    override suspend fun saveLastConnected(hostAddress: String) {
        datastore.edit { status->
            status[DataPreferencesKeys.LAST_CONNECTED] = hostAddress
        }
    }

    override fun readLastConnected(): Flow<String?> {
        return datastore.data.map { host ->
            host[DataPreferencesKeys.LAST_CONNECTED]
        }
    }
}