package me.hu6r1s.mailbotix.global.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailConfig {

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${google.client.id}")
  private String clientId;

  @Value("${google.client.secret}")
  private String clientSecret;

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static MemoryDataStoreFactory dataStoreFactory;

  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

  @Bean
  public NetHttpTransport googleNetHttpTransport() throws GeneralSecurityException, IOException {
    return GoogleNetHttpTransport.newTrustedTransport();
  }

  @Bean
  public MemoryDataStoreFactory memoryDataStoreFactory() throws IOException {
    if (dataStoreFactory == null) {
      dataStoreFactory = new MemoryDataStoreFactory();
    }
    return dataStoreFactory;
  }

  @Bean
  public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(NetHttpTransport httpTransport, MemoryDataStoreFactory dataStoreFactory) throws IOException {
    GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
    web.setClientId(clientId);
    web.setClientSecret(clientSecret);
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);

    return new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(dataStoreFactory)
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .build();
  }

  public Gmail getGmailService(Credential credential) throws IOException, GeneralSecurityException {
    NetHttpTransport HTTP_TRANSPORT = googleNetHttpTransport();
    return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }
}
