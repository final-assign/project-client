package com.project_client;

import com.project_client.general.RequestDTO;
import com.project_client.login.LoginResponseDTO;
import com.project_client.menu.MenuRequestDTO;
import com.project_client.menu.MenuRequestType;
import com.project_client.user.UserType;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {

    private final static String HOST = "localhost";
    private static final int PORT = 5000;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Socket cliSocket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        byte[] data;
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

                if(type == 0x01)
                    attemptLogin(dos);

                else if(type == 0x02){

                    switch (code){
                        case 0x01 -> {

                            System.out.print("Login Success. Type: ");

                            data = new byte[Utils.bytesToInt(header, 2)];
                            dis.readFully(data);

                            LoginResponseDTO loginResponseDTO = new LoginResponseDTO(data);
                            System.out.println(loginResponseDTO.getUserType());

                            if(loginResponseDTO.getUserType() == UserType.ADMIN)
                                selectAdminMenu(dos);
                            //inputMenu .....
                        }
                        case 0x02 ->{

                            System.out.println("Invalid ID or Password");
                            attemptLogin(dos);
                        }
                    }
                }

                else break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (cliSocket != null) cliSocket.close(); } catch (Exception e) {}
        }
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
}