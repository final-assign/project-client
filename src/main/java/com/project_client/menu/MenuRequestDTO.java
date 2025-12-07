package com.project_client.menu;

import com.project_client.general.RequestDTO;
import lombok.Builder;

@Builder
public class MenuRequestDTO implements RequestDTO {

    final private MenuRequestType requestType;

    public byte[] toBytes(){

        byte[] header = new byte[6];
        header[0] = 0x01;
        header[1] = requestType.getValue();

        return header;
    }
}
