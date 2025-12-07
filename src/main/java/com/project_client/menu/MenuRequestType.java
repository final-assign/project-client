package com.project_client.menu;

import lombok.Getter;

@Getter
public enum MenuRequestType {
    REST_REQ(0x80),
    MENU_ADD(0x81),
    MENU_UPDATE(0x82),
    SEND_MENU_DETAILS(0x83),
    UPLOAD_CSV(0x88);

    private final byte value;

    MenuRequestType(int value) {

        this.value = (byte) value;
    }
}