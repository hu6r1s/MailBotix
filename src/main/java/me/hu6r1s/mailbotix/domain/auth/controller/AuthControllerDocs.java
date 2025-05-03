package me.hu6r1s.mailbotix.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.hu6r1s.mailbotix.domain.auth.dto.AuthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Tag(name = "OAuth API 명세서", description = "OAuth 인증을 위한 API 명세입니다.")
public interface AuthControllerDocs {

  @Operation(summary = "사용자 인증", description = "사용자가 구글에 인증을 요청하는 API")
  ResponseEntity<String> getGoogleAuthUrl(HttpServletRequest request);

  @Operation(summary = "사용자 인가", description = "구글에서 사용자 인증코드를 통해 토근을 발급하는 API")
  RedirectView googleCallback(@RequestParam String code, @RequestParam String state, HttpServletRequest request, HttpServletResponse response) throws IOException;

  @Operation(summary = "사용자 상태", description = "사용자가 현재 권한이 있는지 확인하는 API")
  ResponseEntity<AuthStatus> getAuthStatus(HttpServletRequest request);

  @Operation(summary = "로그아웃", description = "사용자 권한을 제거하는 API")
  ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response);
}
