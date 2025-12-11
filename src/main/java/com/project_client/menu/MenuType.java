package com.project_client.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MenuType {

    BREAKFAST(1),
    LUNCH(2),
    DINNER(3);

    private final int value;
}
