package com.ukp.axrcockpit.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ukp.axrcockpit.util.Logger;

public class VideoDecoder {
    private static final Logger logger = Logger.get(VideoDecoder.class);

    private final Object mLock = new Object();
    private volatile MediaCodec mCodec;
    private volatile Worker mWorker;
    private final AtomicBoolean mIsConfigured = new AtomicBoolean(false);

    public interface OnFrameRenderedListener {
        void onFrameRendered();
    }
    private volatile OnFrameRenderedListener mOnFrameRenderedListener;

    public void setOnFrameRenderedListener(OnFrameRenderedListener listener) {
        mOnFrameRenderedListener = listener;
    }

    public interface OnResolutionDetectedListener {
        void onResolutionDetected(int width, int height);
    }
    private volatile OnResolutionDetectedListener mOnResolutionDetectedListener;

    public void setOnResolutionDetectedListener(OnResolutionDetectedListener listener) {
        mOnResolutionDetectedListener = listener;
    }

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        Worker worker = mWorker;
        if (worker != null) {
            worker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
        Worker worker = mWorker;
        if (worker != null) {
            worker.configure(surface, width, height, csd0, csd1);
        }
    }


    public void start() {
        synchronized (mLock) {
            if (mWorker == null) {
                mWorker = new Worker();
                mWorker.setRunning(true);
                mWorker.start();
            }
        }
    }

    public void stop() {
        synchronized (mLock) {
            if (mWorker != null) {
                mWorker.setRunning(false);
                mWorker = null;
            }
            mIsConfigured.set(false);
            if (mCodec != null) {
                try {
                    mCodec.stop();
                    mCodec.release();
                } catch (Exception e) {
                    // Ignore exception if the codec is already in an invalid state or released
                }
                mCodec = null;
            }
        }
    }

    private class Worker extends Thread {

        private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

        Worker() {
        }

        private void setRunning(boolean isRunning) {
            mIsRunning.set(isRunning);
        }

        private void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
            if (width <= 0 || height <= 0 || surface == null || !surface.isValid()) {
                return;
            }
            synchronized (mLock) {
                if (mCodec != null) {
                    mIsConfigured.set(false);
                    try {
                        logger.d("Releasing old codec.");
                        mCodec.stop();
                        mCodec.release();
                    } catch (Exception e) {
                        logger.w("Error during codec release", e);
                    }
                    mCodec = null;
                }

                MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
                format.setByteBuffer("csd-0", csd0);
                format.setByteBuffer("csd-1", csd1);
                try {
                    mCodec = MediaCodec.createDecoderByType("video/avc");
                    mCodec.configure(format, surface, null, 0);
                    mCodec.start();
                    mIsConfigured.set(true);
                } catch (IOException | IllegalStateException | IllegalArgumentException e) {
                    if (mCodec != null) {
                        try {
                            mCodec.release();
                        } catch (Exception ignore) {
                        }
                        mCodec = null;
                    }
                    mIsConfigured.set(false);
                }
            }
        }


        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            try {
                MediaCodec codec = mCodec;
                if (mIsConfigured.get() && mIsRunning.get() && codec != null) {
                    // Using small timeout to avoid blocking indefinitely while holding reference to codec
                    int index = codec.dequeueInputBuffer(10000); 
                    if (index >= 0) {
                        ByteBuffer buffer = codec.getInputBuffer(index);
                        if (buffer != null) {
                            buffer.clear();
                            buffer.put(data, offset, size);
                            codec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
                        }
                    }
                }
            } catch (IllegalStateException e) {
                // Ignore exception if the codec becomes invalid or released during state transitions
            }
        }


        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mIsRunning.get()) {
                    MediaCodec codec = mCodec;
                    if (mIsConfigured.get() && codec != null) {
                        try {
                            int index = codec.dequeueOutputBuffer(info, 10000); // 10ms
                            if (index >= 0) {
                                // setting true is telling system to render frame onto Surface
                                codec.releaseOutputBuffer(index, true);
                                if (mOnFrameRenderedListener != null) {
                                    mOnFrameRenderedListener.onFrameRendered();
                                }
                                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                    break;
                                }
                            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                MediaFormat format = codec.getOutputFormat();
                                int width = format.getInteger(MediaFormat.KEY_WIDTH);
                                int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                                if (format.containsKey("crop-left") && format.containsKey("crop-right") &&
                                    format.containsKey("crop-top") && format.containsKey("crop-bottom")) {
                                    width = format.getInteger("crop-right") - format.getInteger("crop-left") + 1;
                                    height = format.getInteger("crop-bottom") - format.getInteger("crop-top") + 1;
                                }
                                if (format.containsKey("sar-width") && format.containsKey("sar-height")) {
                                    int sarWidth = format.getInteger("sar-width");
                                    int sarHeight = format.getInteger("sar-height");
                                    if (sarHeight > 0) {
                                        width = (width * sarWidth) / sarHeight;
                                    }
                                }
                                if (mOnResolutionDetectedListener != null) {
                                    mOnResolutionDetectedListener.onResolutionDetected(width, height);
                                }
                            }
                        } catch (IllegalStateException e) {
                            // Codec might have been stopped/released
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } catch (Exception e) {
                // Global catch to prevent thread crash
            }
        }
    }
}
