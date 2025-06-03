package me.hu6r1s.mailbotix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import me.hu6r1s.mailbotix.domain.mail.dto.response.MailListContainerResponse;
import me.hu6r1s.mailbotix.domain.mail.service.GoogleMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleMailServiceTest {

  @Mock
  private Gmail mockGmailClient;
  @Mock
  private Gmail.Users mockUsers;
  @Mock
  private Gmail.Users.Messages mockMessages;
  @Mock
  private Gmail.Users.Messages.List mockListRequest;

  @InjectMocks
  private GoogleMailService googleMailService;

  @BeforeEach
  void setUp() throws IOException {
    lenient().when(mockGmailClient.users()).thenReturn(mockUsers);
    lenient().when(mockUsers.messages()).thenReturn(mockMessages);
    lenient().when(mockMessages.list(anyString())).thenReturn(mockListRequest);
  }

  @Test
  @DisplayName("listEmails 호출 시 지정된 size로 setMaxResults 호출")
  void listEmails_SetsMaxResultsCorrectly() throws IOException, GeneralSecurityException {
    // given
    int expectedSize = 25;
    Long expectedLongSize = (long) expectedSize;
    String userId = "asd123";
    ListMessagesResponse mockApiResponse = new ListMessagesResponse();
    when(mockListRequest.setMaxResults(expectedLongSize)).thenReturn(mockListRequest);
    when(mockListRequest.execute()).thenReturn(mockApiResponse);

    // when
    MailListContainerResponse result = googleMailService.listEmails(userId, expectedSize);

    // then
    verify(mockListRequest).setMaxResults(expectedLongSize);
    assertThat(result.getMailListResponseList()).isNotNull();
    assertThat(result.getMailListResponseList()).hasSize(expectedSize);
  }

}
