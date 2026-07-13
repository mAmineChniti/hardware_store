package tn.inovexahub.hardware_store.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

  @Size(max = 100, message = "Full name must not exceed 100 characters")
  private String fullName;

  private String role;
}
