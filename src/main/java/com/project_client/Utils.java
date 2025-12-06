package com.project_client;

public class Utils {
    public static byte[] intToBytes(int data) {
        return new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff),
        };
    }

    public static int bytesToInt(byte[] data, int start) {
        return (int)(
                (0xff & data[start]) << 24 |
                        (0xff & data[start + 1]) << 16 |
                        (0xff & data[start + 2]) << 8  |
                        (0xff & data[start + 3]) << 0
        );
    }

    public static byte[] shortToBytes(int data) {
        return new byte[] {
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff),
        };
    }

    public static short bytesToShort(byte[] data, int start) {
        return (short)(
                (0xff & data[start]) << 8 |
                        (0xff & data[start + 1]) << 0
        );
    }
}