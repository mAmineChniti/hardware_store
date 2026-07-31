package tn.inovexahub.hardware_store.service;

public class InvalidOtpException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public InvalidOtpException(String message) {
    super(message);
  }
}
