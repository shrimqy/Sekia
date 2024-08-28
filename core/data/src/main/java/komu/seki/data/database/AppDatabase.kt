package komu.seki.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import komu.seki.common.util.Constants

interface AppDatabase {
    fun devicesDao(): DeviceDao
    fun networkDao(): NetworkDao

    /**
     * Execute the whole database calls as an atomic operation
     */
    suspend fun <T> transaction(block: suspend () -> T): T

    companion object {
        fun createRoom(context: Context): AppDatabase = Room
            .databaseBuilder(context, AppRoomDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}


@Database(
    entities = [ Device::class, Network::class ],
    version = 1,
    exportSchema = false
)

internal abstract class AppRoomDatabase : RoomDatabase(), AppDatabase {
    abstract override fun devicesDao(): DeviceDao
    abstract override fun networkDao(): NetworkDao

    override suspend fun <T> transaction(block: suspend () -> T): T = withTransaction(block)
}