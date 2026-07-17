package tn.inovexahub.hardware_store.exception;

public class InvalidPaymentException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidPaymentException(String message) {
    super(message);
  }
}
