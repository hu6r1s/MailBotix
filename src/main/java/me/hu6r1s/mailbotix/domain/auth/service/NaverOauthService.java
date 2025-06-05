package me.hu6r1s.mailbotix.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.auth.dto.NaverTokenResponse;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverOauthService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${naver.client.id}")
  private String NAVER_CLIENT_ID;
  @Value("${naver.client.secret}")
  private String NAVER_CLIENT_SECRET;
  @Value("${naver.redirect.uri}")
  private String NAVER_REDIRECT_URI;
  @Value("${naver.authorization.uri}")
  private String NAVER_AUTHORIZATION_URI;
  @Value("${naver.token.uri}")
  private String NAVER_TOKEN_URI;

  @Value("${naver.scope}")
  private String NAVER_SCOPES;

  public String buildAuthUrl(String state) {
    return UriComponentsBuilder.fromUriString(NAVER_AUTHORIZATION_URI)
        .queryParam("response_type", "code")
        .queryParam("client_id", NAVER_CLIENT_ID)
        .queryParam("redirect_uri", NAVER_REDIRECT_URI)
        .queryParam("state", state)
        .queryParam("scope", NAVER_SCOPES)
        .build().toUriString();
  }

  public NaverTokenResponse getToken(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", NAVER_CLIENT_ID);
    params.add("client_secret", NAVER_CLIENT_SECRET);
    params.add("code", code);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      NaverTokenResponse tokenResponse = restTemplate.postForObject(NAVER_TOKEN_URI, request,
          NaverTokenResponse.class);
      return tokenResponse;
    } catch (HttpClientErrorException e) {
      log.error("Naver token exchange HTTP error: {} - {}", e.getStatusCode(),
          e.getResponseBodyAsString(), e);
      String errorBody = e.getResponseBodyAsString();
      try {
        if (errorBody != null && !errorBody.isEmpty()) {
          NaverTokenResponse errorResponse = objectMapper.readValue(errorBody, NaverTokenResponse.class);
          throw new AuthenticationRequiredException("Naver token exchange failed: " +
              (errorResponse.getErrorDescription() != null ? errorResponse.getErrorDescription() : errorBody));
        }
      } catch (Exception parseEx) {
        log.error("Failed to parse Naver error response body: {}", parseEx.getMessage());
      }
      throw new AuthenticationRequiredException("Naver token exchange failed: " + e.getMessage());
    } catch (Exception e) {
      log.error("Error during Naver token exchange", e);
      throw new RuntimeException("Failed to exchange Naver code for token.", e);
    }
  }
}
