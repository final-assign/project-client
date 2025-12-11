package com.project_client.order;

import com.project_client.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import static com.project_client.Main.sc;

private static void StatisticsUI(DataInputStream dis, DataOutputStream dos) {
    try {
        long selectedRestaurantId = 0;

        // ===== 식당 선택 =====
        boolean restaurantSelectionMenu = true;
        while (restaurantSelectionMenu) {
            System.out.println("===== 식당 매출 조회 =====");
            System.out.println("식당을 선택하세요:");
            System.out.println("1. 학생식당");
            System.out.println("2. 교직원식당");
            System.out.println("3. 분식당");
            System.out.print("번호 입력: ");

            int restaurantChoice;
            try {
                restaurantChoice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue; // 식당 선택지로 돌아가기
            }

            // 식당 ID 설정
            switch (restaurantChoice) {
                case 1 -> selectedRestaurantId = 1; // 학생식당 ID
                case 2 -> selectedRestaurantId = 2; // 교직원식당 ID
                case 3 -> selectedRestaurantId = 3; // 분식당 ID
                default -> {
                    System.out.println("잘못된 선택입니다.");
                    continue; // 식당 선택지로 돌아가기
                }
            }

            dos.writeByte(PacketType.REQUEST.getValue());
            dos.writeByte((byte) 0x81);  // 식당 아이디 조회 - 식당 조회
            dos.write(Utils.intToBytes(0));
            dos.flush();

            // 식당 목록 응답 받기
            byte[] header = new byte[6];
            dis.readFully(header);
            byte type = header[0];
            byte code = header[1];
            int len = Utils.bytesToInt(header, 2);
            byte[] data = null;
            if (len > 0) {
                data = new byte[len];
                dis.readFully(data);
            }

            if (type != PacketType.RESPONSE.getValue() || code != (byte) 0x81) {
                System.out.println("식당 목록 조회 실패");
                continue; // 식당 선택지로 돌아가기
            }

            RestaurantListResponseDTO listDTO = (data != null && data.length > 0)
                    ? RestaurantListResponseDTO.fromBytes(data)
                    : null;

            if (listDTO == null || listDTO.getList() == null || listDTO.getList().isEmpty()) {
                System.out.println("등록된 식당이 없습니다.");
                continue; // 식당 선택지로 돌아가기
            }

            // 식당 선택 성공
            restaurantSelectionMenu = false;
        }

        // ===== 통계 항목 선택 메뉴 =====
        boolean statisticsMenu = true;
        while (statisticsMenu) {
            System.out.println("조회할 통계 항목을 선택하세요:");
            System.out.println("1. 총매출");
            System.out.println("2. 일자별 매출");
            System.out.println("3. 시간대별 매출");
            System.out.print("번호 입력: ");

            int statChoice;
            try {
                statChoice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            if (statChoice < 1 || statChoice > 3) {
                System.out.println("잘못된 선택입니다.");
                continue;
            }

            byte[] restIdBytes = Utils.longToBytes(selectedRestaurantId);
            dos.writeByte(PacketType.REQUEST.getValue());
            dos.writeByte((byte) 0xB1); // 매출 통계 조회
            dos.write(Utils.intToBytes(restIdBytes.length));
            dos.write(restIdBytes);
            dos.flush();

            // 응답 받기
            byte[] header = new byte[6];
            dis.readFully(header);
            byte responseType = header[0];
            byte responseCode = header[1];
            int responseLen = Utils.bytesToInt(header, 2);

            if (responseType != PacketType.RESPONSE.getValue() || responseCode != (byte) 0xB1) {
                System.out.println("매출 통계 조회 실패!");
                continue; // 통계 선택지로 돌아가기
            }

            byte[] body = new byte[responseLen];
            if (responseLen > 0) {
                dis.readFully(body);
            }

            RestaurantSalesDTO dto = RestaurantSalesDTO.fromBytes(body);

            // 통계 결과 출력
            System.out.println("\n===== 통계 결과 =====");
            switch (statChoice) {
                case 1 -> { // 총매출
                    if (dto.getOverallSales() != null) {
                        RestaurantSalesDTO.OverallSales os = dto.getOverallSales();
                        System.out.printf("식당명: %s\n", dto.getRestaurantName());
                        System.out.printf("총 주문 수: %d건\n", os.getOrderCount());
                        System.out.printf("총 매출: %,d원\n", os.getSalesAmount());
                        System.out.printf("평균 객단가: %,d원\n", os.getAvgOrderPrice());
                    } else {
                        System.out.println("총매출 데이터가 없습니다.");
                    }
                }
                case 2 -> { // 일자별
                    System.out.printf("식당명: %s\n", dto.getRestaurantName());
                    System.out.println("[일자별 매출]");
                    if (dto.getDailySalesList() != null && !dto.getDailySalesList().isEmpty()) {
                        for (RestaurantSalesDTO.DailySales ds : dto.getDailySalesList()) {
                            System.out.printf("%s  | %3d건  | %,8d원\n",
                                    ds.getDate(),
                                    ds.getOrderCount(),
                                    ds.getSalesAmount());
                        }
                    } else {
                        System.out.println("일자별 매출 데이터 없음");
                    }
                }
                case 3 -> { // 시간대별
                    System.out.printf("식당명: %s\n", dto.getRestaurantName());
                    System.out.println("[시간대별 매출]");
                    if (dto.getTimeSalesList() != null && !dto.getTimeSalesList().isEmpty()) {
                        for (RestaurantSalesDTO.TimeSales ts : dto.getTimeSalesList()) {
                            System.out.printf("%s  | %3d건  | %,8d원\n",
                                    ts.getTimeRange(),
                                    ts.getOrderCount(),
                                    ts.getSalesAmount());
                        }
                    } else {
                        System.out.println("시간대별 매출 데이터 없음");
                    }
                }
            }

            // 통계 출력
            statisticsMenu = false;
        }

        // ===== 다음 행동 선택 =====
        boolean nextActionMenu = true;
        while (nextActionMenu) {
            System.out.println("\n===== 다음 행동 선택 =====");
            System.out.println("1. 전체 메뉴로 돌아가기");
            System.out.println("2. 다른 식당 매출 조회하기");
            System.out.print("번호 입력: ");

            int nextChoice;
            try {
                nextChoice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            if (nextChoice == 1) {
                // 전체 메뉴로 돌아가기
                return;
            } else if (nextChoice == 2) {
                // 다른 식당 매출 조회
                StatisticsUI(dis, dos);
                return;
            } else {
                System.out.println("잘못된 선택입니다.");
                continue; // 다음 행동 선택지로 돌아가기
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("매출 통계 조회 중 오류 발생");
    }
}