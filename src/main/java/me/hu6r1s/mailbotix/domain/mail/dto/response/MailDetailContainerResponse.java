package me.hu6r1s.mailbotix.domain.mail.dto.response;

import com.google.api.client.auth.oauth2.Credential;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MailDetailContainerResponse {

  MailDetailResponse mailDetailResponse;
  private final Credential credential;
}
