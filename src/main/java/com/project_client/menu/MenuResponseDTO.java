package com.project_client.menu;

import com.project_client.Utils;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MenuResponseDTO {
    private final List<MenuDetail> menuList;

    public MenuResponseDTO(byte[] body) {
        this.menuList = new ArrayList<>();
        int cursor = 0;

        // 1. 메뉴 개수 읽기 (4byte)
        // (서버 코드: cursor = Utils.intToBytes(listSize, res, cursor);)
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4;

        for (int i = 0; i < count; i++) {
            // [주의] 서버 코드가 Primitive Type 앞에 길이(4byte)를 안 붙이고 있음.
            // 따라서 바로 값을 읽어야 함.

            // 1. Menu ID (8 bytes)
            // (서버 코드: cursor = Utils.longToBytes(menu.getMenuId(), res, cursor);)
            long menuId = Utils.bytesToLong(body, cursor);
            cursor += 8;

            // 2. Menu Name (String) -> [Length 4] + [Bytes]
            // (서버 코드: cursor = Utils.stringToBytes(menu.getMenuName(), res, cursor);)
            String menuName = readString(body, cursor);
            // 문자열 길이(4byte) + 실제 데이터 길이만큼 커서 이동
            int nameLen = Utils.bytesToInt(body, cursor);
            cursor += (4 + nameLen);

            // 3. Standard Price (4 bytes)
            // (서버 코드: cursor = Utils.intToBytes(menu.getStandardPrice(), res, cursor);)
            int standardPrice = Utils.bytesToInt(body, cursor);
            cursor += 4;

            // 4. Student Price (4 bytes)
            // (서버 코드: cursor = Utils.intToBytes(studentPrice, res, cursor);)
            int studentPrice = Utils.bytesToInt(body, cursor);
            cursor += 4;

            menuList.add(new MenuDetail(menuId, menuName, standardPrice, studentPrice));
        }
    }

    // 문자열 읽기 헬퍼
    private String readString(byte[] body, int cursor) {
        int len = Utils.bytesToInt(body, cursor);
        return new String(body, cursor + 4, len, StandardCharsets.UTF_8);
    }

    @Getter
    @ToString
    public static class MenuDetail {
        private final long menuId;
        private final String menuName;
        private final int standardPrice;
        private final int studentPrice;

        public MenuDetail(long menuId, String menuName, int standardPrice, int studentPrice) {
            this.menuId = menuId;
            this.menuName = menuName;
            this.standardPrice = standardPrice;
            this.studentPrice = studentPrice;
        }
    }
}