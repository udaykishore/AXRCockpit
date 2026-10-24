package com.ukp.axrcockpit;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.ukp.axrcockpit.util.Logger;
import android.view.MotionEvent;
import android.view.Surface;

import com.ukp.axrcockpit.decoder.VideoDecoder;
import com.ukp.axrcockpit.model.ByteUtils;
import com.ukp.axrcockpit.model.MediaPacket;
import com.ukp.axrcockpit.model.VideoPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class Scrcpy extends Service {
    private static final Logger logger = Logger.get(Scrcpy.class);


    public static final int DEFAULT_PORT = 7007;

    private String serverAdr;
    private Surface surface;
    private int screenWidth;
    private int screenHeight;
    private int port = DEFAULT_PORT; // Default port
    private final java.util.concurrent.LinkedBlockingQueue<byte[]> eventQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    private VideoDecoder videoDecoder;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final IBinder mBinder = new MyServiceBinder();
    private boolean first_time = true;
    private final AtomicBoolean LetServiceRunning = new AtomicBoolean(true);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private ServiceCallbacks serviceCallbacks;
    private Thread thread;
    private java.net.Socket activeSocket;
    private VideoPacket.StreamSettings streamSettings;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String newIp = intent.getStringExtra("IP");
            if (newIp != null) serverAdr = newIp;
            int newPort = intent.getIntExtra("PORT", -1);
            if (newPort != -1) port = newPort;
        }
        LetServiceRunning.set(true);
        return START_STICKY;
    }

    public void setServiceCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }


    private void setupVideoDecoder() {
        videoDecoder = new VideoDecoder();
        videoDecoder.setOnFrameRenderedListener(new VideoDecoder.OnFrameRenderedListener() {
            @Override
            public void onFrameRendered() {
                if (serviceCallbacks != null) {
                    serviceCallbacks.onFrameRendered();
                }
            }
        });
        videoDecoder.setOnResolutionDetectedListener(new VideoDecoder.OnResolutionDetectedListener() {
            @Override
            public void onResolutionDetected(int width, int height) {
                if (serviceCallbacks != null) {
                    serviceCallbacks.onResolutionDetected(width, height);
                }
            }
        });
    }

    public void setParms(Surface NewSurface, int NewWidth, int NewHeight) {
        this.screenWidth = NewWidth;
        this.screenHeight = NewHeight;
        this.surface = NewSurface;
        if (videoDecoder == null) {
            setupVideoDecoder();
        }
        videoDecoder.start();
        updateAvailable.set(true);

    }

    public void start(Surface surface, String serverAdr, int screenHeight, int screenWidth, int port) {
        if (thread != null && thread.isAlive()) {
            setParms(surface, screenWidth, screenHeight);
            return;
        }
        setupVideoDecoder();
        videoDecoder.start();
        this.serverAdr = serverAdr;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.surface = surface;
        this.port = port;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startConnection();
            }
        });
        thread.start();
    }

    public void restart() {
        LetServiceRunning.set(false);
        if (activeSocket != null) {
            try {
                activeSocket.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LetServiceRunning.set(true);
        first_time = true;
        start(surface, serverAdr, screenHeight, screenWidth, port);
    }

    public void pause() {
        videoDecoder.stop();

    }

    public void resume() {
        videoDecoder.start();
        updateAvailable.set(true);
    }

    public void StopService() {
        sendShutdownCommand();
        LetServiceRunning.set(false);
        
        // Reset state for potential reuse or next connection
        first_time = true;
        updateAvailable.set(false);
        shutdownRequested.set(false);
        // Removed forceful socket close to allow pending events to be sent
    }


    public boolean touchevent(MotionEvent touch_event, int displayW, int displayH) {

        int[] buf = new int[]{touch_event.getAction(), touch_event.getButtonState(), (int) touch_event.getX() * screenWidth / displayW, (int) touch_event.getY() * screenHeight / displayH};
        final byte[] array = new byte[buf.length * 4]; // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        eventQueue.add(array);
        return true;
    }

    public void sendKeyevent(int keycode) {
        // Send 16 bytes (4 ints) to match server expectations
        int[] buf = new int[]{keycode, 0, 0, 0};

        final byte[] array = new byte[buf.length * 4];   // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        eventQueue.add(array);
    }

    public void sendResolutionChange(int newMaxSize) {
        // Send 16 bytes (4 ints) to match server expectations
        // 1030 is the code for resolution change, specific to scrcpy-server
        int[] buf = new int[]{1030, newMaxSize, 0, 0}; // 1030 is the code for resolution change
        final byte[] array = new byte[buf.length * 4];
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        eventQueue.add(array);
    }

    public void sendShutdownCommand() {
        // Send 16 bytes (4 ints) to match server expectations
        // 1031 is the code for shutdown, specific to scrcpy-server
        int[] buf = new int[]{1031, 0, 0, 0}; // 1031 is the code for shutdown
        final byte[] array = new byte[buf.length * 4];
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        eventQueue.add(array);
        shutdownRequested.set(true);
    }

    private void startConnection() {
        setupVideoDecoder();
        videoDecoder.start();
        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;
        Socket socket = null;
        int attempts = 50;
        shutdownRequested.set(false); // Reset shutdown flag before connection loop
        while (LetServiceRunning.get()) {
            try {
                socket = new Socket(serverAdr, port);
                activeSocket = socket;
                eventQueue.clear(); // Clear stale events on reconnect
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                final DataOutputStream finalDataOutputStream = dataOutputStream;
                final AtomicBoolean connectionActive = new AtomicBoolean(true);
                Thread writerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (connectionActive.get() && (LetServiceRunning.get() || !eventQueue.isEmpty())) {
                                byte[] pendingEvent = eventQueue.take();
                                finalDataOutputStream.write(pendingEvent, 0, pendingEvent.length);
                                
                                byte[] nextEvent = eventQueue.poll();
                                while (nextEvent != null) {
                                    finalDataOutputStream.write(nextEvent, 0, nextEvent.length);
                                    nextEvent = eventQueue.poll();
                                }
                                finalDataOutputStream.flush();
                                
                                if (shutdownRequested.get() && eventQueue.isEmpty()) {
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            // Expected when interrupted by reader thread
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                writerThread.start();

                byte[] packetSize = new byte[4];
                while (LetServiceRunning.get()) {
                    try {
                        dataInputStream.readFully(packetSize, 0, 4);

                        int size = ByteUtils.bytesToInt(packetSize);
                        byte[] packet = new byte[size];
                        dataInputStream.readFully(packet, 0, size);
                        VideoPacket videoPacket = VideoPacket.fromArray(packet);
                        if (videoPacket.type == MediaPacket.Type.VIDEO) {
                            byte[] data = videoPacket.data;
                            
                            // Try to extract stream settings if we don't have them yet
                            if (streamSettings == null || videoPacket.flag == VideoPacket.Flag.CONFIG) {
                                VideoPacket.StreamSettings settings = VideoPacket.getStreamSettings(data);
                                if (settings != null) {
                                    streamSettings = settings;
                                    if (videoPacket.flag == VideoPacket.Flag.CONFIG && !first_time && serviceCallbacks != null) {
                                        serviceCallbacks.loadNewRotation();
                                    }
                                }
                            }

                            // If we have settings and need to configure (new connection, config frame, or first keyframe)
                            if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get() || (videoPacket.flag == VideoPacket.Flag.KEY_FRAME && first_time)) {
                                logger.d("Configuring decoder. Flag: " + videoPacket.flag + ", updateAvailable: " + updateAvailable.get());
                                updateAvailable.set(false);
                                first_time = false;
                                if (streamSettings != null) {
                                    videoDecoder.configure(surface, screenWidth, screenHeight, streamSettings.sps, streamSettings.pps);
                                }
                            }
                            
                            if (videoPacket.flag == VideoPacket.Flag.END) {
                                // need close stream
                            } else if (videoPacket.flag != VideoPacket.Flag.CONFIG) {
                                videoDecoder.decodeSample(data, 0, data.length, 0, videoPacket.flag.getFlag());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (serviceCallbacks != null) {
                            serviceCallbacks.onDisconnected();
                        }
                        break;
                    }
                }
                connectionActive.set(false);
                writerThread.interrupt();
                try {
                    writerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                try {
                    attempts = attempts - 1;
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
//                 logger.e(e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        });

    }

    public interface ServiceCallbacks {
        void loadNewRotation();
        void onDisconnected();
        void onFrameRendered();
        void onResolutionDetected(int width, int height);
    }

    public class MyServiceBinder extends Binder {
        public Scrcpy getService() {
            return Scrcpy.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LetServiceRunning.set(false);
        if (videoDecoder != null) {
            videoDecoder.stop();
        }
        if (activeSocket != null) {
            try {
                activeSocket.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        eventQueue.clear();
    }
}
