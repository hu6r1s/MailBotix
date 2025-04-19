package me.hu6r1s.mailbotix.global.exception;

public class AuthenticationRequiredException extends
    RuntimeException {

  public AuthenticationRequiredException(String message) {
    super(message);
  }
  public AuthenticationRequiredException(String message, Throwable cause) {
    super(message, cause);
  }
}
