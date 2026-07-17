package tn.inovexahub.hardware_store.exception;

public class SupplierNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SupplierNotFoundException(String message) {
    super(message);
  }

  public SupplierNotFoundException(Long id) {
    super("Supplier not found with id: " + id);
  }
}
