package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for registering a new user")
public class RegisterRequest {

  @Schema(
      description = "User's username (3-50 characters)",
      example = "jane_smith",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @Schema(
      description = "User's password (minimum 6 characters)",
      example = "securePass456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;

  @Schema(
      description = "User's full name",
      example = "Jane Smith",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Full name is required")
  @Size(max = 100, message = "Full name must not exceed 100 characters")
  private String fullName;

  @Schema(
      description = "User's role (EMPLOYEE or ADMIN)",
      example = "EMPLOYEE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Role is required")
  private String role;
}
