package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for refreshing access token using refresh token")
public class RefreshTokenRequest {

  @Schema(
      description = "JWT refresh token to use for obtaining new access token",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  private String refreshToken;
}
