package tn.inovexahub.hardware_store.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import tn.inovexahub.hardware_store.service.InvalidOtpException;

class ExceptionClassTest {

  // ── ClientNotFoundException ────────────────────────────────────────────

  @Test
  void clientNotFoundException_withMessage() {
    ClientNotFoundException ex = new ClientNotFoundException("test message");
    assertEquals("test message", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void clientNotFoundException_withId() {
    ClientNotFoundException ex = new ClientNotFoundException(42L);
    assertEquals("Client not found with id: 42", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void clientNotFoundException_withZeroId() {
    ClientNotFoundException ex = new ClientNotFoundException(0L);
    assertEquals("Client not found with id: 0", ex.getMessage());
  }

  // ── SupplierNotFoundException ──────────────────────────────────────────

  @Test
  void supplierNotFoundException_withMessage() {
    SupplierNotFoundException ex = new SupplierNotFoundException("test message");
    assertEquals("test message", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void supplierNotFoundException_withId() {
    SupplierNotFoundException ex = new SupplierNotFoundException(99L);
    assertEquals("Supplier not found with id: 99", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  // ── CreditLimitExceededException ───────────────────────────────────────

  @Test
  void creditLimitExceededException_withMessage() {
    CreditLimitExceededException ex = new CreditLimitExceededException("over limit");
    assertEquals("over limit", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  // ── InvalidPaymentException ────────────────────────────────────────────

  @Test
  void invalidPaymentException_withMessage() {
    InvalidPaymentException ex = new InvalidPaymentException("bad payment");
    assertEquals("bad payment", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  // ── InvalidOtpException ────────────────────────────────────────────────

  @Test
  void invalidOtpException_withMessage() {
    InvalidOtpException ex = new InvalidOtpException("otp expired");
    assertEquals("otp expired", ex.getMessage());
    assertInstanceOf(IllegalArgumentException.class, ex);
  }
}
