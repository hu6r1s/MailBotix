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
import me.hu6r1s.mailbotix.domain.auth.dto.NaverTokenResponse;
import me.hu6r1s.mailbotix.domain.auth.service.GoogleOauthService;
import me.hu6r1s.mailbotix.domain.auth.service.NaverOauthService;
import me.hu6r1s.mailbotix.domain.mail.MailProvider;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.util.CookieUtils;
import me.hu6r1s.mailbotix.global.util.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
  private final NaverOauthService naverOauthService;
  private final StringRedisTemplate redisTemplate;
  private final TokenUtils tokenUtils;
  private final CookieUtils cookieUtils;

  @Value("${frontend.url}")
  private String frontendUrl;

  @Override
  @GetMapping("/{provider}/url")
  public ResponseEntity<String> getAuthUrl(@PathVariable String provider, HttpServletRequest request) {
    HttpSession session = request.getSession();
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    session.setAttribute("state", state);

    MailProvider mailProvider = MailProvider.valueOf(provider.toUpperCase());
    String authUrl = switch (mailProvider) {
      case GOOGLE -> googleOauthService.buildAuthUrl(state);
      case NAVER -> naverOauthService.buildAuthUrl(state);
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
    session.removeAttribute("state");

    String userId;
    String refreshToken = null;
    Long expiresIn;
    String accessToken;
    String redirectUrl;

    MailProvider mailProvider = MailProvider.valueOf(provider.toUpperCase());
    switch (mailProvider) {
      case GOOGLE -> {
        TokenResponse tokenResponse = googleOauthService.getToken(code);
        userId = googleOauthService.getUserIdFromIdToken((String) tokenResponse.get("id_token"));
        googleOauthService.storeCredential(userId, tokenResponse);
        refreshToken = tokenResponse.getRefreshToken();
        expiresIn = tokenResponse.getExpiresInSeconds();
        accessToken = (String) tokenResponse.get("id_token");
        redirectUrl = frontendUrl;
      }
      case NAVER -> {
        NaverTokenResponse tokenResponse = naverOauthService.getToken(code);
        if (tokenResponse.getError() != null) {
          throw new AuthenticationRequiredException("Naver authentication failed: " + tokenResponse.getErrorDescription());
        }

        refreshToken = tokenResponse.getRefreshToken();
        expiresIn = (tokenResponse.getExpiresIn() != null) ? tokenResponse.getExpiresIn().longValue() : 3600L;
        accessToken = tokenResponse.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        userId = tokenUtils.getUserInfoFromToken(requestEntity).getId();
        redirectUrl = frontendUrl + "/app-password";
      }
      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    }

    String providerLower = provider.toLowerCase();
    if (refreshToken != null) {
      redisTemplate.opsForValue().set(providerLower + ":" + userId + ":refresh", refreshToken, Duration.ofDays(7L));
    }
    session.setAttribute("provider", providerLower);

    ResponseCookie tokenCookie = ResponseCookie.from("access_token", accessToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(Duration.ofSeconds(expiresIn))
        .build();
    response.addHeader("Set-Cookie", tokenCookie.toString());

    return new RedirectView(redirectUrl);
  }

  @Override
  @GetMapping("/status")
  public ResponseEntity<AuthStatus> getAuthStatus(HttpServletRequest request) {
    String accessToken = cookieUtils.getAccessTokenFromCookie(request);
    if (accessToken != null) {
      return ResponseEntity.ok(new AuthStatus(true, accessToken));
    } else {
      return ResponseEntity.ok(new AuthStatus(false, null));
    }
  }

  @Override
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    String userId = cookieUtils.getAccessTokenFromCookie(request);
    if (userId != null) {
      HttpSession session = request.getSession(false);
      String provider = (String) session.getAttribute("provider");
      googleOauthService.deleteDataStore(provider +":" + userId + ":refresh");

      ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
          .httpOnly(true)
          .secure(true)
          .path("/")
          .maxAge(0)
          .build();
      response.addHeader("Set-Cookie", deleteCookie.toString());

      ResponseCookie deleteAppPassword = ResponseCookie.from("app_password", "")
          .httpOnly(true)
          .secure(true)
          .path("/")
          .maxAge(0)
          .build();
      response.addHeader("Set-Cookie", deleteAppPassword.toString());
      redisTemplate.delete(userId);

      if (session != null) {
        session.invalidate();
      }
    }
    return ResponseEntity.ok().build();
  }
}
