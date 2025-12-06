package com.project_client;

import com.project_client.login.LoginResponseDTO;

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

                            System.out.println("Login Success. Type: ");

                            data = new byte[Utils.bytesToInt(header, 2)];
                            System.out.println(data.length);
                            dis.readFully(data);

                            LoginResponseDTO loginResponseDTO = new LoginResponseDTO(data);
                            System.out.println(loginResponseDTO.getUserType());
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
}