package me.hu6r1s.mailbotix.domain.gmail.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.hu6r1s.mailbotix.global.config.GmailConfig;
import org.springframework.stereotype.Service;

@Service
public class GmailService {

  private final Gmail gmail;

  public GmailService(GmailConfig gmailConfig) throws IOException, GeneralSecurityException {
    this.gmail = gmailConfig.getGmailService();
  }

  public List<Map<String, Object>> listEmails() throws IOException {
    List<Map<String, Object>> emailList = new ArrayList<>();
    ListMessagesResponse response = gmail.users().messages().list("me")
        .setLabelIds(Collections.singletonList("INBOX"))
        .setMaxResults(10L)
        .execute();

    List<Message> messages = response.getMessages();
    if (messages == null || messages.isEmpty()) {
      return emailList;
    }

    for (Message message : messages) {
      Message fullMessage = gmail.users().messages().get("me", message.getId())
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

  public String getEmailContent(String messageId) throws IOException {
    Message message = gmail.users().messages().get("me", messageId).execute();
    return message.getSnippet();
  }

  private boolean isSameDay(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
    return fmt.format(d1).equals(fmt.format(d2));
  }

  private boolean isSameYear(Date d1, Date d2) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy");
    return fmt.format(d1).equals(fmt.format(d2));
  }

}
