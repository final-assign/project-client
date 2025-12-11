package com.project_client.payment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;


public class StudentPaymentUI {
    public static void processPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId) {
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
                    boolean cardPaymentSuccess = processCardPayment(dis, dos, selectedMenuId);

                    if (cardPaymentSuccess) {
                        System.out.println("카드 결제가 성공했습니다.");
                        paymentChoice = false;
                        // StudentMenuInquiryUI의 다음 행동 메뉴 호출
                        return;
                    } else {
                        System.out.println("카드 결제가 실패했습니다.");
                        // 결제 방법 선택으로 돌아감
                    }

                } else if (choice == 2) {
                    // 쿠폰 결제 처리
                    boolean couponPaymentSuccess = processCouponPayment(dis, dos, selectedMenuId);

                    if (couponPaymentSuccess) {
                        System.out.println("쿠폰 결제가 성공했습니다.");
                        paymentChoice = false;
                        // StudentMenuInquiryUI의 다음 행동 메뉴 호출
                        return;
                    } else {
                        System.out.println("쿠폰 결제가 실패했습니다.");
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

    private static boolean processCardPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId) {
        try {
            // PayRequestDTO 호출 및 처리
            // PayRequestDTO를 서버로 전송하고 응답 받기
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean processCouponPayment(DataInputStream dis, DataOutputStream dos, Long selectedMenuId) {
        try {
            // CouponPayRequestDTO 호출 및 처리
            // CouponPayRequestDTO를 서버로 전송하고 응답 받기

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}