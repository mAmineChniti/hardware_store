package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a product variant")
public class CreateVariantRequest {

  @Schema(
      description = "Unique SKU for variant",
      example = "SCREW-6MM",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "SKU is required")
  private String sku;

  @Schema(
      description = "Variant name (optional, can be derived from attributes)",
      example = "6mm Steel Screws")
  private String variantName;

  @Schema(
      description = "JSON attributes (e.g., {\"calibre\": \"6mm\", \"material\": \"steel\"})",
      example = "{\"calibre\": \"6mm\", \"material\": \"steel\"}",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Attributes are required")
  private String attributes;
}
