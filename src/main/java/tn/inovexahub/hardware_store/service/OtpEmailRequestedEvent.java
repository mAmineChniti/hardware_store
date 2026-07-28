package tn.inovexahub.hardware_store.service;

public class OtpEmailRequestedEvent {

  private final String email;
  private final String otpCode;
  private final int expiryMinutes;

  public OtpEmailRequestedEvent(String email, String otpCode, int expiryMinutes) {
    this.email = email;
    this.otpCode = otpCode;
    this.expiryMinutes = expiryMinutes;
  }

  public String getEmail() {
    return email;
  }

  public String getOtpCode() {
    return otpCode;
  }

  public int getExpiryMinutes() {
    return expiryMinutes;
  }
}
