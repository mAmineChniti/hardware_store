package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

  Optional<Document> findByDocumentNumber(String documentNumber);

  List<Document> findByClientId(Long clientId);

  List<Document> findByUserId(Long userId);

  List<Document> findByDocumentType(DocumentType documentType);

  List<Document> findByStatus(DocumentStatus status);

  List<Document> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

  @Query("SELECT d FROM Document d WHERE d.client.id = :clientId AND d.isCreditSale = true")
  List<Document> findCreditSalesByClient(@Param("clientId") Long clientId);

  @Query("SELECT d FROM Document d WHERE d.status = :status AND d.documentType = :documentType")
  List<Document> findDocumentsByStatusAndType(
      @Param("status") DocumentStatus status, @Param("documentType") DocumentType documentType);

  default List<Document> findValidatedInvoices() {
    return findDocumentsByStatusAndType(DocumentStatus.VALIDATED, DocumentType.INVOICE);
  }
}
