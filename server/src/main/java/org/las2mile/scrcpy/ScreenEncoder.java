package org.las2mile.scrcpy;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import org.las2mile.scrcpy.model.MediaPacket;
import org.las2mile.scrcpy.model.VideoPacket;
import org.las2mile.scrcpy.wrappers.SurfaceControl;
import org.las2mile.scrcpy.wrappers.DisplayManager;
import org.las2mile.scrcpy.wrappers.MediaProjectionGlobal;
import org.las2mile.scrcpy.Ln;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenEncoder implements Device.RotationListener, Device.ResolutionListener {

    private static final int DEFAULT_FRAME_RATE = 60; // fps
    private static final int DEFAULT_I_FRAME_INTERVAL = 1; // seconds

    private static final int REPEAT_FRAME_DELAY = 6; // repeat after 6 frames

    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;

    private final AtomicBoolean rotationChanged = new AtomicBoolean();
    private final AtomicBoolean resolutionChanged = new AtomicBoolean();

    private int bitRate;
    private int frameRate;
    private int iFrameInterval;

    public ScreenEncoder(int bitRate, int frameRate, int iFrameInterval) {
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.iFrameInterval = iFrameInterval;
    }

    public ScreenEncoder(int bitRate) {
        this(bitRate, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
    }

    private static MediaCodec createCodec() throws IOException {
        return MediaCodec.createEncoderByType("video/avc");
    }

    private static MediaFormat createFormat(int bitRate, int frameRate, int iFrameInterval) throws IOException {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "video/avc");
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            format.setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1);
        }

        // display the very first frame, and recover from bad quality when no new frames
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,
                MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate); // µs
        return format;
    }

    private static IBinder createDisplay() {
        return SurfaceControl.createDisplay("scrcpy", false);
    }

    private static void configure(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private static void setSize(MediaFormat format, int width, int height) {
        format.setInteger(MediaFormat.KEY_WIDTH, width);
        format.setInteger(MediaFormat.KEY_HEIGHT, height);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    private static void destroyDisplay(IBinder display) {
        SurfaceControl.destroyDisplay(display);
    }

    @Override
    public void onRotationChanged(int rotation) {
        rotationChanged.set(true);
    }

    public boolean consumeRotationChange() {
        return rotationChanged.getAndSet(false);
    }

    @Override
    public void onResolutionChanged(int maxSize) {
        Ln.i("onResolutionChanged called with maxSize: " + maxSize);
        resolutionChanged.set(true);
    }

    public boolean consumeResolutionChange() {
        return resolutionChanged.getAndSet(false);
    }

    public void streamScreen(Device device, OutputStream outputStream) throws IOException {
        Ln.i("streamScreen.");
        MediaFormat format = createFormat(bitRate, frameRate, iFrameInterval);
        device.setRotationListener(this);
        device.setResolutionListener(this);
        boolean alive = true;
        try {
            do {
                    Ln.i("streamScreen do-while loop iteration started");
                    VirtualDisplay virtualDisplay = null;
                    MediaCodec codec = createCodec();
                    IBinder display = null;
                    Rect deviceRect = device.getScreenInfo().getDeviceSize().toRect();
                    Rect videoRect = device.getScreenInfo().getVideoSize().toRect();
                    setSize(format, videoRect.width(), videoRect.height());
                    configure(codec, format);
                    Surface surface = codec.createInputSurface();
                    try {
                        virtualDisplay = DisplayManager.createVirtualDisplay(
                                "displayManager VirtualDisplay",
                                videoRect.width(),
                                videoRect.height(),
                                0,
                                surface);
                        Ln.i("Using the DisplayManager system API.");
                    } catch (Exception displayManagerException) {
                        try {
                            virtualDisplay = MediaProjectionGlobal.createVirtualDisplay(
                                    "mediaProjection global",
                                    videoRect.width(),
                                    videoRect.height(),
                                    0,
                                    surface);
                            Ln.i("Using the MediaProjectionGlobal system API.");
                        } catch (NoSuchMethodException | NullPointerException mediaProjectionGlobalException) {
                            Ln.i("createDisplay.");
                            display = createDisplay();
                            setDisplaySurface(display, surface, deviceRect, videoRect);
                        }
                    }
                codec.start();
                try {
                    alive = encode(device, codec, outputStream);
                } finally {

                    codec.stop();
                    if (display != null) {
                        destroyDisplay(display);
                    }
                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                    }
                    codec.release();
                    surface.release();
                }
             } while (alive && !device.isStopRequested());


        } finally {
            Ln.i("streamScreen finally.");
            device.setRotationListener(null);
        }
    }

    private boolean encode(Device device, MediaCodec codec, OutputStream outputStream) throws IOException {
        @SuppressWarnings("checkstyle:MagicNumber")
        // byte[] buf = new byte[bitRate / 8]; // may contain up to 1 second of video
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int frameCount = 0;

        while (!eof && !device.isStopRequested()) {
            if (consumeRotationChange()) {
                Ln.i("Rotation change detected in encoder loop, breaking loop");
                break;
            }
            if (consumeResolutionChange()) {
                Ln.i("Resolution change detected in encoder loop, breaking loop");
                break;
            }
            // Poll for screen/rotation changes since IRotationWatcher is disabled
            if (device.checkScreenChanged()) {
                Ln.i("Screen change detected in encoder loop");
                break; // Break to recreate codec with new size
            }

            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 100000); // Wait up to 100ms instead of indefinitely
            try {
                if (outputBufferId >= 0) {
                    eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                        outputBuffer = outputBuffers[outputBufferId];
                    } else {
                        outputBuffer = codec.getOutputBuffer(outputBufferId);
                    }

                    if (bufferInfo.size > 0 && outputBuffer != null) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] b = new byte[outputBuffer.remaining()];
                        outputBuffer.get(b);

                        MediaPacket.Type type = MediaPacket.Type.VIDEO;
                        VideoPacket.Flag flag = VideoPacket.Flag.CONFIG;

                        if ((bufferInfo.flags
                                & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            flag = VideoPacket.Flag.END;
                        } else if ((bufferInfo.flags
                                & MediaCodec.BUFFER_FLAG_KEY_FRAME) == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            flag = VideoPacket.Flag.KEY_FRAME;
                        } else if (bufferInfo.flags == 0) {
                            flag = VideoPacket.Flag.FRAME;
                        }
                        VideoPacket packet = new VideoPacket(type, flag, bufferInfo.presentationTimeUs, b);
                        outputStream.write(packet.toByteArray());
                    }

                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }

        return !eof;
    }

}
