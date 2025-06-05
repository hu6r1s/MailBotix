package me.hu6r1s.mailbotix.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverUserProfileResponse {

  @JsonProperty("resultcode") private String resultCode;
  private String message;
  private Response response;

  @Getter
  @NoArgsConstructor
  public static class Response {
    private String id;
    private String email;
  }
}
