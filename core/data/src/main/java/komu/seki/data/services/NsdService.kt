package komu.seki.data.services

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdService @Inject constructor(@ApplicationContext private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _services = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    private var serviceDiscoveryStatus by mutableStateOf(false)
    val services: StateFlow<List<NsdServiceInfo>> = _services

    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("myMulticastLock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
            serviceDiscoveryStatus = true
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service discovery success, $service")
            if (service.serviceType == SERVICE_TYPE) {
                nsdManager.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e(TAG, "Service lost: $service")
            _services.value = _services.value.filter { it.serviceName != service.serviceName }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
            serviceDiscoveryStatus = false
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed to start: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
            serviceDiscoveryStatus = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed to stop: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
            serviceDiscoveryStatus = false
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service resolved: $serviceInfo")
            _services.value = (_services.value + serviceInfo).distinctBy { it.serviceName }
        }
    }

    fun startDiscovery() {
        Log.d(TAG, "Starting service discovery")
        _services.value = emptyList()  // Clear previous results
        if (!serviceDiscoveryStatus) {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            serviceDiscoveryStatus = true
        } else {
            Log.d(TAG, "Listener already connected")
        }
    }

    fun stopDiscovery() {
        if (serviceDiscoveryStatus) {
            Log.d(TAG, "Stopping service discovery")
            nsdManager.stopServiceDiscovery(discoveryListener)
            serviceDiscoveryStatus = false
        } else {
            Log.d(TAG, "Service discovery is not running, ignoring stop request")
        }
    }

    fun resolveService(serviceInfo: NsdServiceInfo, resolveListener: NsdManager.ResolveListener) {
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    fun registerService(serviceInfo: NsdServiceInfo, registrationListener: NsdManager.RegistrationListener) {
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService(registrationListener: NsdManager.RegistrationListener) {
        nsdManager.unregisterService(registrationListener)
    }


    fun releaseMulticastLock() {
        multicastLock?.release()
    }

    companion object {
        private const val TAG = "NsdService"
        private const val SERVICE_TYPE = "_foo._tcp." // Ensure this matches the advertised service type
    }
}
