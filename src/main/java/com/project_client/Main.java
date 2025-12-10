package com.project_client;

import com.project_client.general.RequestDTO;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.MenuRequestDTO;
import com.project_client.menu.MenuRequestType;
import com.project_client.order.OrderByRestaurantRequestDTO;
import com.project_client.user.UserType;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static com.project_client.Utils.readString;

public class Main {

    private final static String HOST = "localhost";
    private static final int PORT = 5000;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Socket cliSocket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        byte[] data = null;
        try {
            cliSocket = new Socket(HOST, PORT);
            dis = new DataInputStream(cliSocket.getInputStream());
            dos = new DataOutputStream(cliSocket.getOutputStream());

            byte[] header = new byte[6];

            while (true) {
                dis.readFully(header);

                byte type = header[0];
                byte code = header[1];
                int len = Utils.bytesToInt(header, 2);

                if (type == 0x01)
                    attemptLogin(dos);

                else if (type == 0x02) {
                    read(dos, dis, code, header, data);

                } else break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cliSocket != null) cliSocket.close();
            } catch (Exception e) {
            }
        }
    }

    private static void read(DataOutputStream dos, DataInputStream dis, byte code, byte[] header, byte[] data) throws IOException {
        switch (code) {
            case 0x01 -> {
                System.out.print("Login Success. Type: ");

                data = new byte[Utils.bytesToInt(header, 2)];
                dis.readFully(data);

                LoginResponseDTO loginResponseDTO = new LoginResponseDTO(data);
                System.out.println(loginResponseDTO.getUserType());

                if (loginResponseDTO.getUserType() == UserType.ADMIN)
                    selectAdminMenu(dos);
                //inputMenu .....
            }
            case 0x02 -> {

                System.out.println("Invalid ID or Password");
                attemptLogin(dos);
            }

            case (byte) 0xA1 -> {
                //내 기능만 생각.. 나중에 choice 같은 변수 선언 해서 해야 할 듯..
                printOrderList(data);

                selectAdminMenu(dos);
            }
        }
    }

    private static void printOrderList(byte[] data) {

    }

    private static void attemptLogin(DataOutputStream dos) throws IOException {

        System.out.print("Input ID: ");
        String id = sc.nextLine();
        System.out.print("Input Password: ");
        String pw = sc.nextLine();

        byte[] idBytes = id.getBytes();
        byte[] pwBytes = pw.getBytes();

        short idLen = (short) idBytes.length;
        short pwLen = (short) pwBytes.length;

        int bodySize = 2 + 2 + idBytes.length + pwBytes.length;

        dos.writeByte(1);
        dos.writeByte(1);
        dos.write(Utils.intToBytes(bodySize));

        dos.write(Utils.shortToBytes(idLen));
        dos.write(Utils.shortToBytes(pwLen));
        dos.write(idBytes);
        dos.write(pwBytes);

        dos.flush();
    }

    private static void selectAdminMenu(DataOutputStream dos) throws IOException {


        String menu = """
                ================================================
                1) 메뉴 정보 등록 및 수정
                2) 메뉴 이미지 업로드
                3) 식당별 메뉴 가격 책정
                4) 쿠폰 정책 관리
                5) 결제 처리 시스템
                6) 전체 주문 및 결제 내역 통합 조회
                7) 매출 현황 및 식당별 이용 통계 분석
                0) 프로그램 종료
                ================================================
                >> 원하시는 번호를 입력하세요: """;

        System.out.print(menu);
        int choice = sc.nextInt();

        RequestDTO requestDTO = null;
        switch (choice) {
            case 1 -> {

                requestDTO = MenuRequestDTO.builder().requestType(MenuRequestType.REST_REQ).build();
            }
            case 2 -> {

            }
            case 3 -> {
            }
            case 4 -> {
            }
            case 5 -> {
            }
            case 6 -> {

            }
            case 7 -> {
            }
            case 0 -> {
            }
        }

        dos.write(requestDTO.toBytes());
        dos.flush();
    }

    private static void handleRestaurantListAndRequestHistory(byte[] body, DataOutputStream dos) throws IOException {
        int cursor = 0;
        cursor += 4;
        int count = Utils.bytesToInt(body, cursor);
        cursor += 4;

        // 식당 목록 출력 (함수화 가능할듯)
        System.out.println("================ 식당 목록 ================");
        for (int i = 0; i < count; i++) {
            //식당 id
            cursor += 4;
            long rId = Utils.bytesToLong(body, cursor);
            cursor += 8;

            // 식당 이름 Len 4
            String rName = readString(body, cursor);
            cursor += (4 + rName.getBytes(StandardCharsets.UTF_8).length);

            System.out.printf("[%d] %s\n", rId, rName);
        }
        System.out.println("=========================================");

        // 식당 ID, 기간 입력, 날짜만 입력받음.. 서버에서 시간 붙여줌
        System.out.print("\n조회할 식당 ID를 입력하세요: ");
        long targetId = Long.parseLong(sc.nextLine());

        System.out.print("조회 시작일 (예: 2024-01-01): ");
        String startAt = sc.nextLine();

        System.out.print("조회 종료일 (예: 2024-01-31): ");
        String endAt = sc.nextLine();

        // 0xA2 요청 전송
        byte[] bodyData = new OrderByRestaurantRequestDTO(targetId, startAt, endAt).toBytes();

        // 헤더 전송
        dos.writeByte(0x01); // 요청
        dos.writeByte(0xA2); // 결제 내역
        dos.write(Utils.intToBytes(bodyData.length)); // 바디 길이

        // 바디 전송
        dos.write(bodyData);
        dos.flush();

        System.out.println("[Client] 내역 조회 요청 전송 완료.");
    }
}