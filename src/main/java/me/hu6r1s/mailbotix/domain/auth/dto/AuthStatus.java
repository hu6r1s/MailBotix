package me.hu6r1s.mailbotix.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthStatus {

  private boolean loggedIn;
  private String userId;
}
