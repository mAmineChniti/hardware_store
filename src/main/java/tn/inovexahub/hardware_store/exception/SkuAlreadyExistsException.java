package tn.inovexahub.hardware_store.exception;

public class SkuAlreadyExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SkuAlreadyExistsException(String sku) {
    super("SKU already exists: " + sku);
  }
}
