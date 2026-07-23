package tn.inovexahub.hardware_store.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ClientNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleClientNotFoundException(
      ClientNotFoundException ex, HttpServletRequest request) {
    log.error("Client not found: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Client Not Found",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(SupplierNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSupplierNotFoundException(
      SupplierNotFoundException ex, HttpServletRequest request) {
    log.error("Supplier not found: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Supplier Not Found",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(CreditLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleCreditLimitExceededException(
      CreditLimitExceededException ex, HttpServletRequest request) {
    log.error("Credit limit exceeded: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Credit Limit Exceeded",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(InvalidPaymentException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPaymentException(
      InvalidPaymentException ex, HttpServletRequest request) {
    log.error("Invalid payment: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Payment",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    log.error("Validation error: {}", ex.getMessage());
    List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      validationErrors.add(
          new ErrorResponse.ValidationError(error.getField(), error.getDefaultMessage()));
    }
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Request validation failed",
            LocalDateTime.now(),
            request.getRequestURI(),
            validationErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.error("Malformed request body: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Malformed Request",
            "Request body is malformed or contains invalid values",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, HttpServletRequest request) {
    log.error("Constraint violation: {}", ex.getMessage());
    List<ErrorResponse.ValidationError> validationErrors =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    new ErrorResponse.ValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .collect(Collectors.toList());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Request constraint validation failed",
            LocalDateTime.now(),
            request.getRequestURI(),
            validationErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {
    log.error("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());
    ErrorResponse error =
        new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getStatusCode().toString(),
            ex.getReason() != null ? ex.getReason() : "Response Status Error",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(ex.getStatusCode()).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    log.error("Authentication failed: {}", ex.getMessage());
    String message = "Authentication failed";
    if (ex instanceof BadCredentialsException) {
      message = "Invalid username or password";
    }
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication Failed",
            message,
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    log.error("Access denied: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            "You do not have permission to access this resource",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    log.error("Type mismatch: {}", ex.getMessage());
    Class<?> requiredType = ex.getRequiredType();
    String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";
    String message = String.format("Parameter '%s' should be of type %s", ex.getName(), typeName);
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Type Mismatch",
            message,
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.error("Illegal argument: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Argument",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(
      RuntimeException ex, HttpServletRequest request) {
    log.error("Runtime error: {}", ex.getMessage(), ex);
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            LocalDateTime.now(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
