package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating a product's stock quantity")
public class UpdateStockRequest {

  @Schema(
      description = "Stock quantity change (+10 to add, -5 to subtract)",
      example = "10.0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Quantity change is required")
  private BigDecimal quantityChange;
}
