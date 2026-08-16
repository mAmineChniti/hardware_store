package tn.inovexahub.hardware_store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.CreditHistory;

@Repository
public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {

  List<CreditHistory> findByClientIdOrderByEntryDateDesc(Long clientId);

  @Query(
      "SELECT ch FROM CreditHistory ch WHERE ch.client.id = :clientId "
          + "AND ch.deleted = false ORDER BY ch.entryDate DESC")
  List<CreditHistory> findActiveCreditHistoryByClient(Long clientId);
}
