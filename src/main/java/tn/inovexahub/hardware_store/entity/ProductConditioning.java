package tn.inovexahub.hardware_store.entity;

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
public class ProductConditioning {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "description", length = 100)
  private String description; // e.g., "Rouleau 100m"

  @Column(name = "quantity_per_unit", precision = 19, scale = 3)
  private BigDecimal quantityPerUnit;

  @Column(name = "unit_price", precision = 19, scale = 3)
  private BigDecimal unitPrice;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

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
