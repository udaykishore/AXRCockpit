package com.ukp.axrcockpit.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class ControlItem(
    val label: String,
    val icon: ImageVector,
    val command: String,
    val stopCommand: String? = null,
    val packageName: String? = null,
    val type: ControlType = ControlType.BUTTON,
    val requiresUserDebug: Boolean = false,
    val dialogTitle: String? = null,
    val placeholder: String? = null
)

enum class ControlType {
    BUTTON, SLIDER, TOGGLE, DIALOG
}
