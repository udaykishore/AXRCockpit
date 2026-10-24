package com.ukp.axrcockpit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ukp.axrcockpit.db.CustomCommandEntity
import kotlin.math.roundToInt

@Composable
fun RavineAppScreen(
    windowSizeClass: WindowSizeClass,
    configuration: android.content.res.Configuration,
    runningPackages: Set<String>?,
    statsState: State<DeviceStateWrapper>,
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
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val useSideBySide = isLandscape || isWideScreen

    var isFloating by remember { mutableStateOf(false) }
    var pipOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(50f, 250f)) }
    var pipWidth by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current
    var editingItem by remember { mutableStateOf<Triple<String, ControlItem, ControlItem>?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        MovingBlobsBackground()
        
        Column(modifier = Modifier.fillMaxSize()) {
            GlassmorphismStatsToolbar(
                title = "Ravine",
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
                        placeholderText = "Search Ravine features...",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        RavineAllInOneContent(
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
                            placeholderText = "Search Ravine features...",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            RavineAllInOneContent(
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
fun RavineAllInOneContent(
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
    val sessionControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Session Controls", RavineFeatures.SessionControls, customCommands)
            .zip(RavineFeatures.SessionControls)
        filterAndSortList(base, searchQuery, "Session Controls")
    }
    val ravineDemos = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Ravine Demos", RavineFeatures.RavineDemos, customCommands)
            .zip(RavineFeatures.RavineDemos)
        filterAndSortList(base, searchQuery, "Ravine Demos")
    }
    val geminiControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Gemini", RavineFeatures.GeminiControls, customCommands)
            .zip(RavineFeatures.GeminiControls)
        filterAndSortList(base, searchQuery, "Gemini")
    }
    val demoAssistance = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Demo Assistance", RavineFeatures.DemoAssistance, customCommands)
            .zip(RavineFeatures.DemoAssistance)
        filterAndSortList(base, searchQuery, "Demo Assistance")
    }
    val ecDimming = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "EC Dimming", RavineFeatures.ECDimming, customCommands)
            .zip(RavineFeatures.ECDimming)
        filterAndSortList(base, searchQuery, "EC Dimming")
    }
    val serviceControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Service Controls", RavineFeatures.ServiceControls, customCommands)
            .zip(RavineFeatures.ServiceControls)
        filterAndSortList(base, searchQuery, "Service Controls")
    }
    val onboarding = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Onboarding", RavineFeatures.Onboarding, customCommands)
            .zip(RavineFeatures.Onboarding)
        filterAndSortList(base, searchQuery, "Onboarding")
    }
    val appsAndCore = remember(customCommands, searchQuery) {
        val base = getCustomizedList("RAVINE", "Apps & Core", RavineFeatures.AppsAndCore, customCommands)
            .zip(RavineFeatures.AppsAndCore)
        filterAndSortList(base, searchQuery, "Apps & Core")
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        if (sessionControls.isNotEmpty()) {
            RavineControlGrid("Session Controls", sessionControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Session Controls")
        }
        if (ravineDemos.isNotEmpty()) {
            RavineControlGrid("Ravine Demos", ravineDemos, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Ravine Demos")
        }
        if (geminiControls.isNotEmpty()) {
            RavineControlGrid("Gemini", geminiControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Gemini")
        }
        if (demoAssistance.isNotEmpty()) {
            RavineControlGrid("Demo Assistance", demoAssistance, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Demo Assistance")
        }
        if (ecDimming.isNotEmpty()) {
            RavineControlGrid("EC Dimming", ecDimming, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "EC Dimming")
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

        if (serviceControls.isNotEmpty()) {
            RavineControlGrid("Service Controls", serviceControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Service Controls")
        }
        if (onboarding.isNotEmpty()) {
            RavineControlGrid("Onboarding", onboarding, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Onboarding")
        }
        if (appsAndCore.isNotEmpty()) {
            RavineControlGrid("Apps & Core", appsAndCore, columns, onExecuteCommand, runningPackages, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Apps & Core")
        }
    }
}

@Composable
fun RavineControlGrid(
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
                                            onEditItem("RAVINE_${category}_${defaultItem.label}", item, defaultItem)
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
                    }
                }
            }
        }
    }
}
