package com.project_client.restaurant;

import com.project_client.Utils;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class RestaurantListResponseDTO {
    private final ArrayList<Restaurant> list = new ArrayList<>();

    public RestaurantListResponseDTO(byte[] data) {
        int offset = 0;

        int size = Utils.bytesToInt(data, offset);
        offset += 4;

        for (int i = 0; i < size; i++) {
            long id = Utils.bytesToLong(data, offset);
            offset += 8;

            String nameStr = Utils.readString(data, offset);
            offset += 4 + nameStr.getBytes().length;
            RestaurantName name = RestaurantName.valueOf(nameStr);

            String description = Utils.readString(data, offset);
            offset += 4 + description.getBytes().length;

            Restaurant restaurant = new Restaurant(id, name, description, new ArrayList<>());

            int operatingInfosSize = Utils.bytesToInt(data, offset);
            offset += 4;

            for (int j = 0; j < operatingInfosSize; j++) {
                long infoId = Utils.bytesToLong(data, offset);
                offset += 8;

                long restaurantId = Utils.bytesToLong(data, offset);
                offset += 8;

                String startAt = Utils.readString(data, offset);
                offset += 4 + startAt.getBytes().length;

                String endAt = Utils.readString(data, offset);
                offset += 4 + endAt.getBytes().length;

                long menuTypeId = Utils.bytesToLong(data, offset);
                offset += 8;

                String menuTypeName = Utils.readString(data, offset);
                offset += 4 + menuTypeName.getBytes().length;

                MenuType menuType = new MenuType(menuTypeId, menuTypeName);
                RestaurantOperatingInfo operatingInfo = new RestaurantOperatingInfo(infoId, restaurantId, startAt, endAt, menuType);
                restaurant.getOperatingInfos().add(operatingInfo);
            }
            list.add(restaurant);
        }
    }
}
