package com.ukp.axrcockpit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object GalaxyFeatures {
    private const val FORCE_STOP_APPS_CMD =
        "pm list packages | cut -f 2 -d : | grep -v -E 'com.ukp.axrcockpit|com.google.android.tts' | xargs -P 6 -n 1 am force-stop; sleep 1; settings put secure voice_interaction_service com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"

    val DeviceControls = listOf(
        ControlItem(
            "Reconfig 1:1",
            Icons.Default.Accessibility,
            "rm -f /data/vendor/openpx/eye_tracking/eye_tracking_calibration.bin; am startservice -a com.samsung.android.fit.demo.RESET_RX_INSERT_DATA -n com.samsung.android.fit/.service.FitService; settings put system screen_brightness 255; settings put system doff_screen_timeout_ms 3000; opx_cli \"exit-on-error; create 12; cal-unregister-rx-insert left testrxl; exit\"; opx_cli \"exit-on-error; create 12; cal-unregister-rx-insert right testrxr; exit\"; pm grant com.android.chrome android.permission.POST_NOTIFICATIONS; echo '85 90 95 100 110 120 140' > /sys/class/sec_fan/fan/tune_steps_map; cmd media_session volume --stream 1 --set 5; cmd media_session volume --stream 3 --set 5; cmd media_session volume --stream 5 --set 5; cmd media_session volume --stream 11 --set 10; am broadcast -a ix.moohan.sysui.TOGGLE_PASSTHROUGH --es enable true; $FORCE_STOP_APPS_CMD",
            requiresUserDebug = true
        ),
        ControlItem(
            "Reconfig Stage",
            Icons.Default.SettingsAccessibility,
            "settings put system screen_brightness 255; settings put system doff_screen_timeout_ms 3000; surfaceflinger --refresh-rate=60; setprop cpm.default_refresh_rate 60; surfaceflinger --disable-mirror-frs; echo '200 200 200 200 200 200 200' > /sys/class/sec_fan/fan/tune_steps_map; $FORCE_STOP_APPS_CMD",
            requiresUserDebug = true
        ),
        ControlItem(
            "Force Stop Apps",
            Icons.Default.DeleteSweep,
            FORCE_STOP_APPS_CMD
        ),
        ControlItem("Tidy Up", Icons.Default.GridView, "am broadcast -a ix.moohan.sysui.KEYBOARD_SHORTCUT --es SHORTCUT TIDY_UP_PANELS"),
        ControlItem("Recenter", Icons.Default.FilterCenterFocus, "surfaceflinger --recenter", requiresUserDebug = true),
        ControlItem("Passthrough", Icons.Default.Visibility, "am broadcast -a 'ix.moohan.sysui.TOGGLE_PASSTHROUGH' --es enable 'true'", "am broadcast -a 'ix.moohan.sysui.TOGGLE_PASSTHROUGH' --es enable 'false'", "ix.moohan.passthrough"),
        ControlItem("Launcher", Icons.Default.Apps, "input keyevent KEYCODE_POWER"),
        ControlItem("Sleep", Icons.Default.Bedtime, "input keyevent KEYCODE_SLEEP"),
        ControlItem("Wake Up", Icons.Default.WbSunny, "input keyevent KEYCODE_WAKEUP"),
        ControlItem("Toggle Mic", Icons.Default.Mic, "input keyevent KEYCODE_MUTE"),
        ControlItem("Toggle Speaker", Icons.Default.VolumeUp, "input keyevent 164"),
        ControlItem("Hand: Left", Icons.Default.PanTool, "settings put system primary_hand 0", requiresUserDebug = true),
        ControlItem("Hand: Right", Icons.Default.PanTool, "settings put system primary_hand 1", requiresUserDebug = true),
        // Major Additions
        ControlItem("Input: Raycast", Icons.Default.TouchApp, "settings put system natural_input_mode 1", requiresUserDebug = true),
        ControlItem("Input: Multimodal", Icons.Default.AutoAwesome, "settings put system natural_input_mode 6", requiresUserDebug = true),
        ControlItem("GNav", Icons.Default.Navigation, "am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable false", "am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true", "ix.moohan.gnav", requiresUserDebug = true),
        ControlItem("Set RX Data", Icons.Default.Visibility, "am startservice -a com.samsung.android.fit.demo.SAVE_RX_INSERT_DATA -n com.samsung.android.fit/.service.FitService --es lenses_prescription %s", type = ControlType.DIALOG, dialogTitle = "Enter RX Data: L_SPH L_CYL L_AXIS R_SPH R_CYL R_AXIS", placeholder = "e.g., -1.5 -0.5 90 -1.0 -0.75 85"),
        ControlItem("Reset RX Data", Icons.Default.RestartAlt, "am startservice -a com.samsung.android.fit.demo.RESET_RX_INSERT_DATA -n com.samsung.android.fit/.service.FitService")
    )

    val Onboarding = listOf(
        ControlItem("IPD", Icons.Default.Visibility, "am start -a com.samsung.android.fit.launch.adjustment_ipd -n com.samsung.android.fit/.views.ipd.IpdAdjustmentActivity", "am force-stop com.samsung.android.fit", "com.samsung.android.fit"),
        ControlItem("Eye Calib.", Icons.Default.RemoveRedEye, "am start -n com.google.xr.eyetracking.calibration/.EyeTrackingDemoActivity", "am force-stop com.google.xr.eyetracking.calibration", "com.google.xr.eyetracking.calibration"),
        ControlItem("Input Onb", Icons.Default.School, "am start -n com.samsung.xr.mtutorial/com.unity3d.player.CustomGameActivity", "am force-stop com.samsung.xr.mtutorial", "com.samsung.xr.mtutorial"),
        ControlItem("Retail Onb", Icons.Default.Storefront, "am start -n com.samsung.xr.mtutorial.hand.unity/com.unity3d.player.CustomGameActivity", "am force-stop com.samsung.xr.mtutorial.hand.unity", "com.samsung.xr.mtutorial.hand.unity")
    )

    val AppsAndCore = listOf(
        ControlItem("Gemini Molecule", Icons.Default.AutoAwesome, "am start -n com.google.xr.gemini_molecule/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.gemini_molecule", "com.google.xr.gemini_molecule"),
        ControlItem("Gemini Melody", Icons.Default.MusicNote, "am start -n com.google.xr.music_string/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.music_string", "com.google.xr.music_string"),
        ControlItem("Asteroid", Icons.Default.Games, "am start -n com.thirtyninjas.asteroiddemo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.thirtyninjas.asteroiddemo", "com.thirtyninjas.asteroiddemo"),
        ControlItem("Canva", Icons.Default.Brush, "am start -n com.canva.editor/com.canva.app.editor.splash.SplashActivity", "am force-stop com.canva.editor", "com.canva.editor"),
        ControlItem("Chrome", Icons.Default.Launch, "am start -n com.android.chrome/com.google.android.apps.chrome.Main", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("Scene Viewer", Icons.Default.ViewInAr, "am start -a android.intent.action.VIEW -d 'https://arvr.google.com/scene-viewer/1.2?file=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/model.glb&title=Golden+eagle&referrer=google.com:ANIMALS:kp&sound=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/Eagle_Golden_Unisex_Adult.mp3' -p com.google.android.googlequicksearchbox --es browser_fallback_url 'https://arvr.google.com/scene-viewer?file=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/model.glb&title=Golden+eagle&referrer=google.com:ANIMALS:kp&sound=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/Eagle_Golden_Unisex_Adult.mp3'", "am force-stop com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox:sceneviewer"),
        ControlItem("Cosmos", Icons.Default.AutoMode, "am start -n com.atlanticstudios.blackholes/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.atlanticstudios.blackholes", "com.atlanticstudios.blackholes"),
        ControlItem("Demeo", Icons.Default.Games, "am start -n com.resolutiongames.demeo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.resolutiongames.demeo", "com.resolutiongames.demeo"),
        ControlItem("Djay", Icons.Default.MusicNote, "am start -n com.algoriddim.djay_free/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.algoriddim.djay_free", "com.algoriddim.djay_free"),
        ControlItem("Docs", Icons.Default.Description, "am start -n com.google.android.apps.docs.editors.docs/com.google.android.apps.docs.app.NewMainProxyActivity", "am force-stop com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.docs"),
        ControlItem("GemCanvas", Icons.Default.Brush, "am start -a android.intent.action.VIEW -d 'https://gemini.google.com/app/e504f59fcc9b5650'", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("Gemini", Icons.Default.AutoAwesome, "am start -n com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.surfaces.voice.robin.xr.main.XrModeActivity", "am force-stop com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox:gemini"),
        ControlItem("Gmail", Icons.Default.Email, "am start -n com.google.android.gm/com.google.android.gm.ConversationListActivityGmail", "am force-stop com.google.android.gm", "com.google.android.gm"),
        ControlItem("Maps 2D", Icons.Default.Map, "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity", "am force-stop com.google.android.apps.maps", "com.google.android.apps.maps"),
        ControlItem("Maps XR", Icons.Default.Map, "am start -n com.google.android.apps.immersiveviewxr/.ImmersiveViewXrActivity", "am force-stop com.google.android.apps.immersiveviewxr", "com.google.android.apps.immersiveviewxr"),
        ControlItem("Meet", Icons.Default.VideoCall, "am start -n com.google.android.apps.tachyon/com.google.android.apps.tachyon.MainActivity", "am force-stop com.google.android.apps.tachyon", "com.google.android.apps.tachyon"),
        ControlItem("Moonrider", Icons.Default.SportsEsports, "am start -a android.intent.action.VIEW -d 'https://moonrider.xyz'", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("PC Connect", Icons.Default.CastConnected, "am start -n com.google.android.apps.vr.charon/.home.HomeActivity", "am force-stop com.google.android.apps.vr.charon", "com.google.android.apps.vr.charon"),
        ControlItem("PC Link", Icons.Default.DesktopWindows, "am start -n com.samsung.android.mirroringplayer/com.samsung.android.mirroringplayer.player.mcf.LocalPcActivity", "am force-stop com.samsung.android.mirroringplayer", "com.samsung.android.mirroringplayer"),
        ControlItem("Photos 2D", Icons.Default.Photo, "am start -n com.google.android.apps.photos/.home.HomeActivity", "am force-stop com.google.android.apps.photos", "com.google.android.apps.photos"),
        ControlItem("Photos XR", Icons.Default.Photo, "am start -n com.google.android.apps.photosxr/.home.HomeActivity", "am force-stop com.google.android.apps.photosxr", "com.google.android.apps.photosxr"),
        ControlItem("Pinterest", Icons.Default.Pin, "am start -n com.pinterest/com.pinterest.activity.PinterestActivity", "am force-stop com.pinterest", "com.pinterest"),
        ControlItem("Pulsar", Icons.Default.Palette, "am start -n com.adobe.pulsar/com.adobe.silvy.MainActivity", "am force-stop com.adobe.pulsar", "com.adobe.pulsar"),
        ControlItem("Slides", Icons.Default.Slideshow, "am start -n com.google.android.apps.docs.editors.slides/com.google.android.apps.docs.app.NewMainProxyActivity", "am force-stop com.google.android.apps.docs.editors.slides", "com.google.android.apps.docs.editors.slides"),
        ControlItem("Stardew", Icons.Default.Agriculture, "am start -n com.chucklefish.stardewvalley/com.chucklefish.stardewvalley.MainActivity", "am force-stop com.chucklefish.stardewvalley", "com.chucklefish.stardewvalley"),
        ControlItem("YouTube", Icons.Default.PlayArrow, "am start -a android.intent.action.VIEW -d https://www.youtube.com/playlist?list=LL", "am force-stop com.google.android.apps.youtube.xr", "com.google.android.apps.youtube.xr")
    )
}
