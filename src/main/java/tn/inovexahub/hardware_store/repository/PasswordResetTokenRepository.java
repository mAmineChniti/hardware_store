package tn.inovexahub.hardware_store.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.used = false")
  Optional<PasswordResetToken> findByUserIdAndUsedFalseForUpdate(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId AND t.used = false")
  void revokeAllActiveForUserId(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
  void revokeAllForUserId(@Param("userId") Long userId);
}
