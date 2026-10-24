package com.ukp.axrcockpit.ui

import com.ukp.axrcockpit.ConnectionStatus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer

// 📊 Live stats payload architecture
data class DeviceStateWrapper(
    val isAttached: Boolean = false,
    val isLoading: Boolean = false, // Indicates transition from offline to online
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val thermalTempCelsius: Float? = null,
    val wifiSpeedMbps: Int? = null,
    val wifiBandGhz: String? = null,
    val powerDrawWatts: Float? = null,
    val wifiChannel: Int? = null,
    val wifiBandwidthMhz: Int? = null,
    val buildType: String? = null,
    val spatializationEnabled: Boolean? = null
)

@Composable
fun DottedProgressBar(
    modifier: Modifier = Modifier,
    circleColor: Color = Color.Gray,
    circleSize: androidx.compose.ui.unit.Dp = 4.dp,
    spaceBetween: androidx.compose.ui.unit.Dp = 2.dp,
    travelDistance: androidx.compose.ui.unit.Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    val circle1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.0f at 0 with androidx.compose.animation.core.LinearOutSlowInEasing
                1.0f at 300 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 600 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 1200 with androidx.compose.animation.core.LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "circle1"
    )
    
    val circle2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.0f at 200 with androidx.compose.animation.core.LinearOutSlowInEasing
                1.0f at 500 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 800 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 1200 with androidx.compose.animation.core.LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "circle2"
    )
    
    val circle3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.0f at 400 with androidx.compose.animation.core.LinearOutSlowInEasing
                1.0f at 700 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 1000 with androidx.compose.animation.core.LinearOutSlowInEasing
                0.0f at 1200 with androidx.compose.animation.core.LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "circle3"
    )
    
    val travelDistancePx = with(androidx.compose.ui.platform.LocalDensity.current) { travelDistance.toPx() }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    translationY = -circle1Offset * travelDistancePx
                }
                .clip(CircleShape)
                .background(circleColor)
        )
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    translationY = -circle2Offset * travelDistancePx
                }
                .clip(CircleShape)
                .background(circleColor)
        )
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    translationY = -circle3Offset * travelDistancePx
                }
                .clip(CircleShape)
                .background(circleColor)
        )
    }
}

@Composable
fun GlassmorphismStatsToolbar(
    title: String,
    deviceState: State<DeviceStateWrapper>,
    isConnected: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatsDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val stats = deviceState.value

    val hasValues = stats.wifiSpeedMbps != null || stats.powerDrawWatts != null || stats.thermalTempCelsius != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x4D1E1E2E)) // Transparent glass
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 🍔 Left: Drawer nav icon
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Main menu navigation",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 🏷️ Center Group: Centered Title Matched with badging stats
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 🔋🛰️ Badges wrapper
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { if (isConnected && hasValues) showStatsDialog = true }
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isConnected) {
                    // State 1: Disconnected
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLandscape) "Device Offline" else "Offline",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                } else if (!hasValues || stats.isLoading) {
                    // State 2: Connected but no values or loading
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        DottedProgressBar(
                            circleColor = Color.Gray,
                            circleSize = 4.dp,
                            spaceBetween = 2.dp,
                            travelDistance = 4.dp
                        )
                    }
                } else {
                // Connection indicator (Green Dot)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )

                // Battery
                val batteryColor = when {
                    stats.batteryPercent < 20 -> Color.Red
                    stats.batteryPercent < 50 -> Color.Yellow
                    else -> Color(0xFF4CAF50)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (stats.isCharging) Icons.Default.Bolt else Icons.Default.BatteryFull,
                        contentDescription = if (stats.isCharging) "Charging" else "Battery",
                        tint = batteryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        if (isLandscape) "BAT: ${stats.batteryPercent}%" else "${stats.batteryPercent}%",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Thermals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Zone temperature",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        stats.thermalTempCelsius?.let { if (isLandscape) "TMP: ${it.toInt()}°C" else "${it.toInt()}°C" } ?: if (isLandscape) "TMP: N/A" else "N/A",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Wi-Fi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wifi quality",
                        tint = Color(0xFF03A9F4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        stats.wifiSpeedMbps?.let { if (isLandscape) "WIFI: ${it}M" else "${it}M" } ?: if (isLandscape) "WIFI: Off" else "Off",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Power consumption
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Current Draw",
                        tint = Color(0xFFE1BEE7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        stats.powerDrawWatts?.let { 
                            val formatted = "%.2f".format(it)
                            if (isLandscape) "PWR: ${formatted}W" else "${formatted}W" 
                        } ?: if (isLandscape) "PWR: ---" else "---",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }


    }
    // 📋 Advanced Statistics Dialog pop up
    if (showStatsDialog) {
        Dialog(onDismissRequest = { showStatsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E24),
                modifier = Modifier.padding(24.dp).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Device Metrics Diagnostic", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    StatRowItem("Wireless Frequency Band", stats.wifiBandGhz ?: "Offline", Icons.Default.Wifi)
                    StatRowItem("Parsed Channel index", stats.wifiChannel?.toString() ?: "N/A", Icons.Default.Settings)
                    StatRowItem("Link Bandwidth width", stats.wifiBandwidthMhz?.let { "${it} Mhz" } ?: "N/A", Icons.Default.Settings)

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showStatsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        Dialog(onDismissRequest = { showHelpDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E24),
                modifier = Modifier.padding(24.dp).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Help & About", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text("Tutorial: App Buttons", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    
                    val tutorials = listOf(
                        "Tidy Up" to "Arranges app panels in the grid.",
                        "Recenter" to "Centers the display to your current view.",
                        "Passthrough" to "Toggles see-through mode on/off.",
                        "Launcher" to "Opens the main application launcher.",
                        "Back" to "Simulates the back button press."
                    )
                    
                    tutorials.forEach { (label, desc) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                            Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showHelpDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatRowItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f) // Push value to the right safely
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
