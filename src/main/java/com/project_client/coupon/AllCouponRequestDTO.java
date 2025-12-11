package com.project_client.coupon;

import com.project_client.general.RequestDTO;

public class AllCouponRequestDTO implements RequestDTO {
    @Override
    public byte[] toBytes() {
        byte[] header = new byte[6];
        header[0] = 0x01;
        header[1] = 0x42;

        return header;
    }
}
