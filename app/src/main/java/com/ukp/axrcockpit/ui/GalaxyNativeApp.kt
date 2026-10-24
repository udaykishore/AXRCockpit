package com.ukp.axrcockpit.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import com.ukp.axrcockpit.db.CustomCommandEntity
import kotlin.math.roundToInt
import kotlinx.coroutines.delay



@Composable
fun GalaxyAppScreen(
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
    alwaysShowPlaceholder: Boolean = true,
    videoAspectRatio: Float = 16f / 9f,
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 15,
    geminiMaxVolume: Int = 15,
    mediaVolume: Int = 10,
    notificationVolume: Int = 5,
    systemVolume: Int = 5,
    geminiVolume: Int = 10,
    customCommands: Map<String, CustomCommandEntity> = emptyMap(),
    isEditMode: Boolean = false,
    onSaveCustomCommand: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onResetCustomCommand: (String) -> Unit = {}
) {
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = windowSizeClass.widthSizeClass != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact
    val useSideBySide = isLandscape || isWideScreen

    var isFloating by remember { mutableStateOf(false) }
    var pipOffset by remember { mutableStateOf<Offset>(Offset(50f, 250f)) }
    var pipWidth by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current
    var editingItem by remember { mutableStateOf<Triple<String, ControlItem, ControlItem>?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        MovingBlobsBackground()
        
        Column(modifier = Modifier.fillMaxSize()) {
            GlassmorphismStatsToolbar(
                title = "Galaxy XR",
                deviceState = statsState,
                isConnected = isConnected,
                onMenuClick = onMenuClick
            )

            if (!useSideBySide) { // Stacked layout for compact portrait
                if (!isFloating && (isScrcpyRunning || (alwaysShowPlaceholder && isConnected))) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                        Box {
                            if (isScrcpyRunning) {
                                Box {
                                    scrcpyContent(Modifier.aspectRatio(videoAspectRatio))
                                    
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
                        placeholderText = "Search Galaxy features...",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        GalaxyAllInOneContent(
                            onExecuteCommand, 
                            columns = 3, 
                            runningPackages = runningPackages,
                            buildType = statsState.value.buildType,
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
                            placeholderText = "Search Galaxy features...",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            GalaxyAllInOneContent(
                                onExecuteCommand, 
                                columns = 2, 
                                runningPackages = runningPackages,
                                buildType = statsState.value.buildType,
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
fun GalaxyAllInOneContent(
    onExecuteCommand: (String, String?, Boolean) -> Unit, 
    columns: Int, 
    runningPackages: Set<String>?, 
    buildType: String? = null, 
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 15,
    geminiMaxVolume: Int = 15,
    mediaVolume: Int = 10,
    notificationVolume: Int = 5,
    systemVolume: Int = 5,
    geminiVolume: Int = 10,
    customCommands: Map<String, CustomCommandEntity> = emptyMap(),
    isEditMode: Boolean = false,
    onEditItem: (String, ControlItem, ControlItem) -> Unit = { _, _, _ -> },
    searchQuery: String = ""
) {
    val deviceControls = remember(customCommands, searchQuery) {
        val base = getCustomizedList("GALAXY", "Device Controls", GalaxyFeatures.DeviceControls, customCommands)
            .zip(GalaxyFeatures.DeviceControls)
        filterAndSortList(base, searchQuery, "Device Controls")
    }
    val onboarding = remember(customCommands, searchQuery) {
        val base = getCustomizedList("GALAXY", "Onboarding", GalaxyFeatures.Onboarding, customCommands)
            .zip(GalaxyFeatures.Onboarding)
        filterAndSortList(base, searchQuery, "Onboarding")
    }
    val appsAndCore = remember(customCommands, searchQuery) {
        val base = getCustomizedList("GALAXY", "Apps & Core", GalaxyFeatures.AppsAndCore, customCommands)
            .zip(GalaxyFeatures.AppsAndCore)
        filterAndSortList(base, searchQuery, "Apps & Core")
    }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        if (deviceControls.isNotEmpty()) {
            GalaxyControlGrid("Device Controls", deviceControls, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Device Controls")
        }
        val showTuningSection = searchQuery.isEmpty() ||
                "Tuning & Volumes".contains(searchQuery, ignoreCase = true) ||
                "Headset Height".contains(searchQuery, ignoreCase = true) ||
                "Fan Speed".contains(searchQuery, ignoreCase = true) ||
                "Volume".contains(searchQuery, ignoreCase = true)
        if (showTuningSection) {
            GalaxyTuningSection(
                onExecuteCommand = onExecuteCommand, 
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
        if (onboarding.isNotEmpty()) {
            GalaxyControlGrid("Onboarding", onboarding, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Onboarding")
        }
        if (appsAndCore.isNotEmpty()) {
            GalaxyControlGrid("Apps & Core", appsAndCore, columns, onExecuteCommand, runningPackages, initiallyExpanded = true, buildType = buildType, isEditMode = isEditMode, onEditItem = onEditItem, category = "Apps & Core")
        }
    }
}

@Composable
fun GalaxyControlGrid(
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
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ControlItem?>(null) }
    var dialogInput by remember { mutableStateOf("") }

    if (showDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(selectedItem?.dialogTitle ?: "Enter Data", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = dialogInput,
                        onValueChange = { dialogInput = it },
                        placeholder = { Text(selectedItem?.placeholder ?: "") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val input = dialogInput.trim()
                    if (input.isNotEmpty()) {
                        val finalCommand = if (selectedItem?.label == "Set RX Data") {
                            // Specialized logic for RX Data JSON generation
                            try {
                                val parts = input.split("\\s+".toRegex())
                                val rxData = when (parts.size) {
                                    2 -> {
                                        val lSph = parts[0].toDouble()
                                        val rSph = parts[1].toDouble()
                                        mapOf(
                                            "leftEyeSph" to lSph, "leftEyeCyl" to 0.0, "leftEyeAxis" to 0,
                                            "rightEyeSph" to rSph, "rightEyeCyl" to 0.0, "rightEyeAxis" to 0
                                        )
                                    }
                                    6 -> {
                                        mapOf(
                                            "leftEyeSph" to parts[0].toDouble(), "leftEyeCyl" to parts[1].toDouble(), "leftEyeAxis" to parts[2].toInt(),
                                            "rightEyeSph" to parts[3].toDouble(), "rightEyeCyl" to parts[4].toDouble(), "rightEyeAxis" to parts[5].toInt()
                                        )
                                    }
                                    else -> null
                                }

                                if (rxData != null) {
                                    val jsonString = "{" + rxData.entries.joinToString(",") { 
                                        "\"${it.key}\":${it.value}" 
                                    } + "}"
                                    // Properly escape for shell
                                    val escapedJson = "'$jsonString'"
                                    selectedItem?.command?.replace("%s", escapedJson)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            selectedItem?.command?.replace("%s", input)
                        }

                        if (finalCommand != null) {
                            onExecuteCommand(finalCommand, selectedItem?.packageName, true)
                            showDialog = false
                            dialogInput = ""
                        }
                    }
                }) {
                    Text("Set", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

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
                                Modifier.alpha(0.38f) // Standard disabled alpha
                            } else {
                                Modifier.clickable {
                                    if (isEditMode) {
                                        onEditItem("GALAXY_${category}_${defaultItem.label}", item, defaultItem)
                                    } else {
                                        if (item.type == ControlType.DIALOG) {
                                            selectedItem = item
                                            showDialog = true
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

                    // Visual Stop/Running badge
                    if (!isEditMode && item.stopCommand != null && !isRestricted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color.Red else Color.Gray.copy(alpha = 0.6f))
                                .clickable {
                                    onExecuteCommand(item.stopCommand, item.packageName, false)
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop ${item.label}",
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
fun GalaxyTuningSection(
    onExecuteCommand: (String, String?, Boolean) -> Unit,
    initiallyExpanded: Boolean = true,
    buildType: String? = null,
    mediaMaxVolume: Int = 15,
    notificationMaxVolume: Int = 7,
    systemMaxVolume: Int = 15,
    geminiMaxVolume: Int = 15,
    initialMediaVolume: Int = 10,
    initialNotificationVolume: Int = 5,
    initialSystemVolume: Int = 5,
    initialGeminiVolume: Int = 10
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var height by remember { mutableFloatStateOf(1.6f) }
    var fanSpeed by remember { mutableFloatStateOf(50f) }
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

                // Custom Sliders
                listOf(
                    "Headset Height" to Triple(height, 1.0f..2.0f) { v: Float -> height = v; onExecuteCommand("surfaceflinger --set-height-above-floor=$v", null, true) },
                    "Fan Speed" to Triple(fanSpeed, 0f..100f) { v: Float -> fanSpeed = v; onExecuteCommand("val=${v.toInt()}; echo \$val \$val \$val \$val \$val \$val \$val > /sys/class/sec_fan/fan/tune_steps_map", null, true) }
                ).forEach { (label, data) ->
                    val isRestricted = buildType == "user"
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .then(if (isRestricted) Modifier.alpha(0.38f) else Modifier)
                    ) {
                        Text(label, color = Color.White)
                        var sliderValue by remember(data.first) { mutableFloatStateOf(data.first) }
                        Slider(
                            enabled = !isRestricted,
                            value = sliderValue, 
                            onValueChange = { sliderValue = it }, 
                            onValueChangeFinished = { data.third(sliderValue) },
                            valueRange = data.second, 
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White, 
                                activeTrackColor = Color.White,
                                disabledThumbColor = Color.White.copy(alpha = 0.38f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.38f)
                            )
                        )
                    }
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
