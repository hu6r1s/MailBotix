package me.hu6r1s.mailbotix.domain.reply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.domain.reply.dto.request.ReplyGenerationRequest;
import me.hu6r1s.mailbotix.domain.reply.dto.response.ReplyGenerationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ReplyService {

  @Value("${gemini.api.url}")
  private String geminiApiUrl;

  @Value("${gemini.api.key}")
  private String geminiApiKey;

  public ReplyGenerationResponse generateEmailReply(ReplyGenerationRequest replyGenerationRequest) {
    String prompt = buildPrompt(replyGenerationRequest);

    Map<String, Object> requestBody = Map.of(
        "contents", new Object[]{
            Map.of("parts", new Object[]{
                Map.of("text", prompt)
            })
        }
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
    RestTemplate restTemplate = new RestTemplate();

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl + geminiApiKey, request, String.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return extractResponseContent(response.getBody());
      } else {
        return new ReplyGenerationResponse("Gemini API call failed: " + response.getStatusCode());
      }

    } catch (Exception e) {
      return new ReplyGenerationResponse("An error occurred while requesting Gemini API: " + e.getMessage());
    }
  }

  private ReplyGenerationResponse extractResponseContent(String response) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(response);
      String reply = rootNode.path("candidates")
          .get(0)
          .path("content")
          .path("parts")
          .get(0)
          .path("text")
          .asText();
      return new ReplyGenerationResponse(reply);
    } catch (Exception e) {
      return new ReplyGenerationResponse(e.getMessage());
    }
  }

  private String buildPrompt(ReplyGenerationRequest replyGenerationRequest) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Generate an email reply using the same language as the email content. ");
    if (replyGenerationRequest.getTone() != null && !replyGenerationRequest.getTone().isEmpty()) {
      prompt.append("Match the ").append(replyGenerationRequest.getTone()).append(" tone and ensure the response is polite and clear.");
    }
    prompt.append("\nEmail: \n").append(replyGenerationRequest.getEmailContent());
    return prompt.toString();
  }
}
