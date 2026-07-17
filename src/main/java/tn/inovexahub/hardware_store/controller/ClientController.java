package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.CreditHistory;
import tn.inovexahub.hardware_store.entity.PaymentReceipt;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.PaymentMethod;
import tn.inovexahub.hardware_store.exception.ClientNotFoundException;
import tn.inovexahub.hardware_store.exception.InvalidPaymentException;
import tn.inovexahub.hardware_store.repository.UserRepository;
import tn.inovexahub.hardware_store.service.ClientService;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client management including credit and payments")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

  private final ClientService clientService;
  private final UserRepository userRepository;

  public ClientController(ClientService clientService, UserRepository userRepository) {
    this.clientService = clientService;
    this.userRepository = userRepository;
  }

  // ==================== Client CRUD ====================

  @GetMapping
  @Operation(
      summary = "Get all clients",
      description = "Retrieve all active clients ordered by debt")
  public ResponseEntity<List<Client>> getAllClients() {
    return ResponseEntity.ok(clientService.getAllClients());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get client by ID", description = "Retrieve a specific client by its ID")
  public ResponseEntity<Client> getClientById(@PathVariable Long id) {
    return clientService
        .getClientById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/tax-id/{taxId}")
  @Operation(
      summary = "Get client by tax ID",
      description = "Retrieve a client by tax identification number")
  public ResponseEntity<Client> getClientByTaxId(@PathVariable String taxId) {
    return clientService
        .getClientByTaxId(taxId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Create new client", description = "Create a new client account")
  public ResponseEntity<Client> createClient(@Valid @RequestBody Client client) {
    Client createdClient = clientService.createClient(client);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update client", description = "Update an existing client")
  public ResponseEntity<Client> updateClient(
      @PathVariable Long id, @Valid @RequestBody Client clientDetails) {
    try {
      Client updatedClient = clientService.updateClient(id, clientDetails);
      return ResponseEntity.ok(updatedClient);
    } catch (ClientNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete client", description = "Soft delete a client")
  public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
    try {
      clientService.deleteClient(id);
      return ResponseEntity.noContent().build();
    } catch (ClientNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  // ==================== Credit Limit Validation ====================

  @GetMapping("/{id}/credit-limit-check")
  @Operation(
      summary = "Check credit limit",
      description = "Check if a sale would exceed the client's credit limit")
  public ResponseEntity<Boolean> checkCreditLimit(
      @PathVariable Long id, @RequestParam BigDecimal saleAmount) {
    try {
      boolean wouldExceed = clientService.wouldExceedCreditLimit(id, saleAmount);
      return ResponseEntity.ok(wouldExceed);
    } catch (ClientNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  // ==================== Credit History ====================

  @GetMapping("/{id}/credit-history")
  @Operation(
      summary = "Get client credit history",
      description = "Retrieve the complete credit history for a client")
  public ResponseEntity<List<CreditHistory>> getClientCreditHistory(@PathVariable Long id) {
    return ResponseEntity.ok(clientService.getClientCreditHistory(id));
  }

  @GetMapping("/{id}/credit-history/active")
  @Operation(
      summary = "Get active credit history",
      description = "Retrieve active (non-deleted) credit history for a client")
  public ResponseEntity<List<CreditHistory>> getActiveCreditHistory(@PathVariable Long id) {
    return ResponseEntity.ok(clientService.getActiveCreditHistory(id));
  }

  // ==================== Payment Receipts ====================

  @GetMapping("/{id}/payments")
  @Operation(
      summary = "Get client payments",
      description = "Retrieve all payment receipts for a client")
  public ResponseEntity<List<PaymentReceipt>> getClientPayments(@PathVariable Long id) {
    return ResponseEntity.ok(clientService.getClientPaymentReceipts(id));
  }

  @PostMapping("/{id}/payments")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Process payment",
      description = "Process a payment for a client and update credit history")
  public ResponseEntity<PaymentReceipt> processPayment(
      @PathVariable Long id,
      @RequestParam BigDecimal amountPaid,
      @RequestParam PaymentMethod paymentMethod,
      @RequestParam Long userId) {

    try {
      Client client =
          clientService.getClientById(id).orElseThrow(() -> new ClientNotFoundException(id));

      User user = userRepository.findById(userId).orElse(null);

      PaymentReceipt paymentReceipt =
          clientService.processPayment(client, amountPaid, paymentMethod, user);
      return ResponseEntity.status(HttpStatus.CREATED).body(paymentReceipt);
    } catch (ClientNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (InvalidPaymentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  // ==================== Reporting ====================

  @GetMapping("/debtors")
  @Operation(
      summary = "Get debtors",
      description = "Retrieve all clients with outstanding debt, ordered by debt amount")
  public ResponseEntity<List<Client>> getDebtors() {
    return ResponseEntity.ok(clientService.getDebtors());
  }

  @GetMapping("/near-limit")
  @Operation(
      summary = "Get clients near credit limit",
      description = "Retrieve clients within threshold of their credit limit")
  public ResponseEntity<List<Client>> getClientsNearCreditLimit(
      @RequestParam(defaultValue = "100.0") BigDecimal threshold) {
    return ResponseEntity.ok(clientService.getClientsNearCreditLimit(threshold));
  }
}
