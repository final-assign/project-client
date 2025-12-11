package com.project_client.menu;

import com.project_client.Utils;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class MenuInfoDTO {

    private Long menuId;
    private String menuName;
    private int price;
    private boolean isDailyMenu;
    private String servedDate;

    public MenuInfoDTO(byte[] data, int[] offsetRef) {
        int offset = offsetRef[0]; // 현재 읽어야 할 위치 가져오기

        // 1. ID 읽기
        this.menuId = Utils.bytesToLong(data, offset);
        offset += 8;

        // 2. Name 읽기
        short nameLen = Utils.bytesToShort(data, offset);
        offset += 2;
        this.menuName = new String(data, offset, nameLen, StandardCharsets.UTF_8);
        offset += nameLen;

        // 3. Price 읽기
        this.price = Utils.bytesToInt(data, offset);
        offset += 4;

        // 4. Is Daily 읽기
        this.isDailyMenu = (data[offset] == 1);
        offset += 1;

        // 5. Date 읽기
        short dateLen = Utils.bytesToShort(data, offset);
        offset += 2;
        if (dateLen > 0) {
            this.servedDate = new String(data, offset, dateLen, StandardCharsets.UTF_8);
            offset += dateLen;
        } else {
            this.servedDate = null; // 날짜 없으면 null 처리
        }

        // 읽은 만큼 오프셋 업데이트 (중요!)
        offsetRef[0] = offset;
    }
}
