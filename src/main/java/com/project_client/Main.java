package com.project_client;

import com.project_client.general.HeaderType;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.*;
import com.project_client.order.OrderByRestaurantRequestDTO;
import com.project_client.restaurant.Restaurant;
import com.project_client.restaurant.RestaurantListResponseDTO;
import com.project_client.restaurant.RestaurantName;
import com.project_client.restaurant.RestaurantOperatingInfo;
import com.project_client.user.UserType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    static{

        try {
            System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final static String HOST = "localhost";
    private static final int PORT = 5000;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket cliSocket = new Socket(HOST, PORT);
             DataInputStream dis = new DataInputStream(cliSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(cliSocket.getOutputStream())) {

            attemptLogin(dos);

            byte[] header = new byte[6];
            while (true) {
                dis.readFully(header);
                byte type = header[0];
                byte code = header[1];
                int len = Utils.bytesToInt(header, 2);
                byte[] data = new byte[len];
                if (len > 0) {
                    dis.readFully(data);
                }

                if (type == HeaderType.RESPONSE.getValue()) {
                    readResponse(dis, dos, code, data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readResponse(DataInputStream dis, DataOutputStream dos, byte code, byte[] data) throws IOException {
        switch (code) {
            case 0x01 -> { // Login Success
                System.out.print("Login Success. Type: ");
                LoginResponseDTO loginResponseDTO = new LoginResponseDTO(data);
                System.out.println(loginResponseDTO.getUserType());

                if (loginResponseDTO.getUserType() == UserType.ADMIN) {
                    selectAdminMenu(dis, dos);
                }
            }
            case 0x02 -> { // Login Fail
                System.out.println("Invalid ID or Password");
                attemptLogin(dos);
            }
            case (byte) 0xA1 -> {
                printOrderList(data);
                // selectAdminMenu is now a loop, so we don't call it here if we are already in it.
                // But readResponse is called from main loop.
                // If we are here, it means we are not in selectAdminMenu loop (e.g. async notification?)
                // Or this case is now unreachable if selectAdminMenu handles everything.
            }
            default -> {
                System.out.println("Unknown response code: " + code);
            }
        }
    }

    private static void handleImageUpload(DataInputStream dis, DataOutputStream dos, long menuId) throws IOException {
        System.out.println("사진의 위치를 입력하세요 (현재 위치: " + new File("").getAbsolutePath() + "):");
        String filePathStr = sc.nextLine();
        Path path = Paths.get(filePathStr);

        if (!Files.exists(path)) {
            System.out.println("파일이 존재하지 않습니다.");
            return;
        }

        byte[] fileData = Files.readAllBytes(path);

        ImageUploadRequestDTO requestDTO = ImageUploadRequestDTO.builder()
                .menuId(menuId)
                .fileData(fileData)
                .build();

        dos.write(requestDTO.toBytes());
        dos.flush();

        // Wait for image upload response
        byte[] header = new byte[6];
        dis.readFully(header);
        byte type = header[0];
        byte code = header[1];
        int len = Utils.bytesToInt(header, 2);
        byte[] data = new byte[len];
        if (len > 0) {
            dis.readFully(data);
        }

        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x92) {
            System.out.println("사진 등록에 성공했습니다.");
        } else {
            System.out.println("사진 등록 중 예상치 못한 응답을 받았습니다.");
        }
    }

    private static void handleMenuRegistration(DataInputStream dis, DataOutputStream dos) throws IOException {
        try {
            // 1. 식당 선택
            System.out.println("\n>> 메뉴 정보 등록 및 수정을 할 식당을 선택하세요.");
            RestaurantName[] restaurants = RestaurantName.values();
            int i = 1;
            for (RestaurantName name : restaurants) {
                System.out.printf("[%d] %s\n", i++, name.getValue());
            }
            System.out.print("선택: ");
            int restIndex = Integer.parseInt(sc.nextLine()) - 1;
            RestaurantName selectedRestaurant = restaurants[restIndex];
            String selectedRestName = selectedRestaurant.name();

            // 2. 작업 선택
            System.out.println("\n>> 작업을 선택하세요.");
            System.out.println("1. 메뉴 등록");
            System.out.println("2. 메뉴 수정");
            System.out.print("선택: ");
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 1) { // [등록 로직]

                int menuTypeInput = 2; // 기본값: 오늘의 메뉴

                // [식당별 타입 설정] 분식당만 상시/오늘의 메뉴 선택 가능
                if (selectedRestaurant == RestaurantName.SNACK_CAFETERIA) {
                    System.out.println("\n>> 등록할 메뉴의 타입을 선택하세요.");
                    System.out.println("1. 상시 메뉴 (라면, 김밥 등)");
                    System.out.println("2. 오늘의 메뉴 (변동 메뉴)");
                    System.out.print("선택: ");
                    menuTypeInput = Integer.parseInt(sc.nextLine());
                } else {
                    System.out.println("\n>> [" + selectedRestaurant.getValue() + "]은(는) '오늘의 메뉴'만 등록 가능합니다.");
                }

                LocalDate startSalesAt = null;
                LocalDate endSalesAt = null;
                long menuTypeId = 0;

                // [오늘의 메뉴 설정] 날짜 및 시간대 입력
                if (menuTypeInput == 2) {
                    // 시간대 선택
                    if (selectedRestaurant == RestaurantName.STUDENT_CAFETERIA) {
                        System.out.println("\n>> 제공할 시간대를 선택하세요.");
                        System.out.println("1. 조식");
                        System.out.println("2. 중식");
                        System.out.print("선택: ");
                        menuTypeId = Long.parseLong(sc.nextLine());
                        if (menuTypeId != 1 && menuTypeId != 2) return;

                    } else if (selectedRestaurant == RestaurantName.STAFF_CAFETERIA) {
                        System.out.println("\n>> 제공할 시간대를 선택하세요.");
                        System.out.println("2. 중식");
                        System.out.println("3. 석식");
                        System.out.print("선택: ");
                        menuTypeId = Long.parseLong(sc.nextLine());
                        if (menuTypeId != 2 && menuTypeId != 3) return;
                    } else {
                        menuTypeId = 0; // 분식당
                    }

                    // 날짜 입력
                    System.out.print("판매 시작일을 입력하세요 (yyyy-MM-dd): ");
                    String startDateStr = sc.nextLine();
                    startSalesAt = LocalDate.parse(startDateStr);

                    System.out.print("판매 종료일을 입력하세요 (엔터 시 시작일과 동일): ");
                    String endDateStr = sc.nextLine();
                    if (endDateStr.trim().isEmpty()) {
                        endSalesAt = startSalesAt;
                    } else {
                        endSalesAt = LocalDate.parse(endDateStr);
                    }
                }

                // 3. 메뉴 정보 입력 (다중 라인 입력 적용)
                System.out.println("\n--- 메뉴 세부 정보 입력 ---");

                // ==========================================================
                // [핵심 변경] 여러 줄 입력 받아 하나로 합치기
                // ==========================================================
                System.out.println("메뉴 구성을 입력하세요. (한 줄에 반찬 하나씩 입력, 입력이 끝나면 빈 줄에서 Enter)");
                System.out.println("예시:\n현미밥\n된장국\n돈까스\n(엔터)");

                StringBuilder menuNameBuilder = new StringBuilder();
                while (true) {
                    System.out.print("> ");
                    String line = sc.nextLine();

                    // 빈 줄을 입력하면 입력 종료
                    if (line.trim().isEmpty()) {
                        if (menuNameBuilder.isEmpty()) {
                            System.out.println("최소 한 줄은 입력해야 합니다.");
                            continue;
                        }
                        break;
                    }

                    // 기존 내용이 있으면 구분자( / ) 추가
                    if (!menuNameBuilder.isEmpty()) {
                        menuNameBuilder.append(" / ");
                    }
                    menuNameBuilder.append(line);
                }

                String finalMenuName = menuNameBuilder.toString();
                System.out.println(">> 입력된 메뉴 구성: " + finalMenuName);
                // ==========================================================


                int standardPrice = 0;
                int studentPrice = 0;

                // [가격 입력] 분식당만 입력
                if (selectedRestaurant == RestaurantName.SNACK_CAFETERIA) {
                    System.out.print("표준 가격: ");
                    standardPrice = Integer.parseInt(sc.nextLine());
                    System.out.print("학생 가격: ");
                    studentPrice = Integer.parseInt(sc.nextLine());
                } else {
                    standardPrice = 0;
                    studentPrice = 0;
                }

                System.out.print("하루 판매 수량: ");
                int defaultAmount = Integer.parseInt(sc.nextLine());

                boolean isDaily = (menuTypeInput == 2);

                // DTO 생성 및 전송
                MenuRegisterRequestDTO requestDTO = MenuRegisterRequestDTO.builder()
                        .restaurantName(selectedRestName)
                        .menuName(finalMenuName) // [변경] 합쳐진 문자열 전송
                        .standardPrice(standardPrice)
                        .studentPrice(studentPrice)
                        .defaultAmount(defaultAmount)
                        .startSalesAt(startSalesAt)
                        .endSalesAt(endSalesAt)
                        .isDailyMenu(isDaily)
                        .menuTypeId(menuTypeId)
                        .build();

                dos.write(requestDTO.toBytes());
                dos.flush();

                // 응답 수신
                byte[] header = new byte[6];
                dis.readFully(header);
                byte type = header[0];
                byte code = header[1];
                int len = Utils.bytesToInt(header, 2);
                byte[] data = new byte[len];
                if (len > 0) dis.readFully(data);

                if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x87) {
                    long menuId = Utils.bytesToLong(data, 0);
                    System.out.println(">> 메뉴 등록 성공!");
                    System.out.print("   사진을 등록하시겠습니까? (Y/N): ");
                    if (sc.nextLine().equalsIgnoreCase("Y")) {
                        handleImageUpload(dis, dos, menuId);
                    }
                } else {
                    System.out.println(">> 메뉴 등록 실패.");
                }

            } else if (choice == 2) {
                System.out.println(">> 메뉴 수정 기능은 준비 중입니다.");
            }

        } catch (NumberFormatException e) {
            System.out.println(">> [오류] 숫자 형식이 아닙니다.");
        } catch (DateTimeParseException e) {
            System.out.println(">> [오류] 날짜 형식이 잘못되었습니다.");
        }
    }

    private static void printOrderList(byte[] data) {
        System.out.println("주문 내역 출력 기능은 여기에 구현됩니다.");
    }

    private static void attemptLogin(DataOutputStream dos) throws IOException {
        System.out.print("아이디를 입력하세요: ");
        String id = sc.nextLine();
        System.out.print("비밀번호를 입력하세요: ");
        String pw = sc.nextLine();

        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        byte[] pwBytes = pw.getBytes(StandardCharsets.UTF_8);

        int bodySize = 2 + idBytes.length + 2 + pwBytes.length;

        dos.writeByte(HeaderType.REQUEST.getValue());
        dos.writeByte(0x01); // Login Request Code
        dos.write(Utils.intToBytes(bodySize));
        dos.writeShort((short)(idBytes.length));
        dos.writeShort((short)(pwBytes.length));
        dos.write(idBytes);
        dos.write(pwBytes);
        dos.flush();
    }

    private static void selectAdminMenu(DataInputStream dis, DataOutputStream dos) throws IOException {
        while (true) {
            String menu = """
                    ================================================
                    1) 메뉴 정보 등록 및 수정
                    2) 메뉴 이미지 업로드 (메뉴 등록 후 진행)
                    3) 식당별 메뉴 가격 책정
                    4) 쿠폰 정책 관리
                    5) 결제 처리 시스템
                    6) 전체 주문 및 결제 내역 통합 조회
                    7) 매출 현황 및 식당별 이용 통계 분석
                    0) 프로그램 종료
                    ================================================
                    >> 원하시는 번호를 입력하세요:\s""";

            System.out.print(menu);
            try {
                int choice = Integer.parseInt(sc.nextLine());
                switch (choice) {
                    case 1 -> {
                        handleMenuRegistration(dis, dos);
                    }
                    case 3 -> {

                        handleCouponAdd(dis, dos);
                    }

                    case 2, 4, 5, 6, 7 -> {
                        System.out.println("준비 중인 기능입니다.");
                    }
                    case 0 -> System.exit(0);
                    default -> {
                        System.out.println("잘못된 입력입니다.");
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }

    private static void handleRestaurantListAndRequestHistory(byte[] body, DataOutputStream dos) throws IOException {
        // This method seems to be for another feature, keeping it for now.
        int cursor = 0;
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4;

        System.out.println("================ 식당 목록 ================");
        for (int i = 0; i < count; i++) {
            long rId = Utils.bytesToLong(body, cursor);
            cursor += 8;
            String rName = Utils.readString(body, cursor);
            cursor += (4 + rName.getBytes(StandardCharsets.UTF_8).length);
            System.out.printf("[%d] %s\n", rId, rName);
        }
        System.out.println("=========================================");

        System.out.print("\n조회할 식당 ID를 입력하세요: ");
        long targetId = Long.parseLong(sc.nextLine());
        System.out.print("조회 시작일 (예: 2024-01-01): ");
        String startAt = sc.nextLine();
        System.out.print("조회 종료일 (예: 2024-01-31): ");
        String endAt = sc.nextLine();

        byte[] bodyData = new OrderByRestaurantRequestDTO(targetId, startAt, endAt).toBytes();
        dos.writeByte(HeaderType.REQUEST.getValue());
        dos.writeByte((byte) 0xA2);
        dos.write(Utils.intToBytes(bodyData.length));
        dos.write(bodyData);
        dos.flush();
        System.out.println("[Client] 내역 조회 요청 전송 완료.");
    }

    private static void handleCouponAdd(DataInputStream dis, DataOutputStream dos) throws IOException {
        try {
            // ============================================================
            // 1. 식당 선택 & 금액권 모드 진입
            // ============================================================
            System.out.println("\n>> 쿠폰을 등록할 식당을 선택하세요.");
            RestaurantName[] restaurants = RestaurantName.values();

            int i = 1;
            for (RestaurantName name : restaurants) {
                System.out.printf("[%d] %s\n", i++, name.getValue());
            }
            System.out.println("[0] 금액권 (식당 지정 후 금액 입력)"); // 0번 옵션 추가
            System.out.print("선택: ");

            int firstChoice = Integer.parseInt(sc.nextLine());

            RestaurantName selectedRestaurant = null;
            boolean isAmountCouponMode = false;

            if (firstChoice == 0) {
                // [0] 금액권을 선택한 경우 -> 식당을 다시 지정해야 함 (서버에 라인을 알려주기 위해)
                isAmountCouponMode = true;
                System.out.println("\n>> 어느 식당의 금액권을 발행하시겠습니까?");
                int j = 1;
                for (RestaurantName name : restaurants) {
                    System.out.printf("[%d] %s\n", j++, name.getValue());
                }
                System.out.print("식당 선택: ");
                int restIndex = Integer.parseInt(sc.nextLine()) - 1;

                if (restIndex < 0 || restIndex >= restaurants.length) {
                    System.out.println(">> [오류] 잘못된 선택입니다.");
                    return;
                }
                selectedRestaurant = restaurants[restIndex];

            } else {
                // [1~3] 특정 식당을 바로 선택한 경우
                int restIndex = firstChoice - 1;
                if (restIndex < 0 || restIndex >= restaurants.length) {
                    System.out.println(">> [오류] 잘못된 선택입니다.");
                    return;
                }
                selectedRestaurant = restaurants[restIndex];
            }

            // ============================================================
            // 2. 서버에 식당 정보 전송 (컨텍스트 설정)
            // ============================================================
            // 금액권이라도 서버가 "어느 라인"인지 알아야 하므로 메뉴 리스트를 요청하는 척 정보를 보냅니다.
            String selectedRestName = selectedRestaurant.name();
            System.out.println(">> [" + selectedRestaurant.getValue() + "] 데이터 조회 중...");

            MenuListRequestDTO requestDTO = new MenuListRequestDTO(selectedRestName);
            dos.write(requestDTO.toBytes());
            dos.flush();

            // 응답 읽기 (버퍼 비우기 및 메뉴 정보 확보)
            byte[] header = new byte[6];
            dis.readFully(header);
            int len = Utils.bytesToInt(header, 2);
            byte[] data = new byte[len];
            if (len > 0) {
                dis.readFully(data);
            }

            // 메뉴 리스트 응답 파싱
            MenuListResponseDTO responseDTO = new MenuListResponseDTO(data);

            // ============================================================
            // 3. 쿠폰 정보 입력 (모드에 따라 분기)
            // ============================================================
            Long finalMenuId = 0L;
            int finalAmount = 0;

            if (isAmountCouponMode) {
                // --- A. 금액권 모드 (이미 0번을 골라서 들어옴) ---
                System.out.println("-----------------------------------------------");
                System.out.printf(">> [%s] 금액권 생성\n", selectedRestaurant.getValue());
                System.out.print(">> 발행할 금액(원)을 입력하세요: ");
                try {
                    finalAmount = Integer.parseInt(sc.nextLine());
                    if (finalAmount <= 0) {
                        System.out.println(">> [오류] 금액은 0보다 커야 합니다.");
                        return;
                    }
                    finalMenuId = 0L; // 금액권은 ID 0
                } catch (NumberFormatException e) {
                    System.out.println(">> [오류] 숫자를 입력해주세요.");
                    return;
                }

            } else {
                // --- B. 일반 메뉴 선택 모드 ---
                if (responseDTO.getMenuList().isEmpty()) {
                    System.out.println(">> 등록된 메뉴가 없습니다.");
                    return;
                }

                System.out.println("\n----------------- [메뉴 목록] -----------------");
                for (MenuInfoDTO menu : responseDTO.getMenuList()) {
                    String dateInfo = menu.isDailyMenu() ? (" [" + menu.getServedDate() + "]") : " [상시]";
                    System.out.printf("[%d] %s | %d원%s\n",
                            menu.getMenuId(), menu.getMenuName(), menu.getPrice(), dateInfo);
                }
                System.out.println("-----------------------------------------------");

                System.out.print(">> 쿠폰으로 만들 메뉴 번호를 입력하세요: ");
                try {
                    finalMenuId = Long.parseLong(sc.nextLine());

                    // 유효한 메뉴인지 확인 (스트림 대신 for문 사용)
                    boolean exists = false;
                    for (MenuInfoDTO m : responseDTO.getMenuList()) {
                        if (m.getMenuId().equals(finalMenuId)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        System.out.println(">> [오류] 목록에 없는 메뉴 번호입니다.");
                        return;
                    }

                    finalAmount = 0; // 메뉴권은 금액 0 (서버 가격 정책 따름)

                } catch (NumberFormatException e) {
                    System.out.println(">> [오류] 숫자를 입력해주세요.");
                    return;
                }
            }

            // ============================================================
            // 4. 쿠폰 생성 요청 전송 (ID, Amount만 전송)
            // ============================================================
            CouponRegisterRequestDTO registerReq = new CouponRegisterRequestDTO(finalMenuId, finalAmount);
            dos.write(registerReq.toBytes());
            dos.flush();

            System.out.println(">> 쿠폰 생성 요청을 보냈습니다.");

        } catch (NumberFormatException e) {
            System.out.println(">> [오류] 잘못된 입력입니다.");
        }
    }
}
