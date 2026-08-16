package tn.inovexahub.hardware_store.exception;

public class ProductNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ProductNotFoundException(String message) {
    super(message);
  }

  public ProductNotFoundException(Long id) {
    super("Product not found with id: " + id);
  }
}
