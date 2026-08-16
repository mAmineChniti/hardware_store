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
import tn.inovexahub.hardware_store.entity.CreditHistory;
import tn.inovexahub.hardware_store.entity.PaymentReceipt;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.PaymentMethod;
import tn.inovexahub.hardware_store.enums.TransactionType;
import tn.inovexahub.hardware_store.exception.ClientNotFoundException;
import tn.inovexahub.hardware_store.exception.CreditLimitExceededException;
import tn.inovexahub.hardware_store.exception.InvalidPaymentException;
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

  // ==================== processPayment Tests ====================

  @Test
  void processPayment_HappyPath() {
    testClient.setCurrentDebt(new BigDecimal("5000.00"));

    PaymentReceipt savedReceipt = new PaymentReceipt();
    savedReceipt.setId(1L);
    savedReceipt.setReceiptNumber("REC-000001");

    CreditHistory creditHistory = new CreditHistory();
    creditHistory.setId(1L);

    User testUser = new User();
    testUser.setId(1L);

    when(paymentReceiptRepository.getNextReceiptSequence()).thenReturn(1L);
    when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenReturn(savedReceipt);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(creditHistory);
    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    PaymentReceipt result =
        clientService.processPayment(
            testClient, new BigDecimal("2000.00"), PaymentMethod.CASH, testUser);

    assertNotNull(result);
    verify(paymentReceiptRepository, org.mockito.Mockito.times(2)).save(any(PaymentReceipt.class));
    verify(clientRepository).save(testClient);
    verify(creditHistoryRepository).save(any(CreditHistory.class));
  }

  @Test
  void processPayment_ExceedsDebt_ThrowsInvalidPaymentException() {
    testClient.setCurrentDebt(new BigDecimal("1000.00"));

    User testUser = new User();
    testUser.setId(1L);

    when(paymentReceiptRepository.getNextReceiptSequence()).thenReturn(1L);

    assertThrows(
        InvalidPaymentException.class,
        () ->
            clientService.processPayment(
                testClient, new BigDecimal("2000.00"), PaymentMethod.CASH, testUser));
  }

  @Test
  void processPayment_NullAmount_ThrowsInvalidPaymentException() {
    User testUser = new User();
    testUser.setId(1L);

    assertThrows(
        InvalidPaymentException.class,
        () -> clientService.processPayment(testClient, null, PaymentMethod.CASH, testUser));
  }

  @Test
  void processPayment_ZeroOrNegativeAmount_ThrowsInvalidPaymentException() {
    User testUser = new User();
    testUser.setId(1L);

    assertThrows(
        InvalidPaymentException.class,
        () ->
            clientService.processPayment(
                testClient, BigDecimal.ZERO, PaymentMethod.CASH, testUser));
    assertThrows(
        InvalidPaymentException.class,
        () ->
            clientService.processPayment(
                testClient, new BigDecimal("-50.00"), PaymentMethod.CASH, testUser));
  }

  @Test
  void processPayment_PartialPayment_HappyPath() {
    testClient.setCurrentDebt(new BigDecimal("5000.00"));

    PaymentReceipt savedReceipt = new PaymentReceipt();
    savedReceipt.setId(1L);
    savedReceipt.setReceiptNumber("REC-000001");

    User testUser = new User();
    testUser.setId(1L);

    CreditHistory creditHistory = new CreditHistory();
    creditHistory.setId(1L);

    when(paymentReceiptRepository.getNextReceiptSequence()).thenReturn(1L);
    when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenReturn(savedReceipt);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(creditHistory);
    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    PaymentReceipt result =
        clientService.processPayment(
            testClient, new BigDecimal("500.00"), PaymentMethod.CHECK, testUser);

    assertNotNull(result);
    verify(paymentReceiptRepository, org.mockito.Mockito.times(2)).save(any(PaymentReceipt.class));
    verify(clientRepository).save(testClient);
  }

  @Test
  void processPayment_PaymentEqualFullDebt_HappyPath() {
    testClient.setCurrentDebt(new BigDecimal("5000.00"));

    PaymentReceipt savedReceipt = new PaymentReceipt();
    savedReceipt.setId(1L);
    savedReceipt.setReceiptNumber("REC-000001");

    User testUser = new User();
    testUser.setId(1L);

    CreditHistory creditHistory = new CreditHistory();
    creditHistory.setId(1L);

    when(paymentReceiptRepository.getNextReceiptSequence()).thenReturn(1L);
    when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenReturn(savedReceipt);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(creditHistory);
    when(clientRepository.save(any(Client.class))).thenReturn(testClient);

    PaymentReceipt result =
        clientService.processPayment(
            testClient, new BigDecimal("5000.00"), PaymentMethod.CASH, testUser);

    assertNotNull(result);
    verify(paymentReceiptRepository, org.mockito.Mockito.times(2)).save(any(PaymentReceipt.class));
    verify(clientRepository).save(testClient);
  }

  // ==================== addCreditHistoryEntry Tests ====================

  @Test
  void addCreditHistoryEntry_Sale_IncreasesDebt() {
    testClient.setCurrentDebt(new BigDecimal("1000.00"));

    CreditHistory savedCreditHistory = new CreditHistory();
    savedCreditHistory.setId(1L);

    when(clientRepository.save(any(Client.class))).thenReturn(testClient);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(savedCreditHistory);

    CreditHistory result =
        clientService.addCreditHistoryEntry(
            testClient, null, new BigDecimal("500.00"), TransactionType.SALE);

    assertNotNull(result);
    assertEquals(new BigDecimal("1500.00"), testClient.getCurrentDebt());
    verify(clientRepository).save(testClient);
    verify(creditHistoryRepository).save(any(CreditHistory.class));
  }

  @Test
  void addCreditHistoryEntry_NullDocument_Works() {
    CreditHistory savedCreditHistory = new CreditHistory();
    savedCreditHistory.setId(1L);

    when(clientRepository.save(any(Client.class))).thenReturn(testClient);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(savedCreditHistory);

    CreditHistory result =
        clientService.addCreditHistoryEntry(
            testClient, null, new BigDecimal("500.00"), TransactionType.SALE);

    assertNotNull(result);
    verify(creditHistoryRepository).save(any(CreditHistory.class));
  }

  // ==================== addPaymentCreditHistoryEntry Tests ====================

  @Test
  void addPaymentCreditHistoryEntry_Payment_DecreasesDebt() {
    testClient.setCurrentDebt(new BigDecimal("5000.00"));

    PaymentReceipt paymentReceipt = new PaymentReceipt();
    paymentReceipt.setId(1L);

    CreditHistory savedCreditHistory = new CreditHistory();
    savedCreditHistory.setId(1L);

    when(clientRepository.save(any(Client.class))).thenReturn(testClient);
    when(creditHistoryRepository.save(any(CreditHistory.class))).thenReturn(savedCreditHistory);

    CreditHistory result =
        clientService.addPaymentCreditHistoryEntry(
            testClient, paymentReceipt, new BigDecimal("-500.00"));

    assertNotNull(result);
    assertEquals(new BigDecimal("4500.00"), testClient.getCurrentDebt());
    verify(clientRepository).save(testClient);
    verify(creditHistoryRepository).save(any(CreditHistory.class));
  }

  // ==================== wouldExceedCreditLimit Tests ====================

  @Test
  void wouldExceedCreditLimit_NullAmount_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> clientService.wouldExceedCreditLimit(1L, null));
  }

  @Test
  void wouldExceedCreditLimit_ZeroAmount_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> clientService.wouldExceedCreditLimit(1L, BigDecimal.ZERO));
  }

  // ==================== validateCreditLimit Tests ====================

  @Test
  void validateCreditLimit_NullAmount_ThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> clientService.validateCreditLimit(1L, null));
  }

  @Test
  void validateCreditLimit_ZeroAmount_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> clientService.validateCreditLimit(1L, BigDecimal.ZERO));
  }

  // ==================== deleteClient Tests ====================

  @Test
  void deleteClient_NonExistingClient_ThrowsException() {
    when(clientRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ClientNotFoundException.class, () -> clientService.deleteClient(999L));
  }

  // ==================== getClientByTaxId Tests ====================

  @Test
  void getClientByTaxId_ExistingActiveClient_ReturnsClient() {
    testClient.setTaxIdentificationNumber("12345678");
    when(clientRepository.findByTaxIdentificationNumber("12345678"))
        .thenReturn(Optional.of(testClient));

    Optional<Client> result = clientService.getClientByTaxId("12345678");

    assertTrue(result.isPresent());
    assertEquals("Test Client", result.get().getName());
  }

  @Test
  void getClientByTaxId_DeletedClient_ReturnsEmpty() {
    testClient.setDeleted(true);
    when(clientRepository.findByTaxIdentificationNumber("12345678"))
        .thenReturn(Optional.of(testClient));

    Optional<Client> result = clientService.getClientByTaxId("12345678");

    assertFalse(result.isPresent());
  }

  // ==================== getClientPaymentReceipts Tests ====================

  @Test
  void getClientPaymentReceipts_ReturnsReceipts() {
    PaymentReceipt receipt = new PaymentReceipt();
    receipt.setId(1L);
    receipt.setReceiptNumber("REC-000001");

    when(paymentReceiptRepository.findByClientId(1L)).thenReturn(Arrays.asList(receipt));

    List<PaymentReceipt> receipts = clientService.getClientPaymentReceipts(1L);

    assertNotNull(receipts);
    assertEquals(1, receipts.size());
    assertEquals("REC-000001", receipts.get(0).getReceiptNumber());
    verify(paymentReceiptRepository).findByClientId(1L);
  }

  // ==================== getActiveCreditHistory Tests ====================

  @Test
  void getActiveCreditHistory_ReturnsActiveHistory() {
    CreditHistory history = new CreditHistory();
    history.setId(1L);
    history.setAmount(new BigDecimal("500.00"));

    when(creditHistoryRepository.findActiveCreditHistoryByClient(1L))
        .thenReturn(Arrays.asList(history));

    List<CreditHistory> activeHistory = clientService.getActiveCreditHistory(1L);

    assertNotNull(activeHistory);
    assertEquals(1, activeHistory.size());
    assertEquals(new BigDecimal("500.00"), activeHistory.get(0).getAmount());
    verify(creditHistoryRepository).findActiveCreditHistoryByClient(1L);
  }

  // ==================== getClientPaymentReceiptById ====================

  @Test
  void getClientPaymentReceiptById_ReturnsReceipt() {
    PaymentReceipt receipt = new PaymentReceipt();
    receipt.setId(10L);
    receipt.setClient(testClient);

    when(paymentReceiptRepository.findByIdAndClientId(10L, 1L)).thenReturn(Optional.of(receipt));

    Optional<PaymentReceipt> result = clientService.getClientPaymentReceiptById(1L, 10L);

    assertTrue(result.isPresent());
    assertEquals(10L, result.get().getId());
  }

  @Test
  void getClientPaymentReceiptById_NotFound_ReturnsEmpty() {
    when(paymentReceiptRepository.findByIdAndClientId(99L, 1L)).thenReturn(Optional.empty());

    Optional<PaymentReceipt> result = clientService.getClientPaymentReceiptById(1L, 99L);

    assertTrue(result.isEmpty());
  }
}
