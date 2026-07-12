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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.UnitType;

/**
 * Product entity representing stock articles. Section 3: Product entity - unitType: determines if
 * quantity is integer (UNITARY) or decimal (WEIGHT, LENGTH, VOLUME) - isHeavyMaterial: enables dual
 * pricing grid (Sur Place / Livré) - stockQuantity: current physical stock (Double for simplicity
 * with validation flag)
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference", unique = true, length = 50)
  private String reference;

  @Column(name = "barcode", unique = true, length = 50)
  private String barcode;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "category", length = 50)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit_type", nullable = false)
  private UnitType unitType;

  @Column(name = "is_heavy_material", nullable = false)
  private Boolean isHeavyMaterial = false;

  @Column(name = "base_unit", length = 20)
  private String baseUnit; // e.g., "m", "kg", "piece"

  @Column(name = "stock_quantity", precision = 19, scale = 3)
  private Double stockQuantity = 0.0;

  @Column(name = "average_purchase_price", precision = 19, scale = 3)
  private BigDecimal averagePurchasePrice = BigDecimal.ZERO; // PAMP for margin calculation

  @Column(name = "price_on_site", precision = 19, scale = 3)
  private BigDecimal
      priceOnSite; // Prix de Vente Sur Place (nullable, used if isHeavyMaterial = true)

  @Column(name = "price_delivered", precision = 19, scale = 3)
  private BigDecimal
      priceDelivered; // Prix de Vente Livré (nullable, used if isHeavyMaterial = true)

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProductConditioning> conditionings = new ArrayList<>();

  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
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
