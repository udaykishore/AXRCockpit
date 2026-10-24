package com.ukp.axrcockpit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.ukp.axrcockpit.R
import com.ukp.axrcockpit.db.CustomCommandEntity
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

// --- GLASSMORPHISM UI ---

val GlassCardColor = Color(30, 41, 59, 76)
val GlassBorderColor = Color(255, 255, 255, 51)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCardColor)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) { Column { content() } }
}

@Composable
fun MovingBlobsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val xOffset by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), "X")
    val yOffset by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), "Y")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1E293B).copy(alpha = 0.6f), Color.Transparent), center = Offset(xOffset, yOffset), radius = 400f), 400f, Offset(xOffset, yOffset))
            drawCircle(Brush.radialGradient(listOf(Color(0xFF334155).copy(alpha = 0.5f), Color.Transparent), center = Offset(800f - xOffset * 0.5f, 400f + yOffset * 0.5f), radius = 500f), 500f, Offset(800f - xOffset * 0.5f, 400f + yOffset * 0.5f))
        }
    }
}

// --- HEADER ---

@Composable
fun CockpitHeader(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuOnLeft: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp), // Standard toolbar height
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(if (menuOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
        }
    }
}



// --- MAIN SCREEN ---

@Composable
fun AuraAppScreen(
    windowSizeClass: WindowSizeClass,
    configuration: android.content.res.Configuration,
    runningPackages: Set<String>?,
    statsState: androidx.compose.runtime.State<DeviceStateWrapper>,
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onToggleScrcpy: (Boolean) -> Unit,
    isConnected: Boolean,
    isScrcpyRunning: Boolean,
    isResuming: Boolean,
    scrcpyContent: @Composable (Modifier) -> Unit,
    currentResolution: Int = 720,
    alwaysShowPlaceholder: Boolean = true,
    videoAspectRatio: Float = 16f / 9f,
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 7,
    geminiMaxVolume: Int = 15,
    mediaVolume: Int = 10,
    notificationVolume: Int = 5,
    systemVolume: Int = 5,
    geminiVolume: Int = 10,
    button1Label: String = "Custom 1",
    button1Command: String = "",
    button2Label: String = "Custom 2",
    button2Command: String = "",
    onSaveButtonConfig: (Int, String, String) -> Unit = { _, _, _ -> },
    onResetButtonConfig: (Int) -> Unit = {},
    customCommands: Map<String, CustomCommandEntity> = emptyMap(),
    isEditMode: Boolean = false,
    onSaveCustomCommand: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onResetCustomCommand: (String) -> Unit = {}
) {
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = windowSizeClass.widthSizeClass != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact
    val useSideBySide = isLandscape || isWideScreen

    var isFloating by remember { mutableStateOf(false) }
    var pipOffset by remember { mutableStateOf(Offset(50f, 250f)) }
    var pipWidth by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current
    var editingItem by remember { mutableStateOf<Triple<String, ControlItem, ControlItem>?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        MovingBlobsBackground()
        
        Column(modifier = Modifier.fillMaxSize()) {
            GlassmorphismStatsToolbar(
                title = "Aura",
                deviceState = statsState,
                isConnected = isConnected,
                onMenuClick = onMenuClick
            )

            if (!useSideBySide) { // Stacked layout for compact portrait
                if (!isFloating && (isScrcpyRunning || (alwaysShowPlaceholder && isConnected))) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            if (isScrcpyRunning) {
                                Box {
                                    scrcpyContent(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(videoAspectRatio)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    
                                    // Float Toggle Button
                                    IconButton(
                                        onClick = { isFloating = true },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Float", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (isResuming) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Resuming...", color = Color.White)
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(videoAspectRatio)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Screencopy Stopped", color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search Aura features...",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        AuraAllInOneContent(
                            onExecuteCommand, 
                            statsState = statsState,
                            columns = 3, 
                            runningPackages = runningPackages,
                            buildType = statsState.value.buildType,
                            currentResolution = currentResolution,
                            isConnected = isConnected,
                            isScrcpyRunning = isScrcpyRunning,
                            isResuming = isResuming,
                            mediaMaxVolume = mediaMaxVolume,
                            notificationMaxVolume = notificationMaxVolume,
                            systemMaxVolume = systemMaxVolume,
                            geminiMaxVolume = geminiMaxVolume,
                            mediaVolume = mediaVolume,
                            notificationVolume = notificationVolume,
                            systemVolume = systemVolume,
                            geminiVolume = geminiVolume,
                            button1Label = button1Label,
                            button1Command = button1Command,
                            button2Label = button2Label,
                            button2Command = button2Command,
                            onSaveButtonConfig = onSaveButtonConfig,
                            onResetButtonConfig = onResetButtonConfig,
                            customCommands = customCommands,
                            isEditMode = isEditMode,
                            onEditItem = { buttonId, item, defaultItem -> editingItem = Triple(buttonId, item, defaultItem) },
                            searchQuery = searchQuery
                        )
                    }
                }
            } else { // Side-by-Side (Landscape or Large screen portrait)
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AppSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Search Aura features...",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            AuraAllInOneContent(
                                onExecuteCommand, 
                                statsState = statsState,
                                columns = 2, 
                                runningPackages = runningPackages,
                                buildType = statsState.value.buildType,
                                currentResolution = currentResolution,
                                isConnected = isConnected,
                                isScrcpyRunning = isScrcpyRunning,
                                isResuming = isResuming,
                                mediaMaxVolume = mediaMaxVolume,
                                notificationMaxVolume = notificationMaxVolume,
                                systemMaxVolume = systemMaxVolume,
                                geminiMaxVolume = geminiMaxVolume,
                                mediaVolume = mediaVolume,
                                notificationVolume = notificationVolume,
                                systemVolume = systemVolume,
                                geminiVolume = geminiVolume,
                                button1Label = button1Label,
                                button1Command = button1Command,
                                button2Label = button2Label,
                                button2Command = button2Command,
                                onSaveButtonConfig = onSaveButtonConfig,
                                onResetButtonConfig = onResetButtonConfig,
                                customCommands = customCommands,
                                isEditMode = isEditMode,
                                onEditItem = { buttonId, item, defaultItem -> editingItem = Triple(buttonId, item, defaultItem) },
                                searchQuery = searchQuery
                            )
                        }
                    }
                    if (!isFloating && (isScrcpyRunning || (alwaysShowPlaceholder && isConnected))) {
                        Box(modifier = Modifier.weight(1.5f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Box {
                                if (isScrcpyRunning) {
                                    Box {
                                        scrcpyContent(Modifier.fillMaxWidth().aspectRatio(videoAspectRatio).clip(RoundedCornerShape(16.dp)).background(Color.Black))

                                        // Float Toggle Button
                                        IconButton(
                                            onClick = { isFloating = true },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Float", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(videoAspectRatio)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Screencopy Stopped", color = Color.White.copy(alpha = 0.6f))
                                        
                                        // Float Toggle Button
                                        IconButton(
                                            onClick = { isFloating = true },
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Float", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (isResuming && isScrcpyRunning) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Resuming...", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Scrcpy Window Overlay
        if (isFloating && (isScrcpyRunning || (alwaysShowPlaceholder && isConnected))) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pipOffset.x.roundToInt(), pipOffset.y.roundToInt()) }
                    .width(pipWidth)
                    .aspectRatio(videoAspectRatio)
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pipOffset += dragAmount
                        }
                    }
            ) {
                if (isScrcpyRunning) {
                    scrcpyContent(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                    
                    if (isResuming) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Stopped", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
                
                // Dock Toggle Button (Pin icon)
                IconButton(
                    onClick = { isFloating = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = "Dock", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Resize Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newWidth = pipWidth + with(density) { dragAmount.x.toDp() }
                                pipWidth = newWidth.coerceIn(150.dp, 600.dp)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AspectRatio, 
                        contentDescription = "Resize", 
                        tint = Color.White.copy(alpha = 0.7f), 
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        editingItem?.let { (buttonId, item, defaultItem) ->
            EditCommandDialog(
                buttonId = buttonId,
                item = item,
                defaultItem = defaultItem,
                onDismiss = { editingItem = null },
                onSave = { label, command, stopCommand ->
                    onSaveCustomCommand(buttonId, label, command, stopCommand)
                    editingItem = null
                },
                onReset = {
                    onResetCustomCommand(buttonId)
                    editingItem = null
                }
            )
        }
    }
}

@Composable
fun AuraAllInOneContent(
    onExecuteCommand: (String, String?, Boolean) -> Unit, 
    statsState: State<DeviceStateWrapper>,
    columns: Int, 
    runningPackages: Set<String>?, 
    buildType: String? = null, 
    currentResolution: Int = 720, 
    isConnected: Boolean,
    isScrcpyRunning: Boolean,
    isResuming: Boolean,
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 7,
    geminiMaxVolume: Int = 15,
    mediaVolume: Int = 10,
    notificationVolume: Int = 5,
    systemVolume: Int = 5,
    geminiVolume: Int = 10,
    button1Label: String = "Custom 1",
    button1Command: String = "",
    button2Label: String = "Custom 2",
    button2Command: String = "",
    onSaveButtonConfig: (Int, String, String) -> Unit = { _, _, _ -> },
    onResetButtonConfig: (Int) -> Unit = {},
    customCommands: Map<String, CustomCommandEntity> = emptyMap(),
    isEditMode: Boolean = false,
    onEditItem: (String, ControlItem, ControlItem) -> Unit = { _, _, _ -> },
    searchQuery: String = ""
) {
    val deviceControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Device Controls", AuraFeatures.DeviceControls, customCommands)
            .zip(AuraFeatures.DeviceControls)
        filterAndSortList(base, searchQuery, "Device Controls")
    }
    val serviceControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Service Controls", AuraFeatures.ServiceControls, customCommands)
            .zip(AuraFeatures.ServiceControls)
        filterAndSortList(base, searchQuery, "Service Controls")
    }
    val ledControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "LED Controls", AuraFeatures.LEDControls, customCommands)
            .zip(AuraFeatures.LEDControls)
        filterAndSortList(base, searchQuery, "LED Controls")
    }
    val onboarding = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Onboarding", AuraFeatures.Onboarding, customCommands)
            .zip(AuraFeatures.Onboarding)
        filterAndSortList(base, searchQuery, "Onboarding")
    }
    val coreApps = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Core Apps", AuraFeatures.CoreApps, customCommands)
            .zip(AuraFeatures.CoreApps)
        filterAndSortList(base, searchQuery, "Core Apps")
    }
    val chrome = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Chrome & Web", AuraFeatures.Chrome, customCommands)
            .zip(AuraFeatures.Chrome)
        filterAndSortList(base, searchQuery, "Chrome & Web")
    }
    val productivity = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Productivity", AuraFeatures.Productivity, customCommands)
            .zip(AuraFeatures.Productivity)
        filterAndSortList(base, searchQuery, "Productivity")
    }
    val media = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Media & Photos", AuraFeatures.Media, customCommands)
            .zip(AuraFeatures.Media)
        filterAndSortList(base, searchQuery, "Media & Photos")
    }
    val navigation = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "Maps & Navigation", AuraFeatures.Navigation, customCommands)
            .zip(AuraFeatures.Navigation)
        filterAndSortList(base, searchQuery, "Maps & Navigation")
    }
    val tuning = remember(customCommands, searchQuery) {
        val base = getCustomizedList("AURA", "System Tuning", AuraFeatures.Tuning, customCommands)
            .zip(AuraFeatures.Tuning)
        filterAndSortList(base, searchQuery, "System Tuning")
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        if (deviceControls.isNotEmpty()) {
            AuraControlGrid("Device Controls", deviceControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Device Controls")
        }
        
        val showCustomControls = searchQuery.isEmpty() ||
                "Custom Commands".contains(searchQuery, ignoreCase = true) ||
                button1Label.contains(searchQuery, ignoreCase = true) ||
                button2Label.contains(searchQuery, ignoreCase = true)
        if (showCustomControls) {
            CustomControlsSection(
                button1Label = button1Label,
                button1Command = button1Command,
                button2Label = button2Label,
                button2Command = button2Command,
                onExecuteCommand = onExecuteCommand,
                onSaveButtonConfig = onSaveButtonConfig,
                onResetButtonConfig = onResetButtonConfig
            )
        }
        
        // Auto Spatialization Control
        val spatializationEnabled = statsState.value.spatializationEnabled
        val isSpatialRestricted = buildType == "user"
        val showSpatialization = searchQuery.isEmpty() || "Auto Spatialization".contains(searchQuery, ignoreCase = true)
        if (showSpatialization) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if (isSpatialRestricted) Modifier.alpha(0.38f) else Modifier)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto Spatialization", color = Color.White)
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when (spatializationEnabled) {
                                        true -> Color.Green
                                        false -> Color.Red
                                        null -> Color.Gray
                                    }
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExecuteCommand("settings put system auto_spatialization_settings 1", null, true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            enabled = !isSpatialRestricted,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Enable", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onExecuteCommand("settings put system auto_spatialization_settings 0", null, true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            enabled = !isSpatialRestricted,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Disable", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        val showTuningSection = searchQuery.isEmpty() ||
                "Tuning & Volumes".contains(searchQuery, ignoreCase = true) ||
                "Fan Control".contains(searchQuery, ignoreCase = true) ||
                "Dimming Level".contains(searchQuery, ignoreCase = true) ||
                "Brightness".contains(searchQuery, ignoreCase = true) ||
                "Volume".contains(searchQuery, ignoreCase = true)
        if (showTuningSection) {
            TuningSection(
                onExecuteCommand, 
                initiallyExpanded = true, 
                buildType = buildType, 
                mediaMaxVolume = mediaMaxVolume, 
                notificationMaxVolume = notificationMaxVolume, 
                systemMaxVolume = systemMaxVolume, 
                geminiMaxVolume = geminiMaxVolume,
                initialMediaVolume = mediaVolume,
                initialNotificationVolume = notificationVolume,
                initialSystemVolume = systemVolume,
                initialGeminiVolume = geminiVolume
            )
        }
        if (tuning.isNotEmpty()) {
            AuraControlGrid("System Tuning", tuning, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "System Tuning")
        }

        // Meters Reset Height Card
        var resetHeightText by remember { mutableStateOf("1.6") }
        val isHeightRestricted = buildType == "user"
        val showHeightCard = searchQuery.isEmpty() || "Height in meters".contains(searchQuery, ignoreCase = true) || "Reset Height".contains(searchQuery, ignoreCase = true)
        if (showHeightCard) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if (isHeightRestricted) Modifier.alpha(0.38f) else Modifier)
            ) {
                Column {
                    Text("Height in meters", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = resetHeightText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    resetHeightText = input
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isHeightRestricted,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Button(
                            onClick = {
                                val heightVal = resetHeightText.toFloatOrNull()
                                if (heightVal != null) {
                                    onExecuteCommand("surfaceflinger --set-height-above-floor=$heightVal", null, true)
                                }
                            },
                            enabled = !isHeightRestricted,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.8f))
                        ) {
                            Text("Set", color = Color.Black)
                        }
                    }
                }
            }
        }

        if (serviceControls.isNotEmpty()) {
            AuraControlGrid("Service Controls", serviceControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Service Controls")
        }
        if (onboarding.isNotEmpty()) {
            AuraControlGrid("Onboarding", onboarding, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Onboarding")
        }
        if (coreApps.isNotEmpty()) {
            AuraControlGrid("Core Apps", coreApps, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Core Apps")
        }
        if (chrome.isNotEmpty()) {
            AuraControlGrid("Chrome & Web", chrome, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Chrome & Web")
        }
        if (productivity.isNotEmpty()) {
            AuraControlGrid("Productivity", productivity, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Productivity")
        }
        if (media.isNotEmpty()) {
            AuraControlGrid("Media & Photos", media, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Media & Photos")
        }
        if (navigation.isNotEmpty()) {
            AuraControlGrid("Maps & Navigation", navigation, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Maps & Navigation")
        }
        if (ledControls.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val isRestricted = buildType == "user"
                Text(
                    "LED Controls", 
                    color = Color.White.copy(alpha = if (isRestricted) 0.38f else 0.9f), 
                    style = MaterialTheme.typography.titleMedium, 
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ledControls.forEach { pair ->
                        val item = pair.first
                        val defaultItem = pair.second
                        val dotColor = when {
                            defaultItem.label.contains("Red") -> Color.Red
                            defaultItem.label.contains("Green") -> Color.Green
                            defaultItem.label.contains("Blue") -> Color.Blue
                            else -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isRestricted) dotColor.copy(alpha = 0.38f) else dotColor)
                                .border(2.dp, Color.White.copy(alpha = if (isRestricted) 0.2f else 0.7f), CircleShape)
                                .then(
                                    if (isRestricted) Modifier else Modifier.clickable {
                                        if (isEditMode) {
                                            onEditItem("AURA_LED Controls_${defaultItem.label}", item, defaultItem)
                                        } else {
                                            onExecuteCommand(item.command, null, true)
                                        }
                                    }
                                )
                        ) {
                            if (isEditMode && !isRestricted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Green, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraControlGrid(
    title: String,
    items: List<Pair<ControlItem, ControlItem>>,
    columns: Int,
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    runningPackages: Set<String>?,
    initiallyExpanded: Boolean = true,
    buildType: String? = null,
    isEditMode: Boolean = false,
    onEditItem: (String, ControlItem, ControlItem) -> Unit = { _, _, _ -> },
    category: String = ""
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 800.dp)
            ) {
            items(items) { pair ->
                val item = pair.first
                val defaultItem = pair.second
                val isRunning = item.packageName != null && runningPackages?.contains(item.packageName) == true
                val highlightColor = MaterialTheme.colorScheme.primary
                val isRestricted = item.requiresUserDebug && buildType == "user"
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isRunning) highlightColor.copy(alpha = 0.15f) else GlassCardColor)
                        .border(
                            width = 1.dp,
                            color = if (isEditMode && !isRestricted) Color.Green.copy(alpha = 0.6f) else if (isRunning) highlightColor else GlassBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .then(
                            if (isRestricted) {
                                Modifier.alpha(0.38f)
                            } else {
                                Modifier.clickable {
                                    if (isEditMode) {
                                        onEditItem("AURA_${category}_${defaultItem.label}", item, defaultItem)
                                    } else {
                                        if (isRunning && item.stopCommand != null) {
                                            onExecuteCommand(item.stopCommand, item.packageName, false)
                                        } else {
                                            onExecuteCommand(item.command, item.packageName, true)
                                        }
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isRunning) highlightColor else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.height(32.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.label,
                                color = if (isRunning) highlightColor else Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                lineHeight = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Visual Edit badge
                    if (isEditMode && !isRestricted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Visual "Running" badge
                    if (!isEditMode && isRunning && item.stopCommand != null && !isRestricted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Running",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun TuningSection(
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    initiallyExpanded: Boolean = true,
    buildType: String? = null,
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 7,
    geminiMaxVolume: Int = 15,
    initialMediaVolume: Int = 10,
    initialNotificationVolume: Int = 5,
    initialSystemVolume: Int = 5,
    initialGeminiVolume: Int = 10
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var brightness by remember { mutableFloatStateOf(128f) }
    var fanSpeed by remember { mutableFloatStateOf(50f) }
    var isFanForced by remember { mutableStateOf(false) }
    var mediaVolume by remember(initialMediaVolume) { mutableFloatStateOf(initialMediaVolume.toFloat()) }
    var geminiVolume by remember(initialGeminiVolume) { mutableFloatStateOf(initialGeminiVolume.toFloat()) }
    var notificationVolume by remember(initialNotificationVolume) { mutableFloatStateOf(initialNotificationVolume.toFloat()) }
    var systemVolume by remember(initialSystemVolume) { mutableFloatStateOf(initialSystemVolume.toFloat()) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tuning & Volumes", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // Fan Control Card
                val isFanRestricted = buildType == "user"
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .then(if (isFanRestricted) Modifier.alpha(0.38f) else Modifier)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fan Control", color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Force", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    enabled = !isFanRestricted,
                                    checked = isFanForced,
                                    onCheckedChange = { forced ->
                                        isFanForced = forced
                                        if (forced) {
                                            onExecuteCommand("echo 1 > /sys/class/fan/max31760/speed_fixed_flag", null, true)
                                            onExecuteCommand("echo ${fanSpeed.toInt()} > /sys/class/fan/max31760/speed_control", null, true)
                                        } else {
                                            onExecuteCommand("echo 0 > /sys/class/fan/max31760/speed_fixed_flag", null, true)
                                        }
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = fanSpeed,
                            onValueChange = { 
                                fanSpeed = it
                                if (isFanForced) {
                                    onExecuteCommand("echo ${it.toInt()} > /sys/class/fan/max31760/speed_control", null, true)
                                }
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White, 
                                activeTrackColor = Color.White,
                                disabledThumbColor = Color.White.copy(alpha = 0.38f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.38f)
                            ),
                            enabled = !isFanRestricted && isFanForced
                        )
                    }
                }

                // Dimming Slider
                val dimmingLevels = listOf(0.1f, 0.3f, 0.5f, 0.7f, 1.0f)
                var dimIndex by remember { mutableFloatStateOf(0f) }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Dimming Level", color = Color.White)
                    Slider(
                        value = dimIndex,
                        onValueChange = { dimIndex = it },
                        onValueChangeFinished = {
                            val opacity = dimmingLevels[dimIndex.toInt()]
                            onExecuteCommand("am broadcast -a com.android.systemui.xr.SET_PASSTHROUGH_DIMMING_ALPHA -p com.android.systemui --ef com.android.systemui.xr.PASSTHROUGH_OPACITY $opacity", null, true)
                        },
                        valueRange = 0f..4f,
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                }

                // Brightness Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness", color = Color.White)
                        TextButton(
                            onClick = {
                                brightness = 255f
                                onExecuteCommand("settings put system screen_brightness_mode 0; settings put system screen_brightness 255", null, true)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("MAX", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    var sliderValue by remember(brightness) { mutableFloatStateOf(brightness) }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { 
                            brightness = sliderValue
                            onExecuteCommand("settings put system screen_brightness ${sliderValue.toInt()}", null, true) 
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                }

                // Volume Sliders
                listOf(
                    Triple(MediaVolumeStream, mediaVolume, mediaMaxVolume),
                    Triple(NotificationVolumeStream, notificationVolume, notificationMaxVolume),
                    Triple(GeminiVolumeStream, geminiVolume, geminiMaxVolume),
                    Triple(SystemVolumeStream, systemVolume, systemMaxVolume)
                ).forEach { (stream, currentVal, maxVol) ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(stream.label, color = Color.White)
                        var sliderValue by remember(currentVal) { mutableFloatStateOf(currentVal) }
                        
                        LaunchedEffect(sliderValue) {
                            if (sliderValue != currentVal) {
                                delay(50)
                                when (stream) {
                                    is MediaVolumeStream -> mediaVolume = sliderValue
                                    is NotificationVolumeStream -> notificationVolume = sliderValue
                                    is GeminiVolumeStream -> geminiVolume = sliderValue
                                    is SystemVolumeStream -> systemVolume = sliderValue
                                }
                                onExecuteCommand("cmd media_session volume --stream ${stream.streamId} --set ${sliderValue.toInt()}", null, true)
                            }
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = Math.round(it).toFloat() },
                            valueRange = 0f..maxVol.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                        )
                    }
                }
            }
        }
    }
}

// --- CUSTOM SHORTS & CONFIGURATION ---

@Composable
fun CustomControlsSection(
    button1Label: String,
    button1Command: String,
    button2Label: String,
    button2Command: String,
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    onSaveButtonConfig: (Int, String, String) -> Unit,
    onResetButtonConfig: (Int) -> Unit
) {
    var showConfigDialogForButton by rememberSaveable { mutableStateOf<Int?>(null) }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            stringResource(R.string.custom_commands),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomGridButton(
                index = 1,
                label = button1Label,
                command = button1Command,
                modifier = Modifier.weight(1f),
                onExecuteCommand = onExecuteCommand,
                onConfigureClick = { showConfigDialogForButton = 1 }
            )
            
            CustomGridButton(
                index = 2,
                label = button2Label,
                command = button2Command,
                modifier = Modifier.weight(1f),
                onExecuteCommand = onExecuteCommand,
                onConfigureClick = { showConfigDialogForButton = 2 }
            )
        }
    }
    
    showConfigDialogForButton?.let { btnIndex ->
        val currentLabel = if (btnIndex == 1) button1Label else button2Label
        val currentCommand = if (btnIndex == 1) button1Command else button2Command
        
        CustomButtonConfigDialog(
            buttonIndex = btnIndex,
            initialLabel = currentLabel,
            initialCommand = currentCommand,
            onDismiss = { showConfigDialogForButton = null },
            onSave = { label, cmd ->
                onSaveButtonConfig(btnIndex, label, cmd)
                showConfigDialogForButton = null
            },
            onReset = {
                onResetButtonConfig(btnIndex)
                showConfigDialogForButton = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomGridButton(
    index: Int,
    label: String,
    command: String,
    modifier: Modifier = Modifier,
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    onConfigureClick: () -> Unit
) {
    val context = LocalContext.current
    val hasCommand = command.isNotEmpty()
    
    val highlightColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasCommand) GlassCardColor else GlassCardColor.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (hasCommand) highlightColor.copy(alpha = 0.5f) else GlassBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {
                    if (hasCommand) {
                        onExecuteCommand(command, null, true)
                    } else {
                        Toast.makeText(context, context.getString(R.string.long_press_to_configure, index), Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = onConfigureClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            Icon(
                imageVector = if (hasCommand) Icons.Default.Build else Icons.Default.Add,
                contentDescription = label,
                tint = if (hasCommand) highlightColor else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.height(32.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (hasCommand) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CustomButtonConfigDialog(
    buttonIndex: Int,
    initialLabel: String,
    initialCommand: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onReset: () -> Unit
) {
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    var command by rememberSaveable { mutableStateOf(initialCommand) }
    
    val highlightColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.configure_button, buttonIndex), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.button_label)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = highlightColor,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(R.string.adb_command)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = highlightColor,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = false,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label, command) },
                colors = ButtonDefaults.buttonColors(containerColor = highlightColor.copy(alpha = 0.8f))
            ) {
                Text(stringResource(R.string.save), color = onPrimaryColor)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor)
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}