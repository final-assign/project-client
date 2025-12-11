package com.project_client.menu;

import com.project_client.Utils;
import com.project_client.dto.*; // 서버에서 가져올 DTO들

import java.io.DataInputStream;
import java.io.DataOutputStream;

import static com.project_client.Main.sc;


private static void StaffMenuInquiryUI(DataInputStream dis, DataOutputStream dos) {
    try {
        long selectedRestaurantId = 0;

        // ===== 식당 선택 =====
        boolean restaurantSelection = true;
        while (restaurantSelection) {
            System.out.println("===== 식당 선택 =====");
            System.out.println("식당을 선택하세요:");
            System.out.println("1. 교직원식당");
            System.out.println("2. 분식당");
            System.out.print("번호 입력: ");

            int restaurantChoice;
            try {
                restaurantChoice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            // 식당 ID 설정
            switch (restaurantChoice) {
                case 1 -> selectedRestaurantId = 2; // 교직원식당 ID
                case 2 -> selectedRestaurantId = 3; // 분식당 ID
                default -> {
                    System.out.println("잘못된 선택입니다.");
                    continue;
                }
            }

            // 선택한 식당에 대한 처리
            if (restaurantChoice == 1) {
                // 교직원식당 처리
                handleSimpleRestaurant(dis, dos, selectedRestaurantId, restaurantChoice);
            } else if (restaurantChoice == 2) {
                // 분식당 처리
                handleSnackRestaurant(dis, dos, selectedRestaurantId);
            }

            restaurantSelection = false;
        }

        // ===== 결제 완료 후 다음 행동 선택 =====
        boolean nextActionMenu = true;
        while (nextActionMenu) {
            System.out.println("===== 다음 행동 선택 =====");
            System.out.println("1. 이용 내역 조회");
            System.out.println("2. 식당 선택");
            System.out.print("번호 입력: ");

            int nextChoice;
            try {
                nextChoice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("잘못된 입력입니다.");
                continue;
            }

            if (nextChoice == 1) {
                showUsageHistory(dis, dos); // 서버의 이용 내역 조회 UI 클래스 메서드 호출 예정
                return;
            } else if (nextChoice == 2) {
                // 처음 식당 선택으로 돌아가기
                StaffMenuInquiryUI(dis, dos);
                return;
            } else {
                System.out.println("잘못된 선택입니다.");
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("오류가 발생했습니다.");
    }
}

private static void handleStaffRestaurant(DataInputStream dis, DataOutputStream dos,
                                          long restaurantId, int restaurantChoice) throws Exception {

    // 프로토콜: 0x01(요청) + 0x12(식당별 메뉴 요청) + Length + Body(식당 ID)
    byte[] restIdBytes = Utils.longToBytes(restaurantId);
    dos.writeByte(PacketType.REQUEST.getValue()); // 0x01
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

    // 응답 검증: 0x02(응답) + 0x12(메뉴 데이터 반환)
    if (type != PacketType.RESPONSE.getValue() || code != (byte) 0x12) {
        System.out.println("메뉴 조회 실패");
        return;
    }

    // DTO 파싱 - 서버의 MenuResponseDTO 클래스 사용 (변경 필요)
    MenuResponseDTO menuDTO = (body != null && body.length > 0)
            ? MenuResponseDTO.fromBytes(body)
            : null;

    if (menuDTO == null) {
        System.out.println("메뉴 정보를 불러올 수 없습니다.");
        return;
    }

    // 메뉴 정보 출력
    System.out.println("\n===== 오늘의 " + restaurantName + " =====");
    System.out.println("메뉴명: " + menuDTO.getMenuName()); // 서버 DTO에서 메뉴명 가져오기
    System.out.println("가격: " + menuDTO.getstandardPrice() + "원");

    // 메뉴 이미지 다운로드 - 프로토콜 0x01 + 0x21
    downloadMenuImage(dis, dos, menuDTO.getMenuId()); // 서버 DTO에서 메뉴 ID 가져오기

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
            StaffPaymentUI.processPayment(dis, dos, selectedMenu);
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
                                          long restaurantId) throws Exception {

    System.out.println("===== 코너를 선택하시오 =====");
    System.out.println("1. 라면/찌개류");
    System.out.println("2. 돈가스/덮밥류");
    System.out.print("번호 입력: ");

    int cornerChoice;
    try {
        cornerChoice = Integer.parseInt(sc.nextLine());
    } catch (NumberFormatException e) {
        System.out.println("잘못된 입력입니다.");
        return;
    }

    if (cornerChoice < 1 || cornerChoice > 2) {
        System.out.println("잘못된 선택입니다.");
        return;
    }

    // 프로토콜: 0x01(요청) + 0x12(식당별 메뉴 요청) + Length + Body(식당 ID + 코너 정보)
    byte[] restIdBytes = Utils.longToBytes(restaurantId);
    byte[] cornerBytes = Utils.intToBytes(cornerChoice);
    byte[] requestBody = new byte[restIdBytes.length + cornerBytes.length];
    System.arraycopy(restIdBytes, 0, requestBody, 0, restIdBytes.length);
    System.arraycopy(cornerBytes, 0, requestBody, restIdBytes.length, cornerBytes.length);

    dos.writeByte(PacketType.REQUEST.getValue()); // 0x01
    dos.writeByte((byte) 0x12); // 식당별 메뉴 요청
    dos.write(Utils.intToBytes(requestBody.length));
    dos.write(requestBody);
    dos.flush();

    // 응답 받기
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

    // 응답 검증
    if (type != PacketType.RESPONSE.getValue() || code != (byte) 0x12) {
        System.out.println("메뉴 조회 실패");
        return;
    }

    // DTO 파싱 - 서버의 MenuListResponseDTO 클래스 사용
    MenuListResponseDTO menuListDTO = (body != null && body.length > 0)
            ? MenuListResponseDTO.fromBytes(body)
            : null;

    if (menuListDTO == null || menuListDTO.getMenuList() == null || menuListDTO.getMenuList().isEmpty()) {
        System.out.println("메뉴 정보를 불러올 수 없습니다.");
        return;
    }

    // 메뉴 선택
    System.out.println("\n===== 메뉴를 선택하시오 =====");
    int index = 1;
    for (MenuDTO menu : menuListDTO.getMenuList()) { // 직렬화한걸 역직렬화한 후 결과를 넘버로 보여주기
        System.out.println(index + ". " + menu.getMenuName() + " (" + menu.getPrice() + "원)");
        index++;
    }
    System.out.print("번호 입력: ");

    int menuChoice;
    try {
        menuChoice = Integer.parseInt(sc.nextLine());
    } catch (NumberFormatException e) {
        System.out.println("잘못된 입력입니다.");
        return;
    }

    if (menuChoice < 1 || menuChoice > menuListDTO.getMenuList().size()) {
        System.out.println("잘못된 선택입니다.");
        return;
    }

    // 선택한 메뉴 정보
    MenuDTO selectedMenu = menuListDTO.getMenuList().get(menuChoice - 1);

    System.out.println("\n===== 선택한 메뉴 =====");
    System.out.println("메뉴명: " + selectedMenu.getMenuName());
    System.out.println("가격: " + selectedMenu.getPrice() + "원");

    // 메뉴 이미지 다운로드
    downloadMenuImage(dis, dos, selectedMenu.getMenuId());

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
            StaffPaymentUI.processPayment(dis, dos, selectedMenu);
            paymentChoice = false;
        } else if (choice == 2) {
            System.out.println("처음으로 돌아갑니다.");
            return; // 식당 선택으로 돌아가기
        } else {
            System.out.println("잘못된 선택입니다.");
        }
    }
}
    }

private static void downloadMenuImage(DataInputStream dis, DataOutputStream dos, long menuId) throws Exception {
    // 프로토콜: 0x01(요청) + 0x21(이미지 요청) + Length + Body(메뉴 ID)
    byte[] menuIdBytes = Utils.longToBytes(menuId);
    dos.writeByte(PacketType.REQUEST.getValue()); // 0x01
    dos.writeByte((byte) 0x21); // 이미지 요청
    dos.write(Utils.intToBytes(menuIdBytes.length));
    dos.write(menuIdBytes);
    dos.flush();

    // 응답 받기
    byte[] header = new byte[6];
    dis.readFully(header);
    byte type = header[0];
    byte code = header[1];
    int len = Utils.bytesToInt(header, 2);

    if (type == PacketType.RESPONSE.getValue() && code == (byte) 0x21 && len > 0) {
        byte[] imageData = new byte[len];
        dis.readFully(imageData);

        // 이미지 데이터 처리
        System.out.println("[메뉴 사진 다운로드 완료]");
        // 이미지를 파일로 저장하거나 UI에 표시하는 로직 추가
    } else {
        System.out.println("[메뉴 사진을 불러올 수 없습니다]");
    }
}

private static void processPayment(DataInputStream dis, DataOutputStream dos, MenuResponseDTO menuDTO) throws Exception {
    // 결제 UI 클래스 메서드 호출
    // PaymentUI.showPaymentScreen(dis, dos, menuDTO);
    System.out.println("\n[결제 화면으로 이동합니다...]");

    // 임시: 결제 완료로 가정
    System.out.println("결제가 완료되었습니다.");
}

private static void showUsageHistory(DataInputStream dis, DataOutputStream dos) throws Exception {
    // T이용 내역 UI 클래스 메서드 호출
    // HistoryUI.showHistory(dis, dos);
    System.out.println("\n[이용 내역 화면으로 이동합니다...]");
}

// PacketType Enum (없다면 추가)
enum PacketType {
    REQUEST((byte) 0x01),
    RESPONSE((byte) 0x02),
    RESULT((byte) 0x03);

    private final byte value;

    PacketType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
