package tn.inovexahub.hardware_store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCostRequest {

  @NotNull(message = "Unit cost is required")
  @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
  private java.math.BigDecimal unitCost;

  @NotNull(message = "Effective date is required")
  private LocalDate effectiveDate;

  @Size(max = 100, message = "Supplier name must not exceed 100 characters")
  private String supplier;

  @Size(max = 500, message = "Notes must not exceed 500 characters")
  private String notes;
}
