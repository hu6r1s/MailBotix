package me.hu6r1s.mailbotix.domain.mail.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailDetailResponse {

  private String threadId;
  private MailDetailHeader headers;
  private List<Attachment> attachments;
  private String body;
}
