package org.las2mile.scrcpy;

public class Options {
    public static final int DEFAULT_PORT = 7007;
    private int maxSize;
    private int bitRate;
    private boolean tunnelForward;
    private int port = DEFAULT_PORT; // Default port

    public int getMaxSize() {
        return maxSize;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isTunnelForward() {
        return tunnelForward;
    }

    public void setTunnelForward(boolean tunnelForward) {
        this.tunnelForward = tunnelForward;
    }
}
