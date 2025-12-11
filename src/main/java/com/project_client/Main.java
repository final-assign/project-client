package com.project_client;

import com.project_client.coupon.AllCouponRequestDTO;
import com.project_client.coupon.AllCouponResponseDTO;
import com.project_client.general.HeaderType;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.MenuRegisterRequestDTO;
import com.project_client.order.OrderByRestaurantRequestDTO;
import com.project_client.order.OrderByUserRequestDTO;
import com.project_client.order.OrderByUserResponseDTO;
import com.project_client.order.OrderDetail;
import com.project_client.restaurant.Restaurant;
import com.project_client.restaurant.RestaurantListResponseDTO;
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
                } else if (loginResponseDTO.getUserType() == UserType.STUDENT) {
                    selectUserMenu(dis, dos);
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

    private static void handleMenuRegistration(DataInputStream dis, DataOutputStream dos, RestaurantListResponseDTO dto) throws IOException {
        try {
            System.out.println(">> 메뉴 정보 등록 및 수정을 할 식당을 선택하세요. : ");
            for (Restaurant r : dto.getList()) {
                System.out.printf("[%d] %s\n", r.getId(), r.getName().getValue());
            }
            long restId = Long.parseLong(sc.nextLine());

            System.out.println(">> 등록은 1, 수정은 2를 누르세요. : ");
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 1) { // 등록
                Restaurant selectedRestaurant = dto.getList().stream().filter(r -> r.getId() == restId).findFirst().orElse(null);
                if (selectedRestaurant == null) {
                    System.out.println("잘못된 식당 ID입니다.");
                    return;
                }

                System.out.println("식당 운영시간을 선택하세요. : ");
                for (RestaurantOperatingInfo info : selectedRestaurant.getOperatingInfos()) {
                    System.out.printf("[%d] %s (%s ~ %s)\n", info.getMenuType().getId(), info.getMenuType().getName(), info.getStartAt(), info.getEndAt());
                }
                long menuTypeId = Long.parseLong(sc.nextLine());

                System.out.println("메뉴명을 입력하세요. : ");
                String menuName = sc.nextLine();
                System.out.println("표준 가격을 입력하세요. : ");
                int standardPrice = Integer.parseInt(sc.nextLine());
                System.out.println("학생 가격을 입력하세요. : ");
                int studentPrice = Integer.parseInt(sc.nextLine());
                System.out.println("하루에 판매할 수량을 입력하세요. : ");
                int defaultAmount = Integer.parseInt(sc.nextLine());

                System.out.println("시작 판매일을 입력하세요 (yyyy-MM-dd 형식, 상시 판매는 X) : ");
                String startDateStr = sc.nextLine();
                LocalDate startSalesAt = startDateStr.equalsIgnoreCase("X") ? null : LocalDate.parse(startDateStr);

                System.out.println("종료 판매일을 입력하세요 (yyyy-MM-dd 형식, 상시 또는 하루만 판매는 X) : ");
                String endDateStr = sc.nextLine();
                LocalDate endSalesAt = endDateStr.equalsIgnoreCase("X") ? null : LocalDate.parse(endDateStr);

                MenuRegisterRequestDTO requestDTO = MenuRegisterRequestDTO.builder()
                        .restId(restId)
                        .menuTypeId(menuTypeId)
                        .menuName(menuName)
                        .standardPrice(standardPrice)
                        .studentPrice(studentPrice)
                        .defaultAmount(defaultAmount)
                        .startSalesAt(startSalesAt)
                        .endSalesAt(endSalesAt)
                        .build();

                byte[] body = requestDTO.toBytes();
                dos.writeByte(HeaderType.REQUEST.getValue());
                dos.writeByte((byte) 0x82); // Menu Register Request Code
                dos.write(Utils.intToBytes(body.length));
                dos.write(body);
                dos.flush();

                // Wait for menu registration response
                byte[] header = new byte[6];
                dis.readFully(header);
                byte type = header[0];
                byte code = header[1];
                int len = Utils.bytesToInt(header, 2);
                byte[] data = new byte[len];
                if (len > 0) {
                    dis.readFully(data);
                }

                if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x87) {
                    long menuId = Utils.bytesToLong(data, 0);
                    System.out.println("메뉴 등록에 성공했습니다. 사진을 등록하시겠습니까? Y, N:");
                    String uploadChoice = sc.nextLine();
                    if (uploadChoice.equalsIgnoreCase("Y")) {
                        handleImageUpload(dis, dos, menuId);
                    }
                } else {
                    System.out.println("메뉴 등록에 실패했습니다.");
                }

            } else { // 수정
                System.out.println("메뉴 수정 기능은 준비 중입니다.");
            }
        } catch (NumberFormatException e) {
            System.out.println("잘못된 숫자 형식입니다. 메뉴 선택으로 돌아갑니다.");
        } catch (DateTimeParseException e) {
            System.out.println("잘못된 날짜 형식입니다. (yyyy-MM-dd). 메뉴 선택으로 돌아갑니다.");
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

    private static void selectUserMenu(DataInputStream dis, DataOutputStream dos) throws IOException {
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
                        dos.writeByte(HeaderType.REQUEST.getValue());
                        dos.writeByte((byte) 0x11); // 식당 리스트
                        dos.write(Utils.intToBytes(0)); // No body
                        dos.flush();

                        // 받으면
                        byte[] header = new byte[6];
                        dis.readFully(header);
                        byte type = header[0];
                        byte code = header[1];
                        int len = Utils.bytesToInt(header, 2);
                        byte[] data = new byte[len];
                        if (len > 0) {
                            dis.readFully(data);
                        }

                        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x11) {
                            RestaurantListResponseDTO dto = new RestaurantListResponseDTO(data);
                            //얘를 재활용 하던, 서버에서 하나 더 만들어서 하던 해야할거 같아요. 학생/교직원 따른 거름망 때문에...
                            //임시 코드라서 일단 재활용
                            handleRestaurantShow(dis, dos, dto);
                        } else {
                            System.out.println("식당 목록을 불러오는데 실패했습니다. Code: " + code);
                        }
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

    private static void handleRestaurantShow(DataInputStream dis, DataOutputStream dos, RestaurantListResponseDTO dto) {

        //1. 이 목록에서 레스토랑 id를 하드코딩해서 거르기.
        //학생 -> 분식당, 학생식당, 교식당
        //교직원 -> 분식당, 교직원식당
        //2. 아니면 줄때 서버에서 걸러서 목록을 주기.
        //필요없나?????
        try {
            System.out.println(">> 메뉴를 조회 할 식당을 선택하세요. : ");
            for (Restaurant r : dto.getList()) {
                System.out.printf("[%d] %s\n", r.getId(), r.getName().getValue());
            }

            System.out.println("0) : 뒤로가기");

            long restId;
            restId = Long.parseLong(sc.nextLine());


            dos.writeByte(HeaderType.REQUEST.getValue());
            dos.writeByte((byte) 0x12); //식당 id 반환
            dos.write(Utils.longToBytes(restId)); // 식당 id
            dos.flush();
            //서버단에서도 userType 체크 필요

            // 받으면
            byte[] header = new byte[6];
            dis.readFully(header);
            byte type = header[0];
            byte code = header[1];
            int len = Utils.bytesToInt(header, 2);
            byte[] data = new byte[len];
            if (len > 0) {
                dis.readFully(data);
            }

            if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x13) {
                //ResponseDTO 제조 필요
                //메뉴 보여줌
                //메뉴 쇼잉까지 구현 가정
                Long menuId = 0L;
                handleMenuShow(dis, dos, menuId); //제조한 dto도 끌고 와야함

            } else {
                System.out.println("식당 ID 조회에 실패하였습니다. Code: " + code);
            }

        } catch (NumberFormatException e) {
            System.out.println("잘못된 숫자 형식입니다. 메뉴 선택으로 돌아갑니다.");
        } catch (DateTimeParseException e) {
            System.out.println("잘못된 날짜 형식입니다. (yyyy-MM-dd). 메뉴 선택으로 돌아갑니다.");
        } catch (IOException e) {
            throw new RuntimeException(e);

        }
    }

    private static void handleMenuShow(DataInputStream dis, DataOutputStream dos, Long menuId) { //제조한 dto 끌고 와야함..
        while (true) {
            System.out.println("""
                    ================================================
                    1) 메뉴 이미지 다운로드
                    2) 선택한 메뉴 구매
                    0) 뒤로 가기
                    ================================================
                    >> 원하시는 번호를 입력하세요:\s""");

            try {
                int choice = Integer.parseInt(sc.nextLine());
                switch (choice) {
                    case 1 -> {
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

                        if (type == HeaderType.RESPONSE.getValue() && code == (byte) 0x21)
                            saveImage(data);
                        else System.out.println("사진 다운로드에 실패하였습니다.");
                    }
                    case 2 -> {
                        //메뉴 구매 구현 필요
                        System.out.println("준비 중인 기능입니다.");
                    }
                    case 0 -> {
                        return;
                    }
                    default -> {
                        System.out.println("잘못된 입력입니다.");
                    }
                }
            } catch (NumberFormatException | IOException e) {
                System.out.println("잘못된 입력입니다. 숫자를 입력해주세요.");
            }
        }
    }

    private static void saveImage(byte[] imageData) throws IOException {
        if (imageData == null || imageData.length == 0) {
            System.out.println(">> 수신된 이미지 데이터가 없습니다.");
            return;
        }

        // 파일명 생성: 덮어쓰기 방지를 위해 현재 시간(Timestamp)을 붙임
        String fileName = "downloaded_menu_" + System.currentTimeMillis() + ".jpg";

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(imageData);
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
                            RestaurantListResponseDTO dto = new RestaurantListResponseDTO(data);
                            handleMenuRegistration(dis, dos, dto);
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

    private static void handleRestaurantListAndRequestHistory(byte[] body, DataOutputStream dos, DataInputStream dis) throws IOException {
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
