package com.komu.sekia.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import komu.seki.data.repository.PreferencesDatastore
import komu.seki.data.network.NsdService
import komu.seki.data.network.WebSocketClient
import komu.seki.data.repository.WebSocketRepositoryImpl
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAppContext(@ApplicationContext context: Context) = context

    @Provides
    @Singleton
    fun providesPreferencesRepository(
        application: Application
    ): PreferencesRepository = PreferencesDatastore(context = application)

    @Provides
    @Singleton
    fun providesWebSocketClient(): WebSocketClient = WebSocketClient()

    @Provides
    @Singleton
    fun providesWebSocketRepository(
        webSocketClient: WebSocketClient
    ): WebSocketRepository = WebSocketRepositoryImpl(webSocketClient)

    @Provides
    @Singleton
    fun providesNsdHelper(
        application: Application
    ): NsdService =
        NsdService(context = application)
}