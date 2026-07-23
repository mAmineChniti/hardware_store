package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier details returned by the API")
public class SupplierResponse {

  @Schema(description = "Unique supplier ID", example = "1")
  private Long id;

  @Schema(description = "Supplier name", example = "ABC Hardware Supplies")
  private String name;

  @Schema(description = "Supplier phone number", example = "+216 20 123 456")
  private String phone;

  @Schema(description = "Supplier email", example = "contact@abchardware.tn")
  private String email;

  @Schema(description = "Supplier address", example = "123 Main St, Tunis")
  private String address;

  @Schema(description = "Tax identification number", example = "123456789")
  private String taxIdentificationNumber;

  @Schema(description = "Contact person at supplier", example = "John Smith")
  private String contactPerson;

  @Schema(description = "Payment terms agreed with supplier", example = "Net 30 days")
  private String paymentTerms;

  @Schema(description = "Additional notes about supplier", example = "Preferred supplier for tools")
  private String notes;

  @Schema(description = "Whether supplier is soft deleted", example = "false")
  private Boolean deleted;

  @Schema(description = "Supplier creation timestamp", example = "2024-01-01T10:00:00")
  private LocalDateTime createdAt;

  @Schema(description = "Supplier last update timestamp", example = "2024-01-02T10:00:00")
  private LocalDateTime updatedAt;
}
