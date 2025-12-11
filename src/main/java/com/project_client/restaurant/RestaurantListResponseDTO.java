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

            int nameLen = Utils.bytesToInt(data, offset);
            String nameStr = Utils.readString(data, offset);
            offset += 4 + nameLen;
            RestaurantName name = RestaurantName.valueOf(nameStr);

            int descLen = Utils.bytesToInt(data, offset);
            String description = Utils.readString(data, offset);
            offset += 4 + descLen;

            Restaurant restaurant = new Restaurant(id, name, description, new ArrayList<>());

            int operatingInfosSize = Utils.bytesToInt(data, offset);
            offset += 4;

            for (int j = 0; j < operatingInfosSize; j++) {
                long infoId = Utils.bytesToLong(data, offset);
                offset += 8;

                long restaurantId = Utils.bytesToLong(data, offset);
                offset += 8;

                int startLen = Utils.bytesToInt(data, offset);
                String startAt = Utils.readString(data, offset);
                offset += 4 + startLen;

                int endLen = Utils.bytesToInt(data, offset);
                String endAt = Utils.readString(data, offset);
                offset += 4 + endLen;

                long menuTypeId = Utils.bytesToLong(data, offset);
                offset += 8;

                int menuTypeLen = Utils.bytesToInt(data, offset);
                String menuTypeName = Utils.readString(data, offset);
                offset += 4 + menuTypeLen;

                MenuType menuType = new MenuType(menuTypeId, menuTypeName);
                RestaurantOperatingInfo operatingInfo = new RestaurantOperatingInfo(infoId, restaurantId, startAt, endAt, menuType);
                restaurant.getOperatingInfos().add(operatingInfo);
            }
            list.add(restaurant);
        }
    }
}