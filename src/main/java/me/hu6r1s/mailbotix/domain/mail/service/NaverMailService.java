package me.hu6r1s.mailbotix.domain.mail.service;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeUtility;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.auth.dto.NaverUserProfileResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListHeader;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import me.hu6r1s.mailbotix.global.util.AppPasswordContext;
import me.hu6r1s.mailbotix.global.util.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service("naverMailService")
@RequiredArgsConstructor
public class NaverMailService implements MailService {

  @Value("${naver.mail.imap.host}")
  private String imapHost;
  @Value("${naver.mail.imap.port}")
  private int imapPort;
  @Value("${spring.mail.host}")
  private String smtpHost;
  @Value("${spring.mail.port}")
  private int smtpPort;

  private final StringRedisTemplate redisTemplate;
  private final JavaMailSender mailSender;
  private final TokenUtils tokenUtils;

  private Session getMailSession() {
    Properties props = new Properties();
    props.put("mail.store.protocol", "imaps");
    props.put("mail.imaps.host", imapHost);
    props.put("mail.imaps.port", imapPort);
    props.put("mail.imaps.ssl.enable", "true");

    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);

    return Session.getInstance(props);
  }

  @Override
  public MailListContainerResponse listEmails(String accessToken, int size)
      throws IOException, GeneralSecurityException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    NaverUserProfileResponse.Response userInfo = tokenUtils.getUserInfoFromToken(requestEntity);
    String password = AppPasswordContext.get();

    Session session = getMailSession();
    List<MailListResponse> mailList = new ArrayList<>();

    Store store = null;
    Folder inbox = null;
    try {
      store = session.getStore("imaps");
      store.connect(userInfo.getEmail(), password);

      inbox = store.getFolder("INBOX");
      inbox.open(Folder.READ_ONLY);

      int totalMessages = inbox.getMessageCount();
      int start = Math.max(1, totalMessages - size + 1);
      int end = totalMessages;
      Message[] messages = inbox.getMessages(start, end);
      for (int i = messages.length -1 ; i >= 0; i--) {
        Message message = messages[i];
        Address[] fromAddresses = message.getFrom();
        String from = MimeUtility.decodeText(fromAddresses[0].toString());
        String subject = MimeUtility.decodeText(message.getSubject());
        boolean hasAttachment = hasAttachment(message);
        boolean unread = message.isSet(Flag.SEEN);
        String messageId = String.valueOf(message.getMessageNumber());

        mailList.add(new MailListResponse(
            messageId, message.getReceivedDate().toString(),
            MailListHeader.builder().subject(subject).from(from).build(),
            hasAttachment,
            unread
        ));
        if(mailList.size() >= size) break;
      }

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    } finally {
      if (inbox != null && inbox.isOpen()) {
        try {
          inbox.close(false);
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      }
      if (store != null && store.isConnected()) {
        try {
          store.close();
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return MailListContainerResponse.builder()
        .mailListResponseList(mailList)
        .build();
  }

  @Override
  public MailDetailContainerResponse getEmailContent(String messageId, String userId)
      throws IOException, GeneralSecurityException {
    return null;
  }

  @Override
  public void sendMail(SendMailRequest sendMailRequest, String userId)
      throws MessagingException, IOException, GeneralSecurityException {

  }

  private boolean hasAttachment(Message message) throws MessagingException, IOException {
    if (message.isMimeType("multipart/*")) {
      Multipart multipart = (Multipart) message.getContent();
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart part = multipart.getBodyPart(i);
        String disposition = part.getDisposition();
        if (disposition != null &&
            (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
          return true;
        }
      }
    }
    return false;
  }
}
