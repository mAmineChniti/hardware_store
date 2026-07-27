package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Atomically revokes the token matching the given hash, but only if it is still active. This acts
   * as a compare-and-set at the database level: the row is only updated while {@code revoked} is
   * still {@code false}, so if the same refresh token is presented concurrently (e.g. replay, or
   * two near-simultaneous refresh calls), exactly one caller observes {@code 1} row updated and the
   * rest observe {@code 0}.
   *
   * @return the number of rows updated (0 or 1)
   */
  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true "
          + "WHERE rt.tokenHash = :tokenHash AND rt.revoked = false")
  int revokeIfActive(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true "
          + "WHERE rt.username = :username AND rt.revoked = false")
  int revokeAllActiveForUser(@Param("username") String username);

  long deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
