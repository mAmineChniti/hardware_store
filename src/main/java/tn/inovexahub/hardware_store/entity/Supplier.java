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
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "address", length = 255)
  private String address;

  @Column(name = "tax_identification_number", length = 50)
  private String taxIdentificationNumber; // Matricule fiscal

  @Column(name = "contact_person", length = 100)
  private String contactPerson;

  @Column(name = "payment_terms", length = 100)
  private String paymentTerms;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "deleted", nullable = false)
  private Boolean deleted = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

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
