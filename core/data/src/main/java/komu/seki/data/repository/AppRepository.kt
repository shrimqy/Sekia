package komu.seki.data.repository

import android.content.Context
import android.net.wifi.WifiSsid
import dagger.hilt.android.qualifiers.ApplicationContext
import komu.seki.data.database.AppDatabase
import komu.seki.data.database.Device
import komu.seki.data.database.DeviceDao
import komu.seki.data.database.Network
import komu.seki.data.database.NetworkDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao,
    private val networkDao: NetworkDao
){
    suspend fun getAllDevicesFlow() = deviceDao.getAllDevicesFlow()
    suspend fun addDevice(device: Device) = deviceDao.addDevice(device)
    suspend fun getDevice(ipAddress: String) = deviceDao.getDevice(ipAddress)
    suspend fun getHostAddress(deviceName: String) = deviceDao.getHostAddress(deviceName)
    suspend fun removeDevice(deviceName: String) = deviceDao.removeDevice(deviceName)
    suspend fun updateDevice(device: Device) = deviceDao.updateDevice(device)

    suspend fun getAllNetworkFlow() = networkDao.getAllNetworksFlow()
    suspend fun addNetwork(network: Network) = networkDao.addNetwork(network)
    suspend fun getNetwork(ssid: String) = networkDao.getNetwork(ssid)
    suspend fun removeNetwork(ssid: String) = networkDao.removeNetwork(ssid)
}