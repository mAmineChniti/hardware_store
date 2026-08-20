package tn.inovexahub.hardware_store.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  // 0 = Timeouts.NO_WAIT: PostgreSQL ignores millisecond timeouts and emits "FOR UPDATE NOWAIT"
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")})
  @Query("SELECT d FROM Document d WHERE d.id = :id")
  Optional<Document> findByIdForUpdate(@Param("id") Long id);

  Optional<Document> findByDocumentNumber(String documentNumber);

  List<Document> findByClientId(Long clientId);

  List<Document> findByUserId(Long userId);

  List<Document> findByDocumentType(DocumentType documentType);

  List<Document> findByStatus(DocumentStatus status);

  List<Document> findByDateGreaterThanEqualAndDateLessThan(
      LocalDateTime startDate, LocalDateTime endDate);

  @Query("SELECT d FROM Document d WHERE d.client.id = :clientId AND d.isCreditSale = true")
  List<Document> findCreditSalesByClient(@Param("clientId") Long clientId);

  @Query(value = "SELECT nextval('seq_quote_number')", nativeQuery = true)
  Long getNextQuoteSequence();

  @Query(value = "SELECT nextval('seq_delivery_note_number')", nativeQuery = true)
  Long getNextDeliveryNoteSequence();

  @Query(value = "SELECT nextval('seq_invoice_number')", nativeQuery = true)
  Long getNextInvoiceSequence();
}
