package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for changing a user's role")
public class ChangeRoleRequest {

  @Schema(
      description = "New role (EMPLOYEE or ADMIN)",
      example = "ADMIN",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Role is required")
  @Pattern(regexp = "EMPLOYEE|ADMIN", message = "Role must be EMPLOYEE or ADMIN")
  private String role;
}
