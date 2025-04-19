package me.hu6r1s.mailbotix.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

  // 4xx Client Errors
  MISSING_OAUTH_PARAM(HttpStatus.BAD_REQUEST, "AUTH-001", "필수 OAuth 파라미터가 누락되었습니다."),

  // 5xx Server/Integration Errors
  GOOGLE_TOKEN_EXCHANGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-101", "Google과의 인증 코드 교환에 실패했습니다."),
  CREDENTIAL_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-102", "사용자 인증 정보 저장에 실패했습니다."),
  CREDENTIAL_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-103", "사용자 인증 정보 로딩에 실패했습니다."),
  CREDENTIAL_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-104", "사용자 인증 정보 삭제에 실패했습니다."),
  GOOGLE_CLIENT_SETUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-105", "Google API 클라이언트 설정 중 보안 오류가 발생했습니다."),
  CREDENTIAL_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-106", "사용자 인증에 실패했습니다."),
  INTERNAL_AUTH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-999", "알 수 없는 인증 오류가 발생했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
