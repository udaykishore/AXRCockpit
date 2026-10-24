package com.ukp.axrcockpit.ui

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.MutableStateFlow

abstract class MainGridItem{
    enum class Type{
        EMPTY,
        IPD,
        GEMINI,
        MIC,
        APPS,
        LAUNCHER,
        VOLUME,
        PASS_THROUGH,
        FLOOR_LEVEL,
        RECENTER,
        RX_INSERT,
        AUTO_SPATIAL,
        DOMINANT_HAND,
        AURA,
    }


}

abstract class MainGridItemWithIconText(
    val canTiltedIcon: Boolean = true,
): MainGridItem(){
    abstract fun getCurrentIcon(): Int
    abstract fun getCurrentLabelName(): String
}

class MainGridRemoteLaunchItem(
    @DrawableRes val icon: Int,
    canTiltedIcon: Boolean = true,

    val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false),

    val runningLabelName: String,
    val stoppedLabelName: String? = null,

    val packageName: String,

    val launchCommand: String,
    val stopCommand: String
): MainGridItemWithIconText(canTiltedIcon){
    override fun getCurrentIcon(): Int{
        return icon
    }

    override fun getCurrentLabelName(): String {
        return if(isRunning.value){
            runningLabelName
        }else{
            stoppedLabelName?: runningLabelName
        }
    }
}

class MainGridRemoteToggleItem(
    @DrawableRes val enabledIcon: Int,
    @DrawableRes val disabledIcon: Int? = null,

    canTiltedIcon: Boolean = true,

    val isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false),

    val enabledLabelName: String,
    val disabledLabelName: String,

    val enabledCommand: String,
    val disabledCommand: String
): MainGridItemWithIconText(canTiltedIcon){
    override fun getCurrentIcon(): Int {
        return if(isEnabled.value){
            enabledIcon
        }else{
            disabledIcon?:enabledIcon
        }
    }

    override fun getCurrentLabelName(): String {
        return if(isEnabled.value){
            enabledLabelName
        }else{
            disabledLabelName
        }
    }
}

class MainGridActionItem(
    val icon: Int,
    canTiltedIcon: Boolean = true,

    val labelName: String
): MainGridItemWithIconText(canTiltedIcon){
    override fun getCurrentIcon(): Int{
        return icon
    }
    override fun getCurrentLabelName(): String {
        return labelName
    }
}

class MainGridRemoteActionItem(
    val icon: Int,
    canTiltedIcon: Boolean = true,

    val labelName: String,
    val command: String
): MainGridItemWithIconText(canTiltedIcon){
    override fun getCurrentIcon(): Int{
        return icon
    }

    override fun getCurrentLabelName(): String {
        return labelName
    }
}


class MainGridVolumeItem(
    val volumeLevel: MutableStateFlow<Int> = MutableStateFlow(0),

    val volumeMuteCommand: String,
    val volumeType: Int
): MainGridItem(){
    fun setVolumeCommand(volumeLevel: Int) = "cmd media_session volume --stream $volumeType --set $volumeLevel"
}




