package com.project_client.payment;

import com.project_client.Utils;
import lombok.NoArgsConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;


@NoArgsConstructor
public class UserPaymentUI {
    public static void processPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId, int menuPrice) {
        Scanner sc = new Scanner(System.in);

        try {
            // ===== 결제방법 선택 =====
            boolean paymentChoice = true;
            while (paymentChoice) {
                System.out.println("===== 결제방법 선택 =====");
                System.out.println("1. 카드결제");
                System.out.println("2. 쿠폰결제");
                System.out.println("3. 취소");
                System.out.print("번호 입력: ");

                int choice;
                try {
                    choice = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("잘못된 입력입니다.");
                    continue;
                }

                if (choice == 1) {
                    // 카드 결제 처리
                    boolean cardPaymentSuccess = processCardPayment(dis, dos, selectedMenuId, menuPrice);

                    if (cardPaymentSuccess) {
                        System.out.println("결제가 성공했습니다.");

                        return;
                    } else {
                        System.out.println("결제 실패.");
                    }

                } else if (choice == 2) {
                    // 쿠폰 결제 처리
                    boolean couponPaymentSuccess = processCouponPayment(dis, dos, selectedMenuId, menuPrice);

                    if (couponPaymentSuccess) {
                        System.out.println("쿠폰 결제가 성공했습니다.");
                        return;
                    } else {
                        System.out.println("쿠폰 결제 실패.");
                        // 결제 방법 선택으로 돌아감
                    }

                } else if (choice == 3) {
                    // StaffMenuInquiryUI로 돌아가기
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

    private static boolean processCardPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId, int menuPrice) {
        try {
            PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
                    .menuId(selectedMenuId)
                    .menuPrice(menuPrice)
                    .build();

            // 전송
            dos.write(requestDTO.toBytes());
            dos.flush();

            byte[] header = new byte[6];
            dis.readFully(header);
            byte code = header[1];
            int bodyLength = Utils.bytesToInt(header, 2);

            byte[] body = null;
            if (bodyLength > 0) {
                body = new byte[bodyLength];
                dis.readFully(body);
            }

            // DTO 생성 (코드와 바디를 넘김)
            PaymentResponseDTO responseDTO = new PaymentResponseDTO(code, body);

            return responseDTO.isSuccess();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean processCouponPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId, int menuPrice) {
        try {
            PaymentCouponRequestDTO requestDTO = PaymentCouponRequestDTO.builder()
                    .menuId(selectedMenuId)
                    .build();

            // 전송
            dos.write(requestDTO.toBytes());
            dos.flush();

            byte[] header = new byte[6];
            dis.readFully(header);
            byte code = header[1];
            int bodyLength = Utils.bytesToInt(header, 2);

            byte[] body = null;
            if (bodyLength > 0) {
                body = new byte[bodyLength];
                dis.readFully(body);
            }

            // DTO 생성 (코드와 바디를 넘김)
            PaymentCouponResponseDTO responseDTO = new PaymentCouponResponseDTO(body);
            //서버에서
            if (responseDTO.getCount() == 0)
                return false;

            System.out.println("선택한 메뉴의 현재 쿠폰 개수 : " + responseDTO.getCount());
            PaymentCouponDecreaseRequestDTO decreaseRequestDTO = PaymentCouponDecreaseRequestDTO.builder()
                    .couponId(responseDTO.getCouponId())
                    .build();

            dos.write(decreaseRequestDTO.toBytes());
            dos.flush();

            dis.readFully(header);
            code = header[1];
            body = new byte[Utils.bytesToInt(header, 2)];
            dis.readFully(body);
            PaymentResponseDTO finalResponseDTO = new PaymentResponseDTO(code, body);

            return finalResponseDTO.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}