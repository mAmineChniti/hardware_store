package tn.inovexahub.hardware_store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.PaymentReceipt;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

  List<PaymentReceipt> findByClientId(Long clientId);

  Optional<PaymentReceipt> findByIdAndClientId(Long id, Long clientId);

  @Query(value = "SELECT nextval('seq_receipt_number')", nativeQuery = true)
  Long getNextReceiptSequence();
}
