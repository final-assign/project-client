package com.project_client.restaurant;

import com.project_client.Utils;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

import static com.project_client.Utils.readString;

@Getter
public class AvailableRestaurantResponseDTO {
    private final List<AvailableRestaurant> list;

    public AvailableRestaurantResponseDTO(byte[] body) {
        this.list = new ArrayList<>();
        int cursor = 0;
        int bodyLength = body.length;

        // 리스트 개수 정보가 없으므로, 바디 끝까지 반복해서 읽음
        while (cursor < bodyLength) {

            // 1. ID (Long) - [주의] 서버가 길이 정보 없이 값만 보냄
            // (서버 코드: offset = Utils.longToBytes(dto.getId(), result, offset);)
            long id = Utils.bytesToLong(body, cursor);
            cursor += 8;

            // 2. Name (String) - [Length 4] + [Bytes]
            String name = readString(body, cursor);
            int nameLen = Utils.bytesToInt(body, cursor);
            cursor += (4 + nameLen);

            // 3. Description (String) - [Length 4] + [Bytes]
            String description = readString(body, cursor);
            int descLen = Utils.bytesToInt(body, cursor);
            cursor += (4 + descLen);

            list.add(new AvailableRestaurant(id, name, description));
        }
    }

}