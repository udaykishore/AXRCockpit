package com.ukp.axrcockpit
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.ukp.axrcockpit.util.AnalyticsManager
import com.ukp.axrcockpit.Scrcpy.ServiceCallbacks
import com.ukp.axrcockpit.util.QrUtil
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.util.Base64
import com.ukp.axrcockpit.util.Logger
import android.view.Surface
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.ukp.axrcockpit.ui.theme.AppTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ukp.axrcockpit.ui.MainGridActionItem
import com.ukp.axrcockpit.ui.MainGridItem
import com.ukp.axrcockpit.ui.MainGridItemFactory
import com.ukp.axrcockpit.ui.MainGridRemoteActionItem
import com.ukp.axrcockpit.ui.MainGridRemoteLaunchItem
import com.ukp.axrcockpit.ui.MainGridRemoteToggleItem
import com.ukp.axrcockpit.ui.MainGridVolumeItem
import com.ukp.axrcockpit.ui.AuraAppScreen
import com.ukp.axrcockpit.ui.RavineAppScreen
import com.ukp.axrcockpit.ui.AdminAppScreen
import com.ukp.axrcockpit.ui.GalaxyAppScreen

import com.ukp.axrcockpit.ui.CockpitHeader
import com.ukp.axrcockpit.ui.GlassmorphismStatsToolbar
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import org.conscrypt.Conscrypt
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.Security
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConnectionStatus { IDLE, PAIRED, CONNECTED, DISCONNECTED, }

class MainActivity : ComponentActivity(), Scrcpy.ServiceCallbacks {
    private val logger = Logger.get(MainActivity::class.java)
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val updateLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            logger.w("In-app update flow failed or was canceled by the user. Exiting application.")
            finish()
        }
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var scrcpy: Scrcpy
    private var firstTime: Boolean = true
    private var serviceBound: Boolean = false
    @Volatile private var waitingForNewConfig: Boolean = false
    private var resumingTimeoutJob: kotlinx.coroutines.Job? = null

    private lateinit var surface: Surface
    private var screenHeight = 720
    private var screenWidth = 1280
    private var serverAdr: String = ""
    private lateinit var sharedPreferences: SharedPreferences

    private var connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            scrcpy = (iBinder as Scrcpy.MyServiceBinder).service
            scrcpy.setServiceCallbacks(this@MainActivity)
            if (::surface.isInitialized) {
                scrcpy.start(
                    surface,
                    serverAdr,
                    screenHeight,
                    screenWidth,
                    Scrcpy.DEFAULT_PORT
                )
            }
            firstTime = false
            serviceBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            serviceBound = false
        }
    }

    override fun loadNewRotation() {
        runOnUiThread {
            if (serviceBound) {
                waitingForNewConfig = false // Received new config
                scrcpy.setParms(surface, screenWidth, screenHeight)
            }
        }
    }

    override fun onFrameRendered() {
        if (!waitingForNewConfig) {
            viewModel.setScrcpyResuming(false)
            resumingTimeoutJob?.cancel()
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            if (viewModel.isScrcpyResuming.value == true) {
                // We are in the middle of a resolution switch, do not disconnect ADB
                // and keep the resuming state!
                logger.d("onDisconnected ignored during resolution switch")
            } else {
                viewModel.disconnect()
                viewModel.setScrcpyResuming(false)
                resumingTimeoutJob?.cancel()
            }
        }
    }

    override fun onResolutionDetected(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val aspectRatio = width.toFloat() / height.toFloat()
            viewModel.setVideoAspectRatio(aspectRatio)
        }
    }

    private fun enterResumingState() {
        viewModel.setScrcpyResuming(true)
        resumingTimeoutJob?.cancel()
        resumingTimeoutJob = lifecycleScope.launch {
            delay(20000) // 20 seconds timeout
            if (viewModel.isScrcpyResuming.value == true) {
                logger.w("Resuming timeout reached. Stopping scrcpy service.")
                viewModel.setScrcpyResuming(false)
                viewModel.setScrcpyRunning(false)
                sharedPreferences.edit().putBoolean("SCRCPY_RUNNING", false).apply()
                if (serviceBound) {
                    withContext(Dispatchers.IO) {
                        scrcpy.StopService()
                        withContext(Dispatchers.Main) {
                            unbindService(serviceConnection)
                            serviceBound = false
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        viewModel.startQrHandshakeListener()



        sharedPreferences = getSharedPreferences("PAIR_SETTINGS", MODE_PRIVATE)
        
        // Restore active device state BEFORE setting content to avoid flicker or black screen
        val savedDevice = sharedPreferences.getString("DEFAULT_DEVICE", null)
        if (savedDevice != null) {
            logger.i("Restoring active device state to $savedDevice")
            try {
                viewModel.setActiveDevice(DeviceType.valueOf(savedDevice))
            } catch (e: Exception) {
                logger.e("Failed to restore device: $savedDevice", e)
            }
            // Clear the saved device so subsequent cold starts go to HOME
            sharedPreferences.edit().remove("DEFAULT_DEVICE").apply()
        }

        AnalyticsManager.initialize(this)

        if (BuildConfig.ENABLE_UPDATE_CHECK) {
            checkForUpdates()
        }

        logger.i("onCreate")
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val configuration = LocalConfiguration.current
            val isConnected by connectionStatus.collectAsStateWithLifecycle()
            val isMuted by viewModel.isMicMuted.observeAsState(initial = false)
            val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()

            AppTheme(darkTheme = true) {
                MainAppScreen(windowSizeClass, configuration, isMuted, isConnected, activeDevice)
            }
        }

        // KEEP SCREEN ON
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.watchConnectAdb().observe(this) { isConnected ->
            if (isConnected) {
                connectedXRDevice()
                startScrcpy()
            } else {
                disconnectedXRDevice()
            }
        }

        viewModel.watchCommandOutput().observe(this) { output ->
            if (output != null) {
                val outputStr = output.toString().trim()
                if (outputStr.startsWith("Error:") ||
                    outputStr.contains("Permission denied") ||
                    outputStr.contains("SecurityException") ||
                    outputStr.contains("Exception")) {
                    val displayStr = outputStr.lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotEmpty() }
                        ?: outputStr
                    val truncatedStr = if (displayStr.length > 150) {
                        displayStr.take(150) + "..."
                    } else {
                        displayStr
                    }
                    Toast.makeText(this, truncatedStr, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.isMicMuted.observe(this){
            (buttonItems[MainGridItem.Type.MIC] as? MainGridRemoteToggleItem)?.run {
                this.isEnabled.tryEmit(!it)
            }
        }

        viewModel.runningPackages.observe(this) { runningSet ->
            appItems.forEachIndexed { index, appItem ->
                appItems[index].isActivated.tryEmit(runningSet.contains(appItem.packageName))
            }

            buttonItems.values.forEach { gridItem ->
                if(gridItem is MainGridRemoteLaunchItem){
                    gridItem.isRunning.tryEmit(runningSet.contains(gridItem.packageName))
                }
            }
        }

        viewModel.remoteMediaVolume.observe(this){
            (buttonItems[MainGridItem.Type.VOLUME] as? MainGridVolumeItem)?.run {
                this.volumeLevel.tryEmit(it)
            }
        }

        viewModel.dominantHandStatus.observe(this){
            (buttonItems[MainGridItem.Type.DOMINANT_HAND] as? MainGridRemoteToggleItem)?.run {
                when(it){
                    0 ->{
                        this.isEnabled.tryEmit(false)
                    }
                    else ->{
                        this.isEnabled.tryEmit(true)
                    }
                }
            }
        }

        viewModel.scrcpyMaxSize.observe(this) { size ->
            if (serviceBound) {
                val (w, h) = when(size) {
                    540 -> Pair(960, 540)
                    720 -> Pair(1280, 720)
                    1080 -> Pair(1920, 1080)
                    1440 -> Pair(2560, 1440)
                    else -> Pair(1280, 720)
                }
                screenWidth = w
                screenHeight = h
                
                if (::scrcpy.isInitialized) {
                    logger.d("Sending resolution change request to server: $size")
                    scrcpy.sendResolutionChange(size)
                    waitingForNewConfig = true // Start waiting
                    enterResumingState()
                } else {
                    logger.w("Scrcpy service not initialized, cannot change resolution dynamically.")
                }
            }
        }
    }



    @Composable
    fun ScrcpyVideoDisplay(
        modifier: Modifier = Modifier,
    ) {
        val isConnected by connectionStatus.collectAsStateWithLifecycle()

        Box(
            modifier = modifier
                .background(Color.DarkGray)
        ) {
            if (isConnected == ConnectionStatus.CONNECTED || isConnected == ConnectionStatus.PAIRED) {
                if (LocalInspectionMode.current) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        Text("Video Stream (Preview)", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            SurfaceView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                this.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                        surface = holder.surface
                                        if (serviceBound) {
                                            scrcpy.setParms(surface, screenWidth, screenHeight)
                                        }
                                    }

                                    override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
                                        if (width == screenWidth && height == screenHeight && holder.surface == surface) {
                                            return // No change, avoid redundant recreations
                                        }
                                        surface = holder.surface
                                        screenWidth = width
                                        screenHeight = height
                                        if (serviceBound) {
                                            enterResumingState()
                                            scrcpy.setParms(surface, screenWidth, screenHeight)
                                        }
                                    }

                                    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                        if (serviceBound) {
                                            scrcpy.pause()
                                        }
                                    }
                                })
                                surface = this.holder.surface
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.device_disconnected), color = Color.White)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun MainAppScreen(
        windowSizeClass: WindowSizeClass,
        configuration: Configuration,
        isMuted: Boolean,
        isConnected: ConnectionStatus,
        activeDevice: DeviceType
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val openPairDialog = remember { androidx.compose.runtime.mutableStateOf(false) }
        val openConnectDialog = remember { androidx.compose.runtime.mutableStateOf(false) }
        val openAdbReportDialog = remember { androidx.compose.runtime.mutableStateOf(false) }
        val adbReportContent = remember { androidx.compose.runtime.mutableStateOf("") }
        val context = androidx.compose.ui.platform.LocalContext.current
        val versionName = remember {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                    drawerContainerColor = Color.DarkGray
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(48.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "AXR Cockpit",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DeviceType.entries.forEach { device ->
                        if (device == DeviceType.HOME) {
                            NavigationDrawerItem(
                                label = { Text(device.label) },
                                selected = activeDevice == device,
                                onClick = {
                                    viewModel.setActiveDevice(device)
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            listOf(DeviceType.GALAXY_XR, DeviceType.AURA, DeviceType.RAVINE, DeviceType.ADMIN).forEach { subDevice ->
                                NavigationDrawerItem(
                                    label = { Text(subDevice.label, fontSize = 13.sp) },
                                    selected = activeDevice == subDevice,
                                    onClick = {
                                        viewModel.setActiveDevice(subDevice)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        val icon = when (subDevice) {
                                            DeviceType.GALAXY_XR -> Icons.Default.ViewInAr
                                            DeviceType.AURA -> Icons.Default.AutoAwesome
                                            DeviceType.RAVINE -> Icons.Default.Landscape
                                            DeviceType.ADMIN -> Icons.Default.AdminPanelSettings
                                            else -> Icons.Default.ArrowDropDown
                                        }
                                        Icon(icon, null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 24.dp)
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 28.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    val isActuallyConnected = isConnected == ConnectionStatus.CONNECTED || isConnected == ConnectionStatus.PAIRED
                    val isConnectEnabled = activeDevice != DeviceType.HOME || isActuallyConnected
                    
                    // Main level items (Rest all at same level as Home)
                    NavigationDrawerItem(
                        label = { 
                            Text(
                                text = if (isActuallyConnected) stringResource(R.string.disconnect) else stringResource(R.string.connect),
                                color = if (isConnectEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ) 
                        },
                        selected = false,
                        onClick = {
                            if (isConnectEnabled) {
                                if (isActuallyConnected) {
                                    disconnectedXRDevice()
                                } else {
                                    openConnectDialog.value = true
                                }
                                scope.launch { drawerState.close() }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(if (isActuallyConnected) R.drawable.ic_stop_vec else R.drawable.ic_play_arrow_vec),
                                contentDescription = if (isActuallyConnected) stringResource(R.string.disconnect) else stringResource(R.string.connect),
                                tint = if (!isConnectEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else if (isActuallyConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    if (isConnected == ConnectionStatus.CONNECTED) {
                        val isScrcpyRunning by viewModel.isScrcpyRunning.observeAsState(false)
                        val isScrcpyResuming by viewModel.isScrcpyResuming.observeAsState(false)
                        val currentResolution by viewModel.scrcpyMaxSize.observeAsState(1440)

                        val toggleScrcpy = remember(isScrcpyResuming, serviceBound, isScrcpyRunning) {
                            { start: Boolean ->
                                if (!isScrcpyResuming) {
                                    if (start) {
                                        if (serviceBound) {
                                            enterResumingState()
                                            scrcpy.resume()
                                            viewModel.setScrcpyRunning(true)
                                        } else {
                                            logger.d("Service not bound, starting scrcpy.")
                                            startScrcpy()
                                        }
                                    } else {
                                        if (serviceBound) {
                                            viewModel.setScrcpyResuming(false)
                                            scrcpy.pause()
                                        }
                                        viewModel.setScrcpyRunning(false)
                                    }
                                }
                            }
                        }

                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = if (isScrcpyRunning) "Stop" else "Start",
                                    color = if (isScrcpyResuming) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            selected = false,
                            onClick = { toggleScrcpy(!isScrcpyRunning) },
                            icon = {
                                Icon(
                                    painter = painterResource(if (isScrcpyRunning) R.drawable.ic_stop_vec else R.drawable.ic_play_arrow_vec),
                                    contentDescription = null,
                                    tint = if (isScrcpyResuming) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else if (isScrcpyRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            badge = {
                                Switch(
                                    checked = isScrcpyRunning,
                                    onCheckedChange = { toggleScrcpy(it) },
                                    enabled = !isScrcpyResuming
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 24.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        var expanded by remember { mutableStateOf(false) }
                        val resolutions = listOf("540", "720", "1080", "1440")

                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = "Resolution",
                                    color = if (isScrcpyResuming) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            selected = false,
                            onClick = { if (!isScrcpyResuming) expanded = true },
                            icon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = if (isScrcpyResuming) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            badge = {
                                Box {
                                    TextButton(
                                        onClick = { expanded = true },
                                        enabled = !isScrcpyResuming
                                    ) {
                                        Text(
                                            text = "${currentResolution}p", 
                                            color = if (isScrcpyResuming) MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.secondary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown, 
                                            null, 
                                            tint = if (isScrcpyResuming) MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        resolutions.forEach { res ->
                                            DropdownMenuItem(
                                                text = { Text(res, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {
                                                    expanded = false
                                                    val size = res.toInt()
                                                    viewModel.setScrcpyMaxSize(size)
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 24.dp)
                        )
                    }

                    NavigationDrawerItem(
                        label = { 
                            Text(
                                stringResource(R.string.pair), 
                                color = if (isConnectEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ) 
                        },
                        selected = false,
                        onClick = {
                            if (isConnectEnabled) {
                                openPairDialog.value = true
                                scope.launch { drawerState.close() }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_link_vec),
                                contentDescription = stringResource(R.string.pair),
                                tint = if (isConnectEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Setup ADB over WiFi") },
                        selected = false,
                        onClick = {
                            val result = UsbAdbManager.sendAdbTcpIpCommand(this@MainActivity)
                            val phoneIp = result["phoneIp"] ?: "Unknown"
                            val deviceIp = result["deviceIp"] ?: "None"
                            val rawOutput = result["rawOutput"] ?: "No data"
                            val message = result["message"] ?: "No message"
                            val publicKey = result["publicKey"] ?: "Unknown"

                            if (deviceIp != "None") {
                                sharedPreferences.edit()
                                    .putString("IP", deviceIp)
                                    .putString("CONN_PORT", "45555")
                                    .apply()
                                serverAdr = deviceIp // Update global
                            }

                            val dialogMessage = """
                                Status: $message
                                Phone IP: $phoneIp
                                Device IP Extracted: $deviceIp
                                
                                Phone's Public Key (Paste in target's adb_keys):
                                $publicKey
                                
                                Raw ADB Output (trimmed):
                                ${rawOutput.take(300)}
                            """.trimIndent()
                            
                            adbReportContent.value = dialogMessage
                            openAdbReportDialog.value = true
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Setup ADB",
                                tint = Color.White
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.help)) },
                        selected = false,
                        onClick = {
                            showHelpDialog()
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_help_vec),
                                contentDescription = stringResource(R.string.help),
                                tint = Color.White
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Open Source Licenses") },
                        selected = false,
                        onClick = {
                            showLicensesDialog()
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Open Source Licenses",
                                tint = Color.White
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(Modifier.weight(1f))


                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "v$versionName • ukp@google.com",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )
                    }
                }
            }
        ) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val orientation = configuration.orientation
            val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val isSquareIsh = isLandscape && (configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat() < 1.3f)

            Scaffold(
                containerColor = Color.Black
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    val customCommands by viewModel.customCommandsMap.collectAsState()
                    val isEditMode by viewModel.isEditMode.collectAsState()
                    if (activeDevice == DeviceType.HOME) {
                        HomeDashboardContent(
                            windowSizeClass = windowSizeClass,
                            configuration = configuration,
                            isMuted = isMuted,
                            isConnected = isConnected,
                            activeDevice = activeDevice,
                            scope = scope,
                            drawerState = drawerState,
                            showConnectDialog = { showConnectDialog() },
                            showPairDialog = { showPairDialog() },
                            showHelpDialog = { showHelpDialog() }
                        )
                    } else if (activeDevice == DeviceType.AURA) {
                        val runningPackages by viewModel.runningPackages.observeAsState()
                        val isScrcpyResuming by viewModel.isScrcpyResuming.observeAsState(false)
                        val isScrcpyRunning by viewModel.isScrcpyRunning.observeAsState(false)
                        val currentResolution by viewModel.scrcpyMaxSize.observeAsState(1440)
                        val videoAspectRatio by viewModel.videoAspectRatio.observeAsState(16f / 9f)
                        val mediaMaxVolume by viewModel.remoteMediaMaxVolume.observeAsState(15)
                        val notificationMaxVolume by viewModel.remoteNotificationMaxVolume.observeAsState(7)
                        val systemMaxVolume by viewModel.remoteSystemMaxVolume.observeAsState(7)
                        val geminiMaxVolume by viewModel.remoteGeminiMaxVolume.observeAsState(15)
                        val mediaVolume by viewModel.remoteMediaVolume.observeAsState(10)
                        val notificationVolume by viewModel.remoteNotificationVolume.observeAsState(5)
                        val systemVolume by viewModel.remoteSystemVolume.observeAsState(5)
                        val geminiVolume by viewModel.remoteGeminiVolume.observeAsState(10)
                        
                        val button1Label by viewModel.button1Label.collectAsState()
                        val button1Command by viewModel.button1Command.collectAsState()
                        val button2Label by viewModel.button2Label.collectAsState()
                        val button2Command by viewModel.button2Command.collectAsState()
                        
                        AuraAppScreen(
                            windowSizeClass = windowSizeClass,
                            configuration = configuration,
                            runningPackages = runningPackages,
                            statsState = viewModel.deviceStats.collectAsState(),
                            currentResolution = currentResolution,
                            videoAspectRatio = videoAspectRatio,
                            mediaMaxVolume = mediaMaxVolume,
                            notificationMaxVolume = notificationMaxVolume,
                            systemMaxVolume = systemMaxVolume,
                            geminiMaxVolume = geminiMaxVolume,
                            mediaVolume = mediaVolume,
                            notificationVolume = notificationVolume,
                            systemVolume = systemVolume,
                            geminiVolume = geminiVolume,
                            onExecuteCommand = { command, pkg, isStart ->
                                if (isConnected != ConnectionStatus.CONNECTED) {
                                    Toast.makeText(this@MainActivity, getString(R.string.device_disconnected), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.execute(command, pkg)
                                    if (command.contains("pm list packages")) {
                                        viewModel.clearRunningPackages()
                                    }
                                    if (pkg != null) {
                                        if (isStart) {
                                            viewModel.addRunningPackage(pkg)
                                        } else {
                                            viewModel.removeRunningPackage(pkg)
                                        }
                                    }
                                }
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onToggleScrcpy = { start ->
                                if (serviceBound) {
                                    if (start) {
                                        enterResumingState()
                                        scrcpy.resume()
                                    } else {
                                        viewModel.setScrcpyResuming(false)
                                        scrcpy.pause()
                                    }
                                    viewModel.setScrcpyRunning(start)
                                }
                            },
                            isConnected = isConnected == ConnectionStatus.CONNECTED,
                            isScrcpyRunning = isScrcpyRunning,
                            isResuming = isScrcpyResuming,
                            scrcpyContent = { modifier ->
                                ScrcpyVideoDisplay(modifier = modifier)
                            },
                            button1Label = button1Label,
                            button1Command = button1Command,
                            button2Label = button2Label,
                            button2Command = button2Command,
                            onSaveButtonConfig = { idx, label, cmd -> viewModel.saveButtonConfig(idx, label, cmd) },
                            onResetButtonConfig = { idx -> viewModel.resetButtonConfig(idx) },
                            customCommands = customCommands,
                            isEditMode = isEditMode,
                            onSaveCustomCommand = { id, label, cmd, stopCmd -> viewModel.saveCustomCommand(id, label, cmd, stopCmd) },
                            onResetCustomCommand = { id -> viewModel.resetCustomCommand(id) }
                        )
                    } else if (activeDevice == DeviceType.RAVINE) {
                        val runningPackages by viewModel.runningPackages.observeAsState()
                        val isScrcpyResuming by viewModel.isScrcpyResuming.observeAsState(false)
                        val isScrcpyRunning by viewModel.isScrcpyRunning.observeAsState(false)
                        val currentResolution by viewModel.scrcpyMaxSize.observeAsState(1440)
                        val videoAspectRatio by viewModel.videoAspectRatio.observeAsState(16f / 9f)
                        val mediaMaxVolume by viewModel.remoteMediaMaxVolume.observeAsState(15)
                        val notificationMaxVolume by viewModel.remoteNotificationMaxVolume.observeAsState(7)
                        val systemMaxVolume by viewModel.remoteSystemMaxVolume.observeAsState(7)
                        val geminiMaxVolume by viewModel.remoteGeminiMaxVolume.observeAsState(15)
                        val mediaVolume by viewModel.remoteMediaVolume.observeAsState(10)
                        val notificationVolume by viewModel.remoteNotificationVolume.observeAsState(5)
                        val systemVolume by viewModel.remoteSystemVolume.observeAsState(5)
                        val geminiVolume by viewModel.remoteGeminiVolume.observeAsState(10)
                        
                        val button1Label by viewModel.button1Label.collectAsState()
                        val button1Command by viewModel.button1Command.collectAsState()
                        val button2Label by viewModel.button2Label.collectAsState()
                        val button2Command by viewModel.button2Command.collectAsState()
                        
                        RavineAppScreen(
                            windowSizeClass = windowSizeClass,
                            configuration = configuration,
                            runningPackages = runningPackages,
                            statsState = viewModel.deviceStats.collectAsState(),
                            currentResolution = currentResolution,
                            videoAspectRatio = videoAspectRatio,
                            mediaMaxVolume = mediaMaxVolume,
                            notificationMaxVolume = notificationMaxVolume,
                            systemMaxVolume = systemMaxVolume,
                            geminiMaxVolume = geminiMaxVolume,
                            mediaVolume = mediaVolume,
                            notificationVolume = notificationVolume,
                            systemVolume = systemVolume,
                            geminiVolume = geminiVolume,
                            onExecuteCommand = { command, pkg, isStart ->
                                if (isConnected != ConnectionStatus.CONNECTED) {
                                    Toast.makeText(this@MainActivity, getString(R.string.device_disconnected), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.execute(command, pkg)
                                    if (command.contains("pm list packages")) {
                                        viewModel.clearRunningPackages()
                                    }
                                    if (pkg != null) {
                                        if (isStart) {
                                            viewModel.addRunningPackage(pkg)
                                        } else {
                                            viewModel.removeRunningPackage(pkg)
                                        }
                                    }
                                }
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onToggleScrcpy = { start ->
                                if (serviceBound) {
                                    if (start) {
                                        enterResumingState()
                                        scrcpy.resume()
                                    } else {
                                        viewModel.setScrcpyResuming(false)
                                        scrcpy.pause()
                                    }
                                    viewModel.setScrcpyRunning(start)
                                }
                            },
                            isConnected = isConnected == ConnectionStatus.CONNECTED,
                            isScrcpyRunning = isScrcpyRunning,
                            isResuming = isScrcpyResuming,
                            scrcpyContent = { modifier ->
                                ScrcpyVideoDisplay(modifier = modifier)
                            },
                            button1Label = button1Label,
                            button1Command = button1Command,
                            button2Label = button2Label,
                            button2Command = button2Command,
                            onSaveButtonConfig = { idx, label, cmd -> viewModel.saveButtonConfig(idx, label, cmd) },
                            onResetButtonConfig = { idx -> viewModel.resetButtonConfig(idx) },
                            customCommands = customCommands,
                            isEditMode = isEditMode,
                            onSaveCustomCommand = { id, label, cmd, stopCmd -> viewModel.saveCustomCommand(id, label, cmd, stopCmd) },
                            onResetCustomCommand = { id -> viewModel.resetCustomCommand(id) }
                        )
                    } else if (activeDevice == DeviceType.GALAXY_XR) {
                        val runningPackages by viewModel.runningPackages.observeAsState()
                        val isScrcpyResuming by viewModel.isScrcpyResuming.observeAsState(false)
                        val isScrcpyRunning by viewModel.isScrcpyRunning.observeAsState(false)
                        val currentResolution by viewModel.scrcpyMaxSize.observeAsState(1440)
                        val videoAspectRatio by viewModel.videoAspectRatio.observeAsState(16f / 9f)
                        val mediaMaxVolume by viewModel.remoteMediaMaxVolume.observeAsState(15)
                        val notificationMaxVolume by viewModel.remoteNotificationMaxVolume.observeAsState(7)
                        val systemMaxVolume by viewModel.remoteSystemMaxVolume.observeAsState(15)
                        val geminiMaxVolume by viewModel.remoteGeminiMaxVolume.observeAsState(15)
                        val mediaVolume by viewModel.remoteMediaVolume.observeAsState(10)
                        val notificationVolume by viewModel.remoteNotificationVolume.observeAsState(5)
                        val systemVolume by viewModel.remoteSystemVolume.observeAsState(5)
                        val geminiVolume by viewModel.remoteGeminiVolume.observeAsState(10)
                        
                        GalaxyAppScreen(
                            windowSizeClass = windowSizeClass,
                            configuration = configuration,
                            runningPackages = runningPackages,
                            statsState = viewModel.deviceStats.collectAsState(),
                            videoAspectRatio = videoAspectRatio,
                            mediaMaxVolume = mediaMaxVolume,
                            notificationMaxVolume = notificationMaxVolume,
                            systemMaxVolume = systemMaxVolume,
                            geminiMaxVolume = geminiMaxVolume,
                            mediaVolume = mediaVolume,
                            notificationVolume = notificationVolume,
                            systemVolume = systemVolume,
                            geminiVolume = geminiVolume,
                            customCommands = customCommands,
                            isEditMode = isEditMode,
                            onSaveCustomCommand = { id, label, cmd, stopCmd -> viewModel.saveCustomCommand(id, label, cmd, stopCmd) },
                            onResetCustomCommand = { id -> viewModel.resetCustomCommand(id) },
                            onExecuteCommand = { command, pkg, isStart ->
                                if (isConnected != ConnectionStatus.CONNECTED) {
                                    Toast.makeText(this@MainActivity, getString(R.string.device_disconnected), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.execute(command, pkg)
                                    if (command.contains("pm list packages")) {
                                        viewModel.clearRunningPackages()
                                    }
                                    if (pkg != null) {
                                        if (isStart) {
                                            viewModel.addRunningPackage(pkg)
                                        } else {
                                            viewModel.removeRunningPackage(pkg)
                                        }
                                    }
                                }
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onToggleScrcpy = { start ->
                                if (serviceBound) {
                                    if (start) {
                                        enterResumingState()
                                        scrcpy.resume()
                                    } else {
                                        viewModel.setScrcpyResuming(false)
                                        scrcpy.pause()
                                    }
                                    viewModel.setScrcpyRunning(start)
                                }
                            },
                            isConnected = isConnected == ConnectionStatus.CONNECTED,
                            isScrcpyRunning = isScrcpyRunning,
                            isResuming = isScrcpyResuming,
                            scrcpyContent = { modifier ->
                                ScrcpyVideoDisplay(modifier = modifier)
                            }
                        )
                    } else if (activeDevice == DeviceType.ADMIN) {
                        AdminAppScreen(
                            windowSizeClass = windowSizeClass,
                            configuration = configuration,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            isEditMode = isEditMode,
                            onEditModeChanged = { enabled ->
                                viewModel.setEditMode(enabled)
                            },
                            onResetAllCommands = {
                                viewModel.resetAllCustomCommands()
                                Toast.makeText(this@MainActivity, "Reset all custom commands to defaults", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            GlassmorphismStatsToolbar(
                                title = activeDevice.label,
                                deviceState = viewModel.deviceStats.collectAsState(),
                                isConnected = isConnected == ConnectionStatus.CONNECTED || isConnected == ConnectionStatus.PAIRED,
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("Screen for ${activeDevice.label}", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (openAdbReportDialog.value) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { openAdbReportDialog.value = false },
                    title = { Text("USB ADB Operation Result", color = Color.White) },
                    text = {
                        Text(adbReportContent.value, color = Color.White)
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = { openAdbReportDialog.value = false }
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color(0xFF1E1E24)
                )
            }

            if (openPairDialog.value) {
                val ipText = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getString("IP", "") ?: "") }
                val portText = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getString("PORT", "45555") ?: "") }
                val pairKeyText = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getString("PAIR_KEY", "000000") ?: "") }

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { openPairDialog.value = false },
                    title = { Text(stringResource(R.string.pair_device), color = Color.White) },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val localIp = remember { QrUtil.getLocalIpAddress() ?: "Unknown IP" }
                            val qrBitmap = remember(localIp) { QrUtil.generateQrCode(localIp, 400) }
                            
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.qr_code),
                                modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = "Scan this QR with the headset to pair\\n(Your IP: $localIp)",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            androidx.compose.material3.OutlinedTextField(
                                value = ipText.value,
                                onValueChange = { ipText.value = it },
                                label = { Text(stringResource(R.string.ip_address)) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Cyan,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = portText.value,
                                onValueChange = { portText.value = it },
                                label = { Text(stringResource(R.string.port)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Cyan,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = pairKeyText.value,
                                onValueChange = { pairKeyText.value = it },
                                label = { Text(stringResource(R.string.pair_key)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Cyan,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                sharedPreferences.edit {
                                    putString("IP", ipText.value)
                                    putString("PORT", portText.value)
                                    putString("PAIR_KEY", pairKeyText.value)
                                }
                                val port = portText.value.toIntOrNull() ?: 45555
                                viewModel.pair(ipText.value, port, pairKeyText.value)
                                openPairDialog.value = false
                            }
                        ) {
                            Text(stringResource(R.string.pair))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.Button(
                            onClick = { openPairDialog.value = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    containerColor = Color(0xFF1E1E24)
                )
            }

            if (openConnectDialog.value) {
                val ipText = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getString("IP", "") ?: "") }
                val portText = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getString("CONN_PORT", "45555") ?: "") }
                val useIpv6 = remember { androidx.compose.runtime.mutableStateOf(sharedPreferences.getBoolean("USE_IPV6", false)) }
                
                val errorMessage = remember(ipText.value, useIpv6.value) {
                    val ip = ipText.value.trim()
                    if (useIpv6.value && !ip.contains(":") && ip.isNotEmpty()) {
                        "Please enter a valid IPv6 address"
                    } else if (!useIpv6.value && ip.contains(":")) {
                        "Please enter a valid IPv4 address"
                    } else {
                        null
                    }
                }

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { openConnectDialog.value = false },
                    title = { Text(stringResource(R.string.connect_device), color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.OutlinedTextField(
                                value = ipText.value,
                                onValueChange = { ipText.value = it },
                                label = { Text(stringResource(R.string.ip_address)) },
                                isError = errorMessage != null,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = if (errorMessage != null) Color.Red else Color.Cyan,
                                    unfocusedBorderColor = if (errorMessage != null) Color.Red else Color.Gray
                                )
                            )
                            if (errorMessage != null) {
                                Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                            }
                            
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = useIpv6.value,
                                    onCheckedChange = { useIpv6.value = it },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = Color.Cyan,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                Text("Enable IPv6", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = portText.value,
                                onValueChange = { portText.value = it },
                                label = { Text(stringResource(R.string.port)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Cyan,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                sharedPreferences.edit {
                                    putString("IP", ipText.value.trim())
                                    putString("CONN_PORT", portText.value.trim())
                                    putBoolean("USE_IPV6", useIpv6.value)
                                }
                                serverAdr = ipText.value.trim()
                                val port = portText.value.trim().toIntOrNull() ?: 45555
                                viewModel.connect(ipText.value.trim(), port)
                                openConnectDialog.value = false
                            },
                            enabled = errorMessage == null && ipText.value.trim().isNotEmpty()
                        ) {
                            Text(stringResource(R.string.connect))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.Button(
                            onClick = { openConnectDialog.value = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    containerColor = Color(0xFF1E1E24)
                )
            }
        }
    }

    @Composable
    fun HomeDashboardContent(
        windowSizeClass: WindowSizeClass,
        configuration: Configuration,
        isMuted: Boolean,
        isConnected: ConnectionStatus,
        activeDevice: DeviceType,
        scope: kotlinx.coroutines.CoroutineScope,
        drawerState: androidx.compose.material3.DrawerState,
        showConnectDialog: () -> Unit,
        showPairDialog: () -> Unit,
        showHelpDialog: () -> Unit
    ) {
        val showAppList = remember { mutableStateOf(false) }
        val orientation = configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val isSquareIsh = isLandscape && (configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat() < 1.3f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CockpitHeader(
                    title = activeDevice.label,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                val devices = remember { DeviceType.entries.filter { it != DeviceType.HOME } }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(items = devices) { device ->
                        val icon = when (device) {
                            DeviceType.HOME -> Icons.Default.Home
                            DeviceType.GALAXY_XR -> Icons.Default.ViewInAr
                            DeviceType.AURA -> Icons.Default.AutoAwesome
                            DeviceType.RAVINE -> Icons.Default.Landscape
                            DeviceType.ADMIN -> Icons.Default.AdminPanelSettings
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(255, 255, 255, 25))
                                .border(1.dp, Color(255, 255, 255, 51), RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.setActiveDevice(device)
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    icon, 
                                    contentDescription = null, 
                                    tint = Color.White, 
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    device.label, 
                                    color = Color.White, 
                                    fontSize = 18.sp, 
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

            if (showAppList.value) {
                logger.e("showAppList")
                AppListPopup(
                    apps = appItems,
                    onClose = { showAppList.value = false },
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }

    @Preview
    @Composable
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    fun MainAppScreenPreview() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        }
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
        AppTheme(darkTheme = true) {
            MainAppScreen(
                windowSizeClass = windowSizeClass,
                configuration = configuration,
                isMuted = false,
                isConnected = ConnectionStatus.DISCONNECTED,
                activeDevice = DeviceType.HOME
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun ButtonsGrid(showAppList: MutableState<Boolean>, windowSizeClass: WindowSizeClass, modifier: Modifier = Modifier) {
        val items = buttonItems.toList().sortedBy { it.second is MainGridVolumeItem }
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val spacing = 8.dp
            val verticalPadding = 8.dp
            // Calculate height for 6 rows to fit exactly. 
            // We use 6 as the base count for the calculation to satisfy the "Pixel 10 Pro" exact fit.
            val itemHeight = (availableHeight - (verticalPadding * 2) - (spacing * 5)) / 6

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, span = { pair ->
                    val span = if (pair.second is MainGridVolumeItem) 3 else 1
                    androidx.compose.foundation.lazy.grid.GridItemSpan(span)
                }) { (type, gridItem) ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (gridItem) {
                            is MainGridRemoteLaunchItem -> MainGridRemoteLaunchButton(type, gridItem)
                            is MainGridRemoteToggleItem -> MainGridRemoteToggleButton(type, gridItem)
                            is MainGridRemoteActionItem -> MainGridRemoteActionItem(type, gridItem)
                            is MainGridVolumeItem -> MainGridVolumeButton(gridItem)
                            is MainGridActionItem -> {
                                GridButton(
                                    icon = gridItem.icon,
                                    label = gridItem.getCurrentLabelName(),
                                    onClick = {
                                        if (type == MainGridItem.Type.APPS) showAppList.value = true
                                        else if (type == MainGridItem.Type.AURA) viewModel.setActiveDevice(DeviceType.AURA)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun runIfConnected(block: () -> Unit) {
        if (connectionStatus.value != ConnectionStatus.CONNECTED) {
            Toast.makeText(this, getString(R.string.device_disconnected), Toast.LENGTH_SHORT).show()
        } else {
            block()
        }
    }

    @Composable
    fun ControlButton(icon: Int, onClick: () -> Unit, background: Color = MaterialTheme.colorScheme.secondaryContainer, iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(background)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    @Composable
    fun MainGridRemoteLaunchButton(type: MainGridItem.Type, item: MainGridRemoteLaunchItem){
        val isRunning = item.isRunning.collectAsStateWithLifecycle()

        GridButton(
            icon = item.getCurrentIcon(),
            canTiltedIcon = item.canTiltedIcon,
            label = item.getCurrentLabelName(),
            isHighlighted = isRunning.value,
            onClick = {
                runIfConnected {
                    logger.e("clicked MainGridToggleItem : $type")
                    val command = if(isRunning.value){
                        item.stopCommand
                    }else{
                        item.launchCommand
                    }

                    AnalyticsManager.logEvent("action_execution", "App", item.getCurrentLabelName(), command)
                    viewModel.execute(command)
                }
            }
        )
    }

    @Composable
    fun MainGridRemoteToggleItem(type: MainGridItem.Type, item: MainGridRemoteToggleItem){
        val isEnabled = item.isEnabled.collectAsStateWithLifecycle()

        GridButton(
            icon = item.getCurrentIcon(),
            canTiltedIcon = item.canTiltedIcon,
            label = item.getCurrentLabelName(),
            isHighlighted = isEnabled.value,
            onClick = {
                runIfConnected {
                    logger.e("clicked MainGridToggleItem : $type")
                    val command = if(isEnabled.value){
                        item.disabledCommand
                    }else{
                        item.enabledCommand
                    }

                    AnalyticsManager.logEvent("action_execution", "API", item.getCurrentLabelName(), command)
                    viewModel.execute(command)

                    //Todo remove
                    item.isEnabled.tryEmit(!isEnabled.value)
                }
            }
        )
    }

    @Composable
    fun MainGridRemoteActionItem(type: MainGridItem.Type, item: MainGridRemoteActionItem){
        GridButton(
            icon = item.getCurrentIcon(),
            canTiltedIcon = item.canTiltedIcon,
            label = item.getCurrentLabelName(),
            onClick = {
                runIfConnected {
                    logger.e("clicked MainGridRemoteActionItem  : $type")
                    AnalyticsManager.logEvent("action_execution", "API", item.getCurrentLabelName(), item.command)
                    viewModel.execute(item.command)
                }
            }
        )
    }

    @Composable
    fun MainGridVolumeButton(item: MainGridVolumeItem){
        val volume by item.volumeLevel.collectAsStateWithLifecycle()
        var isMuted by remember { mutableStateOf(false) }

        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        modifier = Modifier.weight(2f)
                            .height(42.dp)
                            .align(Alignment.CenterVertically),
                        value = volume.toFloat(),
                        onValueChange = { newVolume ->
                            val volumeInt = newVolume.toInt()
                            item.volumeLevel.value = volumeInt
                        },
                        onValueChangeFinished = {
                            runIfConnected {
                                isMuted = volume == 0
                                AnalyticsManager.logEvent("action_execution", "Volume", "Slider", item.setVolumeCommand(volume))
                                viewModel.execute(item.setVolumeCommand(volume))
                            }
                        },
                        valueRange = 0f..15f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if(isMuted) Color.DarkGray else Color.White)
                            .align(Alignment.CenterVertically)
                            .clickable(onClick = {
                                runIfConnected {
                                    AnalyticsManager.logEvent("action_execution", "Volume", "Mute Toggle", item.volumeMuteCommand)
                                    viewModel.execute(item.volumeMuteCommand)
                                    isMuted = !isMuted
                                }
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (isMuted) R.drawable.ic_mute_speaker else R.drawable.qs_btn),
                            contentDescription = null,
                            tint = if (isMuted) Color.White else Color.Black,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun GridButton(
        icon: Int,
        canTiltedIcon: Boolean = false,
        label: String,
        isHighlighted: Boolean = false,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = label,
                    tint = if(canTiltedIcon.not()){
                        Color.Unspecified
                    } else {
                        if(isHighlighted){
                            MaterialTheme.colorScheme.onPrimary
                        }else{
                            Color.Black
                        }
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 3,
                minLines = 3,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun AppListPopup(apps: List<AppItem>, onClose: () -> Unit, windowSizeClass: WindowSizeClass) {
        val columns = when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 3
            WindowWidthSizeClass.Medium -> 4
            WindowWidthSizeClass.Expanded -> 6
            else -> 6 // Default for safety
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClose)
                .background(MaterialTheme.colorScheme.scrim),
            contentAlignment = Alignment.BottomCenter
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(apps) { app ->
                    AppItemView(app = app)
                }
            }
        }
    }

    @Composable
    fun AppItemView(app: AppItem) {
        val isActivated = app.isActivated.collectAsState().value
        GridButton(
            icon = app.icon,
            label = app.name,
            isHighlighted = isActivated,
            onClick = {
                runIfConnected {
                    AnalyticsManager.logEvent("action_execution", "App", app.name, app.commands(!isActivated))
                    viewModel.execute(app.commands(!isActivated))
                    app.isActivated.tryEmit(!isActivated)
                }
            }
        )
    }

    @Composable
    fun MainGridRemoteToggleButton(type: MainGridItem.Type, item: MainGridRemoteToggleItem){
        val isEnabled = item.isEnabled.collectAsStateWithLifecycle()

        GridButton(
            icon = item.getCurrentIcon(),
            canTiltedIcon = item.canTiltedIcon,
            label = item.getCurrentLabelName(),
            isHighlighted = isEnabled.value,
            onClick = {
                runIfConnected {
                    logger.e("clicked MainGridToggleItem : $type")
                    val command = if(isEnabled.value){
                        item.disabledCommand
                    }else{
                        item.enabledCommand
                    }

                    AnalyticsManager.logEvent("action_execution", "API", item.getCurrentLabelName(), command)
                    viewModel.execute(command)

                    //Todo remove
                    item.isEnabled.tryEmit(!isEnabled.value)
                }
            }
        )
    }

    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: Int,
        var isActivated: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val commands: (Boolean) -> String
    )

    val buttonItems = MainGridItemFactory.generateMainGridItems()
    val appItems = listOf(
        AppItem("Hand Onboarding", "com.google.axr.handtutorial", R.drawable.ic_school_vec) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.axr.handtutorial/com.unity3d.player.CustomGameActivity -e from oobe" else "am force-stop com.google.axr.handtutorial"
        },
        AppItem("Gemini Molecule", "com.google.xr.gemini_molecule", R.drawable.ic_gemini_vec) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.xr.gemini_molecule/com.unity3d.player.UnityPlayerGameActivity" else "am force-stop com.google.xr.gemini_molecule"
        },
        AppItem("Gemini Melody", "com.google.xr.music_string", R.drawable.ic_audio_media_note_solid) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.xr.music_string/com.unity3d.player.UnityPlayerGameActivity" else "am force-stop com.google.xr.music_string"
        },
        AppItem("Maps", "com.google.android.apps.maps", R.drawable.ic_maps_vec) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity" else "am force-stop com.google.android.apps.maps"
        },
        AppItem("Maps XR", "com.google.android.apps.immersiveviewxr", R.drawable.ic_maps_vec) { isButtonActive ->
            if (isButtonActive) "am start -a android.intent.action.VIEW -d 'geo:0,0?q=New york Times Square' -p com.google.android.apps.immersiveviewxr" else "am force-stop com.google.android.apps.immersiveviewxr"
        },
        AppItem("Youtube", "com.google.android.apps.youtube.xr", R.drawable.ic_youtube_vec) { isButtonActive ->
            if (isButtonActive) "am start -a android.intent.action.VIEW -d https://www.youtube.com/playlist?list=LL" else ""
        },
        AppItem("Youtube XR", "com.google.android.apps.youtube.xr", R.drawable.ic_youtube_xr_vec) { isButtonActive ->
            if (isButtonActive) "am start -a android.intent.action.VIEW -d \"https://www.youtube.com/watch?v=jqq_ZdD5Zwg\" com.google.android.apps.youtube.xr" else ""
        },
        AppItem("Stop Youtube", "", R.drawable.ic_youtube_xr_vec) { _ ->
            "am force-stop com.google.android.apps.youtube.xr"
        },
        AppItem("Google Photo", "com.google.android.apps.photos", R.drawable.ic_photos_vec) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.android.apps.photos/.home.HomeActivity" else "am force-stop com.google.android.apps.photos"
        },
        AppItem("Eye Onboarding", "com.google.axr.eyetutorial", R.drawable.ic_school_vec) { isButtonActive ->
            if (isButtonActive) "am start -n com.google.axr.eyetutorial/com.unity3d.player.CustomGameActivity -e from oobe" else "am force-stop com.google.axr.eyetutorial"
        },

    )

    fun showPairDialog() {
        if (connectionStatus.value == ConnectionStatus.PAIRED || connectionStatus.value == ConnectionStatus.CONNECTED) {
            Toast.makeText(this, "Already paired or connected!", Toast.LENGTH_SHORT).show()
            return
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val ipInputLayout = TextInputLayout(this).apply {
            hint = "ip"
        }
        val ipEditText = TextInputEditText(this).apply {
            setText(sharedPreferences.getString("IP", ""))
        }
        ipInputLayout.addView(ipEditText)
        layout.addView(ipInputLayout)

        val portInputLayout = TextInputLayout(this).apply {
            hint = "port"
        }
        val portEditText = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(sharedPreferences.getString("PORT", ""))
        }
        portInputLayout.addView(portEditText)
        layout.addView(portInputLayout)

        val pairKeyInputLayout = TextInputLayout(this).apply {
            hint = "pair key"
        }
        val pairKeyEditText = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(sharedPreferences.getString("PAIR_KEY", ""))
        }
        pairKeyInputLayout.addView(pairKeyEditText)
        layout.addView(pairKeyInputLayout)

        val scrollView = android.widget.ScrollView(this).apply {
            addView(layout)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pair))
            .setView(scrollView)
            .setPositiveButton(getString(R.string.pair)) { dialog, _ ->
                val ipText = ipEditText.text.toString().trim()
                val portText = portEditText.text.toString()
                val pairKeyText = pairKeyEditText.text.toString()

                sharedPreferences.edit {
                    putString("IP", ipText)
                    putString("PORT", portText)
                    putString("PAIR_KEY", pairKeyText)
                }

                val port = portText.toIntOrNull() ?: 45555
                viewModel.pair(ipText, port, pairKeyText)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showConnectDialog() {
        if (connectionStatus.value == ConnectionStatus.PAIRED || connectionStatus.value == ConnectionStatus.CONNECTED) {
            Toast.makeText(this, "Already paired or connected!", Toast.LENGTH_SHORT).show()
            return
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val ipInputLayout = TextInputLayout(this).apply {
            hint = "ip"
        }
        val ipEditText = TextInputEditText(this).apply {
            setText(sharedPreferences.getString("IP", ""))
        }
        ipInputLayout.addView(ipEditText)
        layout.addView(ipInputLayout)

        val portInputLayout = TextInputLayout(this).apply {
            hint = "port"
        }
        val portEditText = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(sharedPreferences.getString("CONN_PORT", "45555"))
        }
        portInputLayout.addView(portEditText)
        layout.addView(portInputLayout)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.connect))
            .setView(layout)
            .setPositiveButton(getString(R.string.connect)) { dialog, _ ->
                val ipText = ipEditText.text.toString().trim()
                val portText = portEditText.text.toString()

                sharedPreferences.edit {
                    putString("IP", ipText)
                    putString("CONN_PORT", portText)
                }

                serverAdr = ipText
                viewModel.connect(ipText, portText.toIntOrNull() ?: 45555)
                val observer = object : Observer<Boolean> {
                    override fun onChanged(value: Boolean) {
                        if (value) {
                            viewModel.watchConnectAdb().removeObserver(this)
                            connectedXRDevice()
                            startScrcpy()
                        } else {
                            disconnectedXRDevice()
                        }
                    }
                }
                viewModel.watchConnectAdb().observe(this, observer)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    fun showHelpDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val webview = WebView(this)
        webview.loadUrl("file:///android_asset/help.html")
        webview.settings.javaScriptEnabled = true
        layout.addView(webview)

        val helpDialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()

        helpDialog.setCanceledOnTouchOutside(true)
        helpDialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.8).toInt()
            helpDialog.window?.setLayout(width, height)
        }
        helpDialog.show()
    }

    fun showLicensesDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val webview = WebView(this)
        webview.loadUrl("file:///android_asset/licenses.html")
        webview.settings.javaScriptEnabled = true
        layout.addView(webview)

        val licensesDialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(true)
            .create()

        licensesDialog.setCanceledOnTouchOutside(true)
        licensesDialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.8).toInt()
            licensesDialog.window?.setLayout(width, height)
        }
        licensesDialog.show()
    }

    private fun startScrcpyservice() {
        val intent = Intent(this, Scrcpy::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun startScrcpy() {
        lifecycleScope.launch {
            logger.d("Starting Screencopy server.")
            val assetManager = assets
            try {
                val ipAddress = wifiIpAddress(serverAdr) ?: "0.0.0.0"
                // Use the new custom server jar
                val inputStream = assetManager.open("scrcpy-server.jar")
                val buffer = inputStream.readBytes()
                val fileBase64 = Base64.encode(buffer, 2)
                val command = StringBuilder()
                // Kill any existing server instance before starting a new one, with a sleep to ensure the port is released
                command.append(" pkill -9 -f org.las2mile.scrcpy.Server || true; sleep 1; ")
                // Use the old package name and pass default port Scrcpy.DEFAULT_PORT
                command.append(" CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / org.las2mile.scrcpy.Server ")
                command.append(" " + ipAddress + " " + (viewModel.scrcpyMaxSize.value ?: 720) + " " + 2048000 + " false " + Scrcpy.DEFAULT_PORT + ";")
                viewModel.write(fileBase64, command.toString())
                viewModel.setScrcpyRunning(true)
                sharedPreferences.edit().putBoolean("SCRCPY_RUNNING", true).apply()
                startScrcpyservice()
            } catch (e: IOException) {
                logger.e("Asset Manager: " + (e.message ?: "Unknown error"))
            }
        }
    }

    private fun wifiIpAddress(targetIp: String?): String? {
        try {
            var ipv4: InetAddress? = null
            var ipv6: InetAddress? = null
            
            val isTargetIpv6 = targetIp?.contains(":") == true

            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress.isLoopbackAddress) {
                        continue
                    }
                    if (inetAddress is Inet6Address) {
                        ipv6 = inetAddress
                    } else if (inetAddress is Inet4Address) {
                        ipv4 = inetAddress
                    }
                }
            }
            
            if (isTargetIpv6 && ipv6 != null) {
                return ipv6.hostAddress
            } else if (!isTargetIpv6 && ipv4 != null) {
                return ipv4.hostAddress
            }
            
            if (ipv6 != null) {
                return ipv6.hostAddress
            }
            if (ipv4 != null) {
                return ipv4.hostAddress
            }
            return null
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

    private fun connectedXRDevice(){
        connectionStatus.tryEmit(ConnectionStatus.CONNECTED)
        val port = sharedPreferences.getString("CONN_PORT", "45555")?.toIntOrNull() ?: 45555
        val currentIp = sharedPreferences.getString("IP", "") ?: ""
        // Auto-upgrade to IPv6 removed as per simplified requirement.
        // Restored status monitoring
        viewModel.startRemoteStatusMonitoring()
    }

    private fun disconnectedXRDevice(){
        if (connectionStatus.value != ConnectionStatus.CONNECTED) {
            logger.d("disconnectedXRDevice: Already disconnected or idle, skipping restart loop")
            connectionStatus.tryEmit(ConnectionStatus.DISCONNECTED)
            return
        }

        // 1. Mark as disconnected immediately to prevent re-entry
        connectionStatus.tryEmit(ConnectionStatus.DISCONNECTED)

        val currentDevice = viewModel.activeDevice.value.name
        logger.i("disconnectedXRDevice called from $currentDevice, triggering seamless restart")
        
        // 2. Save current active device to land back on it
        sharedPreferences.edit()
            .putString("DEFAULT_DEVICE", currentDevice)
            .putBoolean("SCRCPY_RUNNING", false)
            .apply()

        viewModel.stopRemoteStatusMonitoring()
        viewModel.setScrcpyRunning(false)
        viewModel.setScrcpyResuming(false)
        viewModel.clearRunningPackages()
        viewModel.disconnect()
        viewModel.stopStatsPolling()
        
        // 3. Clean up ADB instance before restart
        try {
            AdbConnectionManager.resetInstance()
        } catch (e: Exception) {
            // Ignore
        }

        if (::scrcpy.isInitialized && serviceBound) {
            lifecycleScope.launch(Dispatchers.IO) {
                scrcpy.StopService()
                withContext(Dispatchers.Main) {
                    if (serviceBound) {
                        try {
                            unbindService(serviceConnection)
                        } catch (e: Exception) {
                            // Ignore
                        }
                        serviceBound = false
                    }
                    
                    // 4. Restart the activity cleanly without animation
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    overridePendingTransition(0, 0)
                }
            }
        } else {
            // Restart immediately if no service bound
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.ENABLE_UPDATE_CHECK) {
            lifecycleScope.launch {
                try {
                    val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }
                } catch (e: Exception) {
                    logger.e("Failed to resume in-app update flow", e)
                }
            }
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            try {
                val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            } catch (e: Exception) {
                logger.e("Failed to check for or start in-app update", e)
            }
        }
    }
}
