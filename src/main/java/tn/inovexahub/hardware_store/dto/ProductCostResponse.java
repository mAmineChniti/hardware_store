package tn.inovexahub.hardware_store.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCostResponse {

  private Long id;
  private Long productId;
  private String productName;
  private java.math.BigDecimal unitCost;
  private LocalDate effectiveDate;
  private String supplier;
  private String notes;
  private LocalDateTime createdAt;
}
