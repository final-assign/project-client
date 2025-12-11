package com.project_client.menu;

import com.project_client.Utils;
import com.project_client.general.HeaderType;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Builder
public class MenuRegisterRequestDTO {

    private final String restaurantName;
    private final String menuName;
    private final Integer standardPrice;
    private final Integer studentPrice;
    private final Integer defaultAmount;
    private final LocalDate startSalesAt;
    private final LocalDate endSalesAt;
    private final Boolean isDailyMenu; // [추가됨] 상시/오늘의 메뉴 여부
    private final Long menuTypeId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] toBytes() {
        // 1. 문자열 및 날짜 데이터 바이트 변환
        byte[] restNameBytes = restaurantName.getBytes(StandardCharsets.UTF_8);
        byte[] menuNameBytes = menuName.getBytes(StandardCharsets.UTF_8);
        byte[] startSalesAtBytes = startSalesAt != null ? startSalesAt.format(DATE_FORMATTER).getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] endSalesAtBytes = endSalesAt != null ? endSalesAt.format(DATE_FORMATTER).getBytes(StandardCharsets.UTF_8) : new byte[0];

        // 2. boolean -> byte 변환 (true면 1, false면 0)
        byte isDailyMenuByte = (byte) (Boolean.TRUE.equals(isDailyMenu) ? 1 : 0);

        // 3. Body Size 계산
        // 기존 필드들 + isDailyMenu(1바이트) 추가
        int bodySize = 2 + restNameBytes.length  // 식당 이름
                + 2 + menuNameBytes.length
                + 4 + 4 + 4
                + 2 + (startSalesAtBytes.length)
                + 2 + (endSalesAtBytes.length)
                + 1 // isDailyMenu
                + 8; // menuTypeId

        byte[] body = new byte[bodySize];
        int offset = 0;

        // 4. 데이터 쓰기
        offset = Utils.shortToBytes((short) restNameBytes.length, body, offset);
        System.arraycopy(restNameBytes, 0, body, offset, restNameBytes.length);
        offset += restNameBytes.length;

        offset = Utils.shortToBytes((short) menuNameBytes.length, body, offset);
        System.arraycopy(menuNameBytes, 0, body, offset, menuNameBytes.length);
        offset += menuNameBytes.length;

        offset = Utils.intToBytes(standardPrice, body, offset);
        offset = Utils.intToBytes(studentPrice, body, offset);
        offset = Utils.intToBytes(defaultAmount, body, offset);

        offset = Utils.shortToBytes((short) startSalesAtBytes.length, body, offset);
        System.arraycopy(startSalesAtBytes, 0, body, offset, startSalesAtBytes.length);
        offset += startSalesAtBytes.length;

        offset = Utils.shortToBytes((short) endSalesAtBytes.length, body, offset);
        System.arraycopy(endSalesAtBytes, 0, body, offset, endSalesAtBytes.length);
        offset += endSalesAtBytes.length;

        // [추가됨] isDailyMenu 쓰기
        body[offset++] = isDailyMenuByte;

        // 마지막 필드 쓰기
        Utils.longToBytes(menuTypeId, body, offset);

        // Header 생성 및 합치기
        int headerSize = 6;
        byte[] packet = new byte[headerSize + bodySize];

        packet[0] = HeaderType.REQUEST.getValue();
        packet[1] = (byte) 0x81; // Menu Register Request Code
        System.arraycopy(Utils.intToBytes(bodySize), 0, packet, 2, 4);

        // Copy body to packet
        System.arraycopy(body, 0, packet, headerSize, bodySize);

        return packet;
    }
}