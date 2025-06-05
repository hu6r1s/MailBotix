package me.hu6r1s.mailbotix.domain.mail.controller;

import com.auth0.jwt.JWT;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hu6r1s.mailbotix.domain.mail.MailProvider;
import me.hu6r1s.mailbotix.domain.mail.dto.request.SendMailRequest;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailDetailResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListResponse;
import me.hu6r1s.mailbotix.domain.mail.service.MailService;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import me.hu6r1s.mailbotix.global.util.AppPasswordContext;
import me.hu6r1s.mailbotix.global.util.CookieUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
public class MailController implements MailControllerDocs {

  private final Map<String, MailService> mailServices;
  private final CookieUtils cookieUtils;
  private static final String SESSION_PROVIDER_KEY = "provider";

  private MailService getActiveMailService(HttpSession session) {
    if (session == null) {
      log.warn("Session is null.");
      throw new AuthenticationRequiredException("Session not found.Please login again.");
    }
    String provider = (String) session.getAttribute(SESSION_PROVIDER_KEY);
    if (provider == null || provider.isEmpty()) {
      log.warn("Mail provider not found in session.");
      throw new AuthenticationRequiredException("Mail provider not selected. Please login again.");
    }
    MailService service = mailServices.get(provider.toLowerCase() + "MailService");
    if (service == null) {
      log.error("No MailService implementation found for provider: {}", provider);
      throw new IllegalStateException("Unsupported mail provider: " + provider);
    }
    log.debug("Using mail service for provider: {}", provider);
    return service;
  }


  @Override
  @GetMapping("/list")
  public ResponseEntity<List<MailListResponse>> listEmails(
      HttpServletRequest request, HttpServletResponse response,
      @RequestParam(name = "size", defaultValue = "10") int size
  ) throws GeneralSecurityException, IOException {
    try {
      HttpSession session = request.getSession(false);
      MailService mailService = getActiveMailService(session);

      MailProvider mailProvider = MailProvider.valueOf(
          ((String) session.getAttribute(SESSION_PROVIDER_KEY)).toUpperCase());
      MailListContainerResponse mailListContainerResponse = null;
      switch (mailProvider) {
        case GOOGLE -> {
          String accessToken = cookieUtils.getAccessTokenFromCookie(request);
          String userId = JWT.decode(accessToken).getSubject();
          mailListContainerResponse = mailService.listEmails(userId, size);
        }
        case NAVER -> {
          String accessToken = cookieUtils.getAccessTokenFromCookie(request);
          String password = cookieUtils.getAppPasswordFromCookie(request);
          AppPasswordContext.set(password);
          mailListContainerResponse = mailService.listEmails(accessToken, size);
          AppPasswordContext.clear();
        }
      }

      return ResponseEntity.ok(mailListContainerResponse.getMailListResponseList());
    } catch (AuthenticationRequiredException e) {
      log.warn("Authentication required for listing emails: {}", e.getMessage());
      throw new AuthenticationRequiredException("Authentication required for listing emails: {}",
          e);
    } catch (IllegalStateException e) {
      log.error("Error selecting mail service: {}", e.getMessage());
      throw new IllegalStateException("Error selecting mail service: {}", e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @GetMapping("/read/{messageId}")
  public MailDetailResponse getEmailContent(@PathVariable String messageId,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException, GeneralSecurityException {
    HttpSession session = request.getSession(false);
    MailService mailService = getActiveMailService(session);
    String accessToken = cookieUtils.getAccessTokenFromCookie(request);
    MailDetailContainerResponse mailDetailContainerResponse = mailService.getEmailContent(messageId,
        accessToken);

    ResponseCookie userIdCookie = ResponseCookie.from("access_token", accessToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(
            Duration.ofSeconds(mailDetailContainerResponse.getCredential().getExpiresInSeconds()))
        .build();
    response.addHeader("Set-Cookie", userIdCookie.toString());
    return mailDetailContainerResponse.getMailDetailResponse();
  }

  @Override
  @PostMapping("/send")
  public void sendMail(@Valid @RequestBody SendMailRequest sendMailRequest,
      HttpServletRequest request, HttpServletResponse response)
      throws MessagingException, GeneralSecurityException, IOException {
    HttpSession session = request.getSession(false);
    MailService mailService = getActiveMailService(session);
    String accessToken = cookieUtils.getAccessTokenFromCookie(request);
    mailService.sendMail(sendMailRequest, accessToken);
  }
}
