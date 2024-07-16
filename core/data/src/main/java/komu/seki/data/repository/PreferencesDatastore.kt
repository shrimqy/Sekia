package komu.seki.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import komu.seki.common.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appPreferences")

class PreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {
private object PreferencesKeys{
        val ONBOARDING = booleanPreferencesKey(Constants.ONBOARDING)
    }

    private val datastore = context.dataStore
    override suspend fun saveOnboardingStatus() {
        datastore.edit { settings ->
            settings[PreferencesKeys.ONBOARDING] = true
        }
    }

    override fun readOnboardingStatus(): Flow<Boolean> {
        return datastore.data.map { preferences ->
            preferences[PreferencesKeys.ONBOARDING] ?: false
        }
    }
}