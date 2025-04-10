package me.hu6r1s.mailbotix.domain.gmail.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.gmail.service.GmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class GmailController {

  private final GmailService gmailService;

  @GetMapping("/list")
  public List<Map<String, Object>> listEmails() throws IOException {
    return gmailService.listEmails();
  }

  @GetMapping("/read/{messageId}")
  public Map<String, Object> getEmailContent(@PathVariable String messageId) throws IOException {
    return gmailService.getEmailContent(messageId);
  }
}
