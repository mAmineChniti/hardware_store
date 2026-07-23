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
      description = "Refresh token to use for obtaining new access token",
      example = "550e8400-e29b-41d4-a716-446655440000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  private String refreshToken;
}
