package tn.inovexahub.hardware_store.exception;

public class ClientNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ClientNotFoundException(String message) {
    super(message);
  }

  public ClientNotFoundException(Long id) {
    super("Client not found with id: " + id);
  }
}
