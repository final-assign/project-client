package com.project_client.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MenuDTO {
    private long id;
    private String menuName;
    private int price;
    private int amount;
    private int isDailyMenu; // 1: true, 0: false

    // 편의 메서드 (boolean 변환)
    public boolean isDaily() {
        return this.isDailyMenu == 1;
    }
}