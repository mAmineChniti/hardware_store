package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.exception.ClientNotFoundException;
import tn.inovexahub.hardware_store.exception.CreditLimitExceededException;
import tn.inovexahub.hardware_store.repository.ClientRepository;
import tn.inovexahub.hardware_store.repository.CreditHistoryRepository;
import tn.inovexahub.hardware_store.repository.PaymentReceiptRepository;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock private ClientRepository clientRepository;

  @Mock private CreditHistoryRepository creditHistoryRepository;

  @Mock private PaymentReceiptRepository paymentReceiptRepository;

  @InjectMocks private ClientService clientService;

  private Client testClient;

  @BeforeEach
  void setUp() {
    testClient = new Client();
    testClient.setId(1L);
    testClient.setName("Test Client");
    testClient.setCreditLimit(new BigDecimal("10000.00"));
    testClient.setCurrentDebt(new BigDecimal("5000.00"));
    testClient.setDeleted(false);
  }

  @Test
  void getAllClients_ReturnsActiveClients() {
    when(clientRepository.findByDeletedFalseOrderByCurrentDebtDesc())
        .thenReturn(Arrays.asList(testClient));

    List<Client> clients = clientService.getAllClients();

    assertNotNull(clients);
    assertEquals(1, clients.size());
    assertEquals("Test Client", clients.get(0).getName());
    verify(clientRepository).findByDeletedFalseOrderByCurrentDebtDesc();
  }

  @Test
  void getClientById_ExistingClient_ReturnsClient() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));

    Optional<Client> result = clientService.getClientById(1L);

    assertTrue(result.isPresent());
    assertEquals("Test Client", result.get().getName());
  }

  @Test
  void getClientById_NonExistingClient_ReturnsEmpty() {
    when(clientRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    Optional<Client> result = clientService.getClientById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  void createClient_SetsDefaultsAndSaves() {
    Client newClient = new Client();
    newClient.setName("New Client");
    newClient.setCreditLimit(new BigDecimal("5000.00"));

    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    Client savedClient = clientService.createClient(newClient);

    assertNotNull(savedClient);
    assertEquals(BigDecimal.ZERO, newClient.getCurrentDebt());
    assertFalse(newClient.getDeleted());
    verify(clientRepository).save(newClient);
  }

  @Test
  void updateClient_ExistingClient_UpdatesFields() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));
    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    Client updatedDetails = new Client();
    updatedDetails.setName("Updated Name");
    updatedDetails.setPhone("123456789");

    Client result = clientService.updateClient(1L, updatedDetails);

    assertNotNull(result);
    verify(clientRepository).save(testClient);
  }

  @Test
  void updateClient_NonExistingClient_ThrowsException() {
    when(clientRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    Client updatedDetails = new Client();
    updatedDetails.setName("Updated Name");

    assertThrows(
        ClientNotFoundException.class, () -> clientService.updateClient(999L, updatedDetails));
  }

  @Test
  void deleteClient_SetsDeletedFlag() {
    when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    clientService.deleteClient(1L);

    assertTrue(testClient.getDeleted());
    verify(clientRepository).save(testClient);
  }

  @Test
  void wouldExceedCreditLimit_WithinLimit_ReturnsFalse() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));

    boolean result = clientService.wouldExceedCreditLimit(1L, new BigDecimal("4000.00"));

    assertFalse(result);
  }

  @Test
  void wouldExceedCreditLimit_ExceedsLimit_ReturnsTrue() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));

    boolean result = clientService.wouldExceedCreditLimit(1L, new BigDecimal("6000.00"));

    assertTrue(result);
  }

  @Test
  void wouldExceedCreditLimit_NegativeAmount_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> clientService.wouldExceedCreditLimit(1L, new BigDecimal("-100.00")));
  }

  @Test
  void validateCreditLimit_WithinLimit_DoesNotThrow() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));

    clientService.validateCreditLimit(1L, new BigDecimal("4000.00"));

    verify(clientRepository, never()).findById(1L);
  }

  @Test
  void validateCreditLimit_ExceedsLimit_ThrowsException() {
    when(clientRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClient));
    when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

    assertThrows(
        CreditLimitExceededException.class,
        () -> clientService.validateCreditLimit(1L, new BigDecimal("6000.00")));
  }

  @Test
  void getDebtors_ReturnsClientsWithDebt() {
    when(clientRepository.findDebtorsOrderByDebtDesc()).thenReturn(Arrays.asList(testClient));

    List<Client> debtors = clientService.getDebtors();

    assertNotNull(debtors);
    assertEquals(1, debtors.size());
    verify(clientRepository).findDebtorsOrderByDebtDesc();
  }

  @Test
  void getClientsNearCreditLimit_WithinThreshold_ReturnsClients() {
    testClient.setCurrentDebt(new BigDecimal("9500.00"));
    when(clientRepository.findByDeletedFalseOrderByCurrentDebtDesc())
        .thenReturn(Arrays.asList(testClient));

    List<Client> nearLimit = clientService.getClientsNearCreditLimit(new BigDecimal("1000.00"));

    assertNotNull(nearLimit);
    assertEquals(1, nearLimit.size());
  }

  @Test
  void getClientsNearCreditLimit_OutsideThreshold_ReturnsEmpty() {
    testClient.setCurrentDebt(new BigDecimal("1000.00"));
    when(clientRepository.findByDeletedFalseOrderByCurrentDebtDesc())
        .thenReturn(Arrays.asList(testClient));

    List<Client> nearLimit = clientService.getClientsNearCreditLimit(new BigDecimal("100.00"));

    assertNotNull(nearLimit);
    assertEquals(0, nearLimit.size());
  }
}
