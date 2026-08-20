package tn.inovexahub.hardware_store.exception;

public class ProductVariantNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ProductVariantNotFoundException(String message) {
    super(message);
  }

  public ProductVariantNotFoundException(Long id) {
    super("Variant not found with id: " + id);
  }
}
