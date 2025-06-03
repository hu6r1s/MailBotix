package me.hu6r1s.mailbotix.domain.mail.dto.response;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResult {

  private final Gmail gmailService;
  private final Credential credential;
}
