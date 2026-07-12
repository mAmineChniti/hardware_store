package tn.inovexahub.hardware_store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.UserRole;

/** User entity representing system users. Section 1: User entity */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", unique = true, nullable = false, length = 50)
  private String username;

  @Column(name = "password", nullable = false)
  private String password; // BCrypt hashed

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

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
