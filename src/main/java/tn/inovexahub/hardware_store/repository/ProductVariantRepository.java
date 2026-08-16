package tn.inovexahub.hardware_store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

  List<ProductVariant> findByProductIdOrderByVariantNameAscIdAsc(Long productId);

  Optional<ProductVariant> findBySku(String sku);

  boolean existsBySku(String sku);
}
