package com.project_client.payment;

import com.project_client.Utils;
import com.project_client.general.HeaderType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class PaymentRequestDTO {
    private final long menuId;
    private final int menuPrice;

    public byte[] toBytes() {
        // Body Size 계산
        // menuPrice(int, 4byte) + menuId(long, 8byte) = 12 bytes
        int bodySize = 12;

        // 전체 패킷 크기: Header(6) + Body(12)
        byte[] data = new byte[6 + bodySize];
        int cursor = 0;

        // --- Header ---
        data[cursor++] = (byte) HeaderType.REQUEST.getValue(); // 0x01
        data[cursor++] = (byte) 0x32;                          // 결제 요청 코드
        cursor = Utils.intToBytes(bodySize, data, cursor);     // Body Length (12)

        // --- Body ---

        // 가격
        cursor = Utils.intToBytes(menuPrice, data, cursor);

        // id
        cursor = Utils.longToBytes(menuId, data, cursor);

        return data;
    }
}