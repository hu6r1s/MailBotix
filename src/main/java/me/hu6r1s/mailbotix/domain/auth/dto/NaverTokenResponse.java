package me.hu6r1s.mailbotix.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverTokenResponse {
  @JsonProperty("access_token") private String accessToken;
  @JsonProperty("refresh_token") private String refreshToken;
  @JsonProperty("token_type") private String tokenType;
  @JsonProperty("expires_in") private Integer expiresIn;
  private String error;
  @JsonProperty("error_description") private String errorDescription;
}
