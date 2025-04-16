package me.hu6r1s.mailbotix.domain.gmail.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메일 전송 Request DTO")
public class SendMailRequest {

  private String threadId;
  private String to;
  private String subject;
  private String messageContent;
  private String originalMessageId;
}
