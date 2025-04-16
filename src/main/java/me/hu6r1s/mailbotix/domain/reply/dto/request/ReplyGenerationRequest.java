package me.hu6r1s.mailbotix.domain.reply.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메일 답장 생성 Request DTO")
public class ReplyGenerationRequest {

  private String emailContent;
  private String tone;
}
