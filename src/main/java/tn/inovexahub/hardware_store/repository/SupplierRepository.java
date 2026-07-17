package tn.inovexahub.hardware_store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  Optional<Supplier> findByTaxIdentificationNumber(String taxIdentificationNumber);

  List<Supplier> findByDeletedFalse();

  List<Supplier> findByNameContainingIgnoreCase(String name);

  List<Supplier> findByNameContainingIgnoreCaseAndDeletedFalse(String name);
}
