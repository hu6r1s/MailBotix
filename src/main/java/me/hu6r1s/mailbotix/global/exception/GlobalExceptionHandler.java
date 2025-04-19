package me.hu6r1s.mailbotix.global.exception;

import com.google.api.client.auth.oauth2.TokenResponseException;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import me.hu6r1s.mailbotix.global.exception.errorcode.AuthErrorCode;
import me.hu6r1s.mailbotix.global.exception.errorcode.EmailErrorCode;
import me.hu6r1s.mailbotix.global.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(TokenResponseException.class)
  public ResponseEntity<ErrorResponse> handleTokenResponseException(TokenResponseException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.GOOGLE_TOKEN_EXCHANGE_FAILED;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(CredentialStorageException.class)
  public ResponseEntity<ErrorResponse> handleCredentialStorageException(CredentialStorageException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.CREDENTIAL_STORAGE_FAILED;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(CredentialDeleteException.class)
  public ResponseEntity<ErrorResponse> handleCredentialDeleteException(CredentialDeleteException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.CREDENTIAL_DELETE_FAILED;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ErrorResponse> handleIOException(IOException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.INTERNAL_AUTH_ERROR;

    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(errorCode.getMessage())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(MessagingException.class)
  public ResponseEntity<ErrorResponse> handleMessagingException(MessagingException ex, WebRequest request) {
    EmailErrorCode errorCode = EmailErrorCode.EMAIL_MESSAGING_ERROR;

    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(errorCode.getMessage())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(GeneralSecurityException.class)
  public ResponseEntity<ErrorResponse> handleGeneralSecurityException(GeneralSecurityException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.GOOGLE_CLIENT_SETUP_FAILED;

    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(errorCode.getMessage())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
  }

  @ExceptionHandler(AuthenticationRequiredException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleAuthenticationRequiredException(AuthenticationRequiredException ex, WebRequest request) {
    AuthErrorCode errorCode = AuthErrorCode.CREDENTIAL_UNAUTHORIZED;

    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .message(ex.getMessage())
        .status(errorCode.getHttpStatus().value())
        .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }
}
