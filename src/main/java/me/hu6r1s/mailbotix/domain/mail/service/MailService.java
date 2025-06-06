package me.hu6r1s.mailbotix.domain.mail.service;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;

public interface MailService {

  /**
   * 사용자의 메일 목록 조회
   * @param userId 유저 식별자
   * @param size 페이지 당 개수
   * @return 메일 목록 응답 DTO 리스트
   */
  MailListContainerResponse listEmails(String userId, int size)
      throws IOException, GeneralSecurityException, MessagingException;

  /**
   * 사용자의 특정 메일 상세 내용 조회
   * @param messageId Gmail 객체
   * @param userId 사용자 식별자
   * @return 메일 상세 응답 DTO
   */
  MailDetailContainerResponse getEmailContent(String messageId, String userId)
      throws IOException, GeneralSecurityException, MessagingException;

  /**
   * 지정된 사용자의 메일로 답장 보내기
   * @param sendMailRequest 답장 요청 DTO
   * @param userId 사용자 식별자
   */
  void sendMail(SendMailRequest sendMailRequest, String userId)
      throws MessagingException, IOException, GeneralSecurityException;
}
