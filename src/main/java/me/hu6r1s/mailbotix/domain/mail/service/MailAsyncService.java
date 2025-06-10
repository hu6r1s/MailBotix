package me.hu6r1s.mailbotix.domain.mail.service;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailAsyncService {

  @Async
  public void sendMailAsync(SendMailRequest sendMailRequest, Session session, String email,
      String password)
      throws MessagingException {
    MimeMessage mimeMessage = createMessage(session, email, sendMailRequest);
    try (Transport transport = session.getTransport("smtp")) {
      transport.connect(email, password);
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      log.info("Mail sent successfully: From = {}, To = {}", email, sendMailRequest.getTo());
    } catch (AuthenticationFailedException e) {
      log.error("SMTP Authentication failed for {}: {}", email, e.getMessage(), e);
      throw new AuthenticationRequiredException("SMTP authentication failed. Please re-login.", e);
    }
  }

  private MimeMessage createMessage(Session session, String fromEmail,
      SendMailRequest sendMailRequest)
      throws MessagingException {
    MimeMessage mimeMessage = new MimeMessage(session);

    mimeMessage.setFrom(new InternetAddress(fromEmail));

    mimeMessage.addRecipient(Message.RecipientType.TO,
        new InternetAddress(sendMailRequest.getTo()));
    mimeMessage.setSubject(sendMailRequest.getSubject(), "UTF-8");
    mimeMessage.setContent(sendMailRequest.getMessageContent(), "text/html; charset=utf-8");
    mimeMessage.setSentDate(new java.util.Date());
    mimeMessage.setHeader("In-Reply-To", sendMailRequest.getOriginalMessageId());
    mimeMessage.setHeader("References", sendMailRequest.getOriginalMessageId());

    return mimeMessage;
  }
}
