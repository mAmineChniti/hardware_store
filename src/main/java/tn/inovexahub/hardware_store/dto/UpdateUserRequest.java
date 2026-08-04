package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating an existing user's information")
public class UpdateUserRequest {

  @Schema(description = "Updated first name (max 50 characters)", example = "Jane")
  @Size(max = 50, message = "First name must not exceed 50 characters")
  private String firstName;

  @Schema(description = "Updated last name (max 50 characters)", example = "Doe")
  @Size(max = 50, message = "Last name must not exceed 50 characters")
  private String lastName;

  @Schema(description = "Updated email address", example = "jane.doe@example.com")
  @Email(message = "Must be a valid email address")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  private String email;
}
