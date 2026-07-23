package tn.inovexahub.hardware_store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token entity for user authentication token refresh")
public class RefreshToken {

  @Schema(description = "Unique refresh token ID", example = "1")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(description = "Hashed refresh token value", accessMode = Schema.AccessMode.WRITE_ONLY)
  @Column(name = "token", nullable = false, unique = true, length = 64)
  private String token;

  @Schema(description = "User associated with this refresh token")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Schema(description = "Timestamp when refresh token expires", example = "2024-01-01T12:00:00")
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Schema(description = "Whether this refresh token has been revoked", example = "false")
  @Column(name = "revoked", nullable = false)
  private Boolean revoked = false;

  @Schema(description = "Timestamp when refresh token was created", example = "2024-01-01T10:00:00")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Schema(description = "Version number for optimistic locking", example = "1")
  @Version
  @Column(name = "version")
  private Integer version;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
