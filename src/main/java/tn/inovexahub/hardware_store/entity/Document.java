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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;

/**
 * Document entity representing the parent table for Devis, BL, Facture. Section 6: Document entity
 * Uses SINGLE_TABLE inheritance strategy for different document types
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @Column(name = "document_number", unique = true, nullable = false, length = 50)
  private String documentNumber;

  @Column(name = "date", nullable = false)
  private LocalDateTime date;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  @NotNull(message = "Document type is required")
  private DocumentType documentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @NotNull(message = "Document status is required")
  private DocumentStatus status = DocumentStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id")
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "total_excluding_tax", precision = 19, scale = 3)
  @NotNull(message = "Total excluding tax is required")
  @DecimalMin(value = "0.0", message = "Total excluding tax cannot be negative")
  private BigDecimal totalExcludingTax = BigDecimal.ZERO;

  @Column(name = "vat_rate", precision = 5, scale = 2)
  @NotNull(message = "VAT rate is required")
  @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
  private BigDecimal vatRate = new BigDecimal("19.00");

  @Column(name = "total_vat", precision = 19, scale = 3)
  @NotNull(message = "Total VAT is required")
  @DecimalMin(value = "0.0", message = "Total VAT cannot be negative")
  private BigDecimal totalVat = BigDecimal.ZERO;

  @Column(name = "total_including_tax", precision = 19, scale = 3)
  @NotNull(message = "Total including tax is required")
  @DecimalMin(value = "0.0", message = "Total including tax cannot be negative")
  private BigDecimal totalIncludingTax = BigDecimal.ZERO;

  @Column(name = "transport_fee", precision = 19, scale = 3)
  @NotNull(message = "Transport fee is required")
  @DecimalMin(value = "0.0", message = "Transport fee cannot be negative")
  private BigDecimal transportFee = new BigDecimal("10.000"); // Default 10.000 DT for BL

  @Column(name = "stamp_duty", precision = 19, scale = 3)
  @NotNull(message = "Stamp duty is required")
  @DecimalMin(value = "0.0", message = "Stamp duty cannot be negative")
  private BigDecimal stampDuty = new BigDecimal("1.000"); // Default 1.000 DT for Invoices

  @Column(name = "is_credit_sale", nullable = false)
  @NotNull(message = "Credit sale flag is required")
  private Boolean isCreditSale = false;

  @Column(name = "converted_to_invoice_id")
  private Long convertedToInvoiceId;

  @Column(name = "source_delivery_note_id")
  private Long sourceDeliveryNoteId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DocumentLine> lines = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (date == null) {
      date = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
