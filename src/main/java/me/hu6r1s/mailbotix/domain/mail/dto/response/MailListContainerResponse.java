package me.hu6r1s.mailbotix.domain.mail.dto.response;

import com.google.api.client.auth.oauth2.Credential;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MailListContainerResponse {

  List<MailListResponse> mailListResponseList;
  private final Credential credential;
}
