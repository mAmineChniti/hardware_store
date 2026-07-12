package tn.inovexahub.hardware_store.entity;

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
public class PaymentReceipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "receipt_number", unique = true, nullable = false, length = 50)
  private String receiptNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "amount_paid", precision = 19, scale = 3)
  private BigDecimal amountPaid;

  @Column(name = "payment_date", nullable = false)
  private LocalDateTime paymentDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Column(name = "previous_debt", precision = 19, scale = 3)
  private BigDecimal previousDebt; // Snapshot before payment

  @Column(name = "new_debt", precision = 19, scale = 3)
  private BigDecimal newDebt; // Snapshot after payment

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user; // Who registered the payment

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

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
