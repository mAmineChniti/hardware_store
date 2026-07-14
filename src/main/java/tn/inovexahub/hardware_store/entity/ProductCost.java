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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ProductCost entity representing historical unit costs for products. Costs fluctuate over time, so
 * this entity tracks the cost of a product on specific dates for accurate margin calculations.
 */
@Entity
@Table(name = "product_costs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductCost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  private Product product;

  @Column(name = "unit_cost", precision = 19, scale = 3, nullable = false)
  private BigDecimal unitCost;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
