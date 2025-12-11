package com.project_client.menu;

import com.project_client.Utils;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class MenuListResponseDTO {

    private final ArrayList<MenuInfoDTO> menuList;

    public MenuListResponseDTO(byte[] bodyData) {
        this.menuList = new ArrayList<>();
        if (bodyData.length == 0) return;

        int[] offsetRef = {0};

        // 개수 읽기
        int listSize = Utils.bytesToInt(bodyData, offsetRef[0]);
        offsetRef[0] += 4;

        // 루프 돌며 복원
        for (int i = 0; i < listSize; i++) {
            MenuInfoDTO info = new MenuInfoDTO(bodyData, offsetRef);
            menuList.add(info);
        }
    }
}
