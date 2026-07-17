package tn.inovexahub.hardware_store.exception;

public class CreditLimitExceededException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CreditLimitExceededException(String message) {
    super(message);
  }
}
