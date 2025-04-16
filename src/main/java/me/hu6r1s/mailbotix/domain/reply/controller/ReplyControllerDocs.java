package me.hu6r1s.mailbotix.domain.reply.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.hu6r1s.mailbotix.domain.reply.dto.request.ReplyGenerationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Tag(name = "답장 생성 API 명세서", description = "Gemini를 이용한 답장 생성 API 명세입니다.")
public interface ReplyControllerDocs {

  @PostMapping()
  @Operation(summary = "메일 답장 생성", description = "메일 답장을 생성하는 API")
  String generateEmailReply(@RequestBody ReplyGenerationRequest replyGenerationRequest);
}
