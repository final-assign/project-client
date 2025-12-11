package com.project_client.payment;

import com.project_client.Utils;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PaymentCouponResponseDTO {
    private final long couponId;
    private final int count;

    public PaymentCouponResponseDTO(byte[] body) {
        int offset = 0;

        this.couponId = Utils.bytesToLong(body, offset);
        offset += 8; // 데이터 읽고 이동

        this.count = Utils.bytesToInt(body, offset);
        offset += 4; // 데이터 읽고 이동
    }
}
