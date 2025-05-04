package me.hu6r1s.mailbotix.domain.auth.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.auth.dto.AuthStatus;
import me.hu6r1s.mailbotix.global.config.GmailConfig;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.exception.CredentialDeleteException;
import me.hu6r1s.mailbotix.global.exception.CredentialStorageException;
import me.hu6r1s.mailbotix.global.util.CookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

  private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
  private final MemoryDataStoreFactory memoryDataStoreFactory;
  private final StringRedisTemplate redisTemplate;
  private final GmailConfig gmailConfig;

  @Value("${google.redirect.uri}")
  private String REDIRECT_URI;

  @Value("${frontend.url}")
  private String frontendUrl;

  private static final String SESSION_USER_ID_KEY = "userId";

  @Override
  @GetMapping("/google/url")
  public ResponseEntity<String> getGoogleAuthUrl(HttpServletRequest request) {
    HttpSession session = request.getSession();
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    session.setAttribute("state", state);
    AuthorizationCodeRequestUrl authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
        .setRedirectUri(REDIRECT_URI).setState(state);

    return ResponseEntity.ok(authorizationUrl.build());
  }

  @Override
  @GetMapping("/google/callback")
  public RedirectView googleCallback(@RequestParam String code, @RequestParam String state,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    if (!state.equals(session.getAttribute("state"))) {
      throw new AuthenticationRequiredException("CSRF token does not match.");
    }

    try {
      TokenResponse tokenResponse = googleAuthorizationCodeFlow.newTokenRequest(code)
          .setRedirectUri(REDIRECT_URI)
          .execute();

      String userId = getUserIdFromGoogleOrSession((String) tokenResponse.get("id_token"));

      googleAuthorizationCodeFlow.createAndStoreCredential(tokenResponse, userId);
      redisTemplate.opsForValue().set(userId, tokenResponse.getRefreshToken());

      ResponseCookie userIdCookie = ResponseCookie.from("userId", userId)
          .httpOnly(true)
          .secure(true)
          .path("/")
          .sameSite("Lax")
          .maxAge(Duration.ofSeconds(tokenResponse.getExpiresInSeconds()))
          .build();
      response.addHeader("Set-Cookie", userIdCookie.toString());

    } catch (IOException storageEx) {
      throw new CredentialStorageException("Failed to store credential for user", storageEx);
    }
      return new RedirectView(frontendUrl);
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
        try {
          @SuppressWarnings({"unchecked", "rawtypes"})
          DataStore<Serializable> credentialDataStore = memoryDataStoreFactory.getDataStore("StoredCredential");

          if (credentialDataStore != null) {
            credentialDataStore.delete(userId);

            ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "")
                    .httpOnly(true)
                        .secure(true)
                            .path("/")
                                .maxAge(0)
                                    .build();
            response.addHeader("Set-Cookie", deleteCookie.toString());
            redisTemplate.delete(userId);
          }
        } catch (IOException deleteEx) {
          throw new CredentialDeleteException("Failed to delete credential for user " + userId, deleteEx);
        }
      }
    return ResponseEntity.ok().build();
  }

  private String getUserIdFromGoogleOrSession(String idToken) {
    DecodedJWT jwt = JWT.decode(idToken);
    return jwt.getSubject();
  }
}
