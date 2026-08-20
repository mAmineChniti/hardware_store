package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating a product variant")
public class UpdateVariantRequest {

  @Schema(description = "New SKU", example = "SCREW-7MM")
  private String sku;

  @Schema(description = "New variant name", example = "7mm Steel Screws")
  private String variantName;

  @Schema(
      description = "New JSON attributes",
      example = "{\"calibre\": \"7mm\", \"material\": \"steel\"}")
  private String attributes;
}
