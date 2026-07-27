package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response structure")
public class ErrorResponse {

  @Schema(
      description = "HTTP status code",
      example = "400",
      accessMode = Schema.AccessMode.READ_ONLY)
  private int status;

  @Schema(
      description = "Error type/category",
      example = "Validation Error",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String error;

  @Schema(
      description = "Detailed error message",
      example = "Username is required",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String message;

  @Schema(
      description = "Timestamp when the error occurred",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  private LocalDateTime timestamp;

  @Schema(
      description = "Request path that caused the error",
      example = "/api/auth/login",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String path;

  @Schema(
      description = "Validation error details (for validation errors)",
      accessMode = Schema.AccessMode.READ_ONLY)
  private List<ValidationError> validationErrors;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Validation error detail")
  public static class ValidationError {

    @Schema(description = "Field that failed validation", example = "username")
    private String field;

    @Schema(description = "Validation error message", example = "Username is required")
    private String message;
  }
}
