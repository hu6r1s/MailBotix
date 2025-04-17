package me.hu6r1s.mailbotix.domain.gmail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailDetailHeader {

  private String date;
  private String subject;
  private String from;
  private String to;
}
