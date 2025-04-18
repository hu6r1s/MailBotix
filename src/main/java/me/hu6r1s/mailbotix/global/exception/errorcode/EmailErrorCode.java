package me.hu6r1s.mailbotix.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EmailErrorCode {

  // 4xx Client-Related Processing Errors
  EMAIL_MESSAGING_ERROR(HttpStatus.BAD_REQUEST, "EMAIL-001", "메일 생성 또는 처리 중 오류가 발생했습니다. (예: 주소 형식 오류)"),

  // 5xx Server/API Errors
  GMAIL_API_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-101", "Gmail API를 통한 메일 전송에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  EmailErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }
}