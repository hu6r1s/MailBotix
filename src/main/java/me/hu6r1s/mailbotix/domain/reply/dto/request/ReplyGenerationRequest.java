package me.hu6r1s.mailbotix.domain.reply.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyGenerationRequest {

  private String emailContent;
  private String tone;
}
