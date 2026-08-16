package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for adding an inventory batch to a product or variant")
public class BatchRequest {

  @Schema(
      description = "Quantity in batch",
      example = "15.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  private BigDecimal quantity;

  @Schema(description = "Unit cost", example = "15.00", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit cost is required")
  @PositiveOrZero(message = "Unit cost must not be negative")
  private BigDecimal unitCost;

  @Schema(
      description = "Unit selling price",
      example = "20.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit price is required")
  @Positive(message = "Unit price must be positive")
  private BigDecimal unitPrice;

  @Schema(description = "Supplier ID (optional)", example = "1")
  private Long supplierId;

  @Schema(description = "Notes (optional)", example = "Bulk purchase")
  private String notes;
}
