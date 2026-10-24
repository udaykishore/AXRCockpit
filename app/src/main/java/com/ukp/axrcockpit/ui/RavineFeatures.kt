package com.ukp.axrcockpit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

object RavineFeatures {
    val SessionControls = listOf(
        ControlItem("Start Demo Session", Icons.Default.PlayArrow, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.google.xr.ravine_hello_spatial_ar; am force-stop com.DefaultCompany.MixedRealityTemplate; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true),
        ControlItem("End Demo Session", Icons.Default.Stop, "am force-stop com.google.xr.ravine_hello_spatial_ar; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.DefaultCompany.MixedRealityTemplate; su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable false", requiresUserDebug = true),
        ControlItem("Restart Demo App", Icons.Default.Refresh, "am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity"),
        ControlItem("Disable GNav", Icons.Default.VisibilityOff, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true", requiresUserDebug = true),
        ControlItem("Enable GNav", Icons.Default.Visibility, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable false", requiresUserDebug = true),
        ControlItem("Restart Perception Service", Icons.Default.RestartAlt, "su 0 setprop ctl.restart vendor.perceptionhost", requiresUserDebug = true),
        ControlItem("Restart Headset", Icons.Default.PowerSettingsNew, "su 0 reboot", requiresUserDebug = true)
    )

    val RavineDemos = listOf(
        ControlItem("Eye Tracking Calibration", Icons.Default.RemoveRedEye, "am force-stop com.google.pixiesxr; am force-stop com.google.xr.ravine_hello_spatial_ar; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.eyetracking.calibration; am start -n com.google.xr.eyetracking.calibration/com.google.xr.eyetracking.calibration.MainActivity", "am force-stop com.google.xr.eyetracking.calibration; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", "com.google.xr.eyetracking.calibration"),
        ControlItem("Pixies", Icons.Default.AutoAwesome, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.google.xr.ravine_hello_spatial_ar; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.pixiesxr; am start -n com.google.pixiesxr/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.pixiesxr; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", "com.google.pixiesxr", requiresUserDebug = true),
        ControlItem("YouTube", Icons.Default.SmartDisplay, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity --es ravine.directScene 'Assets/Ravine/SpatialYoutube/SpatialYoutube.unity'", "am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true),
        ControlItem("Translation", Icons.Default.Translate, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity --es ravine.directScene 'Assets/Ravine/SpatialTranslationInpaint/Scenes/SpatialTranslationInpaint.unity'", "am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true),
        ControlItem("Navigation", Icons.Default.Navigation, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity --es ravine.directScene 'Assets/Ravine/SpatialNavigation/Scenes/SpatialNavigation.unity'", "am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true),
        ControlItem("Orchid", Icons.Default.AutoAwesome, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity --es ravine.directScene 'Assets/Ravine/SpatialGeminiLive_Orchid/Scenes/SpatialGeminiLive_Orchid.unity'", "am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true),
        ControlItem("Gravity Golf", Icons.Default.SportsGolf, "su 0 am broadcast -a ix.moohan.sysui.FORCE_DISABLE_GNAV --ez disable true; am force-stop com.google.pixiesxr; am force-stop com.google.xr.eyetracking.calibration; am force-stop com.google.xr.ravine_hello_spatial_ar; am force-stop com.DefaultCompany.MixedRealityTemplate; am start -n com.DefaultCompany.MixedRealityTemplate/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.DefaultCompany.MixedRealityTemplate; am force-stop com.google.xr.ravine_hello_spatial_ar; am start -n com.google.xr.ravine_hello_spatial_ar/com.unity3d.player.UnityPlayerGameActivity", requiresUserDebug = true)
    )

    val GeminiControls = listOf(
        ControlItem("Mute Gemini Mic", Icons.Default.MicOff, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.FACILITATOR_GEMINI_MUTE --es prompt _"),
        ControlItem("Unmute Gemini Mic", Icons.Default.Mic, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.FACILITATOR_GEMINI_UNMUTE --es prompt _"),
        ControlItem("Interrupt Gemini", Icons.Default.StopCircle, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.FACILITATOR_GEMINI_INTERRUPT --es prompt _"),
        ControlItem("Reset Gemini Session", Icons.Default.DeleteSweep, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.FACILITATOR_GEMINI_RESET_SESSION --es prompt _")
    )

    val DemoAssistance = listOf(
        ControlItem("Play / Pause Active Media", Icons.Default.PlayArrow, "input keyevent KEYCODE_MEDIA_PLAY_PAUSE"),
        ControlItem("Translate View to English", Icons.Default.Translate, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.TRANSLATE --es prompt \"'translate this to English'\""),
        ControlItem("Translate View to Spanish", Icons.Default.Translate, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.TRANSLATE --es prompt \"'translate this to Spanish'\""),
        ControlItem("Show Navigation Path", Icons.Default.Navigation, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.DRAW_NAVIGATION_PATH --es prompt _"),
        ControlItem("Hide Navigation Path", Icons.Default.VisibilityOff, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.CLEAR_NAVIGATION_PATH --es prompt _")
    )

    val ECDimming = listOf(
        ControlItem("Low", Icons.Default.BrightnessLow, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.EC_DIMMING --es prompt 0.1"),
        ControlItem("Medium", Icons.Default.BrightnessMedium, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.EC_DIMMING --es prompt 0.5"),
        ControlItem("High", Icons.Default.BrightnessHigh, "am broadcast -p com.google.xr.ravine_hello_spatial_ar -a com.google.xr.ravine.EC_DIMMING --es prompt 0.9")
    )

    val DeviceControls = SessionControls
    val ServiceControls = listOf(
        ControlItem("Start RGB Stream", Icons.Default.PlayArrow, "setprop ctl.start vendor.xrpassthrough", requiresUserDebug = true),
        ControlItem("Stop RGB Stream", Icons.Default.Stop, "setprop ctl.stop vendor.xrpassthrough", requiresUserDebug = true),
        ControlItem("Restart Percep Stack", Icons.Default.Refresh, "setprop ctl.restart auracamerahal; sleep 1; setprop ctl.restart vendor.perceptionhost; setprop ctl.restart vendor.xrpassthrough", requiresUserDebug = true)
    )

    val Onboarding = listOf(
        ControlItem("Input Tutorial", Icons.Default.TouchApp, "am start -n com.google.xr.inputonboardingdemo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.inputonboardingdemo", "com.google.xr.inputonboardingdemo"),
        ControlItem("Aura Fit", Icons.Default.FitnessCenter, "am start -n com.google.xr.visualfit/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.visualfit", "com.google.xr.visualfit")
    )

    val AppsAndCore = listOf(
        ControlItem("Gemini", Icons.Default.AutoAwesome, "am start -n com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.surfaces.voice.robin.xr.main.XrModeActivity -f 0x10000000", "am force-stop com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox:gemini"),
        ControlItem("Maps XR", Icons.Default.Map, "am start -n com.google.android.apps.immersiveviewxr/.ImmersiveViewXrActivity", "am force-stop com.google.android.apps.immersiveviewxr", "com.google.android.apps.immersiveviewxr"),
        ControlItem("WiredMirror", Icons.Default.Laptop, "am start -n com.google.android.apps.vr.wiredmirror/.mirroring.MirroringActivity", "am force-stop com.google.android.apps.vr.wiredmirror", "com.google.android.apps.vr.wiredmirror"),
        ControlItem("YouTube", Icons.Default.PlayArrow, "am start -a android.intent.action.VIEW -d https://www.youtube.com/playlist?list=LL -p com.google.android.apps.youtube.xr", "am force-stop com.google.android.apps.youtube.xr", "com.google.android.apps.youtube.xr"),
        ControlItem("Chrome", Icons.Default.Launch, "am start -n com.android.chrome/com.google.android.apps.chrome.Main", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("Gmail", Icons.Default.Email, "am start -n com.google.android.gm/com.google.android.gm.ConversationListActivityGmail", "am force-stop com.google.android.gm", "com.google.android.gm"),
        ControlItem("Docs", Icons.Default.Description, "am start -n com.google.android.apps.docs.editors.docs/com.google.android.apps.docs.app.NewMainProxyActivity -f 0x10000000", "pm disable com.google.android.apps.docs.editors.docs; pm enable com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.docs"),
        ControlItem("Slides", Icons.Default.Slideshow, "am start -n com.google.android.apps.docs.editors.slides/com.google.android.apps.docs.app.NewMainProxyActivity -f 0x10000000", "pm disable com.google.android.apps.docs.editors.slides; pm enable com.google.android.apps.docs.editors.slides", "com.google.android.apps.docs.editors.slides"),
        ControlItem("Photos", Icons.Default.Photo, "am start -n com.google.android.apps.photos/.home.HomeActivity", "am force-stop com.google.android.apps.photos", "com.google.android.apps.photos"),
        ControlItem("Photos XR", Icons.Default.Photo, "am start -n com.google.android.apps.photosxr/.home.HomeActivity", "am force-stop com.google.android.apps.photosxr", "com.google.android.apps.photosxr"),
        ControlItem("Maps 2D", Icons.Default.Map, "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity", "am force-stop com.google.android.apps.maps", "com.google.android.apps.maps")
    )
}
