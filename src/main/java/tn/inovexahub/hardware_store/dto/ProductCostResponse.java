package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product cost entry returned by the API")
public class ProductCostResponse {

  @Schema(description = "Unique cost ID", example = "1")
  private Long id;

  @Schema(description = "Associated product ID", example = "1")
  private Long productId;

  @Schema(description = "Associated product name", example = "Hammer")
  private String productName;

  @Schema(description = "Unit cost", example = "45.000")
  private BigDecimal unitCost;

  @Schema(description = "Effective date for this cost", example = "2024-01-01")
  private LocalDate effectiveDate;

  @Schema(description = "Name of the supplier this cost was purchased from", example = "SOTUVER")
  private String supplier;

  @Schema(description = "Additional notes", example = "Invoice #123 purchase")
  private String notes;

  @Schema(description = "Creation timestamp", example = "2024-01-01T10:00:00")
  private LocalDateTime createdAt;
}
