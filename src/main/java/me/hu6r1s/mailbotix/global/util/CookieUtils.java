package me.hu6r1s.mailbotix.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import me.hu6r1s.mailbotix.global.exception.AuthenticationRequiredException;

public class CookieUtils {

  private CookieUtils() { }

  public static String getUserIdFromCookie(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("userId".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    throw new AuthenticationRequiredException("UserId cookie not found. Please log in again.");
  }
}
