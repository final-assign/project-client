package com.project_client.menu;

import com.project_client.Utils;
import com.project_client.general.HeaderType;
import com.project_client.general.RequestDTO;
import com.project_client.restaurant.RestaurantName;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class MenuListRequestDTO implements RequestDTO {

    final private String restaurantName;

    public byte[] toBytes() {
        // 1. 데이터(Body) 준비
        byte[] nameBytes = restaurantName.getBytes(StandardCharsets.UTF_8);

        // Body 크기 = 식당이름길이필드(2byte) + 실제이름바이트
        int bodySize = 2 + nameBytes.length;

        // 2. 전체 패킷 생성 (Header 6byte + Body)
        // Header: Type(1) + Code(1) + Length(4) = 6 bytes
        byte[] packet = new byte[6 + bodySize];

        // ---------------------------------------------------------
        // [Header 작성]
        // ---------------------------------------------------------
        packet[0] = HeaderType.REQUEST.getValue(); // 0x01
        packet[1] = (byte) 0xC2;                   // Code (요청하신 코드)

        // Body Length (4byte)
        System.arraycopy(Utils.intToBytes(bodySize), 0, packet, 2, 4);

        // ---------------------------------------------------------
        // [Body 작성]
        // ---------------------------------------------------------
        int offset = 6; // 헤더 다음부터 시작

        // 식당 이름 길이 (2byte) 쓰기
        offset = Utils.shortToBytes((short) nameBytes.length, packet, offset);

        // 식당 이름 본문 쓰기
        System.arraycopy(nameBytes, 0, packet, offset, nameBytes.length);

        return packet;
    }
}
