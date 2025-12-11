package com.project_client.restaurant;

public class AvailableRestaurantRequestDTO {
    public byte[] toBytes() {
        // Header(6) + Body(0) = 6 bytes
        byte[] bytes = new byte[6];
        int offset = 0;

        // 헤더만, no data
        bytes[offset++] = (byte) 0x01; // Type: REQUEST
        bytes[offset++] = (byte) 0x14; // Code: AvailableRestaurantRequest

        bytes[offset++] = (byte) ((0 >> 24) & 0xff); // Body Length: 0
        bytes[offset++] = (byte) ((0 >> 16) & 0xff);
        bytes[offset++] = (byte) ((0 >> 8) & 0xff);
        bytes[offset++] = (byte) ((0 >> 0) & 0xff);

        return bytes;
    }

}
