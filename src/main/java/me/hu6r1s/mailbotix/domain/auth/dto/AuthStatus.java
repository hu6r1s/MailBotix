package me.hu6r1s.mailbotix.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 상태 DTO")
public class AuthStatus {

  private boolean loggedIn;
  private String accessToken;
}
