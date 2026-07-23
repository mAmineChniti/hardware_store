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
 * DocumentLine entity representing lines of a document. Section 7: DocumentLine entity -
 * conditioningDescription: snapshot of how product was sold (e.g., "Rouleau" vs "Détail") -
 * isDelivered: maps to the logistics toggle
 */
@Entity
@Table(name = "document_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document line entity representing individual items in a document")
public class DocumentLine {

  @Schema(description = "Unique line ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "Line number in document (assigned automatically)",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "line_number")
  private Integer lineNumber;

  @Schema(description = "Parent document")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @Schema(description = "Product in this line")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Schema(description = "Conditioning description snapshot", example = "Rouleau")
  @Column(name = "conditioning_description", length = 100)
  private String conditioningDescription; // Snapshot of how it was sold

  @Schema(description = "Quantity sold", example = "5.00")
  @Column(name = "quantity", precision = 19, scale = 3)
  private BigDecimal quantity;

  @Schema(description = "Unit price at sale time", example = "25.00")
  @Column(name = "unit_price", precision = 19, scale = 3)
  private BigDecimal unitPrice; // Price per unit applied at sale time

  @Schema(
      description =
          "Unit cost at sale time for margin (snapshotted automatically from the "
              + "product's average purchase price)",
      example = "15.00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "unit_cost", precision = 19, scale = 3)
  private BigDecimal unitCost; // Cost per unit snapshot at sale time for margin calculation

  @Schema(
      description = "Total line excluding tax (computed automatically from quantity and price)",
      example = "125.00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "total_line_excluding_tax", precision = 19, scale = 3)
  private BigDecimal totalLineExcludingTax = BigDecimal.ZERO;

  @Schema(
      description = "Total line including tax (computed automatically)",
      example = "148.75",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "total_line_including_tax", precision = 19, scale = 3)
  private BigDecimal totalLineIncludingTax = BigDecimal.ZERO;

  @Schema(description = "Whether line has been delivered", example = "false")
  @Column(name = "is_delivered", nullable = false)
  private Boolean isDelivered = false;

  @Schema(
      description = "Line creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Line last update timestamp",
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
