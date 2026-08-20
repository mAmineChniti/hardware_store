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
@Schema(description = "Request payload for updating a batch's quantity")
public class UpdateBatchQuantityRequest {

  @Schema(
      description = "New quantity",
      example = "10.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Quantity is required")
  @PositiveOrZero(message = "Quantity must not be negative")
  private BigDecimal quantity;
}
