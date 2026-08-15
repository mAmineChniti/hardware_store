package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.CreditHistory;
import tn.inovexahub.hardware_store.entity.PaymentReceipt;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.PaymentMethod;
import tn.inovexahub.hardware_store.enums.UserRole;
import tn.inovexahub.hardware_store.exception.ClientNotFoundException;
import tn.inovexahub.hardware_store.exception.InvalidPaymentException;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.service.ClientService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

  @Mock private ClientService clientService;
  @Mock private UserRepository userRepository;

  private ClientController clientController;

  @BeforeEach
  void setUp() {
    clientController = new ClientController(clientService, userRepository);
  }

  private Client createClient(Long id, String name) {
    Client client = new Client();
    client.setId(id);
    client.setName(name);
    client.setPhone("+216 20 123 456");
    client.setEmail("client@example.com");
    client.setAddress("123 Main St, Tunis");
    client.setTaxIdentificationNumber("123456789");
    client.setCreditLimit(new BigDecimal("10000.000"));
    client.setCurrentDebt(new BigDecimal("500.000"));
    client.setDeleted(false);
    return client;
  }

  private User createUser(Long id, String email) {
    User user = new User();
    user.setId(id);
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail(email);
    user.setPassword("encodedPassword");
    user.setRole(UserRole.EMPLOYEE);
    user.setEnabled(true);
    return user;
  }

  private PaymentReceipt createPaymentReceipt(Long id) {
    PaymentReceipt receipt = new PaymentReceipt();
    receipt.setId(id);
    receipt.setReceiptNumber("REC-2024-001");
    receipt.setAmountPaid(new BigDecimal("200.000"));
    receipt.setPaymentMethod(PaymentMethod.CASH);
    receipt.setPreviousDebt(new BigDecimal("500.000"));
    receipt.setNewDebt(new BigDecimal("300.000"));
    return receipt;
  }

  // --- getAllClients ---

  @Test
  void getAllClients_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getAllClients()).thenReturn(List.of(client));

    ResponseEntity<List<Client>> response = clientController.getAllClients();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("Ahmed", response.getBody().get(0).getName());
  }

  @Test
  void getAllClients_EmptyList_ReturnsOk() {
    when(clientService.getAllClients()).thenReturn(Collections.emptyList());

    ResponseEntity<List<Client>> response = clientController.getAllClients();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  // --- getClientById ---

  @Test
  void getClientById_Found_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getClientById(1L)).thenReturn(Optional.of(client));

    ResponseEntity<Client> response = clientController.getClientById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Ahmed", response.getBody().getName());
  }

  @Test
  void getClientById_NotFound_ReturnsNotFound() {
    when(clientService.getClientById(999L)).thenReturn(Optional.empty());

    ResponseEntity<Client> response = clientController.getClientById(999L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // --- getClientByTaxId ---

  @Test
  void getClientByTaxId_Found_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getClientByTaxId("123456789")).thenReturn(Optional.of(client));

    ResponseEntity<Client> response = clientController.getClientByTaxId("123456789");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Ahmed", response.getBody().getName());
  }

  @Test
  void getClientByTaxId_NotFound_ReturnsNotFound() {
    when(clientService.getClientByTaxId("999999999")).thenReturn(Optional.empty());

    ResponseEntity<Client> response = clientController.getClientByTaxId("999999999");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // --- createClient ---

  @Test
  void createClient_ValidClient_ReturnsCreated() {
    Client client = createClient(null, "Ahmed");
    Client savedClient = createClient(1L, "Ahmed");
    when(clientService.createClient(any(Client.class))).thenReturn(savedClient);

    ResponseEntity<Client> response = clientController.createClient(client);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
    assertEquals("Ahmed", response.getBody().getName());
  }

  // --- updateClient ---

  @Test
  void updateClient_ExistingClient_ReturnsOk() {
    Client clientDetails = createClient(null, "Updated Name");
    Client updatedClient = createClient(1L, "Updated Name");
    when(clientService.updateClient(eq(1L), any(Client.class))).thenReturn(updatedClient);

    ResponseEntity<Client> response = clientController.updateClient(1L, clientDetails);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Updated Name", response.getBody().getName());
  }

  @Test
  void updateClient_NonExistingClient_ThrowsNotFound() {
    Client clientDetails = createClient(null, "Updated Name");
    when(clientService.updateClient(eq(999L), any(Client.class)))
        .thenThrow(new ClientNotFoundException(999L));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> clientController.updateClient(999L, clientDetails));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- deleteClient ---

  @Test
  void deleteClient_ExistingClient_ReturnsNoContent() {
    org.mockito.Mockito.doNothing().when(clientService).deleteClient(1L);

    ResponseEntity<Void> response = clientController.deleteClient(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(clientService).deleteClient(1L);
  }

  @Test
  void deleteClient_NonExistingClient_ThrowsNotFound() {
    org.mockito.Mockito.doThrow(new ClientNotFoundException(999L))
        .when(clientService)
        .deleteClient(999L);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> clientController.deleteClient(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- checkCreditLimit ---

  @Test
  void checkCreditLimit_WouldExceed_ReturnsTrue() {
    when(clientService.wouldExceedCreditLimit(1L, new BigDecimal("15000.00"))).thenReturn(true);

    ResponseEntity<Boolean> response =
        clientController.checkCreditLimit(1L, new BigDecimal("15000.00"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody());
  }

  @Test
  void checkCreditLimit_WouldNotExceed_ReturnsFalse() {
    when(clientService.wouldExceedCreditLimit(1L, new BigDecimal("500.00"))).thenReturn(false);

    ResponseEntity<Boolean> response =
        clientController.checkCreditLimit(1L, new BigDecimal("500.00"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(false, response.getBody());
  }

  @Test
  void checkCreditLimit_ClientNotFound_ThrowsNotFound() {
    when(clientService.wouldExceedCreditLimit(eq(999L), any(BigDecimal.class)))
        .thenThrow(new ClientNotFoundException(999L));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> clientController.checkCreditLimit(999L, new BigDecimal("500.00")));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void checkCreditLimit_NegativeAmount_ThrowsBadRequest() {
    when(clientService.wouldExceedCreditLimit(1L, new BigDecimal("-100.00")))
        .thenThrow(new IllegalArgumentException("Sale amount must be positive"));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () -> clientController.checkCreditLimit(1L, new BigDecimal("-100.00")));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // --- getClientCreditHistory ---

  @Test
  void getClientCreditHistory_ReturnsOk() {
    CreditHistory history = new CreditHistory();
    history.setId(1L);
    when(clientService.getClientCreditHistory(1L)).thenReturn(List.of(history));

    ResponseEntity<List<CreditHistory>> response = clientController.getClientCreditHistory(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(1L, response.getBody().get(0).getId());
  }

  @Test
  void getClientCreditHistory_EmptyList_ReturnsOk() {
    when(clientService.getClientCreditHistory(1L)).thenReturn(Collections.emptyList());

    ResponseEntity<List<CreditHistory>> response = clientController.getClientCreditHistory(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  // --- getActiveCreditHistory ---

  @Test
  void getActiveCreditHistory_ReturnsOk() {
    CreditHistory history = new CreditHistory();
    history.setId(1L);
    when(clientService.getActiveCreditHistory(1L)).thenReturn(List.of(history));

    ResponseEntity<List<CreditHistory>> response = clientController.getActiveCreditHistory(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getActiveCreditHistory_EmptyList_ReturnsOk() {
    when(clientService.getActiveCreditHistory(1L)).thenReturn(Collections.emptyList());

    ResponseEntity<List<CreditHistory>> response = clientController.getActiveCreditHistory(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  // --- getClientPayments ---

  @Test
  void getClientPayments_ReturnsOk() {
    PaymentReceipt receipt = createPaymentReceipt(1L);
    when(clientService.getClientPaymentReceipts(1L)).thenReturn(List.of(receipt));

    ResponseEntity<List<PaymentReceipt>> response = clientController.getClientPayments(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("REC-2024-001", response.getBody().get(0).getReceiptNumber());
  }

  @Test
  void getClientPayments_EmptyList_ReturnsOk() {
    when(clientService.getClientPaymentReceipts(1L)).thenReturn(Collections.emptyList());

    ResponseEntity<List<PaymentReceipt>> response = clientController.getClientPayments(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  // --- processPayment ---

  @Test
  void processPayment_Success_ReturnsCreated() {
    Client client = createClient(1L, "Ahmed");
    User user = createUser(1L, "admin@example.com");
    PaymentReceipt receipt = createPaymentReceipt(1L);

    when(clientService.getClientById(1L)).thenReturn(Optional.of(client));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(clientService.processPayment(
            eq(client), eq(new BigDecimal("200.00")), eq(PaymentMethod.CASH), eq(user)))
        .thenReturn(receipt);

    ResponseEntity<PaymentReceipt> response =
        clientController.processPayment(1L, new BigDecimal("200.00"), PaymentMethod.CASH, 1L);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("REC-2024-001", response.getBody().getReceiptNumber());
  }

  @Test
  void processPayment_ClientNotFound_ThrowsNotFound() {
    when(clientService.getClientById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () ->
                clientController.processPayment(
                    999L, new BigDecimal("200.00"), PaymentMethod.CASH, 1L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void processPayment_InvalidPaymentExceedsDebt_ThrowsBadRequest() {
    Client client = createClient(1L, "Ahmed");
    User user = createUser(1L, "admin@example.com");

    when(clientService.getClientById(1L)).thenReturn(Optional.of(client));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(clientService.processPayment(
            eq(client), eq(new BigDecimal("99999.00")), eq(PaymentMethod.CASH), eq(user)))
        .thenThrow(new InvalidPaymentException("Payment amount exceeds current debt"));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class,
            () ->
                clientController.processPayment(
                    1L, new BigDecimal("99999.00"), PaymentMethod.CASH, 1L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void processPayment_UserNotFound_SetsUserNull() {
    Client client = createClient(1L, "Ahmed");
    PaymentReceipt receipt = createPaymentReceipt(1L);

    when(clientService.getClientById(1L)).thenReturn(Optional.of(client));
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    when(clientService.processPayment(
            eq(client), eq(new BigDecimal("200.00")), eq(PaymentMethod.CASH), eq(null)))
        .thenReturn(receipt);

    ResponseEntity<PaymentReceipt> response =
        clientController.processPayment(1L, new BigDecimal("200.00"), PaymentMethod.CASH, 999L);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("REC-2024-001", response.getBody().getReceiptNumber());
  }

  // --- getDebtors ---

  @Test
  void getDebtors_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getDebtors()).thenReturn(List.of(client));

    ResponseEntity<List<Client>> response = clientController.getDebtors();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("Ahmed", response.getBody().get(0).getName());
  }

  @Test
  void getDebtors_EmptyList_ReturnsOk() {
    when(clientService.getDebtors()).thenReturn(Collections.emptyList());

    ResponseEntity<List<Client>> response = clientController.getDebtors();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  // --- getClientsNearCreditLimit ---

  @Test
  void getClientsNearCreditLimit_DefaultThreshold_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getClientsNearCreditLimit(new BigDecimal("100.0")))
        .thenReturn(List.of(client));

    ResponseEntity<List<Client>> response =
        clientController.getClientsNearCreditLimit(new BigDecimal("100.0"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getClientsNearCreditLimit_CustomThreshold_ReturnsOk() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getClientsNearCreditLimit(new BigDecimal("500.0")))
        .thenReturn(List.of(client));

    ResponseEntity<List<Client>> response =
        clientController.getClientsNearCreditLimit(new BigDecimal("500.0"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getClientsNearCreditLimit_EmptyList_ReturnsOk() {
    when(clientService.getClientsNearCreditLimit(new BigDecimal("100.0")))
        .thenReturn(Collections.emptyList());

    ResponseEntity<List<Client>> response =
        clientController.getClientsNearCreditLimit(new BigDecimal("100.0"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isEmpty());
  }

  @Test
  void getClientsNearCreditLimit_NullThreshold_UsesDefault() {
    Client client = createClient(1L, "Ahmed");
    when(clientService.getClientsNearCreditLimit(new BigDecimal("100.0")))
        .thenReturn(List.of(client));

    ResponseEntity<List<Client>> response = clientController.getClientsNearCreditLimit(null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(clientService).getClientsNearCreditLimit(new BigDecimal("100.0"));
  }
}
