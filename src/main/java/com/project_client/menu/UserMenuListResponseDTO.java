package com.project_client.menu;

import com.project_client.Utils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
public class UserMenuListResponseDTO {

    private final List<MenuDTO> list = new ArrayList<>();

    // 서버에서 받은 body 데이터를 파싱
    public UserMenuListResponseDTO(byte[] body) {
        int cursor = 0;
        int length = body.length;

        // 리스트 개수(Count) 정보가 없고, 바디 끝까지 읽는 방식입니다.
        while (cursor < length) {

            // 1) 메뉴 ID (Long: 8 bytes)
            long id = Utils.bytesToLong(body, cursor);
            cursor += 8;

            // 2) 이름 길이 (Int: 4 bytes)
            int nameLen = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 3) 이름 (String: N bytes)
            String menuName = new String(body, cursor, nameLen, StandardCharsets.UTF_8);
            cursor += nameLen;

            // 4) Price (Int: 4 bytes)
            int price = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 5) Amount (Int: 4 bytes)
            int amount = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 6) IsDailyMenu (Int: 4 bytes)
            int isDailyMenu = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 리스트에 추가
            list.add(MenuDTO.builder()
                    .id(id)
                    .menuName(menuName)
                    .price(price)
                    .amount(amount)
                    .isDailyMenu(isDailyMenu)
                    .build());
        }
    }
}