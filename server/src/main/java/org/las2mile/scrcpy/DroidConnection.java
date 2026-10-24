package org.las2mile.scrcpy;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.las2mile.scrcpy.Ln;

public final class DroidConnection implements Closeable {


    private static Socket socket = null;
    private OutputStream outputStream;
    private InputStream inputStream;

    private DroidConnection(Socket socket) throws IOException {
        this.socket = socket;

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }


    private static Socket listenAndAccept(int port) throws IOException {
        ServerSocket serverSocket = null;
        int attempts = 10;
        while (attempts > 0) {
            try {
                serverSocket = new ServerSocket(port);
                break;
            } catch (IOException e) {
                attempts--;
                Ln.d("Port " + port + " busy, waiting 1 sec... attempts left: " + attempts);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (serverSocket == null) {
            throw new IOException("Unable to bind to port " + port + " after 10 attempts");
        }
        Socket sock = null;
        try {
            sock = serverSocket.accept();
        } finally {
            serverSocket.close();
        }
        return sock;
    }

    public static DroidConnection open(String ip, int port) throws IOException {
        socket = listenAndAccept(port);
        return new DroidConnection(socket);
    }

    public void close() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }


    public int[] NewreceiveControlEvent() throws IOException {

        byte[] buf = new byte[16];
        int n = inputStream.read(buf, 0, 16);
        if (n == -1) {
            throw new EOFException("Event controller socket closed");
        }


        final int[] array = new int[buf.length / 4];
        for (int i = 0; i < array.length; i++)
            array[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                    (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                    (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                    ((int) (buf[i * 4 + 3]) & 0xFF);
        return array;


    }

}