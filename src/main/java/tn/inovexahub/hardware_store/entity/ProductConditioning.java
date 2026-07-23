package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductConditioning entity representing "Conditionnement" (e.g., 1m wire vs 100m roll). Section
 * 4: ProductConditioning entity Allows non-linear pricing (e.g., 1m=1.5DT, 100m roll=100DT)
 */
@Entity
@Table(name = "product_conditionings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product conditioning representing packaging options")
public class ProductConditioning {

  @Schema(description = "Unique conditioning ID", example = "1")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "Associated product")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Schema(description = "Conditioning description", example = "Roll of 100 meters")
  @Column(name = "description", length = 100)
  private String description; // e.g., "Rouleau 100m"

  @Schema(description = "Quantity per conditioning unit", example = "100.00")
  @Column(name = "quantity_per_unit", precision = 19, scale = 3)
  private BigDecimal quantityPerUnit;

  @Schema(description = "Price per conditioning unit", example = "95.00")
  @Column(name = "unit_price", precision = 19, scale = 3)
  private BigDecimal unitPrice;

  @Schema(description = "Creation timestamp", example = "2024-01-01T10:00:00")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2024-01-02T10:00:00")
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
