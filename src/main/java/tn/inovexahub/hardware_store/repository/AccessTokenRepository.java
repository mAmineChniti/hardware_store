package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.AccessToken;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {

  Optional<AccessToken> findByTokenHash(String tokenHash);

  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE AccessToken at SET at.revoked = true "
          + "WHERE at.userId = :userId AND at.revoked = false")
  int revokeAllActiveForUser(@Param("userId") Long userId);

  long deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
