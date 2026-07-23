package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.PaymentMethod;

/**
 * PaymentReceipt entity for "Règlements Partiels" (Acomptes). Section 8: PaymentReceipt entity -
 * previousDebt/newDebt: snapshots before/after payment
 */
@Entity
@Table(name = "payment_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment receipt entity for tracking client payments")
public class PaymentReceipt {

  @Schema(
      description = "Unique payment receipt ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "Unique receipt number (generated automatically)",
      example = "REC-2024-001",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "receipt_number", unique = true, nullable = false, length = 50)
  private String receiptNumber;

  @Schema(description = "Client who made the payment")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Schema(description = "Amount paid", example = "500.00")
  @Column(name = "amount_paid", precision = 19, scale = 3)
  private BigDecimal amountPaid;

  @Schema(description = "Date of payment", example = "2024-01-01T10:00:00")
  @Column(name = "payment_date", nullable = false)
  private LocalDateTime paymentDate;

  @Schema(description = "Payment method used", example = "CASH")
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Schema(
      description = "Client's debt before payment (snapshotted automatically)",
      example = "1500.00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "previous_debt", precision = 19, scale = 3)
  private BigDecimal previousDebt; // Snapshot before payment

  @Schema(
      description = "Client's debt after payment (snapshotted automatically)",
      example = "1000.00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "new_debt", precision = 19, scale = 3)
  private BigDecimal newDebt; // Snapshot after payment

  @Schema(description = "User who registered the payment")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user; // Who registered the payment

  @Schema(
      description = "Associated credit history entry (generated automatically)",
      accessMode = Schema.AccessMode.READ_ONLY)
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_history_id")
  private CreditHistory creditHistory; // Generated credit history entry

  @Schema(
      description = "Receipt creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Receipt last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (paymentDate == null) {
      paymentDate = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
