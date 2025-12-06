package com.project_client.login;

import com.project_client.user.UserType;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LoginResponseDTO {

    UserType userType;

    public LoginResponseDTO(byte[] bytes){

        String str = new String(bytes);
        userType = UserType.valueOf(str);
    }
}
