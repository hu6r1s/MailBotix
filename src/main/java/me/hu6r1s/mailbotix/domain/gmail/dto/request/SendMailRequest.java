package me.hu6r1s.mailbotix.domain.gmail.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메일 전송 Request DTO")
public class SendMailRequest {

  private String threadId;

  @NotBlank(message = "수신자 이메일은 필수입니다.")
  @Email(message = "유효한 이메일 형식이 아닙니다.")
  private String to;

  @NotBlank(message = "메일 제목은 필수입니다.")
  private String subject;
  private String messageContent;
  private String originalMessageId;
}
