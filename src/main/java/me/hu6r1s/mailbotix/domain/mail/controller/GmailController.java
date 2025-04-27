package me.hu6r1s.mailbotix.domain.mail.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.services.gmail.Gmail;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import me.hu6r1s.mailbotix.domain.mail.service.GoogleMailService;
import me.hu6r1s.mailbotix.global.config.GmailConfig;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.util.CookieUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
@CrossOrigin(origins = "${frontend.url}", allowCredentials = "true")
public class GmailController implements GmailControllerDocs {

  private final GoogleMailService googleMailService;
  private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
  private final GmailConfig gmailConfig;
  private final StringRedisTemplate redisTemplate;

  public Gmail getGmailServiceForCurrentUser(HttpServletRequest request) throws IOException, GeneralSecurityException {
    String userId = CookieUtils.getUserIdFromCookie(request);
    String refreshToken = redisTemplate.opsForValue().get(userId);

    if (refreshToken == null) {
      throw new AuthenticationRequiredException("Session expired. Please log in again.");
    }

    Credential credential = googleAuthorizationCodeFlow.loadCredential(userId);

    if (credential == null) {
      throw new AuthenticationRequiredException("Credential not found for user. Please log in again.");
    }

    Long expiresIn = credential.getExpiresInSeconds();

    if (expiresIn != null && expiresIn <= 60) {
      try {
        boolean refreshed = credential.refreshToken();
        if (!refreshed) {
          throw new AuthenticationRequiredException("Access token refresh failed. Please try again.");
        }
      } catch (TokenResponseException e) {
        throw new AuthenticationRequiredException("Session expired. Please log in again.");
      }
    }

    return gmailConfig.getGmailService(credential);
  }

  @GetMapping("/list")
  public List<MailListResponse> listEmails(
      HttpServletRequest request,
      @RequestParam(name = "size", defaultValue = "10") int size
      )
      throws GeneralSecurityException, IOException {
      Gmail service = getGmailServiceForCurrentUser(request);
      return googleMailService.listEmails(service, size);
  }

  @GetMapping("/read/{messageId}")
  public MailDetailResponse getEmailContent(@PathVariable String messageId, HttpServletRequest request)
      throws IOException, GeneralSecurityException {
      Gmail service = getGmailServiceForCurrentUser(request);
      return googleMailService.getEmailContent(messageId, service);
  }

  @PostMapping("/send")
  public void sendReply(@Valid @RequestBody SendMailRequest sendMailRequest, HttpServletRequest request)
      throws MessagingException, GeneralSecurityException, IOException {
      Gmail service = getGmailServiceForCurrentUser(request);
      googleMailService.sendReply(sendMailRequest, service);
  }
}
