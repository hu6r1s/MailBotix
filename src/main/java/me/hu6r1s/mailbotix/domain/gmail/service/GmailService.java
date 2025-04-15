package me.hu6r1s.mailbotix.domain.gmail.service;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import me.hu6r1s.mailbotix.domain.gmail.dto.request.SendMailRequest;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class GmailService {

  public List<Map<String, Object>> listEmails(Gmail service) throws IOException {
    List<Map<String, Object>> emailList = new ArrayList<>();
    ListMessagesResponse response = service.users().messages().list("me")
        .setLabelIds(Collections.singletonList("INBOX"))
        .setMaxResults(10L)
        .execute();

    List<Message> messages = response.getMessages();
    if (messages == null || messages.isEmpty()) {
      return emailList;
    }

    for (Message message : messages) {
      Message fullMessage = service.users().messages().get("me", message.getId())
          .setFormat("metadata")
          .setMetadataHeaders(Arrays.asList("Subject", "From", "Data"))
          .execute();

      Map<String, Object> mailInfo = new HashMap<>();
      mailInfo.put("messageId", fullMessage.getId());
      mailInfo.put("threadId", fullMessage.getThreadId());

      Map<String, String> headers = new HashMap<>();
      for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
        if (header.getName().equalsIgnoreCase("Subject")) {
          headers.put("subject", header.getValue());
        } else if (header.getName().equalsIgnoreCase("From")) {
          headers.put("from", header.getValue());
        }
      }
      mailInfo.put("headers", headers);

      List<String> labels = fullMessage.getLabelIds();
      mailInfo.put("unread", labels != null && labels.contains("UNREAD"));
      mailInfo.put("starred", labels != null && labels.contains("STARRED"));
      mailInfo.put("important", labels != null && labels.contains("IMPORTANT"));
      mailInfo.put("hasAttachment", labels != null && labels.contains("HAS_ATTACHMENT"));

      Date now = new Date();
      Date date = new Date(fullMessage.getInternalDate());
      String displayDate;

      SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm");
      SimpleDateFormat monthDayFormat = new SimpleDateFormat("M월 d일");
      SimpleDateFormat yearMonthDayFormat = new SimpleDateFormat("yy. MM. dd.");

      boolean isToday = isSameDay(now, date);
      boolean isThisYear = isSameYear(now, date);

      if (isToday) {
        displayDate = timeFormat.format(date);
      } else if (isThisYear) {
        displayDate = monthDayFormat.format(date);
      } else {
        displayDate = yearMonthDayFormat.format(date);
      }

      mailInfo.put("date", displayDate);

      emailList.add(mailInfo);
    }
    return emailList;
  }

  public Map<String, Object> getEmailContent(String messageId, Gmail service) throws IOException {
    Message message = service.users().messages().get("me", messageId)
        .setFormat("full")
        .execute();

    Map<String, Object> mailDetails = new HashMap<>();

    String threadId = message.getThreadId();
    mailDetails.put("threadId", threadId);

    Map<String, String> headers = getHeaders(message);
    mailDetails.put("headers", headers);

    String bodyText = getPlainTextFromMessageParts(message.getPayload());
    mailDetails.put("body", bodyText);

    List<Map<String, Object>> attachments = extractAttachments(message, service);
    mailDetails.put("attachments", attachments);

    return mailDetails;
  }

  public Message sendReply(SendMailRequest sendMailRequest, Gmail service)
      throws MessagingException, IOException {
    MimeMessage mimeMessage = createReplyMessage(sendMailRequest.getTo(),
        sendMailRequest.getSubject(), sendMailRequest.getMessageContent(),
        sendMailRequest.getOriginalMessageId());
    Message message = sendMessage(mimeMessage);
    message.setThreadId(sendMailRequest.getThreadId());
    return service.users().messages().send("me", message).execute();
  }

  private boolean isSameDay(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
    return fmt.format(d1).equals(fmt.format(d2));
  }

  private boolean isSameYear(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy");
    return fmt.format(d1).equals(fmt.format(d2));
  }

  private static Map<String, String> getHeaders(Message message) {
    Map<String, String> headers = new HashMap<>();

    for (MessagePartHeader header : message.getPayload().getHeaders()) {
      switch (header.getName()) {
        case "Subject":
          headers.put("subject", header.getValue());
          break;
        case "From":
          headers.put("from", header.getValue());
          break;
        case "To":
          headers.put("to", header.getValue());
          break;
        case "Date":
          headers.put("date", header.getValue());
          break;
      }
    }
    return headers;
  }

  private String getPlainTextFromMessageParts(MessagePart part) throws IOException {
    if ("text/plain".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
      byte[] bodyBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
      return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    if ("text/html".equalsIgnoreCase(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
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

  private List<Map<String, Object>> extractAttachments(Message message, Gmail service) throws IOException {
    List<Map<String, Object>> attachments = new ArrayList<>();
    List<MessagePart> parts = message.getPayload().getParts();

    if (parts == null) {
      return attachments;
    }

    for (MessagePart part : parts) {
      if (part.getFilename() != null && !part.getFilename().isEmpty()
          && part.getBody().getAttachmentId() != null) {
        String attachmentId = part.getBody().getAttachmentId();
        MessagePartBody attachment = service.users().messages().attachments()
            .get("me", message.getId(), attachmentId)
            .execute();

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("filename", part.getFilename());
        fileInfo.put("mimeType", part.getMimeType());
        fileInfo.put("size", part.getBody().getSize());
        fileInfo.put("data", attachment.getData());

        attachments.add(fileInfo);
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
