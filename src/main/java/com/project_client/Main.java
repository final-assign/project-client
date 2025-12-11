package com.project_client;

import com.project_client.coupon.AllCouponRequestDTO;
import com.project_client.coupon.AllCouponResponseDTO;
import com.project_client.general.HeaderType;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.MenuRegisterRequestDTO;
import com.project_client.menu.MenuResponseDTO;
import com.project_client.order.OrderByRestaurantRequestDTO;
import com.project_client.order.OrderByUserRequestDTO;
import com.project_client.order.OrderByUserResponseDTO;
import com.project_client.order.OrderDetail;
import com.project_client.payment.StaffPaymentUI;
import com.project_client.payment.StudentPaymentUI;
import com.project_client.restaurant.Restaurant;
import com.project_client.restaurant.RestaurantOperatingInfo;
import com.project_client.user.UserType;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import static com.project_client.Utils.readString;

public class Main {

    static {

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
                } else {
                    selectUserMenu(dis, dos, loginResponseDTO.getUserType());
                }
            }
            case 0x02 -> { // Login Fail
                System.out.println("Invalid ID or Password");
                attemptLogin(dos);
            }
            case (byte) 0xA1 -> {
                printOrderList(data);
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
        int bodyLength = 8 + fileData.length;

        dos.writeByte(HeaderType.REQUEST.getValue());
        dos.writeByte((byte) 0x91); // Image Upload Request Code
        dos.write(Utils.intToBytes(bodyLength));
        dos.writeLong(menuId);
        dos.write(fileData);
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
            if (data.length > 0 && data[0] == 1) {
                System.out.println("사진 등록에 성공했습니다.");
            } else {
                System.out.println("사진 등록에 실패했습니다.");
            }
        } else {
            System.out.println("사진 등록 중 예상치 못한 응답을 받았습니다.");
        }
    }



    private static void attemptLogin(DataOutputStream dos) throws IOException {
        System.out.print("아이디를 입력하세요: ");
        String id = sc.nextLine();
        System.out.print("Input Password: ");
        String pw = sc.nextLine();

        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        byte[] pwBytes = pw.getBytes(StandardCharsets.UTF_8);

        int bodySize = 2 + idBytes.length + 2 + pwBytes.length;

        dos.writeByte(HeaderType.REQUEST.getValue());
        dos.writeByte(0x01); // Login Request Code
        dos.write(Utils.intToBytes(bodySize));
        dos.writeShort((short) (idBytes.length));
        dos.writeShort((short) (pwBytes.length));
        dos.write(idBytes);
        dos.write(pwBytes);
        dos.flush();
    }

    private static void selectUserMenu(DataInputStream dis, DataOutputStream dos, UserType userType) throws IOException {
        while (true) {
            String menu = """
                    ================================================
                    1) 메뉴 조회
                    2) 쿠폰 구매
                    3) 잔액 조회
                    4) 결제 내역 조회
                    0) 프로그램 종료
                    ================================================
                    >> 원하시는 번호를 입력하세요:\s""";

            System.out.print(menu);
            try {
                int choice = Integer.parseInt(sc.nextLine());
                switch (choice) {
                    case 1 -> {
                        if (userType.equals(UserType.STAFF))
                            handleStaffRestaurantShow(dis, dos, userType);
                        else
                            handleUserRestaurantShow(dis, dos, userType);
                    }
                    case 2 -> {
                        //쿠폰 구매 관련
                    }
                    case 3 -> {
                        //유저 잔액 조회 관련 (트랜잭션 쇼잉용)
                    }
                    case 4 -> {
                        showUserOrder(dis, dos);
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

    private static void handleUserRestaurantShow(DataInputStream dis, DataOutputStream dos, UserType userType) {
        boolean restaurantSelection = true;

        try {
            while (restaurantSelection) {
                System.out.println("===== 식당 선택 =====");
                System.out.println("식당을 선택하세요:");
                System.out.println("1. 교직원식당");
                System.out.println("2. 분식당");
                System.out.println("3. 학식당");
                System.out.print("번호 입력: ");

                int restaurantChoice = sc.nextInt();

                Long selectedRestaurantId = 0L;
                // 식당 ID 설정
                switch (restaurantChoice) {
                    case 1 -> selectedRestaurantId = 2L; // 교직원식당 ID
                    case 2 -> selectedRestaurantId = 3L;
                    case 3 -> selectedRestaurantId = 1L;// 분식당 ID
                    default -> {
                        System.out.println("잘못된 선택입니다.");
                        continue;
                    }
                }

                // 선택한 식당에 대한 처리
                if (restaurantChoice == 1 || restaurantChoice == 3) {
                    // 교직원식당, 학식당 처리
                    handleSimpleRestaurant(dis, dos, selectedRestaurantId, userType);
                } else
                    // 분식당 처리
                    handleSnackRestaurant(dis, dos, selectedRestaurantId, userType);

                restaurantSelection = false;

            }
        } catch (NumberFormatException e) {
            System.out.println("잘못된 숫자 형식입니다. 메뉴 선택으로 돌아갑니다.");
        } catch (DateTimeParseException e) {
            System.out.println("잘못된 날짜 형식입니다. (yyyy-MM-dd). 메뉴 선택으로 돌아갑니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleStaffRestaurantShow(DataInputStream dis, DataOutputStream dos, UserType userType) {

        boolean restaurantSelection = true;

        try {
            while (restaurantSelection) {
                System.out.println("===== 식당 선택 =====");
                System.out.println("식당을 선택하세요:");
                System.out.println("1. 교직원식당");
                System.out.println("2. 분식당");
                System.out.print("번호 입력: ");

                int restaurantChoice = sc.nextInt();

                Long selectedRestaurantId = 0L;
                // 식당 ID 설정
                switch (restaurantChoice) {
                    case 1 -> selectedRestaurantId = 2L; // 교직원식당 ID
                    case 2 -> selectedRestaurantId = 3L; // 분식당 ID
                    default -> {
                        System.out.println("잘못된 선택입니다.");
                        continue;
                    }
                }

                // 선택한 식당에 대한 처리
                if (restaurantChoice == 1) {
                    // 교직원식당 처리
                    handleSimpleRestaurant(dis, dos, selectedRestaurantId, userType);
                } else {
                    // 분식당 처리
                    handleSnackRestaurant(dis, dos, selectedRestaurantId, userType);
                }

                restaurantSelection = false;

            }
        } catch (NumberFormatException e) {
            System.out.println("잘못된 숫자 형식입니다. 메뉴 선택으로 돌아갑니다.");
        } catch (DateTimeParseException e) {
            System.out.println("잘못된 날짜 형식입니다. (yyyy-MM-dd). 메뉴 선택으로 돌아갑니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleSimpleRestaurant(DataInputStream dis, DataOutputStream dos,
                                               long restaurantId, UserType userType) throws Exception {
        String restaurantName = restaurantId == 2L ? "교직원식당" : "학식당";

        // 프로토콜: 0x01(요청) + 0x12(식당별 메뉴 요청) + Length + Body(식당 ID)
        byte[] restIdBytes = Utils.longToBytes(restaurantId);
        dos.writeByte(HeaderType.REQUEST.getValue()); // 0x01
        dos.writeByte((byte) 0x12); // 식당별 메뉴 요청
        dos.write(restIdBytes);
        dos.flush();

        // 응답 받기: Header(6 bytes) = Type(1) + Code(1) + Length(4)
        byte[] header = new byte[6];
        dis.readFully(header);
        byte type = header[0];
        byte code = header[1];
        int len = Utils.bytesToInt(header, 2);

        byte[] body = null;
        if (len > 0) {
            body = new byte[len];
            dis.readFully(body);
        }

        // 응답 검증: 0x02(응답) + 0x12(메뉴 데이터 반환)
        if (type != HeaderType.RESPONSE.getValue() || code != (byte) 0x12) {
            System.out.println("메뉴 조회 실패");
            return;
        }

        //서버의 MenuResponseDTO 클래스 사용
        MenuResponseDTO menuDTO = (body != null && body.length > 0)
                ? new MenuResponseDTO(body)
                : null;

        if (menuDTO == null) {
            System.out.println("메뉴 정보를 불러올 수 없습니다.");
            return;
        }

        // 메뉴 정보 출력
        System.out.println("\n===== 오늘의 " + restaurantName + " =====");

        for (int i = 0; i < menuDTO.getMenuList().size(); i++) {
            System.out.println("메뉴 ID : " + menuDTO.getMenuList().get(i).getMenuId());
            System.out.println("메뉴명: " + menuDTO.getMenuList().get(i).getMenuName());

            if (userType.equals(UserType.STUDENT))
                System.out.println("가격: " + menuDTO.getMenuList().get(i).getStudentPrice() + "원");
            else
                System.out.println("가격: " + menuDTO.getMenuList().get(i).getStandardPrice() + "원");

        }
        System.out.print("메뉴 ID를 입력하세요 : ");

        long selectedMenu = sc.nextLong();

        // 메뉴 이미지 다운로드 - 프로토콜 0x01 + 0x21
        saveImage(dos, dis, selectedMenu); // 서버 DTO에서 메뉴 ID 가져오기

        // 결제 여부 선택
        boolean paymentChoice = true;
        while (paymentChoice) {
            System.out.println("\n1. 결제");
            System.out.println("2. 취소");
            System.out.print("번호 입력: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            if (choice == 1) {
                // StaffPaymentUI 호출
                if(userType == UserType.STAFF)
                    StaffPaymentUI.processPayment(dis, dos, selectedMenu);
                else
                    StudentPaymentUI.processPayment(dis, dos, selectedMenu);
                paymentChoice = false;
            } else if (choice == 2) {
                System.out.println("처음으로 돌아갑니다.");
                return; // 식당 선택으로 돌아가기
            } else {
                System.out.println("잘못된 선택입니다.");
            }
        }
    }

    private static void handleSnackRestaurant(DataInputStream dis, DataOutputStream dos,
                                              long restaurantId, UserType userType) throws Exception {
        String restaurantName = "분식당";

        // 프로토콜: 0x01(요청) + 0x12(식당별 메뉴 요청) + Length + Body(식당 ID)
        byte[] restIdBytes = Utils.longToBytes(restaurantId);
        dos.writeByte(HeaderType.REQUEST.getValue()); // 0x01
        dos.writeByte((byte) 0x12); // 식당별 메뉴 요청
        dos.write(Utils.intToBytes(restIdBytes.length));
        dos.write(restIdBytes);
        dos.flush();

        // 응답 받기: Header(6 bytes) = Type(1) + Code(1) + Length(4)
        byte[] header = new byte[6];
        dis.readFully(header);
        byte type = header[0];
        byte code = header[1];
        int len = Utils.bytesToInt(header, 2);

        byte[] body = null;
        if (len > 0) {
            body = new byte[len];
            dis.readFully(body);
        }

        // 메뉴 선택
        //서버의 MenuResponseDTO 클래스 사용
        MenuResponseDTO menuDTO = (body != null && body.length > 0)
                ? new MenuResponseDTO(body)
                : null;

        if (menuDTO == null) {
            System.out.println("메뉴 정보를 불러올 수 없습니다.");
            return;
        }

        // 메뉴 정보 출력
        System.out.println("\n===== 오늘의 " + restaurantName + " =====");

        for (int i = 0; i < menuDTO.getMenuList().size(); i++) {
            System.out.println("메뉴 ID : " + menuDTO.getMenuList().get(i).getMenuId());
            System.out.println("메뉴명: " + menuDTO.getMenuList().get(i).getMenuName());

            if (userType.equals(UserType.STUDENT))
                System.out.println("가격: " + menuDTO.getMenuList().get(i).getStudentPrice() + "원");
            else
                System.out.println("가격: " + menuDTO.getMenuList().get(i).getStandardPrice() + "원");

        }

        System.out.print("메뉴 ID를 입력하세요 : ");

        long selectedMenu = sc.nextLong();

        // 메뉴 이미지 다운로드
        saveImage(dos, dis, selectedMenu);

        // 결제 여부 선택
        boolean paymentChoice = true;
        while (paymentChoice) {
            System.out.println("\n1. 결제");
            System.out.println("2. 취소");
            System.out.print("번호 입력: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            if (choice == 1) {
                // StaffPaymentUI 호출
                if(userType == UserType.STAFF)
                    StaffPaymentUI.processPayment(dis, dos, selectedMenu);
                else
                    StudentPaymentUI.processPayment(dis, dos, selectedMenu);

                paymentChoice = false;
            } else if (choice == 2) {
                System.out.println("처음으로 돌아갑니다.");
                return; // 식당 선택으로 돌아가기
            } else {
                System.out.println("잘못된 선택입니다.");
            }
        }
    }

    private static void saveImage(DataOutputStream dos, DataInputStream dis, Long menuId) throws IOException {
        System.out.println("입력한 메뉴 이미지 다운로드");

        dos.writeByte(HeaderType.REQUEST.getValue());
        dos.writeByte((byte) 0x21); //사진 다운로드
        dos.write(Utils.intToBytes(8));
        dos.write(Utils.longToBytes(menuId)); // 메뉴 id
        dos.flush();

        byte[] header = new byte[6];
        dis.readFully(header);
        byte type = header[0];
        byte code = header[1];
        int len = Utils.bytesToInt(header, 2);
        byte[] data = new byte[len];

        if (code != 0x21 || data.length == 0) {
            System.out.println(">> 수신된 이미지 데이터가 없습니다.");
            return;
        }

        // 파일명 생성: 덮어쓰기 방지를 위해 현재 시간(Timestamp)을 붙임
        String fileName = "downloaded_menu_" + System.currentTimeMillis() + ".jpg";

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(data);
            fos.flush();

            // 저장된 절대 경로 출력 안하면 내가 봇찾을듯...
            File file = new File(fileName);
            System.out.println(">> [성공] 이미지가 저장되었습니다!");
            System.out.println(">> 저장 위치: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.out.println(">> [실패] 이미지 저장 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    private static void showUserOrder(DataInputStream dis, DataOutputStream dos) throws IOException {
        System.out.println("""
                ================================================
                
                1) 사용자 주문 조회
                2) 사용자 쿠폰 조회
                
                ================================================""");
        int choice = sc.nextInt();

        sc.nextLine();

        try {
            switch (choice) {
                case 1 -> {
                    System.out.print("조회 시작일 (예: 2024-01-01): ");
                    String startAt = sc.nextLine();
                    System.out.print("조회 종료일 (예: 2025-01-31): ");
                    String endAt = sc.nextLine();
                    OrderByUserRequestDTO dto = new OrderByUserRequestDTO(startAt, endAt);

                    dos.write(dto.toBytes());
                    dos.flush();

                    byte[] header = new byte[6];
                    dis.readFully(header);
                    byte type = header[0];
                    byte code = header[1];
                    int len = Utils.bytesToInt(header, 2);

                    byte[] data = new byte[len];
                    // 받으면

                    if (len > 0) {
                        dis.readFully(data);
                    }

                    if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x41)
                        showUserOrderDetail(data);
                    else System.out.println("주문 조회에 실패하였습니다.");

                }
                case 2 -> {
                    AllCouponRequestDTO dto = new AllCouponRequestDTO();

                    dos.write(dto.toBytes());
                    dos.flush();

                    byte[] header = new byte[6];
                    dis.readFully(header);
                    byte type = header[0];
                    byte code = header[1];
                    int len = Utils.bytesToInt(header, 2);

                    byte[] data = new byte[len];

                    if (len > 0) {
                        dis.readFully(data);
                    }

                    if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x42)
                        showAllUserCoupon(data);
                    else System.out.println("쿠폰 조회에 실패하였습니다.");

                }
            }


        } catch (NumberFormatException | IOException e) {
            System.out.println("잘못된 입력입니다. 숫자를 입력해주세요.");
        }
    }

    private static void showAllUserCoupon(byte[] data) {
        AllCouponResponseDTO dto = new AllCouponResponseDTO(data);

        System.out.println(">>> 보유 쿠폰 목록");
        for (AllCouponResponseDTO.CouponDetail coupon : dto.getCoupons()) {
            System.out.printf("ID: %d | 메뉴: %s | 금액: %d원 | 수량: %d개\n",
                    coupon.getId(), coupon.getMenuName(), coupon.getPrice(), coupon.getQuantity());
        }
    }

    private static void showUserOrderDetail(byte[] data) {
        OrderByUserResponseDTO dto = new OrderByUserResponseDTO(data);

        System.out.println("\n>>> 총 " + dto.getOrders().size() + "건의 주문 내역");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-12s | %-15s | %-8s | %-8s | %-8s | %-10s | %s\n",
                "주문 내역 ID", "식당명", "메뉴명", "가격", "쿠폰", "유형", "상태", "주문일시");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");

        for (OrderDetail order : dto.getOrders()) {
            System.out.printf("%-10d | %-12s | %-15s | %-8d | %-8d | %-8s | %-10s | %s\n",
                    order.getId(),
                    order.getRestaurantName(),
                    order.getMenuName(),
                    order.getPrice(),
                    order.getCouponPrice(),
                    order.getPurchaseType(),
                    order.getStatus(),
                    order.getCreatedAt().toString().replace("T", " ")); // 보기 좋게 T 제거
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");
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
                        dos.writeByte(HeaderType.REQUEST.getValue());
                        dos.writeByte((byte) 0x80); // Restaurant List Request Code
                        dos.write(Utils.intToBytes(0)); // No body
                        dos.flush();

                        // Wait for response
                        byte[] header = new byte[6];
                        dis.readFully(header);
                        byte type = header[0];
                        byte code = header[1];
                        int len = Utils.bytesToInt(header, 2);
                        byte[] data = new byte[len];
                        if (len > 0) {
                            dis.readFully(data);
                        }

                        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x81) {
                            //handleMenuRegistration(dis, dos);
                        } else {
                            System.out.println("식당 목록을 불러오는데 실패했습니다. Code: " + code);
                        }
                    }
                    case 2, 3, 4, 5, 7 -> {
                        System.out.println("준비 중인 기능입니다.");
                    }
                    case 6 -> {
                        dos.writeByte(HeaderType.REQUEST.getValue());
                        dos.writeByte((byte) 0x81); // 식당 조회
                        dos.write(Utils.intToBytes(0)); // No body
                        dos.flush();

                        byte[] header = new byte[6];
                        dis.readFully(header);
                        byte type = header[0];
                        byte code = header[1];
                        int len = Utils.bytesToInt(header, 2);
                        byte[] data = new byte[len];

                        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x81)
                            handleRestaurantListAndRequestHistory(data, dos, dis);

                        else System.out.println("식당 목록을 불러오는데 실패했습니다. Code: " + code);
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

    private static void handleRestaurantListAndRequestHistory(byte[] body, DataOutputStream dos, DataInputStream
            dis) throws IOException {
        int cursor = 0;
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4;

        System.out.println("================ 식당 목록 ================");
        for (int i = 0; i < count; i++) {
            long rId = Utils.bytesToLong(body, cursor);
            cursor += 8;
            String rName = readString(body, cursor);
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

        byte[] header = new byte[6];
        dis.readFully(header);
        byte type = header[0];
        byte code = header[1];
        int len = Utils.bytesToInt(header, 2);
        byte[] data = new byte[len];

        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0xA1)
            printOrderList(data);

        else System.out.println("식당 목록을 불러오는데 실패했습니다. Code: " + code);
    }

    private static void printOrderList(byte[] data) {
        int cursor = 0;

        int bodySize = Utils.bytesToInt(data, cursor);
        cursor += 4;

        int count = Utils.bytesToInt(data, cursor);
        cursor += 4;

        System.out.println("\n>>> 총 " + count + "건의 주문 내역이 조회되었습니다.");
        System.out.println("----------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-10s | %-15s | %-8s | %-8s | %-10s | %-10s | %-20s\n",
                "주문 내역 ID", "학번/교번", "메뉴명", "결제금액", "쿠폰할인", "유형", "상태", "주문시간");
        System.out.println("----------------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < count; i++) {
            // 1. Order ID (Long) -> [Len 4] + [Val 8]
            int idLen = Utils.bytesToInt(data, cursor);
            cursor += 4;
            long id = Utils.bytesToLong(data, cursor);
            cursor += 8;

            // 2. School ID (String) -> [Len 4] + [String]
            String schoolId = readString(data, cursor);
            cursor += (4 + Utils.bytesToInt(data, cursor));

            // 3. Menu Name (String) -> [Len 4] + [String]
            String menuName = readString(data, cursor);
            cursor += (4 + Utils.bytesToInt(data, cursor));

            // 4. Payment Price (Int) -> [Len 4] + [Val 4]
            int priceLen = Utils.bytesToInt(data, cursor);
            cursor += 4;
            int price = Utils.bytesToInt(data, cursor);
            cursor += 4;

            // 5. Coupon Price (Int) -> [Len 4] + [Val 4]
            int couponPriceLen = Utils.bytesToInt(data, cursor);
            int couponPrice = Utils.bytesToInt(data, cursor);
            cursor += 4;

            // 6. Purchase Type (String)
            String pType = readString(data, cursor);
            cursor += (4 + Utils.bytesToInt(data, cursor));

            // 7. Status (String)
            String status = readString(data, cursor);
            cursor += (4 + Utils.bytesToInt(data, cursor));

            // 8. Created At (String)
            String date = readString(data, cursor);
            cursor += (4 + Utils.bytesToInt(data, cursor));

            System.out.printf("%-8d | %-10s | %-15s | %-8d | %-8d | %-10s | %-10s | %s\n",
                    id, schoolId, menuName, price, couponPrice, pType, status, date);
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------");
    }
}
