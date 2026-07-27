package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for adding a new product cost entry (updates PAMP)")
public class ProductCostRequest {

  @Schema(
      description = "Purchase unit cost",
      example = "45.000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit cost is required")
  @DecimalMin(value = "0.001", inclusive = true, message = "Unit cost must be strictly positive")
  private BigDecimal unitCost;

  @Schema(
      description = "Effective date of purchase cost",
      example = "2024-01-01",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Effective date is required")
  private LocalDate effectiveDate;

  @Schema(description = "Supplier ID (optional)", example = "1")
  private Long supplierId;

  @Schema(description = "Cost notes/comments (optional)", example = "Invoice #123 purchase")
  @Size(max = 500, message = "Notes must not exceed 500 characters")
  private String notes;
}
