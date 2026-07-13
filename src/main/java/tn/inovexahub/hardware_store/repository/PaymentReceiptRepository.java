package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.PaymentReceipt;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

  Optional<PaymentReceipt> findByReceiptNumber(String receiptNumber);

  List<PaymentReceipt> findByClientId(Long clientId);

  List<PaymentReceipt> findByUserId(Long userId);

  List<PaymentReceipt> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
