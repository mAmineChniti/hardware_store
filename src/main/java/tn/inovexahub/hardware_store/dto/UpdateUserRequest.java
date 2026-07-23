package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating an existing user's information")
public class UpdateUserRequest {

  @Schema(description = "Updated full name (max 100 characters)", example = "Jane Doe")
  @Size(max = 100, message = "Full name must not exceed 100 characters")
  private String fullName;

  @Schema(description = "Updated user role (EMPLOYEE or ADMIN)", example = "ADMIN")
  private String role;
}
