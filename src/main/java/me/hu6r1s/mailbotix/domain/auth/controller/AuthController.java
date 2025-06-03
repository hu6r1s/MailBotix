package me.hu6r1s.mailbotix.domain.auth.controller;

import com.google.api.client.auth.oauth2.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.auth.dto.AuthStatus;
import me.hu6r1s.mailbotix.domain.auth.service.GoogleOauthService;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.util.CookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${frontend.url}", allowCredentials = "true")
public class AuthController implements AuthControllerDocs {


  private final GoogleOauthService googleOauthService;
  private final StringRedisTemplate redisTemplate;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Override
  @GetMapping("/{provider}/url")
  public ResponseEntity<String> getAuthUrl(@PathVariable String provider, HttpServletRequest request) {
    HttpSession session = request.getSession();
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    session.setAttribute("state", state);

    String authUrl = switch (provider.toLowerCase()) {
      case "google" -> googleOauthService.buildAuthUrl(state);
      // Todo naver 추가
      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    };

    return ResponseEntity.ok(authUrl);
  }

  @Override
  @GetMapping("/{provider}/callback")
  public RedirectView handleCallback(
      @PathVariable String provider,
      @RequestParam String code,
      @RequestParam String state,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (!state.equals(session.getAttribute("state"))) {
      throw new AuthenticationRequiredException("CSRF token does not match.");
    }

    String userId;
    TokenResponse tokenResponse;

    switch (provider.toLowerCase()) {
      case "google" -> {
        tokenResponse = googleOauthService.getToken(code);
        userId = googleOauthService.getUserIdFromIdToken((String) tokenResponse.get("id_token"));
        googleOauthService.storeCredential(userId, tokenResponse);
      }
      // todo naver add
      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
    redisTemplate.opsForValue().set(provider +":" + userId + ":refresh", tokenResponse.getRefreshToken());
    session.setAttribute("provider", provider);

    ResponseCookie userIdCookie = ResponseCookie.from("userId", userId)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(Duration.ofSeconds(tokenResponse.getExpiresInSeconds()))
        .build();
    response.addHeader("Set-Cookie", userIdCookie.toString());

    return new RedirectView((frontendUrl));
  }

  @Override
  @GetMapping("/status")
  public ResponseEntity<AuthStatus> getAuthStatus(HttpServletRequest request) {
    String userId = CookieUtils.getUserIdFromCookie(request);
    if (userId != null) {
      return ResponseEntity.ok(new AuthStatus(true, userId));
    } else {
      return ResponseEntity.ok(new AuthStatus(false, null));
    }
  }

  @Override
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String userId = CookieUtils.getUserIdFromCookie(request);
    if (userId != null) {
      HttpSession session = request.getSession(false);
      String provider = (String) session.getAttribute("provider");
      googleOauthService.deleteDataStore(provider +":" + userId + ":refresh");

      ResponseCookie deleteCookie = ResponseCookie.from("userId", "")
          .httpOnly(true)
          .secure(true)
          .path("/")
          .maxAge(0)
          .build();
      response.addHeader("Set-Cookie", deleteCookie.toString());
      redisTemplate.delete(userId);

      if (session != null) {
        session.invalidate();
      }
    }
    return ResponseEntity.ok().build();
  }
}
