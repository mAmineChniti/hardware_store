package tn.inovexahub.hardware_store.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.DocumentLine;

@Repository
public interface DocumentLineRepository extends JpaRepository<DocumentLine, Long> {

  List<DocumentLine> findByDocumentId(Long documentId);

  List<DocumentLine> findByDocumentIdIn(List<Long> documentIds);

  List<DocumentLine> findByProductId(Long productId);

  List<DocumentLine> findByDocumentIdAndIsDeliveredFalse(Long documentId);

  @Query(
      "SELECT dl FROM DocumentLine dl JOIN dl.document d WHERE d.date >= :startDate AND d.date < :endDate")
  List<DocumentLine> findByDocumentDateRange(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
