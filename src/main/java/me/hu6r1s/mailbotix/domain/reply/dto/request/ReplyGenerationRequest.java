package me.hu6r1s.mailbotix.domain.reply.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메일 답장 생성 Request DTO")
public class ReplyGenerationRequest {

  @NotBlank(message = "답장을 생성할 원본 메일 내용은 필수입니다.")
  private String emailContent;

  private String tone;
}
