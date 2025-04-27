package me.hu6r1s.mailbotix.domain.auth.controller;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.auth.dto.AuthStatus;
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

  @Value("${google.redirect.uri}")
  private String REDIRECT_URI;

  @Value("${frontend.url}")
  private String frontendUrl;

  private static final String SESSION_USER_ID_KEY = "userId";

  @Override
  @GetMapping("/google/url")
  public ResponseEntity<String> getGoogleAuthUrl(HttpServletRequest request) {
    AuthorizationCodeRequestUrl authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
        .setRedirectUri(REDIRECT_URI);

    return ResponseEntity.ok(authorizationUrl.build());
  }

  @Override
  @GetMapping("/google/callback")
  public RedirectView googleCallback(@RequestParam String code, HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    TokenResponse tokenResponse = googleAuthorizationCodeFlow.newTokenRequest(code)
        .setRedirectUri(REDIRECT_URI)
        .execute();

    String userId = UUID.randomUUID().toString();
    try {
      Credential credential = googleAuthorizationCodeFlow.createAndStoreCredential(tokenResponse, userId);
      redisTemplate.opsForValue().set(userId, credential.getRefreshToken());

      ResponseCookie userIdCookie = ResponseCookie.from(SESSION_USER_ID_KEY, userId)
          .httpOnly(true)
          .secure(true)
          .path("/")
          .sameSite("Lax")
          .maxAge(Duration.ofHours(1))
          .build();
      response.addHeader("Set-Cookie", userIdCookie.toString());

    } catch (IOException storageEx) {
      throw new CredentialStorageException("Failed to store credential for user " + userId, storageEx);
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

            ResponseCookie deleteCookie = ResponseCookie.from("userId", "")
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
}
