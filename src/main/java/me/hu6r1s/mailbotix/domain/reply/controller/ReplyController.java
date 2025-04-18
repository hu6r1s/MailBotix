package me.hu6r1s.mailbotix.domain.reply.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.reply.dto.request.ReplyGenerationRequest;
import me.hu6r1s.mailbotix.domain.reply.dto.response.ReplyGenerationResponse;
import me.hu6r1s.mailbotix.domain.reply.service.ReplyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reply")
@CrossOrigin(origins = "${frontend.url}", allowCredentials = "true")
public class ReplyController implements ReplyControllerDocs {

  private final ReplyService replyService;

  @PostMapping()
  public ReplyGenerationResponse generateEmailReply(@Valid @RequestBody ReplyGenerationRequest replyGenerationRequest) {
    return replyService.generateEmailReply(replyGenerationRequest);
  }
}
