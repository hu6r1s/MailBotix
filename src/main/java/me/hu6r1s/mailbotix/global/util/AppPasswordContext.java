package me.hu6r1s.mailbotix.global.util;

public class AppPasswordContext {
  private static final ThreadLocal<String> appPassword = new ThreadLocal<>();

  public static void set(String password) {
    appPassword.set(password);
  }

  public static String get() {
    return appPassword.get();
  }

  public static void clear() {
    appPassword.remove();
  }
}
