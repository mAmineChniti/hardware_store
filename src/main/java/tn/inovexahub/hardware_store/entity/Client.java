package tn.inovexahub.hardware_store.entity;

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
public class Client {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @Column(name = "name", nullable = false, length = 100)
  @NotBlank(message = "Client name is required")
  private String name;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 100)
  @Email(message = "Email must be valid")
  private String email;

  @Column(name = "address", length = 255)
  private String address;

  @Column(name = "tax_identification_number", length = 50)
  private String taxIdentificationNumber; // Matricule fiscal

  @Column(name = "credit_limit", precision = 19, scale = 3)
  @NotNull(message = "Credit limit is required")
  @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
  private BigDecimal creditLimit = BigDecimal.ZERO; // plafond_credit_autorise

  @Column(name = "current_debt", precision = 19, scale = 3)
  @NotNull(message = "Current debt is required")
  @DecimalMin(value = "0.0", message = "Current debt cannot be negative")
  private BigDecimal currentDebt = BigDecimal.ZERO; // Dette_Actuelle

  @Column(name = "deleted", nullable = false)
  @NotNull(message = "Deleted flag is required")
  private Boolean deleted = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
  private List<Document> documents = new ArrayList<>();

  @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
  private List<PaymentReceipt> paymentReceipts = new ArrayList<>();

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
