package komu.seki.data.services

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdService @Inject constructor(@ApplicationContext private val context: Context) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _services = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    private var serviceDiscoveryStatus by mutableStateOf(false)
    val services: StateFlow<List<NsdServiceInfo>> = _services

    private var multicastLock: WifiManager.MulticastLock? = null
    private val executor = Executors.newSingleThreadExecutor()

    init {
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("SekiaMulticastLock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "Service registration failed: Error code: $errorCode")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "Service unregistration failed: Error code: $errorCode")
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            Log.d(TAG, "Service registered successfully: $serviceInfo")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            Log.d(TAG, "Service unregistered successfully: $serviceInfo")
        }
    }

    fun advertiseService(serviceName: String, port: Int) {
        val serviceInfo = NsdServiceInfo().also {
            it.serviceType = SERVICE_TYPE
            it.serviceName = serviceName
            it.port = port
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                stopAdvertisingService()
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
                Log.d(TAG, "Advertising service: $serviceName on port: $port")
            } catch (e: Exception) {
                Log.e(TAG, "Error advertising service: ${e.message}")
            }
        }
    }

    fun stopAdvertisingService() {
        try {
            nsdManager.unregisterService(registrationListener)
            Log.d(TAG, "Stopped advertising service")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error stopping service advertisement: ${e.message}")
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
            serviceDiscoveryStatus = true
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Service found: $service")
            if (service.serviceType == SERVICE_TYPE) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        // Unregister previous ServiceInfoCallback if registered
                        try {
                            nsdManager.unregisterServiceInfoCallback(serviceInfoCallback)
                            Log.d(TAG, "Successfully unregistered previous ServiceInfoCallback")
                        } catch (e: Exception) {
                            Log.e(TAG, "No ServiceInfoCallback was registered: ${e.message}")
                        }

                        // Add a small delay to avoid race conditions
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                nsdManager.registerServiceInfoCallback(service, executor, serviceInfoCallback)
                                Log.d(TAG, "Successfully registered ServiceInfoCallback")
                            } catch (e: IllegalArgumentException) {
                                Log.e(TAG, "Listener already in use or issue in registration: ${e.message}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error registering listener: ${e.message}")
                            }
                        }, 200)
                    } else {
                        // Fallback for older versions
                        nsdManager.resolveService(service, resolveListener)
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Listener already in use or issue in registration: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering listener: ${e.message}")
                }
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

    // Fallback resolve listener for API levels below 34
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed: Error code: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service resolved: $serviceInfo")
            _services.value = (_services.value + serviceInfo).distinctBy { it.serviceName }
        }
    }

    // New ServiceInfoCallback for API level 34+
    private val serviceInfoCallback = @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    object : NsdManager.ServiceInfoCallback {
        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
            Log.e(TAG, "ServiceInfoCallback registration failed: $errorCode")
        }

        override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service updated: $serviceInfo")
            // Continuously update the list of services
            _services.value = (_services.value + serviceInfo).distinctBy { it.serviceName }
        }

        override fun onServiceLost() {
            Log.e(TAG, "Service lost")
            // Since no service info is provided, you'll have to rely on existing service data.
        }

        override fun onServiceInfoCallbackUnregistered() {
            Log.d(TAG, "ServiceInfoCallback unregistered")
        }
    }

    fun startDiscovery() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stopDiscovery()
                delay(50)
                Log.d(TAG, "Starting service discovery")
                _services.value = emptyList()
                nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error starting discovery: ${e.message}")
                stopDiscovery()
                delay(50)
                startDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            Log.d(TAG, "Stopped service discovery")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error stopping discovery: ${e.message}")
        }
    }

    fun releaseMulticastLock() {
        multicastLock?.release()
        Log.d(TAG, "Multicast lock released")
    }

    companion object {
        private const val TAG = "NsdService"
        private const val SERVICE_TYPE = "_foo._tcp." // Make sure this matches across platforms
    }
}