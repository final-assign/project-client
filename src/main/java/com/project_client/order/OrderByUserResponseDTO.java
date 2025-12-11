package com.project_client.order;

import com.project_client.Utils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderByUserResponseDTO {
    private final List<OrderDetail> orders;

    public OrderByUserResponseDTO(byte[] body) {
        this.orders = new ArrayList<>();
        int cursor = 0;

        // 1. 주문 개수 읽기
        // [규칙] Count는 Length Prefix 없이 값(4byte)만 옴
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4; // [수정] 커서 이동 필수

        for (int i = 0; i < count; i++) {
            // [규칙] 기본 필드: [Length(4)] + [Value]
            // [규칙] 문자열 필드: [OuterLength(4)] + [InnerLength(4)] + [StringBytes]

            // 1. Order ID (Long) -> [Len 4] + [Val 8]
            int idLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정] 길이 정보 건너뛰기
            long id = Utils.bytesToLong(body, cursor);
            cursor += 8; // [수정] 데이터(8byte) 읽었으니 이동

            // 2. Menu Name (String) -> [OuterLen 4] + [InnerLen 4] + [Bytes]
            int menuOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정] Outer Length 필드 건너뛰기
            String menuName = readString(body, cursor);
            cursor += menuOuterLen; // [수정] 문자열 패킷 전체 크기만큼 이동

            // 3. Restaurant Name (String)
            int restOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정] Outer Length 필드 건너뛰기
            String restaurantName = readString(body, cursor);
            cursor += restOuterLen; // [수정] 이동

            // 4. Payment Price (Int) -> [Len 4] + [Val 4]
            int priceLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]
            int price = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]

            // 5. Coupon Price (Int) -> [Len 4] + [Val 4]
            int cpLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]
            int couponPrice = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]

            // 6. Purchase Type (String)
            int typeOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]
            String purchaseType = readString(body, cursor);
            cursor += typeOuterLen; // [수정]

            // 7. Status (String)
            int statusOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]
            String status = readString(body, cursor);
            cursor += statusOuterLen; // [수정]

            // 8. Created At (String -> LocalDateTime)
            int dateOuterLen = Utils.bytesToInt(body, cursor);
            cursor += 4; // [수정]
            String dateStr = readString(body, cursor);
            LocalDateTime createdAt = LocalDateTime.parse(dateStr); // ISO-8601 포맷 가정
            cursor += dateOuterLen; // [수정]

            // 객체 생성
            OrderDetail order = OrderDetail.builder()
                    .id(id)
                    .menuName(menuName)
                    .restaurantName(restaurantName)
                    .price(price)
                    .purchaseType(purchaseType)
                    .status(status)
                    .couponPrice(couponPrice)
                    .createdAt(createdAt)
                    .build();

            orders.add(order);
        }
    }

    // 문자열 읽기 헬퍼 (Inner Length 읽고 문자열 추출)
    // 주의: 이 메서드는 커서를 이동시키지 않음. 호출자가 처리해야 함.
    private String readString(byte[] body, int cursor) {
        int len = Utils.bytesToInt(body, cursor);
        return new String(body, cursor + 4, len, StandardCharsets.UTF_8);
    }
}