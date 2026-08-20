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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ProductBatch entity representing inventory batches for FIFO cost tracking. Each batch tracks the
 * purchase cost and user-defined selling price for a specific quantity of stock. Batches can be
 * linked to either a product directly or to a product variant (for products with variants like
 * different calibres, specs, etc.).
 */
@Entity
@Table(
    name = "product_batches",
    indexes = {
      @Index(
          name = "idx_batch_product_variant_available",
          columnList = "product_id, created_at, id"),
      @Index(name = "idx_batch_variant_available", columnList = "variant_id, created_at, id"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "Product inventory batch for FIFO cost tracking")
public class ProductBatch {

  @Schema(description = "Unique batch ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "Optimistic lock version",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Version
  private Long version;

  @Schema(description = "Associated product")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @lombok.EqualsAndHashCode.Exclude
  private Product product;

  @Schema(description = "Associated product variant (optional, for products with variants)")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  @ToString.Exclude
  @lombok.EqualsAndHashCode.Exclude
  private ProductVariant variant;

  @Schema(description = "Remaining quantity in this batch", example = "15.00")
  @Column(name = "quantity", precision = 19, scale = 3, nullable = false)
  @NotNull(message = "Quantity is required")
  @jakarta.validation.constraints.PositiveOrZero(message = "Quantity cannot be negative")
  private BigDecimal quantity;

  @Schema(description = "Unit cost for this batch", example = "15.00")
  @Column(name = "unit_cost", precision = 19, scale = 3, nullable = false)
  @NotNull(message = "Unit cost is required")
  @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
  private BigDecimal unitCost;

  @Schema(description = "User-defined selling price for this batch", example = "20.00")
  @Column(name = "unit_price", precision = 19, scale = 3, nullable = false)
  @NotNull(message = "Unit price is required")
  @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
  private BigDecimal unitPrice;

  @Schema(description = "Associated supplier")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  @ToString.Exclude
  @lombok.EqualsAndHashCode.Exclude
  private Supplier supplier;

  @Schema(description = "Additional notes", example = "Bulk purchase discount")
  @Column(name = "notes", length = 500)
  private String notes;

  @Schema(
      description = "Batch creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Batch last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    validateVariantOwnership();
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    validateVariantOwnership();
    updatedAt = LocalDateTime.now();
  }

  /**
   * Enforce the product-variant ownership invariant: when a variant is present it must belong to
   * the assigned product. Derives the product from the variant when it is absent, and rejects
   * mismatches before persist or update. Batches without a variant are unaffected.
   */
  private void validateVariantOwnership() {
    if (variant == null) {
      return;
    }
    if (product == null) {
      product = variant.getProduct();
      return;
    }
    Product variantProduct = variant.getProduct();
    if (variantProduct == null
        || (product.getId() != null
            && variantProduct.getId() != null
            && !product.getId().equals(variantProduct.getId()))) {
      throw new IllegalArgumentException("Variant does not belong to the assigned product");
    }
  }
}
