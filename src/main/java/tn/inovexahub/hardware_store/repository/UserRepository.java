package tn.inovexahub.hardware_store.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
