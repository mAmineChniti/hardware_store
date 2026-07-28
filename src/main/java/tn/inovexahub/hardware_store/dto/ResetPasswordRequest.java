package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for completing password reset with OTP")
public class ResetPasswordRequest {

  @Schema(
      description = "Email address that received the OTP",
      example = "john.doe@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Email is required")
  @Email(message = "Must be a valid email address")
  private String email;

  @Schema(
      description = "6-digit OTP code received via email",
      example = "482916",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "OTP code is required")
  @Pattern(regexp = "\\d{6}", message = "OTP code must be exactly 6 digits")
  private String otpCode;

  @Schema(
      description = "New password (minimum 6 characters)",
      example = "newSecurePass123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "New password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String newPassword;
}
