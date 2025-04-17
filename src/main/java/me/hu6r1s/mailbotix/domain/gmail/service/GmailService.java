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
import java.util.List;
import java.util.Properties;
import me.hu6r1s.mailbotix.domain.gmail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.gmail.dto.response.Attachment;
import me.hu6r1s.mailbotix.domain.gmail.dto.response.MailDetailHeader;
import me.hu6r1s.mailbotix.domain.gmail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.gmail.dto.response.MailListHeader;
import me.hu6r1s.mailbotix.domain.gmail.dto.response.MailListResponse;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class GmailService {

  public List<MailListResponse> listEmails(Gmail service) throws IOException {
    List<MailListResponse> emailList = new ArrayList<>();
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

      String messageId = fullMessage.getId();

      String subject = "";
      String from = "";
      for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
        if (header.getName().equalsIgnoreCase("Subject")) {
          subject = header.getValue();
        } else if (header.getName().equalsIgnoreCase("From")) {
          from = header.getValue();
        }
      }
      MailListHeader header = MailListHeader.builder()
          .subject(subject)
          .from(from)
          .build();

      List<String> labels = fullMessage.getLabelIds();
      boolean unread = labels != null && labels.contains("UNREAD");
      boolean hasAttachment = labels != null && labels.contains("HAS_ATTACHMENT");

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
      MailListResponse mail = MailListResponse.builder()
          .messageId(messageId)
          .date(displayDate)
          .headers(header)
          .unread(unread)
          .hasAttachment(hasAttachment)
          .build();

      emailList.add(mail);
    }
    return emailList;
  }

  public MailDetailResponse getEmailContent(String messageId, Gmail service) throws IOException {
    Message message = service.users().messages().get("me", messageId)
        .setFormat("full")
        .execute();

    String threadId = message.getThreadId();
    MailDetailHeader headers = getHeaders(message);
    String bodyText = getPlainTextFromMessageParts(message.getPayload());
    List<Attachment> attachments = extractAttachments(message, service);

    return MailDetailResponse.builder().threadId(threadId).headers(headers).body(bodyText).attachments(attachments).build();
  }

  public void sendReply(SendMailRequest sendMailRequest, Gmail service)
      throws MessagingException, IOException {
    MimeMessage mimeMessage = createReplyMessage(sendMailRequest.getTo(),
        sendMailRequest.getSubject(), sendMailRequest.getMessageContent(),
        sendMailRequest.getOriginalMessageId());
    Message message = sendMessage(mimeMessage);
    message.setThreadId(sendMailRequest.getThreadId());
    service.users().messages().send("me", message).execute();
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

  private String getPlainTextFromMessageParts(MessagePart part) {
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
            .get("me", message.getId(), attachmentId)
            .execute();

        String filename = part.getFilename();
        String mimeType = part.getMimeType();
        long size = part.getBody().getSize();
        String data = attachment.getData();
        Attachment file = Attachment.builder().filename(filename).mimeType(mimeType).size(size).data(data).build();
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
