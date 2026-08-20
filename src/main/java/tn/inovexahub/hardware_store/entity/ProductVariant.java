package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ProductVariant entity representing specific variants of a product with flexible JSON attributes.
 * Examples: 6mm screws vs 7mm screws, 12000 BTU AC vs 18000 BTU AC, etc.
 *
 * <p>Each variant can have its own FIFO batch tracking and pricing. The attributes field stores
 * flexible specifications in JSON format (e.g., {"calibre": "6mm", "material": "steel"} or {"btu":
 * 12000, "wph": 1500, "voltage": "220V"}).
 */
@Entity
@Table(
    name = "product_variants",
    indexes = {@Index(name = "idx_product_variants_product_id", columnList = "product_id")})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product variant with flexible specifications")
public class ProductVariant {

  @Schema(
      description = "Unique variant ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @Schema(
      description = "Optimistic lock version",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Version
  private Long version;

  @Schema(description = "Associated base product")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  private Product product;

  @Schema(description = "Unique SKU for this variant", example = "SCREW-6MM")
  @Column(name = "sku", unique = true, nullable = false, length = 50)
  @NotBlank(message = "SKU is required")
  @Size(max = 50, message = "SKU must not exceed 50 characters")
  private String sku;

  @Schema(
      description = "Variant name (optional, can be derived from attributes)",
      example = "6mm Steel Screws")
  @Column(name = "variant_name", length = 100)
  @Size(max = 100, message = "Variant name must not exceed 100 characters")
  private String variantName;

  @Schema(
      description = "Flexible attributes in JSON format",
      example = "{\"calibre\": \"6mm\", \"material\": \"steel\", \"finish\": \"galvanized\"}")
  @Column(name = "attributes", columnDefinition = "TEXT")
  private String attributes;

  @Schema(
      description = "Variant creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Variant last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
