package me.hu6r1s.mailbotix.domain.mail.service;

import com.google.api.services.gmail.Gmail;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;

public interface MailService {

  /**
   * 사용자의 메일 목록 조회
   * @param service Gmail 객체
   * @param size 페이지 당 개수
   * @return 메일 목록 응답 DTO 리스트
   */
  List<MailListResponse> listEmails(Gmail service, int size) throws IOException;

  /**
   * 사용자의 특정 메일 상세 내용 조회
   * @param messageId Gmail 객체
   * @param service 사용자 DB ID
   * @return 메일 상세 응답 DTO
   */
  MailDetailResponse getEmailContent(String messageId, Gmail service) throws IOException;

  /**
   * 지정된 사용자의 메일로 답장 보내기
   * @param sendMailRequest 답장 요청 DTO
   * @param service Gmail 객체
   */
  void sendReply(SendMailRequest sendMailRequest, Gmail service) throws MessagingException, IOException;
}
