package me.hu6r1s.mailbotix.domain.mail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

  private String filename;
  private long size;
  private String data;
  private String mimeType;
}
