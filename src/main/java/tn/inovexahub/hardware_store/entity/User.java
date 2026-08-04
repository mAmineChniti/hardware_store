package tn.inovexahub.hardware_store.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.Locale;
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
@Schema(description = "System user entity")
public class User {

  @Schema(description = "Unique user ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "User's first name", example = "John")
  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Schema(description = "User's last name", example = "Doe")
  @Column(name = "last_name", nullable = false, length = 50)
  private String lastName;

  @Schema(description = "User's email address", example = "john.doe@example.com")
  @Column(name = "email", unique = true, nullable = false, length = 100)
  private String email;

  @Schema(description = "BCrypt hashed password", accessMode = Schema.AccessMode.WRITE_ONLY)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Column(name = "password", nullable = false)
  private String password; // BCrypt hashed

  @Schema(description = "User's role (EMPLOYEE or ADMIN)", example = "EMPLOYEE")
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Schema(description = "Whether the user account is enabled", example = "true")
  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Schema(
      description = "Account creation timestamp",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(
      description = "Account last update timestamp",
      example = "2024-01-02T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    normalizeEmail();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    normalizeEmail();
  }

  private void normalizeEmail() {
    if (email != null) {
      email = email.trim().toLowerCase(Locale.ROOT);
    }
  }
}
