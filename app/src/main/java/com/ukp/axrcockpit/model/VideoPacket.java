package com.ukp.axrcockpit.model;

import java.nio.ByteBuffer;

/**
 * Created by Alexandr Golovach on 27.06.16.
 * https://www.github.com/alexmprog/VideoCodec
 */

public class VideoPacket extends MediaPacket {

    public Flag flag;
    public long presentationTimeStamp;
    public byte[] data;

    public VideoPacket() {
    }

    public VideoPacket(Type type, Flag flag, long presentationTimeStamp, byte[] data) {
        this.type = type;
        this.flag = flag;
        this.presentationTimeStamp = presentationTimeStamp;
        this.data = data;
    }

    // create packet from byte array
    public static VideoPacket fromArray(byte[] values) {
        VideoPacket videoPacket = new VideoPacket();

        // should be a type value - 1 byte
        byte typeValue = values[0];
        // should be a flag value - 1 byte
        byte flagValue = values[1];

        videoPacket.type = Type.getType(typeValue);
        videoPacket.flag = Flag.getFlag(flagValue);

        // should be 8 bytes for timestamp
        byte[] timeStamp = new byte[8];
        System.arraycopy(values, 2, timeStamp, 0, 8);
        videoPacket.presentationTimeStamp = ByteUtils.bytesToLong(timeStamp);

        // all other bytes is data
        int dataLength = values.length - 10;
        byte[] data = new byte[dataLength];
        System.arraycopy(values, 10, data, 0, dataLength);
        videoPacket.data = data;

        return videoPacket;
    }

    // create byte array
    public static byte[] toArray(Type type, Flag flag, long presentationTimeStamp, byte[] data) {

        // should be 4 bytes for packet size
        byte[] bytes = ByteUtils.intToBytes(10 + data.length);

        int packetSize = 14 + data.length; // 4 - inner packet size 1 - type + 1 - flag + 8 - timeStamp + data.length
        byte[] values = new byte[packetSize];

        System.arraycopy(bytes, 0, values, 0, 4);

        // set type value
        values[4] = type.getType();
        // set flag value
        values[5] = flag.getFlag();
        // set timeStamp
        byte[] longToBytes = ByteUtils.longToBytes(presentationTimeStamp);
        System.arraycopy(longToBytes, 0, values, 6, longToBytes.length);

        // set data array
        System.arraycopy(data, 0, values, 14, data.length);
        return values;
    }

    // should call on inner packet
    public static boolean isVideoPacket(byte[] values) {
        return values[0] == Type.VIDEO.getType();
    }

    public static StreamSettings getStreamSettings(byte[] buffer) {
        int spsStart = -1;
        int ppsStart = -1;
        int sliceStart = -1;

        int searchLimit = Math.min(buffer.length, 512);
        for (int i = 0; i <= searchLimit - 3; i++) {
            boolean isStartCode = false;
            int startCodeLength = 0;
            if (i <= searchLimit - 4 && buffer[i] == 0 && buffer[i+1] == 0 && buffer[i+2] == 0 && buffer[i+3] == 1) {
                isStartCode = true;
                startCodeLength = 4;
            } else if (buffer[i] == 0 && buffer[i+1] == 0 && buffer[i+2] == 1) {
                isStartCode = true;
                startCodeLength = 3;
            }

            if (isStartCode) {
                if (spsStart == -1) {
                    spsStart = i;
                } else if (ppsStart == -1) {
                    ppsStart = i;
                } else if (sliceStart == -1) {
                    sliceStart = i;
                    break;
                }
                i += startCodeLength - 1;
            }
        }

        if (spsStart != -1 && ppsStart != -1) {
            int spsLength = ppsStart - spsStart;
            byte[] sps = new byte[spsLength];
            System.arraycopy(buffer, spsStart, sps, 0, spsLength);

            int ppsLength = (sliceStart != -1) ? (sliceStart - ppsStart) : (buffer.length - ppsStart);
            byte[] pps = new byte[ppsLength];
            System.arraycopy(buffer, ppsStart, pps, 0, ppsLength);

            StreamSettings streamSettings = new StreamSettings();
            streamSettings.sps = ByteBuffer.wrap(sps);
            streamSettings.pps = ByteBuffer.wrap(pps);
            return streamSettings;
        }
        return null;
    }

    public byte[] toByteArray() {
        return toArray(type, flag, presentationTimeStamp, data);
    }

    public enum Flag {

        FRAME((byte) 0), KEY_FRAME((byte) 1), CONFIG((byte) 2), END((byte) 4);

        private final byte type;

        Flag(byte type) {
            this.type = type;
        }

        public static Flag getFlag(byte value) {
            for (Flag type : Flag.values()) {
                if (type.getFlag() == value) {
                    return type;
                }
            }

            return null;
        }

        public byte getFlag() {
            return type;
        }
    }

    public static class StreamSettings {
        public ByteBuffer pps;
        public ByteBuffer sps;
    }
}
