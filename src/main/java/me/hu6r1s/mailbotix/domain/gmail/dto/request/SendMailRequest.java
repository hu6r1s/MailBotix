package me.hu6r1s.mailbotix.domain.gmail.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequest {

  private String threadId;
  private String to;
  private String subject;
  private String messageContent;
  private String originalMessageId;
}
