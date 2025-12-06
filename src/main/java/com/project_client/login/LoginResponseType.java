package com.project_client.login;

import lombok.Getter;

@Getter
public enum LoginResponseType{ //애초에 보낼 데이터가 없으니 enum으로 두는 것. 꼭 이렇게 코딩할 필요 없음.

    SUCCESS((byte) 0x01), FAILURE((byte) 0x02), DISCONNECTED((byte) 0x03);

    private final Byte value;

    LoginResponseType(Byte b){

        value = b;
    }
}
