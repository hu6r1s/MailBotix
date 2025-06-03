package me.hu6r1s.mailbotix.domain.mail.service;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.Attachment;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailHeader;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListHeader;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.TokenRefreshResult;
import me.hu6r1s.mailbotix.global.config.GmailConfig;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.jsoup.Jsoup;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service("googleMailService")
@RequiredArgsConstructor
public class GoogleMailService implements MailService {

  private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
  private final StringRedisTemplate redisTemplate;
  private final GmailConfig gmailConfig;
  private static final int BATCH_CHUNK_SIZE = 20;

  public TokenRefreshResult getAuthenticatedGmailClient(String userId)
      throws IOException, GeneralSecurityException {
    String redisRefreshTokenKey = "google:" + userId + ":refresh";
    String refreshToken = redisTemplate.opsForValue().get(redisRefreshTokenKey);

    if (refreshToken == null) {
      throw new AuthenticationRequiredException("Session expired. Please log in again.");
    }

    Credential credential = googleAuthorizationCodeFlow.loadCredential(userId);

    if (credential == null) {
      throw new AuthenticationRequiredException(
          "Credential not found for user. Please log in again.");
    }

    Long expiresIn = credential.getExpiresInSeconds();
    if (expiresIn == null || expiresIn <= 60L) {
      try {
        boolean refreshed = credential.refreshToken();
        if (!refreshed || credential.getAccessToken() == null) {
          throw new AuthenticationRequiredException(
              "Access token refresh failed. Please try again.");
        }

        String newRefreshToken = credential.getRefreshToken();
        if (newRefreshToken != null && !newRefreshToken.equals(refreshToken)) {
          redisTemplate.opsForValue().set(redisRefreshTokenKey, newRefreshToken);
          log.info("Google Refresh Token updated in Redis for user: {}", userId);
        }
        log.info("Google Access Token refreshed for user: {}", userId);
      } catch (TokenResponseException e) {
        redisTemplate.delete(redisRefreshTokenKey);
        googleAuthorizationCodeFlow.getCredentialDataStore().delete(userId);
        log.error("Google token refresh failed for user {}: {}", userId, e.getMessage());
        throw new AuthenticationRequiredException("Session expired. Please log in again.");
      }
    }

    Gmail gmailService = gmailConfig.getGmailService(credential);
    return TokenRefreshResult.builder()
        .gmailService(gmailService)
        .credential(credential)
        .build();
  }

  @Override
  public MailListContainerResponse listEmails(String userId, int size)
      throws IOException, GeneralSecurityException {
    TokenRefreshResult result = getAuthenticatedGmailClient(userId);
    Gmail service = result.getGmailService();

    List<MailListResponse> emailList = Collections.synchronizedList(new ArrayList<>());
    List<Message> messages = fetchInboxMessages(service, size);
    JsonBatchCallback<Message> callback = getMessageJsonBatchCallback(emailList);

    int totalMessages = messages.size();
    for (int i = 0; i < totalMessages; i += BATCH_CHUNK_SIZE) {
      BatchRequest batch = service.batch();
      int end = Math.min(i + BATCH_CHUNK_SIZE, totalMessages);
      List<Message> chunk = messages.subList(i, end);

      for (Message message : chunk) {
        service.users().messages().get("me", message.getId()).setFormat("metadata")
            .queue(batch, callback);
      }

      batch.execute();
    }

    return MailListContainerResponse.builder()
        .mailListResponseList(emailList)
        .credential(result.getCredential())
        .build();
  }

  @Override
  public MailDetailContainerResponse getEmailContent(String messageId, String userId)
      throws IOException, GeneralSecurityException {
    TokenRefreshResult result = getAuthenticatedGmailClient(userId);
    Gmail service = result.getGmailService();
    Message message = service.users().messages().get("me", messageId).setFormat("full").execute();

    String threadId = message.getThreadId();
    MailDetailHeader headers = getHeaders(message);
    String bodyText = getPlainTextFromMessageParts(message.getPayload());
    List<Attachment> attachments = extractAttachments(message, service);

    MailDetailResponse mailDetailResponse = MailDetailResponse.builder().threadId(threadId).headers(headers).body(bodyText)
        .attachments(attachments).build();

    return MailDetailContainerResponse.builder()
        .mailDetailResponse(mailDetailResponse)
        .credential(result.getCredential())
        .build();
  }

  @Override
  public void sendMail(SendMailRequest sendMailRequest, String userId)
      throws MessagingException, IOException, GeneralSecurityException {
    TokenRefreshResult result = getAuthenticatedGmailClient(userId);
    Gmail service = result.getGmailService();
    MimeMessage mimeMessage = createReplyMessage(sendMailRequest.getTo(),
        sendMailRequest.getSubject(), sendMailRequest.getMessageContent(),
        sendMailRequest.getOriginalMessageId());
    Message message = sendMessage(mimeMessage);
    message.setThreadId(sendMailRequest.getThreadId());
    service.users().messages().send("me", message).execute();
  }

  private List<Message> fetchInboxMessages(Gmail service, int size) throws IOException {
    ListMessagesResponse response = service.users().messages().list("me")
        .setLabelIds(Collections.singletonList("INBOX")).setMaxResults((long) size).execute();
    return response.getMessages() != null ? response.getMessages() : Collections.emptyList();
  }

  private JsonBatchCallback<Message> getMessageJsonBatchCallback(List<MailListResponse> emailList) {
    JsonBatchCallback<Message> callback = new JsonBatchCallback<Message>() {
      @Override
      public void onSuccess(Message messageDetail, HttpHeaders responseHeaders) throws IOException {
        MailListResponse dto = parseMessageToDto(messageDetail);
        if (dto != null) {
          emailList.add(dto);
        }
      }

      @Override
      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
        log.error("Failed to get message detail via batch: " + e.getMessage());
      }
    };
    return callback;
  }

  private String formatDisplayDate(Date date) {
    Date now = new Date();

    SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm");
    SimpleDateFormat monthDayFormat = new SimpleDateFormat("M월 d일");
    SimpleDateFormat yearMonthDayFormat = new SimpleDateFormat("yy. MM. dd.");

    if (isSameDay(now, date)) {
      return timeFormat.format(date);
    } else if (isSameYear(now, date)) {
      return monthDayFormat.format(date);
    } else {
      return yearMonthDayFormat.format(date);
    }
  }

  private MailListHeader extractHeader(Message message) {
    String subject = "";
    String from = "";
    for (MessagePartHeader h : message.getPayload().getHeaders()) {
      if ("Subject".equalsIgnoreCase(h.getName())) {
        subject = h.getValue();
      } else if ("From".equalsIgnoreCase(h.getName())) {
        from = h.getValue();
      }
    }

    return MailListHeader.builder().subject(subject).from(from).build();
  }

  private boolean isSameDay(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
    return fmt.format(d1).equals(fmt.format(d2));
  }

  private boolean isSameYear(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy");
    return fmt.format(d1).equals(fmt.format(d2));
  }

  private MailDetailHeader getHeaders(Message message) {
    String subject = "";
    String from = "";
    String to = "";
    String date = "";
    for (MessagePartHeader header : message.getPayload().getHeaders()) {
      switch (header.getName()) {
        case "Subject":
          subject = header.getValue();
          break;
        case "From":
          from = header.getValue();
          break;
        case "To":
          to = header.getValue();
          break;
        case "Date":
          date = header.getValue();
          break;
      }
    }

    return MailDetailHeader.builder().subject(subject).from(from).to(to).date(date).build();
  }

  private MailListResponse parseMessageToDto(Message message) {
    if (message == null) {
      return null;
    }
    boolean unread = message.getLabelIds() != null && message.getLabelIds().contains("UNREAD");
    boolean hasAttachment = message.getPayload() != null && message.getPayload().getParts() != null
        && message.getPayload().getParts().stream()
        .anyMatch(p -> p.getFilename() != null && !p.getFilename().isEmpty());

    MailListHeader header = extractHeader(message);

    Date date = new Date(message.getInternalDate());
    String displayDate = formatDisplayDate(date);

    return MailListResponse.builder().messageId(message.getId()).date(displayDate)
        .hasAttachment(hasAttachment).headers(header).unread(unread).build();
  }

  private String getPlainTextFromMessageParts(MessagePart part) {
    if ("text/plain".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null
        && part.getBody().getData() != null) {
      byte[] bodyBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
      return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    if ("text/html".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null
        && part.getBody().getData() != null) {
      byte[] bodyBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
      String html = new String(bodyBytes, StandardCharsets.UTF_8);
      return Jsoup.parse(html).text();
    }

    if (part.getParts() != null) {
      for (MessagePart subPart : part.getParts()) {
        String result = getPlainTextFromMessageParts(subPart);
        if (!result.isBlank()) {
          return result;
        }
      }
    }

    return "";
  }

  private List<Attachment> extractAttachments(Message message, Gmail service) throws IOException {
    List<Attachment> attachments = new ArrayList<>();
    List<MessagePart> parts = message.getPayload().getParts();

    if (parts == null) {
      return attachments;
    }

    for (MessagePart part : parts) {
      if (part.getFilename() != null && !part.getFilename().isEmpty()
          && part.getBody().getAttachmentId() != null) {
        String attachmentId = part.getBody().getAttachmentId();
        MessagePartBody attachment = service.users().messages().attachments()
            .get("me", message.getId(), attachmentId).execute();

        String filename = part.getFilename();
        String mimeType = part.getMimeType();
        long size = part.getBody().getSize();
        String data = attachment.getData();
        Attachment file = Attachment.builder().filename(filename).mimeType(mimeType).size(size)
            .data(data).build();
        attachments.add(file);
      }
    }

    return attachments;
  }

  private MimeMessage createReplyMessage(String to, String subject, String replyMessage,
      String originalMessageId) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getInstance(props, null);

    MimeMessage reply = new MimeMessage(session);
    reply.setFrom(new InternetAddress("me"));
    reply.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
    reply.setSubject("Re: " + subject);
    reply.setText(replyMessage);

    reply.setHeader("In-Reply-To", originalMessageId);
    reply.setHeader("References", originalMessageId);

    return reply;
  }

  private Message sendMessage(MimeMessage email) throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] rawMessageBytes = buffer.toByteArray();
    String encodedEmail = Base64.getUrlEncoder().encodeToString(rawMessageBytes);

    Message message = new Message();
    message.setRaw(encodedEmail);

    return message;
  }
}
