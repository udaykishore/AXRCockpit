package com.ukp.axrcockpit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object AuraFeatures {
    val DeviceControls = listOf(
        ControlItem("Tidy Up", Icons.Default.GridView, "am broadcast -a ix.moohan.sysui.KEYBOARD_SHORTCUT --es SHORTCUT TIDY_UP_PANELS"),
        ControlItem("Recenter", Icons.Default.FilterCenterFocus, "surfaceflinger --recenter", requiresUserDebug = true),
        ControlItem("Launcher", Icons.Default.Apps, "am broadcast -a ix.moohan.sysui.KEYBOARD_SHORTCUT -e SHORTCUT LAUNCHER"),
        ControlItem("Back", Icons.Default.ArrowBack, "input keyevent KEYCODE_BACK"),
        ControlItem("Stop All Apps", Icons.Default.DeleteSweep, "pm list packages | cut -f 2 -d : | grep -v -E 'com.ukp.axrcockpit|com.google.android.tts' | xargs -P 6 -n 1 am force-stop; sleep 1; settings put secure voice_interaction_service com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"),
        ControlItem("Reboot", Icons.Default.PowerSettingsNew, "reboot", requiresUserDebug = true)

    )

    val ServiceControls = listOf(
        ControlItem("Start RGB Stream", Icons.Default.PlayArrow, "setprop ctl.start vendor.xrpassthrough", requiresUserDebug = true),
        ControlItem("Stop RGB Stream", Icons.Default.Stop, "setprop ctl.stop vendor.xrpassthrough", requiresUserDebug = true),
        ControlItem("Restart Percep Stack", Icons.Default.Refresh, "setprop ctl.restart auracamerahal; sleep 1; setprop ctl.restart vendor.perceptionhost; setprop ctl.restart vendor.xrpassthrough", requiresUserDebug = true)
    )

    val LEDControls = listOf(
        ControlItem("LED Off", Icons.Default.Lightbulb, "echo 0 > /sys/class/leds/aw210xx_led/red/brightness; echo 0 > /sys/class/leds/aw210xx_led/green/brightness; echo 0 > /sys/class/leds/aw210xx_led/blue/brightness", requiresUserDebug = true),
        ControlItem("LED Red", Icons.Default.Lightbulb, "echo 100 > /sys/class/leds/aw210xx_led/red/brightness", requiresUserDebug = true),
        ControlItem("LED Green", Icons.Default.Lightbulb, "echo 100 > /sys/class/leds/aw210xx_led/green/brightness", requiresUserDebug = true),
        ControlItem("LED Blue", Icons.Default.Lightbulb, "echo 100 > /sys/class/leds/aw210xx_led/blue/brightness", requiresUserDebug = true)
    )

    val Onboarding = listOf(
        ControlItem("Input Tutorial", Icons.Default.TouchApp, "am start -n com.google.xr.inputonboardingdemo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.inputonboardingdemo", "com.google.xr.inputonboardingdemo"),
        ControlItem("Aura Fit", Icons.Default.FitnessCenter, "am start -n com.google.xr.visualfit/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.visualfit", "com.google.xr.visualfit")
    )

    val CoreApps = listOf(
        ControlItem("Gemini Molecule", Icons.Default.AutoAwesome, "am start -n com.google.xr.gemini_molecule/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.gemini_molecule", "com.google.xr.gemini_molecule"),
        ControlItem("Gemini Melody", Icons.Default.MusicNote, "am start -n com.google.xr.music_string/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.music_string", "com.google.xr.music_string"),
        ControlItem("WiredMirror", Icons.Default.Laptop, "am start -n com.google.android.apps.vr.wiredmirror/.mirroring.MirroringActivity", "am force-stop com.google.android.apps.vr.wiredmirror", "com.google.android.apps.vr.wiredmirror"),
        ControlItem("YouTube", Icons.Default.PlayArrow, "am start -a android.intent.action.VIEW -d https://www.youtube.com/playlist?list=LL -p com.google.android.apps.youtube.xr", "am force-stop com.google.android.apps.youtube.xr", "com.google.android.apps.youtube.xr"),
        ControlItem("Demeo", Icons.Default.Games, "am start -n com.resolutiongames.demeo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.resolutiongames.demeo", "com.resolutiongames.demeo"),
        ControlItem("Gemini", Icons.Default.AutoAwesome, "am start -n com.google.android.googlequicksearchbox/com.google.android.apps.search.assistant.surfaces.voice.robin.xr.main.XrModeActivity -f 0x10000000", "am force-stop com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox:gemini"),
        ControlItem("Maps XR", Icons.Default.Map, "am start -n com.google.android.apps.immersiveviewxr/.ImmersiveViewXrActivity", "am force-stop com.google.android.apps.immersiveviewxr", "com.google.android.apps.immersiveviewxr"),
        ControlItem("Oh-My-Galaxy", Icons.Default.SportsEsports, "am start -n com.nDreams.Rio/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.nDreams.Rio", "com.nDreams.Rio"),
        ControlItem("Magic Window", Icons.Default.Visibility, "am start -n com.google.MagicWindow/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.MagicWindow", "com.google.MagicWindow"),
        ControlItem("Maps UX Proto", Icons.Default.Map, "am start -n com.google.MapsUXProto/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.MapsUXProto", "com.google.MapsUXProto"),
        ControlItem("Candy Crush", Icons.Default.Games, "am start -n com.king.candycrushsaga/com.king.candycrushsaga.CandyCrushSagaActivity", "am force-stop com.king.candycrushsaga", "com.king.candycrushsaga"),
        ControlItem("Subway Surfers", Icons.Default.DirectionsRun, "am start -n com.kiloo.subwaysurf/com.sybogames.chili.multidex.ChiliMultidexSupportActivity", "am force-stop com.kiloo.subwaysurf", "com.kiloo.subwaysurf"),
    )

    val Chrome = listOf(
        ControlItem("Moonrider", Icons.Default.SportsEsports, "am start -a android.intent.action.VIEW -d 'https://moonrider.xyz'"),
        ControlItem("External Hand Painter", Icons.Default.Brush, "am start -n com.android.chrome/com.google.android.apps.chrome.Main -a android.intent.action.VIEW -d martin0xff.github.io/HandPainter", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("GemCanvas", Icons.Default.Brush, "am start -n com.android.chrome/com.google.android.apps.chrome.Main -a android.intent.action.VIEW -d 'https://gemini.google.com/app/e504f59fcc9b5650'"),
        ControlItem("Chrome", Icons.Default.Launch, "am start -n com.android.chrome/com.google.android.apps.chrome.Main", "am force-stop com.android.chrome", "com.android.chrome"),
        ControlItem("Scene Viewer", Icons.Default.ViewInAr, "am start -a android.intent.action.VIEW -d 'https://arvr.google.com/scene-viewer/1.2?file=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/model.glb&title=Golden+eagle&referrer=google.com:ANIMALS:kp&sound=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/Eagle_Golden_Unisex_Adult.mp3' -p com.google.android.googlequicksearchbox -f 0x10000000 --es browser_fallback_url 'https://arvr.google.com/scene-viewer?file=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/model.glb&title=Golden+eagle&referrer=google.com:ANIMALS:kp&sound=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/Eagle_Golden_Unisex_Adult.mp3'", "am start -a android.intent.action.VIEW -d 'https://arvr.google.com/scene-viewer/1.2?file=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/model.glb&title=Golden+eagle&referrer=google.com:ANIMALS:kp&sound=https://storage.googleapis.com/ar-answers-in-search-models/static/GoldenEagle/Eagle_Golden_Unisex_Adult.mp3' -p com.google.android.googlequicksearchbox -f 0x10000000; input keyevent 4", "com.google.android.googlequicksearchbox:sceneviewer")

    )

    val Productivity = listOf(
        ControlItem("Gmail", Icons.Default.Email, "am start -n com.google.android.gm/com.google.android.gm.ConversationListActivityGmail", "am force-stop com.google.android.gm", "com.google.android.gm"),
        ControlItem("Docs", Icons.Default.Description, "am start -n com.google.android.apps.docs.editors.docs/com.google.android.apps.docs.app.NewMainProxyActivity -f 0x10000000", "pm disable com.google.android.apps.docs.editors.docs; pm enable com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.docs"),
        ControlItem("Slides", Icons.Default.Slideshow, "am start -n com.google.android.apps.docs.editors.slides/com.google.android.apps.docs.app.NewMainProxyActivity -f 0x10000000", "pm disable com.google.android.apps.docs.editors.slides; pm enable com.google.android.apps.docs.editors.slides", "com.google.android.apps.docs.editors.slides")
    )

    val Media = listOf(
        ControlItem("Augmented Veo", Icons.Default.Launch, "am start -n com.google.xr.xru2_demo/com.unity3d.player.UnityPlayerGameActivity", "am force-stop com.google.xr.xru2_demo", "com.google.xr.xru2_demo"),
        ControlItem("Photos", Icons.Default.Photo, "am start -n com.google.android.apps.photos/.home.HomeActivity", "am force-stop com.google.android.apps.photos", "com.google.android.apps.photos"),
        ControlItem("Photos XR", Icons.Default.Photo, "am start -n com.google.android.apps.photosxr/.home.HomeActivity", "am force-stop com.google.android.apps.photosxr", "com.google.android.apps.photosxr"),
        ControlItem("HBOMax", Icons.Default.Movie, "am start -n com.wbd.stream/com.wbd.fuse.appcore.FuseActivity", "am force-stop com.wbd.stream", "com.wbd.stream"),
        ControlItem("MagentaTV", Icons.Default.Tv, "am start -n de.telekom.magentatv.mobile.debug/de.telekom.magentatv.androidtv.ApplicationMainActivity", "am force-stop de.telekom.magentatv.mobile.debug", "de.telekom.magentatv.mobile.debug"),
        ControlItem("XRSA", Icons.Default.Tv, "am start -n com.accedo.xtend.xrsa.fise/com.accedo.xtendxr.activity.MainActivity", "am force-stop com.accedo.xtend.xrsa.fise", "com.accedo.xtend.xrsa.fise")
    )

    val Navigation = listOf(
        ControlItem("Maps 2D", Icons.Default.Map, "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity", "am force-stop com.google.android.apps.maps", "com.google.android.apps.maps"),
        ControlItem("Grand Canyon", Icons.Default.Landscape, "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity -a android.intent.action.VIEW -d 'geo:0,0?q=Grand+Canyon+National+Park' -p com.google.android.immersiveviewxr", "am force-stop com.google.android.immersiveviewxr", "com.google.android.immersiveviewxr")
    )

    val Tuning = listOf(
        ControlItem("Fan Max", Icons.Default.Air, "echo 100 > /sys/class/fan/max31760/speed_control", requiresUserDebug = true),
        ControlItem("Fan Min", Icons.Default.Air, "echo 20 > /sys/class/fan/max31760/speed_control", requiresUserDebug = true),
        ControlItem("Reset Height", Icons.Default.Height, "surfaceflinger --set-height-above-floor=1.6", requiresUserDebug = true)
    )
}
