package com.ukp.axrcockpit.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ukp.axrcockpit.db.CustomCommandEntity

fun getCustomizedList(
    deviceType: String,
    category: String,
    defaultItems: List<ControlItem>,
    customMap: Map<String, CustomCommandEntity>
): List<ControlItem> {
    return defaultItems.map { item ->
        val id = "${deviceType}_${category}_${item.label}"
        val custom = customMap[id]
        if (custom != null) {
            val legacyCommands = setOf(
                "pm list packages | cut -f 2 -d : | grep -v 'com.ukp.axrcockpit' | xargs -P 6 -n 1 am force-stop",
                "reconfigure_press",
                "reconfigure_stage",
                "force_stop_all_apps"
            )
            val effectiveCommand = if (custom.command in legacyCommands) item.command else custom.command
            item.copy(
                label = custom.label,
                command = effectiveCommand,
                stopCommand = custom.stopCommand
            )
        } else {
            item
        }
    }
}

@Composable
fun AdminAppScreen(
    windowSizeClass: WindowSizeClass,
    configuration: android.content.res.Configuration,
    onMenuClick: () -> Unit,
    isEditMode: Boolean,
    onEditModeChanged: (Boolean) -> Unit,
    onResetAllCommands: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MovingBlobsBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            CockpitHeader(
                title = "Admin Portal",
                onMenuClick = onMenuClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Button Edit Mode",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Allow customization of ADB commands directly on all features screens.",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isEditMode,
                            onCheckedChange = onEditModeChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Cyan,
                                checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Reset Custom Commands",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Revert all customized commands back to original application defaults.",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onResetAllCommands,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset All Customize Buttons", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditCommandDialog(
    buttonId: String,
    item: ControlItem,
    defaultItem: ControlItem,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onReset: () -> Unit
) {
    var labelText by remember { mutableStateOf(item.label) }
    var commandText by remember { mutableStateOf(item.command) }
    var stopCommandText by remember { mutableStateOf(item.stopCommand ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Command Button", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Original Button Details:",
                            color = Color.Cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Label: ${defaultItem.label}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            "Command: ${defaultItem.command}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        if (defaultItem.stopCommand != null) {
                            Text(
                                "Stop Command: ${defaultItem.stopCommand}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Button Label") },
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("ADB Command") },
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stopCommandText,
                    onValueChange = { stopCommandText = it },
                    label = { Text("Stop Command (Optional)") },
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Cyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text("Reset", color = Color.Red)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = { onSave(labelText, commandText, stopCommandText.ifBlank { null }) }
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = null,
        containerColor = Color(0xFF1E1E24)
    )
}
