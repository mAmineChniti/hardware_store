package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuditLog entity for generic audit of critical actions. Section 10: AuditLog entity (optional but
 * recommended)
 *
 * <p>{@code userId} is an optional identity snapshot: a nullable scalar with no JPA association to
 * {@link User} (and no DB foreign key). {@code email} mirrors the user's email at action time.
 * Audit rows deliberately remain valid if the acting user is later deleted, and system-triggered
 * actions may have no user at all.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entity for tracking critical system actions")
public class AuditLog {

  @Schema(
      description = "Unique audit log ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "ID of the user who performed the action", example = "1")
  @Column(name = "user_id")
  private Long userId;

  @Schema(
      description = "Email of the user who performed the action",
      example = "john.doe@example.com")
  @Column(name = "email", length = 100)
  private String email;

  @Schema(description = "Action performed", example = "DELETE_CLIENT")
  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Schema(description = "Type of entity affected", example = "Client")
  @Column(name = "entity_type", length = 50)
  private String entityType;

  @Schema(description = "ID of entity affected", example = "5")
  @Column(name = "entity_id")
  private Long entityId;

  @Schema(description = "Additional details about the action", example = "Deleted client with ID 5")
  @Column(name = "details", columnDefinition = "TEXT")
  private String details;

  @Schema(
      description = "Timestamp when action was performed",
      example = "2024-01-01T10:00:00",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;

  @PrePersist
  protected void onCreate() {
    if (timestamp == null) {
      timestamp = LocalDateTime.now();
    }
  }
}
