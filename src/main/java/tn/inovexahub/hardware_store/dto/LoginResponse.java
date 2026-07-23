package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description =
        "Response payload containing access and refresh tokens after successful login/refresh")
public class LoginResponse {

  @Schema(
      description = "JWT access token for API authentication",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(
      description = "JWT refresh token for obtaining new access tokens",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String refreshToken;

  @Schema(description = "Time in seconds until access token expires", example = "900")
  private long accessTokenExpiresIn;

  @Schema(description = "Time in seconds until refresh token expires", example = "1800")
  private long refreshTokenExpiresIn;

  @Schema(description = "Token type (always Bearer)", example = "Bearer")
  private String tokenType = "Bearer";

  @Schema(description = "Authenticated user's username", example = "john_doe")
  private String username;

  @Schema(description = "Authenticated user's role", example = "EMPLOYEE")
  private String role;
}
