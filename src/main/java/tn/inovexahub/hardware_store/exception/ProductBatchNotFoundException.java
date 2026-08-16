package tn.inovexahub.hardware_store.exception;

public class ProductBatchNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ProductBatchNotFoundException(String message) {
    super(message);
  }

  public ProductBatchNotFoundException(Long id) {
    super("Product batch not found with id: " + id);
  }
}
