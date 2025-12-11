package com.project_client.order;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
    private Long id;
    private String menuName;
    private String restaurantName;
    private int couponPrice;
    private String purchaseType;
    private String status;
    private LocalDateTime createdAt;
    private int price;
}
