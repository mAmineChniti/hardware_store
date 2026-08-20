package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating a batch's pricing")
public class UpdateBatchPricingRequest {

  @Schema(
      description = "New unit cost",
      example = "16.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit cost is required")
  @PositiveOrZero(message = "Unit cost must not be negative")
  private BigDecimal unitCost;

  @Schema(
      description = "New unit selling price",
      example = "22.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit price is required")
  @PositiveOrZero(message = "Unit price must not be negative")
  private BigDecimal unitPrice;

  @Schema(
      description =
          "Admin override to allow price below cost. "
              + "Non-admin callers must set this to false or null; "
              + "the override is ignored for non-admin users.",
      example = "false")
  private Boolean adminOverride;
}
