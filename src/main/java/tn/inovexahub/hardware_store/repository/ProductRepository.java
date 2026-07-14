package tn.inovexahub.hardware_store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.UnitType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  Optional<Product> findByReference(String reference);

  List<Product> findByCategory(String category);

  List<Product> findByUnitType(UnitType unitType);

  List<Product> findByIsHeavyMaterialTrue();

  @Query(
      "SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  List<Product> searchByKeyword(String keyword);

  @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold")
  List<Product> findLowStock(java.math.BigDecimal threshold);
}
