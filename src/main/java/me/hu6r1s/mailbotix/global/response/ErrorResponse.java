package me.hu6r1s.mailbotix.global.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class ErrorResponse {

  private final int status;
  private final String error;
  private final String code;
  private final String message;
}
