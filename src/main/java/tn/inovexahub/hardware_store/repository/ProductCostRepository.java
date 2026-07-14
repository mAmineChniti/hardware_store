package tn.inovexahub.hardware_store.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductCost;

@Repository
public interface ProductCostRepository extends JpaRepository<ProductCost, Long> {

  List<ProductCost> findByProductOrderByEffectiveDateDesc(Product product);

  Optional<ProductCost> findTopByProductOrderByEffectiveDateDesc(Product product);

  Optional<ProductCost> findByProductAndEffectiveDate(Product product, LocalDate effectiveDate);

  @Query(
      "SELECT pc FROM ProductCost pc WHERE pc.product = :product "
          + "AND pc.effectiveDate <= :date ORDER BY pc.effectiveDate DESC")
  Optional<ProductCost> findMostRecentCostBeforeDate(
      @Param("product") Product product, @Param("date") LocalDate date);

  @Query(
      "SELECT pc FROM ProductCost pc WHERE pc.product = :product "
          + "AND pc.effectiveDate BETWEEN :startDate AND :endDate ORDER BY pc.effectiveDate ASC")
  List<ProductCost> findByProductAndEffectiveDateBetween(
      @Param("product") Product product,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
