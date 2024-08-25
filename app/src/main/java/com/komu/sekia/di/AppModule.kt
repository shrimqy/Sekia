package com.komu.sekia.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import komu.seki.data.repository.PlaybackRepositoryImpl
import komu.seki.data.repository.PreferencesDatastore
import komu.seki.data.services.NsdService
import komu.seki.data.repository.WebSocketRepositoryImpl
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAppContext(@ApplicationContext context: Context) = context

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): AppCoroutineScope {
        return object : AppCoroutineScope {
            override val coroutineContext =
                SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("App")
        }
    }

    @Provides
    @Singleton
    fun providesPreferencesRepository(
        application: Application
    ): PreferencesRepository = PreferencesDatastore(context = application)

    @Provides
    @Singleton
    fun providedPlaybackRepository(): PlaybackRepository {
        return PlaybackRepositoryImpl()
    }


    @Provides
    @Singleton
    fun provideWebSocketRepository(
        application: Application,
        playbackRepository: PlaybackRepository
    ): WebSocketRepository {
        return WebSocketRepositoryImpl(application, playbackRepository)
    }


    @Provides
    @Singleton
    fun providesNsdHelper(
        application: Application
    ): NsdService =
        NsdService(context = application)
}