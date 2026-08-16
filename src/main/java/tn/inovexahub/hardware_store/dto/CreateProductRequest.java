package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.UnitType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a product with an initial inventory batch")
public class CreateProductRequest {

  @Schema(
      description = "Product reference",
      example = "PROD001",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Reference is required")
  private String reference;

  @Schema(
      description = "Product name",
      example = "Hammer",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Product name is required")
  private String name;

  @Schema(description = "Product description", example = "Heavy duty steel hammer")
  private String description;

  @Schema(description = "Product image (base64 string)")
  private String image;

  @Schema(description = "Product category", example = "Tools")
  private String category;

  @Schema(
      description = "Unit type",
      example = "UNITARY",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Unit type is required")
  private UnitType unitType;

  @Schema(description = "Base unit", example = "piece")
  private String baseUnit;

  @Schema(
      description = "Initial batch quantity",
      example = "100",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Initial quantity is required")
  @Positive(message = "Initial quantity must be positive")
  private BigDecimal initialQuantity;

  @Schema(
      description = "Initial batch unit cost",
      example = "15.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Initial unit cost is required")
  @Positive(message = "Initial unit cost must be positive")
  private BigDecimal initialUnitCost;

  @Schema(
      description = "Initial batch unit selling price",
      example = "20.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Initial unit price is required")
  @Positive(message = "Initial unit price must be positive")
  private BigDecimal initialUnitPrice;

  @Schema(
      description = "Default supplier ID for product and initial batch (optional)",
      example = "1")
  private Long supplierId;

  @Schema(description = "Initial batch notes (optional)", example = "Initial stock")
  private String notes;
}
