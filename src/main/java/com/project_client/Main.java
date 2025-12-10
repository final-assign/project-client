package com.project_client;

import com.project_client.general.HeaderType;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.MenuRegisterRequestDTO;
import com.project_client.order.OrderByRestaurantRequestDTO;
import com.project_client.restaurant.Restaurant;
import com.project_client.restaurant.RestaurantListResponseDTO;
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


    private static void printOrderList(byte[] data) {
        System.out.println("주문 내역 출력 기능은 여기에 구현됩니다.");
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
        dos.writeShort(idBytes.length);
        dos.write(idBytes);
        dos.writeShort(pwBytes.length);
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
                    >> 원하시는 번호를 입력하세요: """;

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
                    case 2, 3, 4, 5, 6, 7 -> {
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
}
