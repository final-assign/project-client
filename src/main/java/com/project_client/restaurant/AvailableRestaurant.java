package com.project_client.restaurant;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AvailableRestaurant {
    private final long id;
    private final String name;
    private final String description;

    public AvailableRestaurant(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
