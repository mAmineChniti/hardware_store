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
 * DocumentLine entity representing lines of a document. Section 7: DocumentLine entity -
 * conditioningDescription: snapshot of how product was sold (e.g., "Rouleau" vs "Détail") -
 * isDelivered: maps to the logistics toggle
 */
@Entity
@Table(name = "document_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "line_number")
  private Integer lineNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(name = "conditioning_description", length = 100)
  private String conditioningDescription; // Snapshot of how it was sold

  @Column(name = "quantity", precision = 19, scale = 3)
  private Double quantity;

  @Column(name = "unit_price", precision = 19, scale = 3)
  private BigDecimal unitPrice; // Price per unit applied at sale time

  @Column(name = "unit_cost", precision = 19, scale = 3)
  private BigDecimal unitCost; // Cost per unit snapshot at sale time for margin calculation

  @Column(name = "total_line_excluding_tax", precision = 19, scale = 3)
  private BigDecimal totalLineExcludingTax = BigDecimal.ZERO;

  @Column(name = "total_line_including_tax", precision = 19, scale = 3)
  private BigDecimal totalLineIncludingTax = BigDecimal.ZERO;

  @Column(name = "is_delivered", nullable = false)
  private Boolean isDelivered = false;

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
