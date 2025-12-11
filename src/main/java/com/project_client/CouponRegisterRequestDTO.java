package com.project_client;

import com.project_client.general.HeaderType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@AllArgsConstructor
@RequiredArgsConstructor
public class CouponRegisterRequestDTO {

    private Long menuId; // 0이면 금액권, 그 외는 메뉴 교환권
    private int amount;  // 금액권일 때만 사용, 메뉴권이면 0

    public byte[] toBytes() {
        // Header(6) + Body(12) = 18 bytes
        byte[] bytes = new byte[18];
        int offset = 0;

        // --- Header 작성 ---
        bytes[offset++] = (byte) HeaderType.REQUEST.getValue(); // 1 byte
        bytes[offset++] = (byte) 0xC3;                          // 1 byte
        offset = Utils.intToBytes(12, bytes, offset);           // Body 길이 (4 byte)

        // --- Body 작성 ---
        offset = Utils.longToBytes(menuId, bytes, offset);      // menuId (8 byte)
        offset = Utils.intToBytes(amount, bytes, offset);       // amount (4 byte)

        return bytes;
    }

}
