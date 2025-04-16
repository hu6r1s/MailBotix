package me.hu6r1s.mailbotix.domain.gmail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import me.hu6r1s.mailbotix.domain.gmail.dto.request.SendMailRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "메일 관련 API 명세서", description = "메일 조회 및 전송 API 명세입니다.")
public interface GmailControllerDocs {

  @Operation(summary = "메일 리스트 조회", description = "메일 리스트로 조회하는 API")
  List<Map<String, Object>> listEmails(HttpServletRequest request) throws GeneralSecurityException, IOException;

  @Operation(summary = "특정 메일 조회", description = "특정 메일의 상세 내용을 조회하는 API")
  Map<String, Object> getEmailContent(@PathVariable String messageId, HttpServletRequest request)
      throws IOException, GeneralSecurityException;

  @Operation(summary = "메일 발송", description = "메일을 수신자에게 발송하는 API")
  String sendReply(@RequestBody SendMailRequest sendMailRequest, HttpServletRequest request);
}
