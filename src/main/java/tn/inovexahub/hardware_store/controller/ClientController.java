package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of active clients retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Client.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Client>> getAllClients() {
    return ResponseEntity.ok(clientService.getAllClients());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get client by ID", description = "Retrieve a specific client by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Client.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<Client> getClientById(
      @Parameter(description = "ID of client to retrieve", example = "1", required = true)
          @PathVariable
          Long id) {
    return clientService
        .getClientById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/tax-id/{taxId}")
  @Operation(
      summary = "Get client by tax ID",
      description = "Retrieve a client by tax identification number")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Client.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<Client> getClientByTaxId(
      @Parameter(
              description = "Tax Identification Number (Matricule Fiscal)",
              example = "123456789",
              required = true)
          @PathVariable
          String taxId) {
    return clientService
        .getClientByTaxId(taxId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Create new client", description = "Create a new client account")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Client created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Client.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Client> createClient(
      @RequestBody(description = "Client creation details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Client client) {
    Client createdClient = clientService.createClient(client);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update client", description = "Update an existing client")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Client updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Client.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<Client> updateClient(
      @Parameter(description = "ID of client to update", example = "1", required = true)
          @PathVariable
          Long id,
      @RequestBody(description = "Client updated details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Client clientDetails) {
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
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Client deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<Void> deleteClient(
      @Parameter(description = "ID of client to delete", example = "1", required = true)
          @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credit limit check result (true if exceeded)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "Invalid sale amount", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<Boolean> checkCreditLimit(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable Long id,
      @Parameter(
              description = "Sale amount to test against credit limit",
              example = "500.00",
              required = true)
          @RequestParam
          BigDecimal saleAmount) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credit history entries retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CreditHistory.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<CreditHistory>> getClientCreditHistory(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable Long id) {
    return ResponseEntity.ok(clientService.getClientCreditHistory(id));
  }

  @GetMapping("/{id}/credit-history/active")
  @Operation(
      summary = "Get active credit history",
      description = "Retrieve active (non-deleted) credit history for a client")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active credit history entries retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CreditHistory.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<CreditHistory>> getActiveCreditHistory(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable Long id) {
    return ResponseEntity.ok(clientService.getActiveCreditHistory(id));
  }

  // ==================== Payment Receipts ====================

  @GetMapping("/{id}/payments")
  @Operation(
      summary = "Get client payments",
      description = "Retrieve all payment receipts for a client")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment receipts retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PaymentReceipt.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<PaymentReceipt>> getClientPayments(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable Long id) {
    return ResponseEntity.ok(clientService.getClientPaymentReceipts(id));
  }

  @PostMapping("/{id}/payments")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Process payment",
      description = "Process a payment for a client and update credit history")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment receipt created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentReceipt.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid payment parameters or amount",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Client not found", content = @Content)
      })
  public ResponseEntity<PaymentReceipt> processPayment(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable Long id,
      @Parameter(description = "Amount paid", example = "200.00", required = true) @RequestParam
          BigDecimal amountPaid,
      @Parameter(description = "Payment method (CASH, CHEQUE, BANK_TRANSFER)", required = true)
          @RequestParam
          PaymentMethod paymentMethod,
      @Parameter(description = "User ID who received payment", example = "1", required = true)
          @RequestParam
          Long userId) {

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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Debtor clients retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Client.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Client>> getDebtors() {
    return ResponseEntity.ok(clientService.getDebtors());
  }

  @GetMapping("/near-limit")
  @Operation(
      summary = "Get clients near credit limit",
      description = "Retrieve clients within threshold of their credit limit")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clients near credit limit retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Client.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Client>> getClientsNearCreditLimit(
      @Parameter(description = "Threshold amount remaining until credit limit", example = "100.0")
          @RequestParam(defaultValue = "100.0")
          BigDecimal threshold) {
    if (threshold == null) {
      threshold = new BigDecimal("100.0");
    }
    return ResponseEntity.ok(clientService.getClientsNearCreditLimit(threshold));
  }
}
