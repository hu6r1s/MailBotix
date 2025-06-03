package me.hu6r1s.mailbotix.domain.auth.service;

import com.auth0.jwt.JWT;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import java.io.IOException;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.global.exception.CredentialDeleteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOauthService {

  private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
  private final MemoryDataStoreFactory memoryDataStoreFactory;

  @Value("${google.redirect.uri}")
  private String REDIRECT_URI;

  public String buildAuthUrl(String state) {
    return googleAuthorizationCodeFlow.newAuthorizationUrl()
        .setRedirectUri(REDIRECT_URI)
        .setState(state)
        .build();
  }

  public TokenResponse getToken(String code) {
    try {
      return googleAuthorizationCodeFlow.newTokenRequest(code)
          .setRedirectUri(REDIRECT_URI)
          .execute();
    } catch (IOException e) {
      throw new RuntimeException("Failed to store credential for user: ", e);
    }
  }

  public String getUserIdFromIdToken(String idToken) {
    return JWT.decode(idToken).getSubject();
  }

  public void storeCredential(String userId, TokenResponse tokenResponse) {
    try {
      googleAuthorizationCodeFlow.createAndStoreCredential(
          tokenResponse,
          userId
      );
    } catch (IOException e) {
      throw new RuntimeException("Failed to store credential", e);
    }
  }

  public void deleteDataStore(String userId) {
    try {
      DataStore<Serializable> credentialDataStore = memoryDataStoreFactory.getDataStore("StoredCredential");
      if (credentialDataStore != null) {
        credentialDataStore.delete(userId);
      }
    } catch (IOException e) {
      throw new CredentialDeleteException("Failed to delete credential for user " + userId, e);
    }
  }
}
