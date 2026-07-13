package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByUsername(String username);

  List<AuditLog> findByEntityType(String entityType);

  List<AuditLog> findByEntityId(Long entityId);

  List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

  @Query(
      "SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
  List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

  @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :startDate ORDER BY a.timestamp DESC")
  List<AuditLog> findRecentLogs(LocalDateTime startDate);
}
