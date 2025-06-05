package me.hu6r1s.mailbotix.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.auth.dto.NaverUserProfileResponse;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUtils {

  @Value("${naver.userinfo.uri}")
  private String NAVER_USER_INFO_URI;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public NaverUserProfileResponse.Response getUserInfoFromToken(HttpEntity request) {
    try {
      String responseBody = restTemplate.exchange(
          NAVER_USER_INFO_URI,
          HttpMethod.GET,
          request,
          String.class
      ).getBody();

      if (responseBody == null || responseBody.isEmpty()) {
        throw new RuntimeException("Naver user profile response is empty.");
      }

      NaverUserProfileResponse profileResponse = objectMapper.readValue(responseBody,
          NaverUserProfileResponse.class);

      if ("00".equals(profileResponse.getResultCode()) && profileResponse.getResponse() != null) {
        return profileResponse.getResponse();
      } else {
        log.error("Failed to get Naver user profile: ResultCode={}, Message={}",
            profileResponse.getResultCode(), profileResponse.getMessage());
        throw new AuthenticationRequiredException(
            "Failed to retrieve Naver user profile: " + profileResponse.getMessage());
      }
    } catch (HttpClientErrorException e) {
      log.error("Naver user profile HTTP error: {} - {}", e.getStatusCode(),
          e.getResponseBodyAsString(), e);
      throw new AuthenticationRequiredException(
          "Naver user profile request failed: " + e.getResponseBodyAsString());
    } catch (Exception e) {
      log.error("Error during Naver user profile retrieval", e);
      throw new RuntimeException("Failed to retrieve Naver user profile.", e);
    }
  }
}
