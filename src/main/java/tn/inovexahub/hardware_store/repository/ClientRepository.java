package tn.inovexahub.hardware_store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.inovexahub.hardware_store.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

  Optional<Client> findByTaxIdentificationNumber(String taxIdentificationNumber);

  Optional<Client> findByIdAndDeletedFalse(Long id);

  List<Client> findByDeletedFalseOrderByCurrentDebtDesc();

  @Query(
      "SELECT c FROM Client c WHERE c.deleted = false AND c.currentDebt > 0 ORDER BY c.currentDebt DESC")
  List<Client> findDebtorsOrderByDebtDesc();

  @Query(
      "SELECT c FROM Client c WHERE c.deleted = false AND (c.currentDebt + :amount) > c.creditLimit")
  List<Client> findClientsExceedingCreditLimit(@Param("amount") java.math.BigDecimal amount);
}
