package tn.inovexahub.hardware_store.service;

public class InvalidOtpException extends IllegalArgumentException {

  public InvalidOtpException(String message) {
    super(message);
  }
}
