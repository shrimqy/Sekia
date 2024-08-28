package komu.seki.data.database

import android.net.wifi.WifiSsid
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    @Query("SELECT * FROM Network")
    suspend fun getAllNetworks(): List<Network>

    @Query("SELECT * FROM Network")
    fun getAllNetworksFlow(): Flow<List<Network>>

    @Query("SELECT * FROM Network WHERE ssid = :ssid")
    fun getNetwork(ssid: String): Flow<Network>

    @Query("DELETE FROM Network WHERE ssid = :ssid")
    suspend fun removeNetwork(ssid: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addNetwork(network: Network)
}