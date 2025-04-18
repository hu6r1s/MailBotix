package me.hu6r1s.mailbotix.domain.auth.controller;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.auth.dto.AuthStatus;
import me.hu6r1s.mailbotix.global.exception.CredentialDeleteException;
import me.hu6r1s.mailbotix.global.exception.CredentialStorageException;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${google.redirect.uri}")
  private String REDIRECT_URI;

  @Value("${frontend.url}")
  private String frontendUrl;

  private static final String SESSION_USER_ID_KEY = "userId";

  @GetMapping("/google/url")
  public ResponseEntity<String> getGoogleAuthUrl(HttpServletRequest request) {
    String state = UUID.randomUUID().toString();
    request.getSession().setAttribute("state", state);

    AuthorizationCodeRequestUrl authorizationUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
        .setRedirectUri(REDIRECT_URI);
//        .setState(state);

    return ResponseEntity.ok(authorizationUrl.build());
  }

  @GetMapping("/google/callback")
  public RedirectView googleCallback(@RequestParam String code, HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession();

    TokenResponse tokenResponse = googleAuthorizationCodeFlow.newTokenRequest(code)
        .setRedirectUri(REDIRECT_URI)
        .execute();

    String userId = UUID.randomUUID().toString();
    session.setAttribute(SESSION_USER_ID_KEY, userId);
    try {
      googleAuthorizationCodeFlow.createAndStoreCredential(tokenResponse, userId);


    } catch (IOException storageEx) {
      throw new CredentialStorageException("Failed to store credential for user " + userId, storageEx);
    }
      return new RedirectView(frontendUrl);
  }

  @GetMapping("/status")
  public ResponseEntity<AuthStatus> getAuthStatus(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null && session.getAttribute(SESSION_USER_ID_KEY) != null) {
      String userId = (String) session.getAttribute(SESSION_USER_ID_KEY);
      return ResponseEntity.ok(new AuthStatus(true, userId));
    } else {
      return ResponseEntity.ok(new AuthStatus(false, null));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      String userId = (String) session.getAttribute(SESSION_USER_ID_KEY);
      if (userId != null) {
        try {
          @SuppressWarnings({"unchecked", "rawtypes"})
          DataStore<Serializable> credentialDataStore = memoryDataStoreFactory.getDataStore("StoredCredential");

          if (credentialDataStore != null) {
            credentialDataStore.delete(userId);
          }
        } catch (IOException deleteEx) {
          throw new CredentialDeleteException("Failed to delete credential for user " + userId, deleteEx);
        }
      }
      session.invalidate();
    }
    return ResponseEntity.ok().build();
  }
}