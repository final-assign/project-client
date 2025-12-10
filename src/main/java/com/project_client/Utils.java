package com.project_client;

import java.nio.charset.StandardCharsets;

public class Utils {
    public static byte[] intToBytes(int data) {
        return new byte[]{
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }

    public static int intToBytes(int data, byte[] dest, int offset) {

        dest[offset++] = (byte) ((data >> 24) & 0xff);
        dest[offset++] = (byte) ((data >> 16) & 0xff);
        dest[offset++] = (byte) ((data >> 8) & 0xff);
        dest[offset++] = (byte) ((data >> 0) & 0xff);

        return offset;
    }

    public static int bytesToInt(byte[] data, int start) {
        return (int) (
                (0xff & data[start]) << 24 |
                        (0xff & data[start + 1]) << 16 |
                        (0xff & data[start + 2]) << 8 |
                        (0xff & data[start + 3]) << 0
        );
    }

    public static byte[] shortToBytes(int data) {
        return new byte[]{
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }

    public static short bytesToShort(byte[] data, int start) {
        return (short) (
                (0xff & data[start]) << 8 |
                        (0xff & data[start + 1]) << 0
        );
    }

    public static byte[] longToBytes(long data) {
        return new byte[]{
                (byte) ((data >> 56) & 0xff),
                (byte) ((data >> 48) & 0xff),
                (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff),
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }

    public static long bytesToLong(byte[] data, int start) {
        return (long) (
                (long) (0xff & data[start]) << 56 |
                        (long) (0xff & data[start + 1]) << 48 |
                        (long) (0xff & data[start + 2]) << 40 |
                        (long) (0xff & data[start + 3]) << 32 |
                        (long) (0xff & data[start + 4]) << 24 |
                        (long) (0xff & data[start + 5]) << 16 |
                        (long) (0xff & data[start + 6]) << 8 |
                        (long) (0xff & data[start + 7]) << 0
        );
    }

    public static String readString(byte[] body, int cursor) {
        int len = Utils.bytesToInt(body, cursor);
        return new String(body, cursor + 4, len, StandardCharsets.UTF_8);
    }

    public static int longToBytes(long data, byte[] dest, int offset) {
        dest[offset++] = (byte)((data >> 56) & 0xff);
        dest[offset++] = (byte)((data >> 48) & 0xff);
        dest[offset++] = (byte)((data >> 40) & 0xff);
        dest[offset++] = (byte)((data >> 32) & 0xff);
        dest[offset++] = (byte)((data >> 24) & 0xff);
        dest[offset++] = (byte)((data >> 16) & 0xff);
        dest[offset++] = (byte)((data >> 8) & 0xff);
        dest[offset++] = (byte)((data >> 0) & 0xff);
        return offset;
    }

    public static int shortToBytes(short data, byte[] dest, int offset) {

        dest[offset++] = (byte)((data >> 8) & 0xff);
        dest[offset++] = (byte)((data >> 0) & 0xff);

        return offset;
    }
}