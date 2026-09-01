package sefirah.worker

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import sefirah.domain.interfaces.NetworkManager
import sefirah.domain.model.RequestWorkerLaunch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class WorkerManager @Inject constructor(
    private val context: Context,
    private val networkManager: NetworkManager,
) {
    @Volatile
    private var worker: IWorkerService? = null

    @Volatile
    private var watching = false

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy { userServiceArgs() }

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (watching) {
            Log.i(TAG, "Shizuku binder received — restarting clipboard watcher")
            startClipboardWatcher()
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
    }

    fun startClipboardWatcher() {
        watching = true
        thread(name = "sefirah-worker-start", isDaemon = true) {
            try {
                when {
                    isWorkerCurrent() -> requestStartWatching()
                    ShizukuHelper.isAuthorized() -> startWorkerViaShizuku()
                    else -> {
                        requestWorkerExit()
                        val launchCommand = WorkerStarter.command(context)
                        Log.i(TAG, "No Shizuku - requesting ADB worker launch: adb shell $launchCommand")
                        networkManager.broadcastMessage(RequestWorkerLaunch(launchCommand))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "startClipboardWatcher failed", e)
            }
        }
    }

    fun stopClipboardWatcher() {
        watching = false
        requestStopWatching()
    }

    /** Count one inbound setPrimaryClip; worker skips that clip timestamp (and its classification re-fires). */
    fun suppressNextOutbound() {
        try {
            worker?.suppressNextOutbound()
        } catch (e: Exception) {
            Log.w(TAG, "suppressNextOutbound failed", e)
        }
    }

    fun isWorkerAlive(): Boolean =
        try {
            val w = worker ?: return false
            w.asBinder().pingBinder()
        } catch (_: Exception) {
            false
        }

    /** Alive and reporting [WORKER_VERSION] (missing getVersion → false). */
    private fun isWorkerCurrent(): Boolean =
        try {
            val w = worker ?: return false
            if (!w.asBinder().pingBinder()) return false
            w.version == WORKER_VERSION
        } catch (e: Exception) {
            Log.i(TAG, "Worker version check failed: ${e.message}")
            false
        }

    fun registerWorker(workerBinder: IBinder) {
        val w = IWorkerService.Stub.asInterface(workerBinder)
        val version = runCatching { w.version }.getOrNull()
        if (version != WORKER_VERSION) {
            Log.w(TAG, "Rejecting worker version=$version expected=$WORKER_VERSION — destroy")
            try {
                w.destroy()
            } catch (_: DeadObjectException) {
                // destroy() System.exit before the binder reply
            } catch (e: Exception) {
                Log.w(TAG, "destroy stale worker failed", e)
            }
            return
        }

        try {
            workerBinder.linkToDeath({
                Log.w(TAG, "Worker died")
                if (w == null || worker === w || worker?.asBinder() == w.asBinder()) {
                    worker = null
                    if (watching) {
                        startClipboardWatcher()
                    }
                }
            }, 0)
        } catch (e: Exception) {
            Log.w(TAG, "linkToDeath failed", e)
        }
        worker = w
        Log.i(TAG, "Worker registered (version=$version)")

        if (watching) {
            requestStartWatching()
        }
    }

    /** Ask the bound worker to exit (cooperative). */
    private fun requestWorkerExit() {
        val w = worker ?: return
        worker = null
        try {
            w.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "requestWorkerExit failed", e)
        }
    }

    private fun requestStartWatching() {
        try {
            worker?.startWatching()
        } catch (e: Exception) {
            Log.w(TAG, "startWatching failed", e)
        }
    }

    private fun requestStopWatching() {
        try {
            worker?.stopWatching()
        } catch (e: Exception) {
            Log.w(TAG, "stopWatching failed", e)
        }
    }

    private fun startWorkerViaShizuku() {
        Log.i(TAG, "Starting worker via Shizuku: ${WorkerStarter.command(context)}")
        val args = userServiceArgs
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                try {
                    IWorkerLauncherService.Stub.asInterface(service).startWorker()
                } catch (e: Exception) {
                    Log.e(TAG, "startWorker failed", e)
                } finally {
                    try {
                        Shizuku.unbindUserService(args, this, true)
                    } catch (e: Exception) {
                        Log.w(TAG, "unbindUserService failed", e)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        Shizuku.bindUserService(args, connection)
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs {
        val version = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            info.versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            1
        }
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return Shizuku.UserServiceArgs(
            ComponentName(context.packageName, WorkerLauncherUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix(USER_SERVICE_TAG)
            .debuggable(debuggable)
            .version(version)
    }

    companion object {
        private const val TAG = "WorkerManager"
        const val NICE_NAME = "sefirah_worker"

        /**
         * Must match `:worker` `BuildConfig.VERSION_CODE`. Bump when worker code / AIDL changes
         * so an old still-running `app_process` is replaced.
         */
        const val WORKER_VERSION = 1

        private const val USER_SERVICE_TAG = "worker-launcher"
    }
}