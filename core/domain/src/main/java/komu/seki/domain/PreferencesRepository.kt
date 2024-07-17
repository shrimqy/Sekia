package komu.seki.domain

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun saveOnboardingStatus()
    fun readOnboardingStatus(): Flow<Boolean>
}