package tn.inovexahub.hardware_store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.DocumentLine;

@Repository
public interface DocumentLineRepository extends JpaRepository<DocumentLine, Long> {

  List<DocumentLine> findByDocumentId(Long documentId);

  List<DocumentLine> findByProductId(Long productId);

  List<DocumentLine> findByDocumentIdAndIsDeliveredFalse(Long documentId);
}
