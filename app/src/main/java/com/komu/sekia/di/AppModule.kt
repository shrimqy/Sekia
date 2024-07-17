package com.komu.sekia.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import komu.seki.data.repository.PreferencesDatastore
import komu.seki.data.repository.PreferencesRepository
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
}