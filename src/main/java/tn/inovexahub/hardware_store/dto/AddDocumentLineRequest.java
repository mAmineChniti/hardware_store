package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for adding a line to a document")
public class AddDocumentLineRequest {

  @Schema(description = "Product ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Product ID is required")
  @Positive(message = "Product ID must be positive")
  private Long productId;

  @Schema(
      description = "Line item quantity",
      example = "5.0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  private BigDecimal quantity;

  @Schema(description = "Custom unit price (optional)", example = "15.500")
  @jakarta.validation.constraints.PositiveOrZero(message = "Unit price must not be negative")
  private BigDecimal unitPrice;

  @Schema(description = "Conditioning unit description (optional)", example = "Boîte de 10")
  private String conditioningDescription;

  @Schema(description = "Delivered status (optional)", example = "true")
  private Boolean isDelivered;

  @Schema(description = "Product conditioning ID (optional)", example = "2")
  @Positive(message = "Conditioning ID must be positive")
  private Long conditioningId;

  @Schema(description = "Product variant ID (optional)", example = "5")
  @Positive(message = "Variant ID must be positive")
  private Long variantId;
}
