package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client entity representing customers ("Tiers" with "Carnet" system). Section 2: Client entity -
 * creditLimit: maps to "plafond_credit_autorise" - currentDebt: maps to "Dette_Actuelle" (updated
 * via event listeners)
 */
@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer entity representing clients")
public class Client {

  @Schema(description = "Unique client ID", example = "1")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "Optimistic lock version", example = "1")
  @Version
  private Long version;

  @Schema(description = "Client name", example = "Ahmed Ben Ali")
  @Column(name = "name", nullable = false, length = 100)
  @NotBlank(message = "Client name is required")
  private String name;

  @Schema(description = "Client phone number", example = "+216 20 123 456")
  @Column(name = "phone", length = 20)
  private String phone;

  @Schema(description = "Client email", example = "ahmed@example.com")
  @Column(name = "email", length = 100)
  @Email(message = "Email must be valid")
  private String email;

  @Schema(description = "Client address", example = "123 Main St, Tunis")
  @Column(name = "address", length = 255)
  private String address;

  @Schema(description = "Tax identification number", example = "123456789")
  @Column(name = "tax_identification_number", length = 50)
  private String taxIdentificationNumber; // Matricule fiscal

  @Schema(description = "Allowed credit limit", example = "10000.00")
  @Column(name = "credit_limit", precision = 19, scale = 3)
  @NotNull(message = "Credit limit is required")
  @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
  private BigDecimal creditLimit = BigDecimal.ZERO; // plafond_credit_autorise

  @Schema(description = "Current outstanding debt", example = "500.00")
  @Column(name = "current_debt", precision = 19, scale = 3)
  @NotNull(message = "Current debt is required")
  @DecimalMin(value = "0.0", message = "Current debt cannot be negative")
  private BigDecimal currentDebt = BigDecimal.ZERO; // Dette_Actuelle

  @Schema(description = "Whether client is soft deleted", example = "false")
  @Column(name = "deleted", nullable = false)
  @NotNull(message = "Deleted flag is required")
  private Boolean deleted = false;

  @Schema(description = "Client creation timestamp", example = "2024-01-01T10:00:00")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(description = "Client last update timestamp", example = "2024-01-02T10:00:00")
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Schema(description = "Documents associated with client")
  @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
  private List<Document> documents = new ArrayList<>();

  @Schema(description = "Payment receipts for client")
  @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
  private List<PaymentReceipt> paymentReceipts = new ArrayList<>();

  @Schema(description = "Credit history for client")
  @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
  private List<CreditHistory> creditHistory = new ArrayList<>();

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
