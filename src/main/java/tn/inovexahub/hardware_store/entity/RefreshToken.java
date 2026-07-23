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
 * Persisted record of an issued refresh token, identified by the SHA-256 hash of its raw JWT value
 * (the raw token itself is never stored). Backs server-side revocation and atomic rotation so that
 * a revoked or already-rotated refresh token cannot be replayed.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Persisted refresh token record used for revocation and rotation")
public class RefreshToken {

  @Schema(
      description = "Unique refresh token record ID",
      example = "1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
      description = "SHA-256 hash of the raw refresh token",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Schema(
      description = "Username the refresh token was issued to",
      example = "john_doe",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "username", nullable = false, length = 50)
  private String username;

  @Schema(
      description = "Expiration timestamp of the refresh token",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Schema(
      description = "Whether the token has been revoked or already rotated",
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
