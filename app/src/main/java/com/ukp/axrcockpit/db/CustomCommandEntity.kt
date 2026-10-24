package com.ukp.axrcockpit.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey val id: String, // Unique ID: e.g. "AURA_DeviceControls_Recenter"
    val label: String,
    val command: String,
    val stopCommand: String? = null
)
