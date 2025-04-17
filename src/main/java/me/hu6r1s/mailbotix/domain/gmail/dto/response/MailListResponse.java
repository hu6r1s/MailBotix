package me.hu6r1s.mailbotix.domain.gmail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailListResponse {
  private String messageId;
  private String date;
  private MailListHeader headers;
  private boolean hasAttachment;
  private boolean unread;
}