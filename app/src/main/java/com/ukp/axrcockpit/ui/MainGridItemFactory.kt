package com.ukp.axrcockpit.ui

import com.ukp.axrcockpit.R

object MainGridItemFactory {
    fun generateMainGridItems(): Map<MainGridItem.Type, MainGridItem>{
        return mutableMapOf(
            MainGridItem.Type.IPD to
            MainGridRemoteLaunchItem(
                icon = R.drawable.ic_unit_convert_ruler,
                runningLabelName = "IPD",

                packageName = "com.google.android.fit",

                launchCommand = "am start -a com.google.android.fit.launch.adjustment_ipd -n com.google.android.fit/.views.ipd.IpdAdjustmentActivity",
                stopCommand  = "am force-stop com.google.android.fit & settings put system user_device_fitting_in_progress 0"
            ),

            MainGridItem.Type.GEMINI to
            MainGridRemoteLaunchItem(
                icon = R.drawable.ic_gemini_vec,
                canTiltedIcon = false,

                runningLabelName = "Gemini ON",
                stoppedLabelName = "Gemini OFF",

                packageName = "com.google.android.googlequicksearchbox",

                launchCommand = "am start -n com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.surfaces.voice.robin.xr.main.XrModeActivity",
                stopCommand = "am force-stop com.google.android.googlequicksearchbox"
            ),

            MainGridItem.Type.MIC to
            MainGridRemoteToggleItem(
                enabledIcon = R.drawable.ic_voice_rec_microphone,
                disabledIcon = R.drawable.ic_voice_off_mute_microphone_slash,

                enabledLabelName = "MIC ON",
                disabledLabelName = "MIC OFF",

                enabledCommand = "input keyevent KEYCODE_MUTE",
                disabledCommand = "input keyevent KEYCODE_MUTE"
            ),

            MainGridItem.Type.APPS to
            MainGridActionItem(
                icon = R.drawable.ic_android_xr_vec,

                labelName = "Apps"
            ),

            MainGridItem.Type.LAUNCHER to
                    MainGridRemoteActionItem(
                icon = R.drawable.ic_rocket_vec,

                labelName = "Launcher",
                command = "input keyevent KEYCODE_POWER"
            ),

            MainGridItem.Type.VOLUME to
            MainGridVolumeItem(
                volumeType = 3,
                volumeMuteCommand = "input keyevent 164"
            ),

            MainGridItem.Type.FLOOR_LEVEL to
            MainGridRemoteActionItem(
                icon = R.drawable.ic_sys_quick_floor_level,

                labelName = "Floor level",
                command = "surfaceflinger --set-height-above-floor=1.6 && surfaceflinger --recenter"
            ),

            MainGridItem.Type.PASS_THROUGH to
            MainGridRemoteToggleItem(
                enabledIcon = R.drawable.ic_sys_quick_virtual_environment,

                enabledLabelName = "Environment ON",
                disabledLabelName = "Environment OFF",

                enabledCommand = "am broadcast -a ix.moohan.sysui.TOGGLE_PASSTHROUGH --es enable false && setprop ix.moohan.passthrough false",
                disabledCommand = "am broadcast -a ix.moohan.sysui.TOGGLE_PASSTHROUGH --es enable true && setprop ix.moohan.passthrough true"
            ),
            MainGridItem.Type.RECENTER to
            MainGridRemoteActionItem(
                icon = R.drawable.ic_recenter,
                canTiltedIcon = false,

                labelName = "Recenter",
                command = "surfaceflinger --recenter"
            ),
            MainGridItem.Type.AUTO_SPATIAL to
            MainGridRemoteToggleItem(
                enabledIcon = R.drawable.ic_3d_interactive_shot_box_counterclockwise,

                enabledLabelName = "Auto spatial ON",
                disabledLabelName = "Auto spatial OFF",

                enabledCommand = "settings put system auto_spatialization_settings 1",
                disabledCommand = "settings put system auto_spatialization_settings 0"
            ),
            MainGridItem.Type.DOMINANT_HAND to
                MainGridRemoteToggleItem(
                    enabledIcon = R.drawable.ic_gestures_hand,

                    enabledLabelName = "Dominant left hand",
                    disabledLabelName = "Dominant right hand",

                    enabledCommand = "settings put system primary_hand 0",
                    disabledCommand = "settings put system primary_hand 1"
                ),
            MainGridItem.Type.AURA to
                MainGridActionItem(
                    icon = R.drawable.ic_android_xr_vec,
                    labelName = "Aura"
                ),
        )
    }
}
