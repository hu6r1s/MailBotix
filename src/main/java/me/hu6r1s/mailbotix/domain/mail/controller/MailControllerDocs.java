package me.hu6r1s.mailbotix.domain.mail.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "메일 관련 API 명세서", description = "메일 조회 및 전송 API 명세입니다.")
public interface MailControllerDocs {

  @Operation(summary = "메일 리스트 조회", description = "메일 리스트로 조회하는 API")
  ResponseEntity<List<MailListResponse>> listEmails(HttpServletRequest request, HttpServletResponse response, int size) throws GeneralSecurityException, IOException;

  @Operation(summary = "특정 메일 조회", description = "특정 메일의 상세 내용을 조회하는 API")
  MailDetailResponse getEmailContent(@PathVariable String messageId, HttpServletRequest request, HttpServletResponse response)
      throws IOException, GeneralSecurityException, MessagingException;

  @Operation(summary = "메일 발송", description = "메일을 수신자에게 발송하는 API")
  void sendMail(@RequestBody SendMailRequest sendMailRequest, HttpServletRequest request, HttpServletResponse response)
      throws MessagingException, GeneralSecurityException, IOException;
}
