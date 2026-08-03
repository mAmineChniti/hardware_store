package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
      description = "User's first name",
      example = "Jane",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "First name is required")
  @Size(max = 50, message = "First name must not exceed 50 characters")
  private String firstName;

  @Schema(
      description = "User's last name",
      example = "Smith",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Last name is required")
  @Size(max = 50, message = "Last name must not exceed 50 characters")
  private String lastName;

  @Schema(
      description = "User's email address",
      example = "jane.smith@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Email is required")
  @Email(message = "Must be a valid email address")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  private String email;

  @Schema(
      description = "User's password (minimum 6 characters)",
      example = "securePass456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;
}
