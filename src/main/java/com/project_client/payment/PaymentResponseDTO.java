package com.project_client.payment;

import com.project_client.Utils;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@Getter
@ToString
public class PaymentResponseDTO {
    private final boolean isSuccess;
    private String message; // 실패 사유 또는 서버 메시지

    public PaymentResponseDTO(byte code, byte[] body) {
        // 코드 0x34는 실패로 간주 (그 외는 성공, 예: 0x33)
        this.isSuccess = (code != (byte) 0x34);

        // Body가 있고 실패한 경우, 에러 메시지가 들어있다고 가정하고 파싱
        // (성공 시 Body가 비어있다면 이 부분은 실행되지 않거나 무시됨)
        if (body != null && body.length > 0) {
            int cursor = 0;
            // 만약 서버가 [Length(4) + String] 구조로 보낸다면:
            try {
                if (body.length >= 4) {
                    int len = Utils.bytesToInt(body, cursor);
                    // body 길이가 충분하다면 문자열 읽기
                    if (cursor + 4 + len <= body.length) {
                        this.message = new String(body, cursor + 4, len, StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                // 파싱 실패 시 바디 전체를 문자열로 변환하거나 null 처리
                this.message = "Unknown Error";
            }
        } else {
            this.message = isSuccess ? "결제 성공" : "알 수 없는 오류";
        }
    }
}