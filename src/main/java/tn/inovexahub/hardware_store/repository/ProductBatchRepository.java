package tn.inovexahub.hardware_store.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.ProductBatch;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

  List<ProductBatch> findByProductIdOrderByCreatedAtAscIdAsc(Long productId);

  @Query(
      "SELECT pb FROM ProductBatch pb"
          + " WHERE pb.product.id = :productId AND pb.variant IS NULL"
          + " AND pb.quantity > 0 ORDER BY pb.createdAt ASC, pb.id ASC")
  List<ProductBatch> findAvailableBatchesByProductId(@Param("productId") Long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  // 0 = Timeouts.NO_WAIT: PostgreSQL ignores millisecond timeouts and emits "FOR UPDATE NOWAIT"
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")})
  @Query(
      "SELECT pb FROM ProductBatch pb"
          + " WHERE pb.product.id = :productId AND pb.variant IS NULL"
          + " AND pb.quantity > 0 ORDER BY pb.createdAt ASC, pb.id ASC")
  List<ProductBatch> lockAvailableBatchesByProductId(@Param("productId") Long productId);

  @Query(
      "SELECT SUM(pb.quantity) FROM ProductBatch pb"
          + " WHERE pb.product.id = :productId AND pb.quantity > 0")
  Optional<BigDecimal> sumAllAvailableQuantityByProductId(@Param("productId") Long productId);

  @Query(
      "SELECT SUM(pb.quantity * pb.unitCost)"
          + " FROM ProductBatch pb"
          + " WHERE pb.product.id = :productId AND pb.quantity > 0")
  Optional<BigDecimal> sumAllAvailableBatchesCost(@Param("productId") Long productId);

  boolean existsByProductIdAndVariantIsNull(Long productId);

  List<ProductBatch> findByVariantIdOrderByCreatedAtAscIdAsc(Long variantId);

  boolean existsByVariantId(Long variantId);

  @Query(
      "SELECT pb FROM ProductBatch pb"
          + " WHERE pb.variant.id = :variantId AND pb.quantity > 0"
          + " ORDER BY pb.createdAt ASC, pb.id ASC")
  List<ProductBatch> findAvailableBatchesByVariantId(@Param("variantId") Long variantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  // 0 = Timeouts.NO_WAIT: PostgreSQL ignores millisecond timeouts and emits "FOR UPDATE NOWAIT"
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")})
  @Query(
      "SELECT pb FROM ProductBatch pb"
          + " WHERE pb.variant.id = :variantId AND pb.quantity > 0"
          + " ORDER BY pb.createdAt ASC, pb.id ASC")
  List<ProductBatch> lockAvailableBatchesByVariantId(@Param("variantId") Long variantId);

  @Query(
      "SELECT SUM(pb.quantity) FROM ProductBatch pb WHERE pb.variant.id = :variantId AND pb.quantity > 0")
  Optional<BigDecimal> sumAvailableQuantityByVariantId(@Param("variantId") Long variantId);
}
