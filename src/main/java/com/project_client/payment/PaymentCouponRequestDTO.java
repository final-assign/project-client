package com.project_client.payment;

import com.project_client.Utils;
import com.project_client.general.HeaderType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Builder
@RequiredArgsConstructor
public class PaymentCouponRequestDTO {
    private final long menuId;

    public byte[] toBytes() {
        // id(long, 8byte)
        int bodySize = 8;

        // 전체 패킷 크기: Header(6) + Body(12)
        byte[] data = new byte[6 + bodySize];
        int cursor = 0;

        // 헤더
        data[cursor++] = (byte) HeaderType.REQUEST.getValue(); // 0x01
        data[cursor++] = (byte) 0x39;                          // 결제 요청 코드
        cursor = Utils.intToBytes(bodySize, data, cursor);    // Body Length (12)

        // id
        cursor = Utils.longToBytes(menuId, data, cursor);

        return data;
    }
}

