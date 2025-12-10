package com.project_client.order;

import com.project_client.Utils;
import java.nio.charset.StandardCharsets;

public class OrderByRestaurantRequestDTO {
    private final long restaurantId;
    private final String startAt;
    private final String endAt;

    public OrderByRestaurantRequestDTO(long restaurantId, String startAt, String endAt) {
        this.restaurantId = restaurantId;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public byte[] toBytes() {
        // 문자열 변환
        byte[] startBytes = startAt.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = endAt.getBytes(StandardCharsets.UTF_8);

        // Body 크기 계산
        // 식당ID : 길이 4 + 값 8 = 12
        // startAt : 길이 4 + 데이터
        // endAt : 길이 4 + 데이터
        int bodySize = 12 + (4 + startBytes.length) + (4 + endBytes.length);

        byte[] data = new byte[bodySize];
        int cursor = 0;

        // 식당 ID 쓰기
        // - 길이 (8)
        System.arraycopy(Utils.intToBytes(8), 0, data, cursor, 4);
        cursor += 4;
        // - 값 (restaurantId)
        System.arraycopy(Utils.longToBytes(restaurantId), 0, data, cursor, 8);
        cursor += 8;

        // 2. 시작일시 쓰기
        // - 길이
        System.arraycopy(Utils.intToBytes(startBytes.length), 0, data, cursor, 4);
        cursor += 4;
        // - 값
        System.arraycopy(startBytes, 0, data, cursor, startBytes.length);
        cursor += startBytes.length;

        // 3. 종료일시 쓰기
        // - 길이
        System.arraycopy(Utils.intToBytes(endBytes.length), 0, data, cursor, 4);
        cursor += 4;
        // - 값
        System.arraycopy(endBytes, 0, data, cursor, endBytes.length);

        return data;
    }
}