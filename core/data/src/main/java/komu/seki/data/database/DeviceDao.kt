package komu.seki.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Dao
interface DeviceDao {
    @Query("SELECT * FROM Device")
    suspend fun getAllDevices(): List<Device>

    @Query("SELECT * FROM Device")
    fun getAllDevicesFlow(): Flow<List<Device>>

    @Query("SELECT * FROM Device WHERE ipAddress = :ipAddress")
    fun getDevice(ipAddress: String): Flow<Device?>

    @Query("SELECT ipAddress FROM Device WHERE deviceName = :deviceName")
    suspend fun getHostAddress(deviceName: String): String?

    @Query("DELETE FROM Device WHERE deviceName = :deviceName")
    suspend fun removeDevice(deviceName: String)

    @Update
    suspend fun updateDevice(device: Device)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDevice(device: Device)
}