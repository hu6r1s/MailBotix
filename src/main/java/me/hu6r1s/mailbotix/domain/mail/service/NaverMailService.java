package me.hu6r1s.mailbotix.domain.mail.service;

import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Part;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.auth.dto.NaverUserProfileResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.Attachment;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailHeader;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListHeader;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.util.AppPasswordContext;
import me.hu6r1s.mailbotix.global.util.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
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
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");

    return Session.getInstance(props);
  }

  @Override
  public MailListContainerResponse listEmails(String accessToken, int size)
      throws IOException {
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
      UIDFolder uidFolder = (UIDFolder) inbox;
      for (int i = messages.length -1 ; i >= 0; i--) {
        Message message = messages[i];
        Address[] fromAddresses = message.getFrom();
        String from = MimeUtility.decodeText(fromAddresses[0].toString());
        String subject = MimeUtility.decodeText(message.getSubject());
        boolean hasAttachment = hasAttachment(message);
        boolean unread = message.isSet(Flag.SEEN);
        String messageId = String.valueOf(uidFolder.getUID(message));
        mailList.add(new MailListResponse(
            messageId, message.getReceivedDate().toString(),
            MailListHeader.builder().subject(subject).from(from).build(),
            hasAttachment,
            unread
        ));
        if(mailList.size() >= size) break;
      }

      return MailListContainerResponse.builder()
          .mailListResponseList(mailList)
          .build();
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
  }

  @Override
  public MailDetailContainerResponse getEmailContent(String messageId, String accessToken)
      throws IOException, MessagingException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    NaverUserProfileResponse.Response userInfo = tokenUtils.getUserInfoFromToken(requestEntity);
    String password = AppPasswordContext.get();

    Session session = getMailSession();
    Store store = null;
    Folder inbox = null;
    try {
      store = session.getStore("imaps");
      store.connect(userInfo.getEmail(), password);

      inbox = store.getFolder("INBOX");
      inbox.open(Folder.READ_ONLY);

      UIDFolder uidFolder = (UIDFolder) inbox;
      Message message = uidFolder.getMessageByUID(Long.parseLong(messageId));

      if (message == null) {
        throw new MessagingException("Message not found with UID: " + messageId);
      }

      MailDetailHeader mailHeaders = parseHeaders(message);
      List<Attachment> attachments = new ArrayList<>();
      StringBuilder bodyText = new StringBuilder();

      parseMessageContent(message, bodyText, attachments);

      MailDetailResponse mailDetailResponse = MailDetailResponse.builder()
          .threadId(messageId)
          .headers(mailHeaders)
          .body(bodyText.toString())
          .attachments(attachments)
          .build();

      return MailDetailContainerResponse.builder()
          .mailDetailResponse(mailDetailResponse)
          .build();
    } catch (AuthenticationFailedException authFailed) {
      throw new AuthenticationRequiredException("Mail server authentication failed.", authFailed);
    } catch (NoSuchProviderException e) {
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
  }

  @Override
  public void sendMail(SendMailRequest sendMailRequest, String accessToken)
      throws MessagingException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    NaverUserProfileResponse.Response userInfo = tokenUtils.getUserInfoFromToken(requestEntity);
    String password = AppPasswordContext.get();

    Session session = getMailSession();
    Transport transport = null;

    try {
      MimeMessage mimeMessage = createMessage(session, userInfo.getEmail(), sendMailRequest);
      transport = session.getTransport("smtp");
      transport.connect(userInfo.getEmail(), password);
      transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
    } catch (AuthenticationFailedException authFailed) {
      log.error("SMTP XOAUTH2 Authentication failed for {}: {}", userInfo.getEmail(), authFailed.getMessage(), authFailed);
      throw new AuthenticationRequiredException("SMTP authentication failed. Please re-login.", authFailed);
    } catch (SendFailedException sendFailed) {
      log.error("Failed to send email. Invalid addresses might exist.", sendFailed);
      throw new MessagingException("Failed to send email due to invalid recipient addresses.", sendFailed);
    } finally {
      if (transport != null && transport.isConnected()) {
        transport.close();
      }
    }

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

  private MailDetailHeader parseHeaders(Message message) throws MessagingException, UnsupportedEncodingException {
    String from = MimeUtility.decodeText(message.getFrom()[0].toString());
    String subject = message.getSubject() != null ? MimeUtility.decodeText(message.getSubject()) : "";
    String sentDate = message.getSentDate() != null ? message.getSentDate().toString() : "";

    String to = "";
    if (message.getRecipients(Message.RecipientType.TO) != null) {
      List<String> toList = new ArrayList<>();
      for (Address address : message.getRecipients(Message.RecipientType.TO)) {
        toList.add(MimeUtility.decodeText(address.toString()));
      }
      to = String.join(", ", toList);
    }

    return MailDetailHeader.builder().from(from).to(to).subject(subject).date(sentDate).build();
  }

  private void parseMessageContent(Part part, StringBuilder bodyContent, List<Attachment> attachments) throws IOException, MessagingException {
    if (part.isMimeType("text/html") && bodyContent.length() == 0) {
      bodyContent.append((String) part.getContent());
    } else if (part.isMimeType("text/plain") && bodyContent.length() == 0) {
      bodyContent.append((String) part.getContent());
    }

    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getFileName() != null && !part.getFileName().isEmpty() && !part.isMimeType("multipart/*")) ) {
      attachments.add(Attachment.builder()
          .filename(MimeUtility.decodeText(part.getFileName()))
          .mimeType(part.getContentType())
          .size(part.getSize())
          .build());
      return;
    }

    if (part.isMimeType("multipart/*")) {
      Multipart multipart = (Multipart) part.getContent();
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        parseMessageContent(bodyPart, bodyContent, attachments);
      }
    }
  }

  private MimeMessage createMessage(Session session, String fromEmail, SendMailRequest sendMailRequest)
      throws MessagingException {
      MimeMessage mimeMessage = new MimeMessage(session);

      mimeMessage.setFrom(new InternetAddress(fromEmail));

      mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(sendMailRequest.getTo()));
      mimeMessage.setSubject(sendMailRequest.getSubject(), "UTF-8");
      mimeMessage.setContent(sendMailRequest.getMessageContent(), "text/html; charset=utf-8");
      mimeMessage.setSentDate(new java.util.Date());
      mimeMessage.setHeader("In-Reply-To", sendMailRequest.getOriginalMessageId());
      mimeMessage.setHeader("References", sendMailRequest.getOriginalMessageId());

      return mimeMessage;
  }
}
