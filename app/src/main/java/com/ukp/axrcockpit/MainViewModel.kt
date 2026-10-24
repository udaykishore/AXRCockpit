package com.ukp.axrcockpit

import android.app.Application
import android.content.Context
import android.os.Build
import com.ukp.axrcockpit.util.Logger
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbOutputStream
import io.github.muntashirakon.adb.AdbPairingRequiredException
import io.github.muntashirakon.adb.AdbStream
import io.github.muntashirakon.adb.LocalServices
import io.github.muntashirakon.adb.android.AdbMdns
import io.github.muntashirakon.adb.android.AdbMdns.OnAdbDaemonDiscoveredListener
import com.ukp.axrcockpit.util.ShellUtil
import com.ukp.axrcockpit.util.CustomButtonsPreferencesManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import androidx.lifecycle.viewModelScope
import com.ukp.axrcockpit.ui.DeviceStateWrapper
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.ukp.axrcockpit.db.AppDatabase
import com.ukp.axrcockpit.db.CustomCommandEntity



enum class DeviceType(val label: String) {
    HOME("Home"),
    GALAXY_XR("Galaxy XR"),
    AURA("Aura"),
    RAVINE("Ravine"),
    ADMIN("Admin")
}
class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val prefsManager: CustomButtonsPreferencesManager = CustomButtonsPreferencesManager(application)
) : AndroidViewModel(application) {
    private val logger = Logger.get(MainViewModel::class.java)

    private val db = AppDatabase.getDatabase(application)
    private val customCommandDao = db.customCommandDao()

    val customCommandsMap: StateFlow<Map<String, CustomCommandEntity>> = customCommandDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun saveCustomCommand(id: String, label: String, command: String, stopCommand: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = CustomCommandEntity(id = id, label = label, command = command, stopCommand = stopCommand)
            customCommandDao.insertOrUpdate(entity)
        }
    }

    fun resetCustomCommand(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            customCommandDao.deleteById(id)
        }
    }

    fun resetAllCustomCommands() {
        viewModelScope.launch(Dispatchers.IO) {
            customCommandDao.deleteAll()
        }
    }

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    companion object {
        private const val DEFAULT_PORT = 45555
    }

    private val defaultDemoAppPackages = mutableListOf<String>()


    private val _button1Label = MutableStateFlow(prefsManager.getButton1Label())
    val button1Label: StateFlow<String> = _button1Label.asStateFlow()

    private val _button1Command = MutableStateFlow(prefsManager.getButton1Command())
    val button1Command: StateFlow<String> = _button1Command.asStateFlow()

    private val _button2Label = MutableStateFlow(prefsManager.getButton2Label())
    val button2Label: StateFlow<String> = _button2Label.asStateFlow()

    private val _button2Command = MutableStateFlow(prefsManager.getButton2Command())
    val button2Command: StateFlow<String> = _button2Command.asStateFlow()

    fun saveButtonConfig(buttonIndex: Int, label: String, command: String) {
        prefsManager.saveButtonConfig(buttonIndex, label, command)

        if (buttonIndex == 1) {
            _button1Label.value = label
            _button1Command.value = command
        } else {
            _button2Label.value = label
            _button2Command.value = command
        }
    }

    fun resetButtonConfig(buttonIndex: Int) {
        prefsManager.resetButtonConfig(buttonIndex)

        val defaultLabel = prefsManager.getDefaultLabel(buttonIndex)
        if (buttonIndex == 1) {
            _button1Label.value = defaultLabel
            _button1Command.value = ""
        } else {
            _button2Label.value = defaultLabel
            _button2Command.value = ""
        }
    }

    init {
        loadWhitelists()
    }

    private fun loadWhitelists() {
        // Load default whitelist
        try {
            val jsonString = getApplication<Application>().assets.open("demo_apps_default.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val pkg = jsonArray.getString(i).trim()
                if (pkg.isNotEmpty()) {
                    defaultDemoAppPackages.add(pkg)
                }
            }
            logger.d("Loaded ${defaultDemoAppPackages.size} default demo app packages.")
        } catch (e: Exception) {
            logger.e("Error loading default demo app packages", e)
        }


    }

    private val _activeDevice = MutableStateFlow(DeviceType.HOME)
    val activeDevice: StateFlow<DeviceType> = _activeDevice.asStateFlow()

    fun setActiveDevice(device: DeviceType) {
        _activeDevice.value = device
        if (device != DeviceType.HOME) {
            startStatsPolling()
        } else {
            stopStatsPolling()
        }
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(3)
    private val statsStreamLock = Any()
    private var adbStatsStream: AdbStream? = null
    private var adbStatsOutputStream: java.io.OutputStream? = null
    private val shellStreamLock = Any()
    private val connectAdb = MutableLiveData<Boolean>()
    private val pairAdb = MutableLiveData<Boolean>()
    private val askPairAdb = MutableLiveData<Boolean>()
    private val commandOutput = MutableLiveData<CharSequence?>()
    private val pairingPort = MutableLiveData<Int>()

    // LiveData for microphone mute status (newly added)
    private val _isMicMuted = MutableLiveData<Boolean>()
    var isMicMuted: LiveData<Boolean> = _isMicMuted

    private val _runningPackages = MutableLiveData<Set<String>>(emptySet())
    val runningPackages: LiveData<Set<String>> = _runningPackages

    private val _isScrcpyResuming = MutableLiveData<Boolean>(false)
    val isScrcpyResuming: LiveData<Boolean> = _isScrcpyResuming

    private val _isScrcpyRunning = MutableLiveData<Boolean>(false)
    val isScrcpyRunning: LiveData<Boolean> = _isScrcpyRunning



    private val _scrcpyMaxSize = MutableLiveData<Int>(720)
    val scrcpyMaxSize: LiveData<Int> = _scrcpyMaxSize

    private val _videoAspectRatio = MutableLiveData<Float>(16f / 9f)
    val videoAspectRatio: LiveData<Float> = _videoAspectRatio

    fun setScrcpyResuming(resuming: Boolean) {
        _isScrcpyResuming.postValue(resuming)
    }

    fun setVideoAspectRatio(aspectRatio: Float) {
        _videoAspectRatio.postValue(aspectRatio)
    }

    fun setScrcpyRunning(running: Boolean) {
        _isScrcpyRunning.postValue(running)
    }

    fun setScrcpyMaxSize(size: Int) {
        _scrcpyMaxSize.postValue(size)
    }

    fun addRunningPackage(pkg: String) {
        val current = _runningPackages.value ?: emptySet()
        _runningPackages.postValue(current + pkg)
    }

    fun removeRunningPackage(pkg: String) {
        val current = _runningPackages.value ?: emptySet()
        _runningPackages.postValue(current - pkg)
    }

    fun clearRunningPackages() {
        _runningPackages.postValue(emptySet())
    }

    fun stopStatsPolling() {
        statsJob?.cancel()
        _deviceStats.value = DeviceStateWrapper()
        synchronized(statsStreamLock) {
            try {
                adbStatsOutputStream?.close()
                adbStatsStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
            adbStatsOutputStream = null
            adbStatsStream = null
        }
    }

    private val _remoteMediaVolume = MutableLiveData<Int>(0)
    val remoteMediaVolume: LiveData<Int> = _remoteMediaVolume

    private val _remoteNotificationVolume = MutableLiveData<Int>(0)
    val remoteNotificationVolume: LiveData<Int> = _remoteNotificationVolume

    private val _remoteSystemVolume = MutableLiveData<Int>(0)
    val remoteSystemVolume: LiveData<Int> = _remoteSystemVolume

    private val _remoteGeminiVolume = MutableLiveData<Int>(0)
    val remoteGeminiVolume: LiveData<Int> = _remoteGeminiVolume

    private val _remoteNotificationMaxVolume = MutableLiveData<Int>(7)
    val remoteNotificationMaxVolume: LiveData<Int> = _remoteNotificationMaxVolume

    private val _remoteMediaMaxVolume = MutableLiveData<Int>(15)
    val remoteMediaMaxVolume: LiveData<Int> = _remoteMediaMaxVolume

    private val _remoteSystemMaxVolume = MutableLiveData<Int>(15)
    val remoteSystemMaxVolume: LiveData<Int> = _remoteSystemMaxVolume

    private val _remoteGeminiMaxVolume = MutableLiveData<Int>(15)
    val remoteGeminiMaxVolume: LiveData<Int> = _remoteGeminiMaxVolume

    private val _dominantHandStatus = MutableLiveData<Int>(0)
    val dominantHandStatus: LiveData<Int> = _dominantHandStatus

    private var lastManualSpatialToggleTime = 0L

    private var adbShellStream: AdbStream? = null

    @Volatile
    private var clearEnabled = false
    private fun readOutput() {
        try {
            BufferedReader(InputStreamReader(adbShellStream!!.openInputStream())).use { reader ->
                val sb = StringBuilder()
                var s: String?
                while ((reader.readLine().also { s = it }) != null) {
                    if (clearEnabled) {
                        sb.delete(0, sb.length)
                        clearEnabled = false
                    }
                    sb.append(s).append("\n")
                    commandOutput.postValue(sb.toString())
                }
            }
        } catch (e: IOException) {
            if (e.message?.contains("Stream closed") == true) {
                logger.d("ADB shell stream closed.")
            } else {
                logger.w("Error reading from ADB shell stream", e)
            }
        }
    }

    private val remoteStatusMonitoringScope = CoroutineScope(Dispatchers.Default + CoroutineName("remote_status_scope"))

    fun watchConnectAdb(): LiveData<Boolean> {
        return connectAdb
    }

    fun watchPairAdb(): LiveData<Boolean?> {
        return pairAdb
    }

    fun watchAskPairAdb(): LiveData<Boolean?> {
        return askPairAdb
    }

    fun watchCommandOutput(): LiveData<CharSequence?> {
        return commandOutput
    }

    fun watchPairingPort(): LiveData<Int?> {
        return pairingPort
    }

    override fun onCleared() {
        super.onCleared()

        executor.submit(Runnable {
            try {
                if (adbShellStream != null) {
                    adbShellStream!!.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                synchronized(statsStreamLock) {
                    adbStatsOutputStream?.close()
                    adbStatsStream?.close()
                    adbStatsOutputStream = null
                    adbStatsStream = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                AdbConnectionManager.getInstance(getApplication()).close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        executor.shutdown()
    }

    fun connect(ip: String, port: Int) {
        executor.submit(Runnable {
            try {
                val manager = AdbConnectionManager.getInstance(getApplication())
                var connectionStatus: Boolean
                try {
                    connectionStatus = manager.connect(ip, port)
                } catch (th: Throwable) {
                    connectionStatus = false
                }
                connectAdb.postValue(connectionStatus)
                if (connectionStatus) {
                    executor.submit(Runnable {
                        // NOTE: This delay is needed to let the connection settle before requesting stats/volume.
                        // Using Thread.sleep here to maintain serialization with other tasks in the executor.
                        Thread.sleep(1000)
                        refreshStatsImmediately()
                        queryVolumeInfo()
                    })
                }
            } catch (th: Throwable) {
                connectAdb.postValue(false)
            }
        })
    }

    fun startQrHandshakeListener() {
        Thread {
            var serverSocket: java.net.ServerSocket? = null
            try {
                serverSocket = java.net.ServerSocket(9999)
                logger.d("QR Handshake listener started on port 9999")
                while (true) {
                    val socket = serverSocket.accept()
                    val input = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                    val headsetIp = input.readLine()
                    logger.d("Received IP from headset: $headsetIp")
                    if (headsetIp != null) {
                        connect(headsetIp, DEFAULT_PORT)
                    }
                    socket.close()
                }
            } catch (e: Exception) {
                if (e is java.net.BindException) {
                    logger.w("QR Handshake listener port 9999 already in use, skipping new instance.")
                } else {
                    logger.e("Failed in QR Handshake listener", e)
                }
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }.start()
    }

    fun autoConnect() {
        executor.submit(Runnable { this.autoConnectInternal() })
    }

    fun disconnect() {
        executor.submit(Runnable {
            try {
                val manager = AdbConnectionManager.getInstance(getApplication())
                manager.disconnect()
                AdbConnectionManager.resetInstance()
                connectAdb.postValue(false)
            } catch (th: Throwable) {
                th.printStackTrace()
                connectAdb.postValue(true)
            }
        })
    }

    fun getPairingPort() {
        executor.submit(Runnable {
            val atomicPort = AtomicInteger(-1)
            val resolveHostAndPort = CountDownLatch(1)

            val adbMdns = AdbMdns(
                getApplication<Application>(),
                AdbMdns.SERVICE_TYPE_TLS_PAIRING,
                OnAdbDaemonDiscoveredListener { hostAddress: InetAddress?, port: Int ->
                    atomicPort.set(port)
                    resolveHostAndPort.countDown()
                })
            adbMdns.start()

            try {
                if (!resolveHostAndPort.await(1, TimeUnit.MINUTES)) {
                    return@Runnable
                }
            } catch (ignore: InterruptedException) {
            } finally {
                adbMdns.stop()
            }
            pairingPort.postValue(atomicPort.get())
        })
    }



    fun pair(ip: String, port: Int, pairingCode: String) {
        executor.submit(Runnable {
            try {
                val pairingStatus: Boolean
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val manager = AdbConnectionManager.getInstance(getApplication())
                    pairingStatus = manager.pair(ip, port, pairingCode)
                } else pairingStatus = false
                pairAdb.postValue(pairingStatus)
                autoConnectInternal()
            } catch (th: Throwable) {
                th.printStackTrace()
                pairAdb.postValue(false)
            }
        })
    }

    @WorkerThread
    private fun autoConnectInternal() {
        try {
            val manager = AdbConnectionManager.getInstance(getApplication())
            var connected = false
            try {
                connected = manager.autoConnect(getApplication(), 5000)
            } catch (e: AdbPairingRequiredException) {
                askPairAdb.postValue(true)
                return
            } catch (th: Throwable) {
                th.printStackTrace()
            }
            if (!connected) {
                connected = manager.connect(DEFAULT_PORT)
            }
            if (connected) {
                connectAdb.postValue(true)
                refreshStatsImmediately()
                queryVolumeInfo()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    fun execute(command: String, packageName: String? = null) {
        logger.d("execute() called with command: $command, pkg: $packageName")
        if (command.contains("TOGGLE_PASSTHROUGH")) {
            val currentRunning = _runningPackages.value ?: emptySet()
            if (command.contains("enable true")) {
                _runningPackages.postValue(currentRunning + "ix.moohan.passthrough")
            } else if (command.contains("enable false")) {
                _runningPackages.postValue(currentRunning - "ix.moohan.passthrough")
            }
        }

        if (command.contains("auto_spatialization_settings")) {
            val enabled = command.endsWith("1")
            lastManualSpatialToggleTime = System.currentTimeMillis()
            _deviceStats.value = _deviceStats.value.copy(spatializationEnabled = enabled)
        }

        executor.submit(Runnable {
            runAdbShellCommand(command)
        })
    }

    private fun runAdbShellCommand(command: String) {
        try {
            synchronized(shellStreamLock) {
                if (adbShellStream == null || adbShellStream!!.isClosed()) {
                    val manager = AdbConnectionManager.getInstance(getApplication())
                    adbShellStream = manager.openStream(LocalServices.SHELL)
                    viewModelScope.launch(Dispatchers.IO) {
                        readOutput()
                    }
                }
            }
            if (command == "clear") {
                clearEnabled = true
            }
            adbShellStream!!.openOutputStream().use { os ->
                os.write("su\n".toByteArray(StandardCharsets.UTF_8))
                os.flush()
                os.write(String.format("%1\$s\n", command).toByteArray(StandardCharsets.UTF_8))
                os.flush()
                os.write("\n".toByteArray(StandardCharsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            commandOutput.postValue("Error: both su and ADB failed. ${e.message}")
        }
    }

    private fun parseAmStartCommand(command: String, packageName: String?, context: android.content.Context): android.content.Intent? {
        val trimmed = command.trim()
        if (trimmed.startsWith("monkey")) {
            val pkg = packageName ?: extractPkgFromMonkey(trimmed) ?: return null
            return context.packageManager.getLaunchIntentForPackage(pkg)
        }
        
        if (trimmed.startsWith("am start")) {
            val intent = android.content.Intent()
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 1. Handle component name (-n package/activity)
            if (trimmed.contains(" -n ")) {
                val nIndex = trimmed.indexOf(" -n ")
                val componentPart = trimmed.substring(nIndex + 4).split(Regex("\\s+")).firstOrNull()
                if (componentPart != null && componentPart.contains("/")) {
                    val parts = componentPart.split("/")
                    val pkg = parts[0]
                    val activity = parts[1]
                    val fullActivity = if (activity.startsWith(".")) pkg + activity else activity
                    intent.setClassName(pkg, fullActivity)
                    return intent
                }
            }
            
            // 2. Handle Action and Data (-a action -d data)
            var action: String? = null
            var data: String? = null
            var pkg: String? = packageName
            
            val parts = trimmed.split(Regex("\\s+"))
            var i = 0
            while (i < parts.size) {
                when (parts[i]) {
                    "-a" -> {
                        if (i + 1 < parts.size) {
                            action = parts[i + 1]
                            i++
                        }
                    }
                    "-d" -> {
                        if (i + 1 < parts.size) {
                            data = parts[i + 1]
                            i++
                        }
                    }
                    "-p" -> {
                        if (i + 1 < parts.size) {
                            pkg = parts[i + 1]
                            i++
                        }
                    }
                }
                i++
            }
            
            if (action != null) {
                intent.action = action
            } else if (data != null) {
                intent.action = android.content.Intent.ACTION_VIEW
            }
            
            if (data != null) {
                intent.data = android.net.Uri.parse(data.trim('\'').trim('"'))
            }
            
            if (pkg != null) {
                intent.setPackage(pkg)
            }
            
            if (intent.action != null || intent.component != null) {
                return intent
            }
        }
        
        return null
    }

    private fun extractPkgFromMonkey(command: String): String? {
        val parts = command.split(Regex("\\s+"))
        val pIndex = parts.indexOf("-p")
        if (pIndex != -1 && pIndex + 1 < parts.size) {
            return parts[pIndex + 1]
        }
        return null
    }




    fun write(fileBase64: ByteArray, command: String?) {
        executor.submit(Runnable {
            try {
                val manager = AdbConnectionManager.getInstance(getApplication())
                val adbShellStreamForScrcpy = manager.openStream(LocalServices.SHELL)
                val os = adbShellStreamForScrcpy.openOutputStream()
                try {
                    os.write(("" + '\n').toByteArray(StandardCharsets.US_ASCII))
                    os.flush()
                    os.write("\n".toByteArray(StandardCharsets.US_ASCII))

                    val len = fileBase64.size
                    val filePart = ByteArray(4056)
                    var sourceOffset = 0
                    try {
                        os.write((" cd data/local/tmp " + '\n').toByteArray(StandardCharsets.US_ASCII))
                        os.write((" rm -f serverBase64 " + '\n').toByteArray(StandardCharsets.US_ASCII))
                        while (sourceOffset < len) {
                            if (len - sourceOffset >= 4056) {
                                System.arraycopy(
                                    fileBase64,
                                    sourceOffset,
                                    filePart,
                                    0,
                                    4056
                                ) //Writing in 4KB pieces. 4096-40  ---> 40 Bytes for actual command text.
                                sourceOffset = sourceOffset + 4056
                                val ServerBase64part = String(filePart, StandardCharsets.US_ASCII)
                                os.write(
                                    (" echo " + ServerBase64part + " >> serverBase64" + '\n').toByteArray(
                                        StandardCharsets.US_ASCII
                                    )
                                )
                            } else {
                                val rem = len - sourceOffset
                                val remPart = ByteArray(rem)
                                System.arraycopy(fileBase64, sourceOffset, remPart, 0, rem)
                                sourceOffset = sourceOffset + rem
                                val ServerBase64part = String(remPart, StandardCharsets.US_ASCII)
                                os.write(
                                    (" echo " + ServerBase64part + " >> serverBase64" + '\n').toByteArray(
                                        StandardCharsets.US_ASCII
                                    )
                                )
                            }
                        }
                        os.write(
                            (" base64 -d < serverBase64 > scrcpy-server.jar && rm serverBase64" + '\n').toByteArray(
                                StandardCharsets.US_ASCII
                            )
                        )
                        os.write((command + '\n').toByteArray(StandardCharsets.US_ASCII))
                        os.flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
    }

    fun startRemoteStatusMonitoring(){
        stopRemoteStatusMonitoring()
        remoteStatusMonitoringScope.launch {
            try{
                while(true){
                    if (!coroutineContext.isActive) {
                        break
                    }

                    val micJob = launch {
                        fetchRemoteStatus { adbOutputStream, bufferReader ->
                            dispatchMicStatus(adbOutputStream, bufferReader)
                        }
                        delay(1000)
                    }
                    val runningAppJob = launch {
                        fetchRemoteStatus { adbOutputStream, bufferReader ->
                            dispatchRemoteRunningAppStatus(adbOutputStream, bufferReader)
                        }
                        delay(1000)
                    }
                    val mediaVolumeJob = launch {
                        fetchRemoteStatus { adbOutputStream, bufferReader ->
                            dispatchRemoteMediaVolumeStatus(adbOutputStream, bufferReader)
                        }
                        delay(1000)
                    }
                    val dominantHandJob = launch {
                        fetchRemoteStatus { adbOutputStream, bufferReader ->
                            dispatchDominantHandStatus(adbOutputStream, bufferReader)
                        }
                        delay(1000)
                    }
                    val passthroughJob = launch {
                        fetchRemoteStatus { adbOutputStream, bufferReader ->
                            dispatchPassthroughStatus(adbOutputStream, bufferReader)
                        }
                        delay(100)
                    }

                    micJob.join()
                    runningAppJob.join()
                    mediaVolumeJob.join()
                    dominantHandJob.join()
                    passthroughJob.join()
                    delay(800)
                }
            }catch (e: RuntimeException){
                logger.w("Failed remote status", e)
            }finally {
                logger.d("finish stopRemoteStatusMonitoring")
            }
        }
    }

    fun stopRemoteStatusMonitoring(){
        if(remoteStatusMonitoringScope.coroutineContext.isActive){
            remoteStatusMonitoringScope.coroutineContext.cancel()
        }
    }

    @Synchronized
    fun fetchRemoteStatus(block: ((AdbOutputStream, BufferedReader) -> Unit)){
        var streamForRemoteStatus: AdbStream? = null
        var manager: AbsAdbConnectionManager? = null
        try {
            manager = AdbConnectionManager.getInstance(getApplication())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        try {
            streamForRemoteStatus = manager!!.openStream(LocalServices.SHELL)
            streamForRemoteStatus?.openOutputStream().use { adbOutputStream ->
                BufferedReader(InputStreamReader(streamForRemoteStatus?.openInputStream())).use { bufferedReader ->
                    block.invoke(adbOutputStream!!, bufferedReader)
                }
            }
        } catch (e: IllegalStateException) {
            logger.v("failed IllegalStateException : ${e.message}")
        } catch (e: Exception) {
            logger.w("failed Exception in fetchRemoteStatus", e)
        } finally {
            try {
                streamForRemoteStatus?.close()
            } catch (e: Exception) {
                logger.v("streamForRemoteStatus close failed: ${e.message}")
            }
        }
    }

    private fun dispatchMicStatus(adbOutputStream: AdbOutputStream, bufferedReader: BufferedReader){
        val command =
            "dumpsys audio | grep 'mic mute' | sed -n 's/.*from system=\\([^ ]*\\).*/\\1/p'"

        var line: String?
        var result: String? = null

        // try getting mic status
        adbOutputStream.write(String.format("%s\n", command).toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()

        adbOutputStream.write("exit\n".toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()

        // Important: Sending the 'exit' command is necessary to close the shell session,
        // which allows reader.readLine() to return null and exit the loop.
        // Otherwise, readLine() might block indefinitely.
//                    os.write("exit\n".getBytes(StandardCharsets.UTF_8));
//                    os.flush();

        // The output of the command will usually be one line or a few lines at most.
        // The sed command will either output one line or nothing.
        while ((bufferedReader.readLine().also { line = it }) != null) {
            // Ignore the output of the "exit" command itself or any blank lines
            if (line!!.trim { it <= ' ' }
                    .isEmpty() || line.contains("grep 'mic mute'") || line.contains(
                    "sed -n"
                )) continue
            if (line.trim { it <= ' ' } == "true" || line.trim { it <= ' ' } == "false") {
                result = line.trim { it <= ' ' }
                break // Exit the loop as we've found the desired result
            }
        }

        if (result != null) {
            _isMicMuted.postValue("true" == result)
        } else {
            logger.w("failed dispatchMicStatus")
        }
    }

    private fun dispatchRemoteRunningAppStatus(adbOutputStream: AdbOutputStream, bufferedReader: BufferedReader){
        // try getting running top app status
        val command = "ps -A | grep 'com.'; dumpsys window | grep -E 'XrModeActivity|SceneViewer'\n"
        adbOutputStream.write(String.format("%s\n", command).toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()
        adbOutputStream.write("exit\n".toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()

        var line: String?
        val runningSet = mutableSetOf<String>()

        while ((bufferedReader.readLine().also { line = it }) != null) {
            val trimmedLine = line!!.trim()
            if (trimmedLine.isEmpty() || trimmedLine.contains("grep 'com.'") || trimmedLine.contains("dumpsys window")) continue
            
            if (trimmedLine.contains("XrModeActivity")) {
                runningSet.add("com.google.android.googlequicksearchbox:gemini")
            }
            if (trimmedLine.contains("SceneViewer") || trimmedLine.contains("sceneviewer")) {
                runningSet.add("com.google.android.googlequicksearchbox:sceneviewer")
            }

            // Extracts package name from ps output (usually the last column)
            val parts = trimmedLine.split(Regex("\\s+"))
            if (parts.isNotEmpty()) {
                val pkg = parts.last()
                if (pkg.contains("com.") && !pkg.contains("/")) {
                    runningSet.add(pkg)
                }
            }
        }
        
        if (runningSet.isNotEmpty()) {
            _runningPackages.postValue(runningSet)
        } else {
            _runningPackages.postValue(emptySet())
        }
    }

    private fun dispatchRemoteMediaVolumeStatus(adbOutputStream: AdbOutputStream, bufferedReader: BufferedReader){
        val command =
            "cmd media_session volume --stream 3 --get | sed -n '/volume is/ p'"
        adbOutputStream.write(String.format("%s\n", command).toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()
        // Important: Sending the 'exit' command is necessary to close the shell session,
        // which allows reader.readLine() to return null and exit the loop.
        // Otherwise, readLine() might block indefinitely.
        adbOutputStream.write("exit\n".toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()

        var line: String?
        var matchResult: MatchResult? = null
        var volume: Int = 0

        // The output of the command will usually be one line or a few lines at most.
        // The sed command will either output one line or nothing.
        while ((bufferedReader.readLine().also { line = it }) != null) {
            // Ignore the output of the "exit" command itself or any blank lines
            if (line!!.trim { it <= ' ' }
                    .isEmpty() || line.contains("sed -n")) continue

            if (line.trim { it <= ' ' }.contains("volume is")) {
//                Log.e(TAG,"line : ${line.toString()}")
                val regex = Regex("\\b\\d+\\b")
                matchResult = regex.find(line)
                if(matchResult != null){
                    volume = matchResult.value.toInt()
//                    Log.e(TAG,"volume : ${volume}")
                }
                break
            }
        }
        if (matchResult != null) {
            if(_remoteMediaVolume.value != volume){
                _remoteMediaVolume.postValue(volume)
            }
        } else {
            logger.w("failed dispatchRemoteMediaVolumeStatus")
        }
    }

    fun queryVolumeInfo() {
        fetchRemoteStatus { adbOutputStream, bufferedReader ->
            val command = "dumpsys audio"
            adbOutputStream.write(String.format("%s\n", command).toByteArray(StandardCharsets.UTF_8))
            adbOutputStream.flush()
            adbOutputStream.write("exit\n".toByteArray(StandardCharsets.UTF_8))
            adbOutputStream.flush()

            var line: String?
            var currentStream: String? = null

            while ((bufferedReader.readLine().also { line = it }) != null) {
                val trimmedLine = line!!.trim()
                
                // 1. Check for stream header
                if (trimmedLine.startsWith("- STREAM_NOTIFICATION")) {
                    currentStream = "NOTIFICATION"
                } else if (trimmedLine.startsWith("- STREAM_MUSIC")) {
                    currentStream = "MUSIC"
                } else if (trimmedLine.startsWith("- STREAM_SYSTEM:") || trimmedLine.startsWith("- STREAM_SYSTEM (")) {
                    currentStream = "SYSTEM"
                } else if (trimmedLine.startsWith("- STREAM_11") || trimmedLine.startsWith("- Stream 11") || trimmedLine.startsWith("- STREAM_ASSISTANT")) {
                    currentStream = "ASSISTANT"
                }

                if (currentStream == null) continue

                // 2. Parse Max limit
                if (trimmedLine.startsWith("Max:")) {
                    val maxVol = trimmedLine.substringAfter("Max:").trim().toIntOrNull()
                    if (maxVol != null) {
                        updateMaxVolume(currentStream, maxVol)
                    }
                } 
                
                // 3. Also try format [0..X] for max volume just in case
                val regexBracket = Regex("""\[\d+\.\.(\d+)\]""")
                val matchBracket = regexBracket.find(trimmedLine)
                if (matchBracket != null) {
                    val maxVol = matchBracket.groupValues[1].toIntOrNull()
                    if (maxVol != null) {
                        updateMaxVolume(currentStream, maxVol)
                    }
                }

                // 4. Parse Current volume
                if (trimmedLine.startsWith("Current:")) {
                    val regexUsb = Regex("""\(usb_device\):\s*(\d+)""")
                    val regexDefault = Regex("""\(default\):\s*(\d+)""")
                    
                    val matchUsb = regexUsb.find(trimmedLine)
                    if (matchUsb != null) {
                        val volume = matchUsb.groupValues[1].toInt()
                        updateCurrentVolume(currentStream, volume)
                    } else {
                        val matchDefault = regexDefault.find(trimmedLine)
                        if (matchDefault != null) {
                            val volume = matchDefault.groupValues[1].toInt()
                            updateCurrentVolume(currentStream, volume)
                        }
                    }
                    // Reset after parsing current to look for next stream
                    currentStream = null 
                }
            }
        }
    }

    private fun updateMaxVolume(stream: String, maxVol: Int) {
        when (stream) {
            "NOTIFICATION" -> _remoteNotificationMaxVolume.postValue(maxVol)
            "MUSIC" -> _remoteMediaMaxVolume.postValue(maxVol)
            "SYSTEM" -> _remoteSystemMaxVolume.postValue(maxVol)
            "ASSISTANT" -> _remoteGeminiMaxVolume.postValue(maxVol)
        }
    }

    private fun updateCurrentVolume(stream: String, volume: Int) {
        when (stream) {
            "NOTIFICATION" -> _remoteNotificationVolume.postValue(volume)
            "MUSIC" -> _remoteMediaVolume.postValue(volume)
            "SYSTEM" -> _remoteSystemVolume.postValue(volume)
            "ASSISTANT" -> _remoteGeminiVolume.postValue(volume)
        }
    }

    private fun dispatchDominantHandStatus(adbOutputStream: AdbOutputStream, bufferedReader: BufferedReader){
        // try getting running top app status
        val command =
            "settings get system primary_hand"
        adbOutputStream.write(String.format("%s\n", command).toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()
        // Important: Sending the 'exit' command is necessary to close the shell session,
        // which allows reader.readLine() to return null and exit the loop.
        // Otherwise, readLine() might block indefinitely.
        adbOutputStream.write("exit\n".toByteArray(StandardCharsets.UTF_8))
        adbOutputStream.flush()

        var line: String?
        var result = ""
        val keyword = listOf("0", "1", "null")

        // The output of the command will usually be one line or a few lines at most.
        // The sed command will either output one line or nothing.
        while ((bufferedReader.readLine().also { line = it }) != null) {
            // Ignore the output of the "exit" command itself or any blank lines

            if (line!!.trim { it <= ' ' }
                    .isEmpty() || line.contains("primary_hand")) continue
            if (keyword.contains(line.trim { it <= ' ' })) {
                result = line.trim { it <= ' ' }
                break
            }
        }

        if (result.contains("null"))
            result = "1"

        if(_dominantHandStatus.value != result.toInt()){
            _dominantHandStatus.postValue(result.toInt())
        }
    }

    private fun dispatchPassthroughStatus(adbOutputStream: AdbOutputStream, bufferedReader: BufferedReader) {
        // Disabled property polling for passthrough as it's unreliable.
        // State is managed optimistically in execute().
    }

    // --- Live Telemetry Live Stats Polling Loop ---
    private val _deviceStats = MutableStateFlow(DeviceStateWrapper())
    val deviceStats: StateFlow<DeviceStateWrapper> = _deviceStats.asStateFlow()

    private var statsJob: kotlinx.coroutines.Job? = null

    fun refreshStatsImmediately() {
        statsJob?.cancel()
        _deviceStats.value = _deviceStats.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            startStatsPolling()
        }
    }

    fun startStatsPolling() {
        if (statsJob?.isActive == true) return
        statsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive) {
                try {
                    val activeDev = _activeDevice.value
                    val isConnected = connectAdb.value ?: false
                    
                    if (activeDev != DeviceType.HOME) {
                        if (!isConnected) {
                            _deviceStats.value = DeviceStateWrapper(isAttached = false)
                        } else {
                            // Open stream lazily inside the loop when connected
                            synchronized(statsStreamLock) {
                                if (adbStatsStream == null || adbStatsStream!!.isClosed()) {
                                    try {
                                        adbStatsOutputStream?.close()
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    val manager = AdbConnectionManager.getInstance(getApplication())
                                    adbStatsStream = manager.openStream(LocalServices.SHELL)
                                    adbStatsOutputStream = adbStatsStream!!.openOutputStream()
                                    launch(Dispatchers.IO) {
                                        readStatsOutput()
                                    }
                                }
                            }
                            
                            val currentStats = _deviceStats.value
                            val hasValues = currentStats.wifiSpeedMbps != null || currentStats.powerDrawWatts != null || currentStats.thermalTempCelsius != null
                            // Only show loading indicator if we don't have values yet
                            if (!hasValues) {
                                _deviceStats.value = currentStats.copy(isLoading = true)
                            }
                            
                            // Write command to stream instead of one-shot
                            writeStatsCommand(activeDev)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Stats Polling failed", e)
                    _deviceStats.value = DeviceStateWrapper(isAttached = false)
                }
                val isPresentlyAttached = _deviceStats.value.isAttached
                delay(if (isPresentlyAttached) 5000 else 500)
            }
        }
    }

    private fun writeStatsCommand(device: DeviceType) {
        val combinedCmd = when (device) {
            DeviceType.GALAXY_XR -> {
                "echo '[BAT]'; dumpsys battery | grep -E 'level:|status:'; echo '[THERM]'; cat /sys/class/sec/sec-ap-thermistor/temperature; echo '[WIFI]'; cmd wifi status; echo '[PWR]'; cat /sys/class/sec/max34427/all_ch1; echo '[BUILD]'; getprop ro.build.type; echo '[SPATIAL]'; settings get system auto_spatialization_settings; echo '[END_STATS]'"
            }
            else -> {
                "echo '[BAT]'; dumpsys battery | grep -E 'level:|status:'; echo '[THERM]'; cat /sys/class/thermal/thermal_zone0/temp; echo '[WIFI]'; cmd wifi status; echo '[PWR]'; cat /sys/class/power_supply/battery/current_now; echo '[BUILD]'; getprop ro.build.type; echo '[SPATIAL]'; settings get system auto_spatialization_settings; echo '[END_STATS]'"
            }
        }
        
        try {
            adbStatsOutputStream?.let { os ->
                os.write(String.format("%1\$s\n", combinedCmd).toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }
        } catch (e: Exception) {
            logger.e("Failed to write stats command", e)
        }
    }

    private fun readStatsOutput() {
        try {
            BufferedReader(InputStreamReader(adbStatsStream!!.openInputStream())).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    if (line?.trim() == "[END_STATS]") {
                        val parsedStats = parseStats(sb.toString())
                        _deviceStats.value = parsedStats.copy(isAttached = true)
                        sb.delete(0, sb.length) // Clear for next block
                    } else {
                        sb.append(line).append("\n")
                        // Guard against OOM if delimiter is missing
                        if (sb.length > 10000) {
                            logger.w("Stats buffer grew too large without finding delimiter. Clearing.")
                            sb.delete(0, sb.length)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.d("Stats stream closed.")
        }
    }

    private fun parseStats(raw: String): DeviceStateWrapper {
        val device = _activeDevice.value
        var batPercent = 100
        var isCharging = false
        var thermalTemp: Float? = null
        var wifiSpeed: Int? = null
        var powerWatts: Float? = null
        var buildType: String? = null
        var spatialEnabled: Boolean? = null

        var currentSection = ""
        val batRaw = StringBuilder()
        val thermRaw = StringBuilder()
        val wifiRaw = StringBuilder()
        val pwrRaw = StringBuilder()
        val buildRaw = StringBuilder()
        val spatialRaw = StringBuilder()

        try {
            for (line in raw.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("[BAT]") -> currentSection = "BAT"
                    trimmed.startsWith("[THERM]") -> currentSection = "THERM"
                    trimmed.startsWith("[WIFI]") -> currentSection = "WIFI"
                    trimmed.startsWith("[PWR]") -> currentSection = "PWR"
                    trimmed.startsWith("[BUILD]") -> currentSection = "BUILD"
                    trimmed.startsWith("[SPATIAL]") -> currentSection = "SPATIAL"
                    else -> {
                        when (currentSection) {
                            "BAT" -> batRaw.append(line).append("\n")
                            "THERM" -> thermRaw.append(line).append("\n")
                            "WIFI" -> wifiRaw.append(line).append("\n")
                            "PWR" -> pwrRaw.append(line).append("\n")
                            "BUILD" -> buildRaw.append(line).append("\n")
                            "SPATIAL" -> spatialRaw.append(line).append("\n")
                        }
                    }
                }
            }

            if (batRaw.isEmpty() && thermRaw.isEmpty() && wifiRaw.isEmpty() && pwrRaw.isEmpty() && buildRaw.isEmpty() && spatialRaw.isEmpty()) {
                logger.w("Failed to parse any live stats section. Preserving cache.")
                return _deviceStats.value
            }

            // Extract values
            val batString = batRaw.toString()
            batPercent = "\\blevel:\\s*(\\d+)".toRegex().find(batString)?.groupValues?.get(1)?.toIntOrNull()
                ?: batString.replace(Regex("[^0-9]"), "").toIntOrNull() // fallback
                ?: 100
            val batStatus = "\\bstatus:\\s*(\\d+)".toRegex().find(batString)?.groupValues?.get(1)?.toIntOrNull()
            isCharging = batStatus == 2 || batStatus == 5 // 2 = Charging, 5 = Full
            thermalTemp = thermRaw.toString().trim().toFloatOrNull()?.let {
                if (device == DeviceType.GALAXY_XR) it / 10f else it / 1000f
            }
            wifiSpeed = "\\b(\\d+)\\s*Mbps\\b".toRegex().find(wifiRaw.toString())?.groupValues?.get(1)?.toIntOrNull()
            
            val pwrStr = pwrRaw.toString().trim()
            powerWatts = if (device == DeviceType.GALAXY_XR) {
                pwrStr.split(",").getOrNull(1)?.replace("W", "")?.trim()?.toFloatOrNull()
            } else {
                pwrStr.toFloatOrNull()?.let { it / 1000000f }
            }

            buildType = buildRaw.toString().trim().ifEmpty { null }
            
            val spatialEnabledFromPoll = spatialRaw.toString().trim().let { 
                when (it) {
                    "1" -> true
                    "0" -> false
                    else -> null
                }
            }

            // Only update spatialization state from poll if enough time has passed since last manual toggle
            // to avoid flickering caused by stale ADB output being read after a command was sent.
            if (System.currentTimeMillis() - lastManualSpatialToggleTime > 3000) {
                spatialEnabled = spatialEnabledFromPoll
            } else {
                spatialEnabled = _deviceStats.value.spatializationEnabled
            }

        } catch (e: Exception) {
            logger.e("Failed to parse batched stats block", e)
        }

        return DeviceStateWrapper(
            batteryPercent = batPercent,
            isCharging = isCharging,
            thermalTempCelsius = thermalTemp,
            wifiSpeedMbps = wifiSpeed,
            powerDrawWatts = powerWatts,
            buildType = buildType,
            spatializationEnabled = spatialEnabled
        )
    }

    suspend fun runAdbOneShotCommand(command: String): String = kotlinx.coroutines.withTimeoutOrNull(5000) {
        suspendCancellableCoroutine<String> { continuation ->
            var stream: AdbStream? = null
            val future = executor.submit(Runnable {
                try {
                    val manager = AdbConnectionManager.getInstance(getApplication())
                    stream = manager.openStream("shell:$command")
                    val output = StringBuilder()
                    stream.openInputStream().use { input_stream ->
                        val buffer = ByteArray(256)
                        var read = 0
                        while (input_stream.read(buffer).also { read = it } != -1) {
                            output.append(String(buffer, 0, read))
                        }
                    }
                    stream.close()
                    if (continuation.isActive) {
                        continuation.resume(output.toString())
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("Stream closed") == true) {
                        // Silently fail without filling Logcat noisy pollution clutter
                    } else {
                        logger.e("AdbOneShot failed: $command", e)
                    }
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            })
            continuation.invokeOnCancellation {
                future.cancel(true)
                try {
                    stream?.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    } ?: ""

    suspend fun checkIsScrcpyServerRunning(): Boolean {
        val output = runAdbOneShotCommand("pgrep -f org.las2mile.scrcpy.Server")
        return output.trim().isNotEmpty()
    }

}
