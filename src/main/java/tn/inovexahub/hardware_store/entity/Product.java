package tn.inovexahub.hardware_store.entity;

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
import lombok.NoArgsConstructor;
import lombok.ToString;
import tn.inovexahub.hardware_store.enums.UnitType;

/**
 * Product entity representing stock articles. Section 3: Product entity - unitType: determines if
 * quantity is integer (UNITARY) or decimal (WEIGHT, LENGTH, VOLUME) - isHeavyMaterial: enables dual
 * pricing grid (Sur Place / Livré) - stockQuantity: current physical stock (Double for simplicity
 * with validation flag)
 */
@Entity
@Table(
    name = "products",
    indexes = {@Index(name = "idx_product_reference", columnList = "reference")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @Column(name = "reference", unique = true, length = 50)
  @NotBlank(message = "Reference is required")
  private String reference;

  @Column(name = "name", nullable = false, length = 100)
  @NotBlank(message = "Product name is required")
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "image", columnDefinition = "TEXT")
  private String image; // Base64 encoded image string

  @Column(name = "category", length = 50)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit_type", nullable = false)
  @NotNull(message = "Unit type is required")
  private UnitType unitType;

  @Column(name = "is_heavy_material", nullable = false)
  @NotNull(message = "Heavy material flag is required")
  private Boolean isHeavyMaterial = false;

  @Column(name = "base_unit", length = 20)
  private String baseUnit; // e.g., "m", "kg", "piece"

  @Column(name = "stock_quantity", precision = 19, scale = 3)
  @NotNull(message = "Stock quantity is required")
  @PositiveOrZero(message = "Stock quantity cannot be negative")
  private BigDecimal stockQuantity = BigDecimal.ZERO;

  @Column(name = "average_purchase_price", precision = 19, scale = 3)
  @NotNull(message = "Average purchase price is required")
  @DecimalMin(value = "0.0", message = "Average purchase price cannot be negative")
  private BigDecimal averagePurchasePrice = BigDecimal.ZERO; // PAMP for margin calculation

  @Column(name = "price_on_site", precision = 19, scale = 3)
  @DecimalMin(value = "0.0", message = "Price on site cannot be negative")
  private BigDecimal
      priceOnSite; // Prix de Vente Sur Place (nullable, used if isHeavyMaterial = true)

  @Column(name = "price_delivered", precision = 19, scale = 3)
  @DecimalMin(value = "0.0", message = "Price delivered cannot be negative")
  private BigDecimal
      priceDelivered; // Prix de Vente Livré (nullable, used if isHeavyMaterial = true)

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  private List<ProductConditioning> conditionings = new ArrayList<>();

  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<DocumentLine> documentLines = new ArrayList<>();

  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<ProductCost> costHistory = new ArrayList<>();

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
