package tn.inovexahub.hardware_store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 20, message = "Phone must not exceed 20 characters")
  private String phone;

  @Email(message = "Email must be valid")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  private String email;

  @Size(max = 255, message = "Address must not exceed 255 characters")
  private String address;

  @Size(max = 50, message = "Tax identification number must not exceed 50 characters")
  private String taxIdentificationNumber;

  @Size(max = 100, message = "Contact person must not exceed 100 characters")
  private String contactPerson;

  @Size(max = 100, message = "Payment terms must not exceed 100 characters")
  private String paymentTerms;

  @Size(max = 500, message = "Notes must not exceed 500 characters")
  private String notes;
}
