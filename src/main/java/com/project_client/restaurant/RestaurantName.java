package com.project_client.restaurant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantName {
    STUDENT_CAFETERIA("학생식당"), STAFF_CAFETERIA("교직원식당"), SNACK_CAFETERIA("분식당");

    private final String value;
}
