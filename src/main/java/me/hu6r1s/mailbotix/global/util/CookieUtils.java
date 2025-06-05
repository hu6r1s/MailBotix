package me.hu6r1s.mailbotix.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieUtils {

  @Value("${app-password.secrey.key}")
  private String APP_PASSWORD_KEY;

  @Value("${app-password.iv}")
  private String APP_PASSWORD_IV;

  public String getAccessTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("access_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    throw new AuthenticationRequiredException("access_token cookie not found. Please log in again.");
  }

  public String getAppPasswordFromCookie(HttpServletRequest request) throws Exception {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("app_password".equals(cookie.getName())) {
          return decryptAES(cookie.getValue(), APP_PASSWORD_KEY);
        }
      }
    }
    throw new AuthenticationRequiredException("access_token cookie not found. Please log in again.");
  }

  private String decryptAES(String encryptedPassword, String secretKey) throws Exception {
    byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);
    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    byte[] ivBytes = APP_PASSWORD_IV.getBytes(StandardCharsets.UTF_8);

    SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
    byte[] original = cipher.doFinal(encryptedBytes);

    return new String(original, StandardCharsets.UTF_8);
  }
}
