package com.project_client.order;

import com.project_client.Utils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@Builder
@RequiredArgsConstructor
public class OrderByUserRequestDTO {
    private final String startAt;
    private final String endAt;

    public byte[] toBytes() {
        // 문자열 변환
        byte[] startBytes = startAt.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = endAt.getBytes(StandardCharsets.UTF_8);

        // [수정 1] Body 크기 계산 오류 수정
        // 식당 ID(12)는 이 요청에 포함되지 않으므로 제거해야 함
        int bodySize = (4 + startBytes.length) + (4 + endBytes.length);

        byte[] data = new byte[bodySize + 6];

        data[0] = 0x01; // req
        data[1] = (byte) 0x41; // Code (사용자 주문 내역 조회 코드 확인 필요)

        int cursor = 2;

        // Body Length 기록
        // Utils.intToBytes는 '다음 커서 위치'를 반환하므로 '='를 써야 함
        cursor = Utils.intToBytes(bodySize, data, cursor);

        // 1. 시작일시 쓰기
        // - 길이
        // [수정 2] '+=' 대신 '=' 사용
        cursor = Utils.intToBytes(startBytes.length, data, cursor);
        // - 값
        System.arraycopy(startBytes, 0, data, cursor, startBytes.length);
        cursor += startBytes.length;

        // 2. 종료일시 쓰기
        // - 길이
        // [수정 2] '+=' 대신 '=' 사용
        cursor = Utils.intToBytes(endBytes.length, data, cursor);
        // - 값
        System.arraycopy(endBytes, 0, data, cursor, endBytes.length);

        return data;
    }
}