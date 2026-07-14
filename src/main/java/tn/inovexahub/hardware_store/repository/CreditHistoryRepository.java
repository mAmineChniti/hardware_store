package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.CreditHistory;
import tn.inovexahub.hardware_store.enums.TransactionType;

@Repository
public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {

  List<CreditHistory> findByClientId(Long clientId);

  List<CreditHistory> findByClientIdOrderByEntryDateDesc(Long clientId);

  List<CreditHistory> findByClientIdAndDeletedFalse(Long clientId);

  List<CreditHistory> findByTransactionType(TransactionType transactionType);

  List<CreditHistory> findByEntryDateBetween(LocalDateTime startDate, LocalDateTime endDate);

  @Query(
      "SELECT ch FROM CreditHistory ch WHERE ch.clientId = :clientId AND ch.deleted = false ORDER BY ch.entryDate DESC")
  List<CreditHistory> findActiveCreditHistoryByClient(Long clientId);
}
