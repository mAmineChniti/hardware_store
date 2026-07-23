package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating a supplier")
public class SupplierRequest {

  @Schema(
      description = "Supplier name",
      example = "ABC Hardware Supplies",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Schema(description = "Supplier phone number", example = "+216 20 123 456")
  @Size(max = 20, message = "Phone must not exceed 20 characters")
  private String phone;

  @Schema(description = "Supplier email", example = "contact@abchardware.tn")
  @Email(message = "Email must be valid")
  @Size(max = 100, message = "Email must not exceed 100 characters")
  private String email;

  @Schema(description = "Supplier address", example = "123 Main St, Tunis")
  @Size(max = 255, message = "Address must not exceed 255 characters")
  private String address;

  @Schema(description = "Tax identification number", example = "123456789")
  @Size(max = 50, message = "Tax identification number must not exceed 50 characters")
  private String taxIdentificationNumber;

  @Schema(description = "Contact person at supplier", example = "John Smith")
  @Size(max = 100, message = "Contact person must not exceed 100 characters")
  private String contactPerson;

  @Schema(description = "Payment terms agreed with supplier", example = "Net 30 days")
  @Size(max = 100, message = "Payment terms must not exceed 100 characters")
  private String paymentTerms;

  @Schema(description = "Additional notes about supplier", example = "Preferred supplier for tools")
  @Size(max = 500, message = "Notes must not exceed 500 characters")
  private String notes;
}
