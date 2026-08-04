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
 * Persisted record of an issued access token, identified by the SHA-256 hash of its raw JWT value
 * (the raw token itself is never stored). Acts as a server-side allowlist so that access tokens can
 * be revoked immediately (e.g. when the account's email changes) rather than only expiring
 * naturally.
 */
@Entity
@Table(name = "access_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Persisted access token record used for server-side revocation")
public class AccessToken {

  @Schema(
      description = "Unique access token record ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "SHA-256 hash of the raw access token",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Schema(
      description = "ID of the user the access token was issued to",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Schema(
      description = "Expiration timestamp of the access token",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Schema(
      description = "Whether the token has been revoked",
      example = "false",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
