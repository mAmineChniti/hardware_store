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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Supplier entity representing vendors who supply products to the hardware store. Similar to Client
 * but for procurement side.
 */
@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vendor entity representing suppliers of products")
public class Supplier {

  @Schema(description = "Unique supplier ID", example = "1")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "Supplier name", example = "ABC Hardware Supplies")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Schema(description = "Supplier phone number", example = "+216 20 123 456")
  @Column(name = "phone", length = 20)
  private String phone;

  @Schema(description = "Supplier email", example = "contact@abchardware.tn")
  @Column(name = "email", length = 100)
  private String email;

  @Schema(description = "Supplier address", example = "123 Main St, Tunis")
  @Column(name = "address", length = 255)
  private String address;

  @Schema(description = "Tax identification number", example = "123456789")
  @Column(name = "tax_identification_number", length = 50)
  private String taxIdentificationNumber; // Matricule fiscal

  @Schema(description = "Contact person at supplier", example = "John Smith")
  @Column(name = "contact_person", length = 100)
  private String contactPerson;

  @Schema(description = "Payment terms agreed with supplier", example = "Net 30 days")
  @Column(name = "payment_terms", length = 100)
  private String paymentTerms;

  @Schema(description = "Additional notes about supplier", example = "Preferred supplier for tools")
  @Column(name = "notes", length = 500)
  private String notes;

  @Schema(description = "Whether supplier is soft deleted", example = "false")
  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

  @Schema(description = "Supplier creation timestamp", example = "2024-01-01T10:00:00")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(description = "Supplier last update timestamp", example = "2024-01-02T10:00:00")
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Schema(description = "Product costs from this supplier")
  @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
  private List<ProductCost> productCosts = new ArrayList<>();

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
