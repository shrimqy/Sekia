package komu.seki.data.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveOnboardingStatus()
    fun readOnboardingStatus(): Flow<Boolean>
}