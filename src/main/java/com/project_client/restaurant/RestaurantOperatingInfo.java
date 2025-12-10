package com.project_client.restaurant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RestaurantOperatingInfo {
    private final long id;
    private final long restaurantId;
    private final String startAt;
    private final String endAt;
    private final MenuType menuType;
}
