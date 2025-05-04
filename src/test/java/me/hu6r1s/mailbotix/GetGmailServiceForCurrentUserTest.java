package me.hu6r1s.mailbotix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.services.gmail.Gmail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.GeneralSecurityException;
import me.hu6r1s.mailbotix.domain.mail.controller.GmailController;
import me.hu6r1s.mailbotix.global.config.GmailConfig;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetGmailServiceForCurrentUserTest {

  @Mock
  private GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
  @Mock
  private GmailConfig gmailConfig;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private HttpSession session;
  @Mock
  private Credential credential;
  @Mock
  private Gmail mockGmailService;

  @InjectMocks
  private GmailController gmailController;

  private final String testUserId = "testUser123";
  private final String sessionUserIdKey = "userId";

  @BeforeEach
  void setUp() throws GeneralSecurityException, IOException {
    lenient().when(gmailConfig.getGmailService(any(Credential.class))).thenReturn(mockGmailService);
  }

  @Test
  @DisplayName("세션 또는 userId 없을 시 AuthenticationRequiredException 발생")
  void getGmailService_NoSessionOrUserId_ThrowsException() {
    // given
    when(request.getSession(false)).thenReturn(null); // 세션 없음 시뮬레이션

    // when & then
    assertThrows(AuthenticationRequiredException.class,
        () -> gmailController.getGmailServiceForCurrentUser(request, response),
        "User not authenticated. Session or userId missing.");

    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(null); // userId 없음 시뮬레이션

    // when & then
    assertThrows(AuthenticationRequiredException.class,
        () -> gmailController.getGmailServiceForCurrentUser(request, response),
        "User not authenticated. Session or userId missing.");
  }

  @Test
  @DisplayName("DataStore에 Credential 없을 시 AuthenticationRequiredException 발생")
  void getGmailService_CredentialNotFound_ThrowsException() throws IOException {
    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(testUserId);
    when(googleAuthorizationCodeFlow.loadCredential(testUserId)).thenReturn(null); // Credential 없음 시뮬레이션

    // when & then
    assertThrows(AuthenticationRequiredException.class,
        () -> gmailController.getGmailServiceForCurrentUser(request, response),
        "Credential not found for user. Please log in again.");
  }

  @Test
  @DisplayName("유효한 Credential 로드 성공 (Refresh 불필요)")
  void getGmailService_ValidCredential_Success() throws Exception {
    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(testUserId);
    when(googleAuthorizationCodeFlow.loadCredential(testUserId)).thenReturn(credential);
    when(credential.getExpiresInSeconds()).thenReturn(3600L); // 1시간 남음 시뮬레이션

    // when
    Gmail resultService = gmailController.getGmailServiceForCurrentUser(request, response);

    // then
    assertThat(resultService).isNotNull(); // Gmail 서비스 객체 반환 확인
    verify(credential, never()).refreshToken(); // refreshToken 호출 안 됨 확인
    verify(gmailConfig, times(1)).getGmailService(credential); // getGmailService 호출 확인
  }

  @Test
  @DisplayName("Access Token 만료 임박 시 Refresh 성공")
  void getGmailService_TokenAboutToExpire_RefreshSuccess() throws Exception {
    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(testUserId);
    when(googleAuthorizationCodeFlow.loadCredential(testUserId)).thenReturn(credential);
    when(credential.getExpiresInSeconds()).thenReturn(50L); // 50초 남음 시뮬레이션
    when(credential.refreshToken()).thenReturn(true); // Refresh 성공 시뮬레이션

    // when
    Gmail resultService = gmailController.getGmailServiceForCurrentUser(request, response);

    // then
    assertThat(resultService).isNotNull();
    verify(credential, times(1)).refreshToken(); // refreshToken 호출 확인
    verify(gmailConfig, times(1)).getGmailService(credential);
  }

  @Test
  @DisplayName("Access Token 만료 임박 시 Refresh 실패 (refreshToken false 반환)")
  void getGmailService_TokenAboutToExpire_RefreshReturnsFalse() throws Exception {
    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(testUserId);
    when(googleAuthorizationCodeFlow.loadCredential(testUserId)).thenReturn(credential);
    when(credential.getExpiresInSeconds()).thenReturn(50L);
    when(credential.refreshToken()).thenReturn(false); // Refresh false 반환 시뮬레이션

    // when & then
    assertThrows(AuthenticationRequiredException.class,
        () -> gmailController.getGmailServiceForCurrentUser(request, response),
        "Could not refresh token (returned false). Please log in again.");
    verify(gmailConfig, never()).getGmailService(any()); // getGmailService 호출 안 됨
  }

  @Test
  @DisplayName("Access Token 만료 임박 시 Refresh 실패 (TokenResponseException 발생)")
  void getGmailService_TokenAboutToExpire_RefreshThrowsTokenResponseException() throws Exception {
    // given
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(sessionUserIdKey)).thenReturn(testUserId);
    when(googleAuthorizationCodeFlow.loadCredential(testUserId)).thenReturn(credential);
    when(credential.getExpiresInSeconds()).thenReturn(50L);
    when(credential.refreshToken()).thenThrow(new AuthenticationRequiredException("Access token refresh failed. Please try again."));

    // when & then
    AuthenticationRequiredException exception = assertThrows(AuthenticationRequiredException.class,
        () -> gmailController.getGmailServiceForCurrentUser(request, response));

    assertThat(exception.getMessage()).contains("refresh failed");
    verify(gmailConfig, never()).getGmailService(any());
  }
}
