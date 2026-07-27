package tn.inovexahub.hardware_store.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.repository.ClientRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientIntegrationTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private ClientRepository clientRepository;

  @Test
  void whenCreateClient_thenClientCanBeRetrieved() {
    Client client = new Client();
    client.setName("Test Client");
    client.setPhone("123456789");
    client.setEmail("test@example.com");
    client.setCreditLimit(new BigDecimal("10000.00"));
    client.setCurrentDebt(BigDecimal.ZERO);
    client.setDeleted(false);

    entityManager.persist(client);
    entityManager.flush();

    Client found = clientRepository.findById(client.getId()).orElse(null);

    assertNotNull(found);
    assertEquals("Test Client", found.getName());
    assertEquals("test@example.com", found.getEmail());
  }

  @Test
  void whenFindByDeletedFalse_thenReturnOnlyActiveClients() {
    Client activeClient = new Client();
    activeClient.setName("Active Client");
    activeClient.setCreditLimit(new BigDecimal("5000.00"));
    activeClient.setCurrentDebt(BigDecimal.ZERO);
    activeClient.setDeleted(false);
    entityManager.persist(activeClient);

    Client deletedClient = new Client();
    deletedClient.setName("Deleted Client");
    deletedClient.setCreditLimit(new BigDecimal("5000.00"));
    deletedClient.setCurrentDebt(BigDecimal.ZERO);
    deletedClient.setDeleted(true);
    entityManager.persist(deletedClient);

    entityManager.flush();

    var activeClients = clientRepository.findByDeletedFalseOrderByCurrentDebtDesc();

    assertNotNull(activeClients);
    assertEquals(1, activeClients.size());
    assertEquals("Active Client", activeClients.get(0).getName());
  }

  @Test
  void whenFindDebtors_thenReturnClientsWithDebt() {
    Client debtor = new Client();
    debtor.setName("Debtor Client");
    debtor.setCreditLimit(new BigDecimal("10000.00"));
    debtor.setCurrentDebt(new BigDecimal("5000.00"));
    debtor.setDeleted(false);
    entityManager.persist(debtor);

    Client noDebt = new Client();
    noDebt.setName("No Debt Client");
    noDebt.setCreditLimit(new BigDecimal("10000.00"));
    noDebt.setCurrentDebt(BigDecimal.ZERO);
    noDebt.setDeleted(false);
    entityManager.persist(noDebt);

    entityManager.flush();

    var debtors = clientRepository.findDebtorsOrderByDebtDesc();

    assertNotNull(debtors);
    assertEquals(1, debtors.size());
    assertEquals("Debtor Client", debtors.get(0).getName());
  }

  @Test
  void whenFindByTaxIdentificationNumber_thenReturnClient() {
    Client client = new Client();
    client.setName("Test Client");
    client.setTaxIdentificationNumber("123456789");
    client.setCreditLimit(new BigDecimal("5000.00"));
    client.setCurrentDebt(BigDecimal.ZERO);
    client.setDeleted(false);
    entityManager.persist(client);

    entityManager.flush();

    var found = clientRepository.findByTaxIdentificationNumber("123456789");

    assertTrue(found.isPresent());
    assertEquals("Test Client", found.get().getName());
  }
}
