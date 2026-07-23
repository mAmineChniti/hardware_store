package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Document entity representing quotes, delivery notes, and invoices")
public class Document {

  @Schema(
      description = "Unique document ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "Optimistic lock version",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Version
  private Long version;

  @Schema(description = "Unique document number", example = "DEV-2024-001")
  @Column(name = "document_number", unique = true, nullable = false, length = 50)
  private String documentNumber;

  @Schema(description = "Document date", example = "2024-01-01T10:00:00")
  @Column(name = "date", nullable = false)
  private LocalDateTime date;

  @Schema(description = "Document type (QUOTE, DELIVERY_NOTE, INVOICE)", example = "QUOTE")
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  @NotNull(message = "Document type is required")
  private DocumentType documentType;

  @Schema(description = "Document status (DRAFT, VALIDATED, CANCELLED)", example = "DRAFT")
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @NotNull(message = "Document status is required")
  private DocumentStatus status = DocumentStatus.DRAFT;

  @Schema(description = "Client associated with document")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id")
  private Client client;

  @Schema(description = "User who created the document")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Schema(description = "Total excluding tax", example = "1000.00")
  @Column(name = "total_excluding_tax", precision = 19, scale = 3)
  @NotNull(message = "Total excluding tax is required")
  @DecimalMin(value = "0.0", message = "Total excluding tax cannot be negative")
  private BigDecimal totalExcludingTax = BigDecimal.ZERO;

  @Schema(description = "VAT rate percentage", example = "19.00")
  @Column(name = "vat_rate", precision = 5, scale = 2)
  @NotNull(message = "VAT rate is required")
  @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
  private BigDecimal vatRate = new BigDecimal("19.00");

  @Schema(description = "Total VAT amount", example = "190.00")
  @Column(name = "total_vat", precision = 19, scale = 3)
  @NotNull(message = "Total VAT is required")
  @DecimalMin(value = "0.0", message = "Total VAT cannot be negative")
  private BigDecimal totalVat = BigDecimal.ZERO;

  @Schema(description = "Total including tax", example = "1190.00")
  @Column(name = "total_including_tax", precision = 19, scale = 3)
  @NotNull(message = "Total including tax is required")
  @DecimalMin(value = "0.0", message = "Total including tax cannot be negative")
  private BigDecimal totalIncludingTax = BigDecimal.ZERO;

  @Schema(description = "Transport fee for delivery notes", example = "10.00")
  @Column(name = "transport_fee", precision = 19, scale = 3)
  @NotNull(message = "Transport fee is required")
  @DecimalMin(value = "0.0", message = "Transport fee cannot be negative")
  private BigDecimal transportFee = new BigDecimal("10.000"); // Default 10.000 DT for BL

  @Schema(description = "Stamp duty for invoices", example = "1.00")
  @Column(name = "stamp_duty", precision = 19, scale = 3)
  @NotNull(message = "Stamp duty is required")
  @DecimalMin(value = "0.0", message = "Stamp duty cannot be negative")
  private BigDecimal stampDuty = new BigDecimal("1.000"); // Default 1.000 DT for Invoices

  @Schema(description = "Whether sale is on credit", example = "false")
  @Column(name = "is_credit_sale", nullable = false)
  @NotNull(message = "Credit sale flag is required")
  private Boolean isCreditSale = false;

  @Schema(
      description = "ID of invoice if document was converted",
      example = "5",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "converted_to_invoice_id")
  private Long convertedToInvoiceId;

  @Schema(
      description = "ID of source delivery note if converted from BL",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "source_delivery_note_id")
  private Long sourceDeliveryNoteId;

  @Schema(
      description = "Document creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Document last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Schema(description = "Lines in the document")
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
