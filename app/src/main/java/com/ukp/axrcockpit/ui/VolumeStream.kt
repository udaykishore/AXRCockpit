package com.ukp.axrcockpit.ui

interface VolumeStream {
    val label: String
    val streamId: Int
    val maxVolume: Int
    val valueRange: ClosedFloatingPointRange<Float>
        get() = 0f..maxVolume.toFloat()
}

object MediaVolumeStream : VolumeStream {
    override val label = "Media Volume"
    override val streamId = 3
    override val maxVolume = 15
}

object NotificationVolumeStream : VolumeStream {
    override val label = "Notification Volume"
    override val streamId = 5
    override val maxVolume = 7
}

object GeminiVolumeStream : VolumeStream {
    override val label = "Gemini Volume"
    override val streamId = 11
    override val maxVolume = 15
}

object SystemVolumeStream : VolumeStream {
    override val label = "System Volume"
    override val streamId = 2
    override val maxVolume = 7
}
