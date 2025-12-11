package com.project_client.coupon;

import com.project_client.Utils;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AllCouponResponseDTO {
    private final List<CouponDetail> coupons;

    public AllCouponResponseDTO(byte[] body) {
        this.coupons = new ArrayList<>();
        int cursor = 0;

        // 1. 쿠폰 개수 읽기
        // (규칙: Count는 Length Prefix 없이 값만 옴)
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4;

        for (int i = 0; i < count; i++) {
            // [규칙] 기본 필드: [Length(4)] + [Value]
            // [규칙] 문자열 필드: [OuterLength(4)] + [InnerLength(4)] + [StringBytes]

            // 1. ID (Long) -> [Len 4] + [Val 8]
            int idLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // 길이 스킵
            long id = Utils.bytesToLong(body, cursor);
            cursor += 8; // 값 읽기

            // 2. Price (Int) -> [Len 4] + [Val 4]
            int priceLen = Utils.bytesToInt(body, cursor);
            cursor += 4;
            int price = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 3. Quantity (Int) -> [Len 4] + [Val 4]
            int qtyLen = Utils.bytesToInt(body, cursor);
            cursor += 4;
            int quantity = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 4. Menu Name (String) -> [OuterLen 4] + [InnerLen 4] + [Bytes]
            int nameOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // Outer Length 스킵
            String menuName = readString(body, cursor);
            cursor += nameOuterLen; // 실제 문자열 패킷 크기만큼 이동

            coupons.add(new CouponDetail(id, menuName, price, quantity));
        }
    }

    // 문자열 파싱 헬퍼
    private String readString(byte[] body, int cursor) {
        int len = Utils.bytesToInt(body, cursor);
        return new String(body, cursor + 4, len, StandardCharsets.UTF_8);
    }

    @Getter
    @ToString
    public static class CouponDetail {
        private final long id;
        private final String menuName;
        private final int price;
        private final int quantity;

        public CouponDetail(long id, String menuName, int price, int quantity) {
            this.id = id;
            this.menuName = menuName;
            this.price = price;
            this.quantity = quantity;
        }
    }
}
