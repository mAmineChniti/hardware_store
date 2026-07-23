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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import tn.inovexahub.hardware_store.enums.TransactionType;

/**
 * CreditHistory entity - MUST BE IMMUTABLE (no deletes, only counter-entries). Section 9:
 * CreditHistory entity - amount: Positive for debt increase (SALE), Negative for payments (PAYMENT)
 * - runningBalance: Client's total debt after this operation - entryDate: Immutable, set
 * via @CreationTimestamp
 */
@Entity
@Table(name = "credit_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE credit_history SET deleted = true WHERE id = ?")
@Schema(description = "Credit history entity tracking client debt transactions")
public class CreditHistory {

  @Schema(
      description = "Unique credit history ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "Client associated with this credit entry")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Schema(description = "Document associated with this credit entry (if from sale)")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id")
  private Document document; // Nullable if direct payment

  @Schema(description = "Payment receipt associated with this credit entry (if from payment)")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_receipt_id")
  private PaymentReceipt paymentReceipt; // Nullable if sale

  @Schema(description = "Transaction type (SALE or PAYMENT)", example = "SALE")
  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false)
  private TransactionType transactionType;

  @Schema(
      description = "Transaction amount (positive for debt increase, negative for payments)",
      example = "500.00")
  @Column(name = "amount", precision = 19, scale = 3)
  private BigDecimal amount; // Positive for debt increase, Negative for payments

  @Schema(
      description = "Client's total debt after this operation (computed automatically)",
      example = "1500.00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "running_balance", precision = 19, scale = 3)
  private BigDecimal runningBalance; // Client's total debt after this operation

  @Schema(
      description = "Whether entry is soft deleted",
      example = "false",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

  @Schema(
      description = "Immutable entry date",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @CreationTimestamp
  @Column(name = "entry_date", nullable = false, updatable = false)
  private LocalDateTime entryDate; // Immutable
}
