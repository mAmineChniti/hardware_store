package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for user login")
public class LoginRequest {

  @Schema(
      description = "User's username",
      example = "john_doe",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Username is required")
  private String username;

  @Schema(
      description = "User's password",
      example = "securePassword123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  private String password;
}
