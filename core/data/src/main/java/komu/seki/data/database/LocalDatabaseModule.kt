package komu.seki.data.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class LocalDatabaseModule {

    companion object {
        @Provides
        @Singleton
        internal fun provideAppRoomDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.createRoom(context)
        }

        @Provides
        @Singleton
        fun provideDeviceDao(database: AppDatabase): DeviceDao = database.devicesDao()

        @Provides
        @Singleton
        fun provideNetworkDao(database: AppDatabase): NetworkDao = database.networkDao()
    }
}