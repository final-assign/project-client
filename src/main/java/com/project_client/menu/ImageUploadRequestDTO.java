package com.project_client.menu;

import com.project_client.Utils;
import com.project_client.general.HeaderType;
import lombok.Builder;

@Builder
public class ImageUploadRequestDTO {
    private final long menuId;
    private final byte[] fileData;

    public byte[] toBytes() {
        // Header
        int headerSize = 6;
        // Body: menuId(8) + fileData
        int bodySize = 8 + fileData.length;
        byte[] packet = new byte[headerSize + bodySize];

        // Header
        packet[0] = HeaderType.REQUEST.getValue();
        packet[1] = (byte) 0x91; // Image Upload Request Code
        System.arraycopy(Utils.intToBytes(bodySize), 0, packet, 2, 4);

        // Body
        int offset = headerSize;
        offset = Utils.longToBytes(menuId, packet, offset);
        System.arraycopy(fileData, 0, packet, offset, fileData.length);

        return packet;
    }
}
