package tn.inovexahub.hardware_store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.ProductConditioning;

@Repository
public interface ProductConditioningRepository extends JpaRepository<ProductConditioning, Long> {

  List<ProductConditioning> findByProductId(Long productId);
}
