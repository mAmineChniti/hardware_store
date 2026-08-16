package tn.inovexahub.hardware_store.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tn.inovexahub.hardware_store.enums.UnitType;

/**
 * Product entity representing stock articles. Section 3: Product entity
 *
 * <p>unitType: Determines how quantity is measured and displayed: - UNITARY: Integer quantities
 * (e.g., 5 pieces, 10 boxes) - WEIGHT: Decimal quantities in kilograms (e.g., 2.5 kg, 100.0 kg) -
 * LENGTH: Decimal quantities in meters (e.g., 3.5 m, 10.0 m) - VOLUME: Decimal quantities in liters
 * (e.g., 1.5 L, 20.0 L)
 *
 * <p>stockQuantity: Current physical stock (BigDecimal for precision, supports both integer and
 * decimal quantities)
 */
@Entity
@Table(
    name = "products",
    indexes = {@Index(name = "idx_product_reference", columnList = "reference")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "Product entity representing stock items")
public class Product {

  @Schema(
      description = "Unique product ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "Optimistic lock version",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Version
  private Long version;

  @Schema(description = "Product reference", example = "PROD001")
  @Column(name = "reference", unique = true, length = 50)
  @NotBlank(message = "Reference is required")
  private String reference;

  @Schema(description = "Product name", example = "Hammer")
  @Column(name = "name", nullable = false, length = 100)
  @NotBlank(message = "Product name is required")
  private String name;

  @Schema(description = "Product description", example = "Heavy duty steel hammer")
  @Column(name = "description", length = 500)
  private String description;

  @Schema(description = "Product image (base64 string)", example = "iVBORw0KGgoAAAANSUhEUgAA...")
  @Column(name = "image", columnDefinition = "TEXT")
  private String image; // Base64 encoded image string

  @Schema(description = "Product category", example = "Tools")
  @Column(name = "category", length = 50)
  private String category;

  @Schema(description = "Unit type", example = "UNITARY")
  @Enumerated(EnumType.STRING)
  @Column(name = "unit_type", nullable = false)
  @NotNull(message = "Unit type is required")
  private UnitType unitType;

  @Schema(description = "Base unit", example = "piece")
  @Column(name = "base_unit", length = 20)
  private String baseUnit; // e.g., "m", "kg", "piece"

  @Schema(description = "Current stock quantity", example = "50.00")
  @Column(name = "stock_quantity", precision = 19, scale = 3)
  @NotNull(message = "Stock quantity is required")
  @PositiveOrZero(message = "Stock quantity cannot be negative")
  private BigDecimal stockQuantity = BigDecimal.ZERO;

  @Schema(description = "Average purchase price", example = "25.00")
  @Column(name = "average_purchase_price", precision = 19, scale = 3)
  @NotNull(message = "Average purchase price is required")
  @DecimalMin(value = "0.0", message = "Average purchase price cannot be negative")
  private BigDecimal averagePurchasePrice = BigDecimal.ZERO; // PAMP for margin calculation

  @Schema(description = "Unit selling price", example = "35.00")
  @Column(name = "unit_price", precision = 19, scale = 3)
  @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
  private BigDecimal unitPrice; // User-defined selling price

  @Schema(description = "Default/preferred supplier")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Supplier supplier;

  @Schema(
      description = "Product creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Product last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Schema(description = "Product conditionings")
  @OneToMany(
      mappedBy = "product",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ProductConditioning> conditionings = new ArrayList<>();

  @Schema(description = "Document lines for this product")
  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
  @JsonIgnore
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<DocumentLine> documentLines = new ArrayList<>();

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
