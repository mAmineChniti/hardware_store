package tn.inovexahub.hardware_store.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.ErrorResponse;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @Mock private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    when(request.getRequestURI()).thenReturn("/api/test");
  }

  @Test
  void handleClientNotFoundException() {
    ClientNotFoundException ex = new ClientNotFoundException("Client not found");

    ResponseEntity<ErrorResponse> response = handler.handleClientNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Client Not Found", response.getBody().getError());
    assertEquals("Client not found", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
    assertNotNull(response.getBody().getTimestamp());
    assertNull(response.getBody().getValidationErrors());
  }

  @Test
  void handleSupplierNotFoundException() {
    SupplierNotFoundException ex = new SupplierNotFoundException("Supplier not found");

    ResponseEntity<ErrorResponse> response = handler.handleSupplierNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Supplier Not Found", response.getBody().getError());
    assertEquals("Supplier not found", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleProductNotFoundException() {
    ProductNotFoundException ex = new ProductNotFoundException("Product not found");

    ResponseEntity<ErrorResponse> response = handler.handleProductNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Product Not Found", response.getBody().getError());
    assertEquals("Product not found", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleSkuAlreadyExistsException() {
    SkuAlreadyExistsException ex = new SkuAlreadyExistsException("SCREW-6MM");

    ResponseEntity<ErrorResponse> response = handler.handleSkuAlreadyExistsException(ex, request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(409, response.getBody().getStatus());
    assertEquals("SKU Already Exists", response.getBody().getError());
    assertEquals("SKU already exists: SCREW-6MM", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleMissingServletRequestParameter() {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("sku", "String");

    ResponseEntity<ErrorResponse> response =
        handler.handleMissingServletRequestParameter(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Missing Parameter", response.getBody().getError());
    assertEquals("Required parameter 'sku' is missing", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleCreditLimitExceededException() {
    CreditLimitExceededException ex =
        new CreditLimitExceededException("Credit limit exceeded for client");

    ResponseEntity<ErrorResponse> response =
        handler.handleCreditLimitExceededException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Credit Limit Exceeded", response.getBody().getError());
    assertEquals("Credit limit exceeded for client", response.getBody().getMessage());
  }

  @Test
  void handleInvalidPaymentException() {
    InvalidPaymentException ex = new InvalidPaymentException("Payment amount is negative");

    ResponseEntity<ErrorResponse> response = handler.handleInvalidPaymentException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Invalid Payment", response.getBody().getError());
    assertEquals("Payment amount is negative", response.getBody().getMessage());
  }

  @Test
  void handleUnreadableMessage() {
    HttpMessageNotReadableException ex =
        org.mockito.Mockito.mock(HttpMessageNotReadableException.class);
    when(ex.getMessage()).thenReturn("Malformed JSON");

    ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Malformed Request", response.getBody().getError());
    assertEquals(
        "Request body is malformed or contains invalid values", response.getBody().getMessage());
  }

  @Test
  void handleResponseStatusException() {
    ResponseStatusException ex =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");

    ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Resource not found", response.getBody().getMessage());
  }

  @Test
  void handleResponseStatusException_nullReason() {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

    ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Response Status Error", response.getBody().getMessage());
  }

  @Test
  void handleAuthenticationException_genericAuth() {
    InsufficientAuthenticationException ex =
        new InsufficientAuthenticationException("Full authentication required");

    ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(401, response.getBody().getStatus());
    assertEquals("Authentication Failed", response.getBody().getError());
    assertEquals("Authentication failed", response.getBody().getMessage());
  }

  @Test
  void handleAuthenticationException_badCredentials() {
    BadCredentialsException ex = new BadCredentialsException("Bad credentials");

    ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(401, response.getBody().getStatus());
    assertEquals("Authentication Failed", response.getBody().getError());
    assertEquals("Invalid email or password", response.getBody().getMessage());
  }

  @Test
  void handleAccessDeniedException() {
    AccessDeniedException ex = new AccessDeniedException("Access is denied");

    ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, request);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(403, response.getBody().getStatus());
    assertEquals("Access Denied", response.getBody().getError());
    assertEquals(
        "You do not have permission to access this resource", response.getBody().getMessage());
  }

  @Test
  void handleTypeMismatchException() {
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatchException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Type Mismatch", response.getBody().getError());
    assertEquals("Parameter 'id' should be of type Long", response.getBody().getMessage());
  }

  @Test
  void handleIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid value provided");

    ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Invalid Argument", response.getBody().getError());
    assertEquals("Invalid value provided", response.getBody().getMessage());
  }

  @Test
  void handleRuntimeException() {
    RuntimeException ex = new RuntimeException("Something went wrong");

    ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().getStatus());
    assertEquals("Internal Server Error", response.getBody().getError());
    assertEquals(
        "An unexpected error occurred. Please try again later.", response.getBody().getMessage());
  }

  @Test
  void handleGenericException() {
    Exception ex = new Exception("Unexpected failure");

    ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().getStatus());
    assertEquals("Internal Server Error", response.getBody().getError());
    assertEquals(
        "An unexpected error occurred. Please try again later.", response.getBody().getMessage());
  }

  @Test
  void handleTypeMismatchException_nullRequiredType() {
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException("abc", null, "id", null, null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatchException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Parameter 'id' should be of type unknown", response.getBody().getMessage());
  }

  @Test
  void handleMethodArgumentNotValidException_WithFieldErrors_ReturnsValidationErrors() {
    FieldError fe1 = new FieldError("registerRequest", "username", "Username is required");
    FieldError fe2 = new FieldError("registerRequest", "email", "Email must be valid");
    BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
    when(bindingResult.getFieldErrors()).thenReturn(java.util.Arrays.asList(fe1, fe2));

    MethodArgumentNotValidException ex =
        org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    ErrorResponse body = response.getBody();
    assertNotNull(body);
    assertEquals("Validation Error", body.getError());
    assertEquals("Request validation failed", body.getMessage());
    var errors = body.getValidationErrors();
    assertNotNull(errors);
    assertEquals(2, errors.size());
    assertEquals("username", errors.get(0).getField());
    assertEquals("Username is required", errors.get(0).getMessage());
    assertEquals("email", errors.get(1).getField());
  }

  @Test
  void handleConstraintViolationException_WithViolations_ReturnsValidationErrors() {
    jakarta.validation.Path path1 = mockValidationPath("name");
    ConstraintViolation<?> violation1 = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(violation1.getPropertyPath()).thenReturn(path1);
    when(violation1.getMessage()).thenReturn("Name is required");

    jakarta.validation.Path path2 = mockValidationPath("email");
    ConstraintViolation<?> violation2 = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(violation2.getPropertyPath()).thenReturn(path2);
    when(violation2.getMessage()).thenReturn("Email must be valid");

    ConstraintViolationException ex =
        new ConstraintViolationException("Validation failed", Set.of(violation1, violation2));

    ResponseEntity<ErrorResponse> response =
        handler.handleConstraintViolationException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    ErrorResponse body2 = response.getBody();
    assertNotNull(body2);
    assertEquals("Validation Error", body2.getError());
    assertEquals("Request constraint validation failed", body2.getMessage());
    var cvErrors = body2.getValidationErrors();
    assertNotNull(cvErrors);
    assertEquals(2, cvErrors.size());
    boolean hasNameError =
        cvErrors.stream()
            .anyMatch(
                e -> "name".equals(e.getField()) && "Name is required".equals(e.getMessage()));
    boolean hasEmailError = cvErrors.stream().anyMatch(e -> "email".equals(e.getField()));
    assertTrue(hasNameError);
    assertTrue(hasEmailError);
  }

  @Test
  void handleConstraintViolationException_EmptyViolations_ReturnsEmptyList() {
    ConstraintViolationException ex = new ConstraintViolationException("No violations", Set.of());

    ResponseEntity<ErrorResponse> response =
        handler.handleConstraintViolationException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getValidationErrors().size());
  }

  @Test
  void handleMethodArgumentNotValidException_NoFieldErrors_ReturnsEmptyList() {
    BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
    when(bindingResult.getFieldErrors()).thenReturn(java.util.Collections.emptyList());

    MethodArgumentNotValidException ex =
        org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getValidationErrors());
    assertTrue(response.getBody().getValidationErrors().isEmpty());
  }

  @Test
  void handleProductVariantNotFoundException() {
    ProductVariantNotFoundException ex = new ProductVariantNotFoundException("Variant not found");

    ResponseEntity<ErrorResponse> response =
        handler.handleProductVariantNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Variant Not Found", response.getBody().getError());
    assertEquals("Variant not found", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleProductVariantNotFoundException_WithId() {
    ProductVariantNotFoundException ex = new ProductVariantNotFoundException(42L);

    ResponseEntity<ErrorResponse> response =
        handler.handleProductVariantNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Variant not found with id: 42", response.getBody().getMessage());
  }

  @Test
  void handleProductBatchNotFoundException() {
    ProductBatchNotFoundException ex = new ProductBatchNotFoundException("Batch not found");

    ResponseEntity<ErrorResponse> response =
        handler.handleProductBatchNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("Product Batch Not Found", response.getBody().getError());
    assertEquals("Batch not found", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleProductBatchNotFoundException_WithId() {
    ProductBatchNotFoundException ex = new ProductBatchNotFoundException(42L);

    ResponseEntity<ErrorResponse> response =
        handler.handleProductBatchNotFoundException(ex, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Product batch not found with id: 42", response.getBody().getMessage());
  }

  @Test
  void handleMissingRequestValue() {
    org.springframework.web.server.MissingRequestValueException ex =
        new org.springframework.web.server.MissingRequestValueException(
            "id", String.class, "Required parameter 'id' is missing", null);

    ResponseEntity<ErrorResponse> response = handler.handleMissingRequestValue(ex, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("Missing Value", response.getBody().getError());
    assertEquals("A required request value is missing", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
  }

  private jakarta.validation.Path mockValidationPath(String name) {
    jakarta.validation.Path path = org.mockito.Mockito.mock(jakarta.validation.Path.class);
    when(path.toString()).thenReturn(name);
    return path;
  }
}
