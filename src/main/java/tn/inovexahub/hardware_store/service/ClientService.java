package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.CreditHistory;
import tn.inovexahub.hardware_store.entity.Document;
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

@Service
@Transactional
public class ClientService {

  private final ClientRepository clientRepository;
  private final CreditHistoryRepository creditHistoryRepository;
  private final PaymentReceiptRepository paymentReceiptRepository;

  public ClientService(
      ClientRepository clientRepository,
      CreditHistoryRepository creditHistoryRepository,
      PaymentReceiptRepository paymentReceiptRepository) {
    this.clientRepository = clientRepository;
    this.creditHistoryRepository = creditHistoryRepository;
    this.paymentReceiptRepository = paymentReceiptRepository;
  }

  // ==================== Client CRUD ====================

  public List<Client> getAllClients() {
    return clientRepository.findByDeletedFalseOrderByCurrentDebtDesc();
  }

  public Optional<Client> getClientById(Long id) {
    return clientRepository.findByIdAndDeletedFalse(id);
  }

  public Optional<Client> getClientByTaxId(String taxIdentificationNumber) {
    return clientRepository
        .findByTaxIdentificationNumber(taxIdentificationNumber)
        .filter(c -> !c.getDeleted());
  }

  public Client createClient(Client client) {
    client.setCurrentDebt(BigDecimal.ZERO);
    client.setDeleted(false);
    return clientRepository.save(client);
  }

  public Client updateClient(Long id, Client clientDetails) {
    Client client =
        clientRepository
            .findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ClientNotFoundException(id));

    client.setName(clientDetails.getName());
    client.setPhone(clientDetails.getPhone());
    client.setEmail(clientDetails.getEmail());
    client.setAddress(clientDetails.getAddress());
    client.setTaxIdentificationNumber(clientDetails.getTaxIdentificationNumber());
    client.setCreditLimit(clientDetails.getCreditLimit());

    return clientRepository.save(client);
  }

  public void deleteClient(Long id) {
    Client client =
        clientRepository.findById(id).orElseThrow(() -> new ClientNotFoundException(id));
    client.setDeleted(true);
    clientRepository.save(client);
  }

  // ==================== Credit Limit Validation ====================

  /**
   * Check if a credit sale would exceed the client's credit limit.
   *
   * @param clientId Client ID
   * @param saleAmount Amount of the new sale (TTC)
   * @return true if the sale would exceed the credit limit, false otherwise
   */
  public boolean wouldExceedCreditLimit(Long clientId, BigDecimal saleAmount) {
    if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Sale amount must be positive");
    }

    Client client =
        clientRepository
            .findByIdAndDeletedFalse(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

    BigDecimal newDebt = client.getCurrentDebt().add(saleAmount);
    return newDebt.compareTo(client.getCreditLimit()) > 0;
  }

  /**
   * Validate credit limit for a sale. Throws exception if limit would be exceeded.
   *
   * @param clientId Client ID
   * @param saleAmount Amount of the new sale (TTC)
   * @throws CreditLimitExceededException if credit limit would be exceeded
   */
  public void validateCreditLimit(Long clientId, BigDecimal saleAmount) {
    if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Sale amount must be positive");
    }
    if (wouldExceedCreditLimit(clientId, saleAmount)) {
      Client client =
          clientRepository
              .findById(clientId)
              .orElseThrow(() -> new ClientNotFoundException(clientId));
      BigDecimal newDebt = client.getCurrentDebt().add(saleAmount);
      BigDecimal excess = newDebt.subtract(client.getCreditLimit());
      throw new CreditLimitExceededException(
          "Vente refusée : Dépassement de plafond de crédit de " + excess + " DT");
    }
  }

  // ==================== Credit History ====================

  public List<CreditHistory> getClientCreditHistory(Long clientId) {
    return creditHistoryRepository.findByClientIdOrderByEntryDateDesc(clientId);
  }

  public List<CreditHistory> getActiveCreditHistory(Long clientId) {
    return creditHistoryRepository.findActiveCreditHistoryByClient(clientId);
  }

  /**
   * Add a credit history entry for a sale (debt increase).
   *
   * @param client Client
   * @param document Document (can be null for direct adjustments)
   * @param amount Amount (positive for debt increase)
   * @param transactionType Transaction type
   * @return Created CreditHistory entry
   */
  public CreditHistory addCreditHistoryEntry(
      Client client, Document document, BigDecimal amount, TransactionType transactionType) {
    CreditHistory creditHistory = new CreditHistory();
    creditHistory.setClient(client);
    creditHistory.setDocument(document);
    creditHistory.setTransactionType(transactionType);
    creditHistory.setAmount(amount);

    // Calculate running balance
    BigDecimal currentDebt = client.getCurrentDebt();
    BigDecimal newDebt = currentDebt.add(amount);
    creditHistory.setRunningBalance(newDebt);

    // Update client's current debt
    client.setCurrentDebt(newDebt);
    clientRepository.save(client);

    CreditHistory saved = creditHistoryRepository.save(creditHistory);
    return saved;
  }

  /**
   * Add a credit history entry for a payment (debt decrease).
   *
   * @param client Client
   * @param paymentReceipt PaymentReceipt
   * @param amount Amount (negative for debt decrease)
   * @return Created CreditHistory entry
   */
  public CreditHistory addPaymentCreditHistoryEntry(
      Client client, PaymentReceipt paymentReceipt, BigDecimal amount) {
    CreditHistory creditHistory = new CreditHistory();
    creditHistory.setClient(client);
    creditHistory.setPaymentReceipt(paymentReceipt);
    creditHistory.setTransactionType(TransactionType.PAYMENT);
    creditHistory.setAmount(amount);

    // Calculate running balance
    BigDecimal currentDebt = client.getCurrentDebt();
    BigDecimal newDebt = currentDebt.add(amount);
    creditHistory.setRunningBalance(newDebt);

    // Update client's current debt
    client.setCurrentDebt(newDebt);
    clientRepository.save(client);

    CreditHistory saved = creditHistoryRepository.save(creditHistory);
    return saved;
  }

  // ==================== Payment Receipts ====================

  public List<PaymentReceipt> getClientPaymentReceipts(Long clientId) {
    return paymentReceiptRepository.findByClientId(clientId);
  }

  /**
   * Process a payment receipt and update credit history.
   *
   * @param client Client
   * @param amountPaid Amount paid
   * @param paymentMethod Payment method
   * @param user User who registered the payment
   * @return Created PaymentReceipt
   */
  public PaymentReceipt processPayment(
      Client client, BigDecimal amountPaid, PaymentMethod paymentMethod, User user) {
    // Generate receipt number
    String receiptNumber = generateReceiptNumber();

    // Create payment receipt
    PaymentReceipt paymentReceipt = new PaymentReceipt();
    paymentReceipt.setReceiptNumber(receiptNumber);
    paymentReceipt.setClient(client);
    paymentReceipt.setAmountPaid(amountPaid);
    paymentReceipt.setPaymentDate(LocalDateTime.now());
    paymentReceipt.setPaymentMethod(paymentMethod);
    paymentReceipt.setUser(user);

    // Snapshot debt before payment
    BigDecimal previousDebt = client.getCurrentDebt();
    paymentReceipt.setPreviousDebt(previousDebt);

    // Calculate new debt
    BigDecimal newDebt = previousDebt.subtract(amountPaid);
    if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidPaymentException("Payment amount exceeds current debt");
    }
    paymentReceipt.setNewDebt(newDebt);

    // Save payment receipt
    PaymentReceipt savedReceipt = paymentReceiptRepository.save(paymentReceipt);

    // Create信用 history entry (negative amount for payment)
    CreditHistory creditHistory =
        addPaymentCreditHistoryEntry(client, savedReceipt, amountPaid.negate());

    // Link credit history to payment receipt
    savedReceipt.setCreditHistory(creditHistory);
    paymentReceiptRepository.save(savedReceipt);

    return savedReceipt;
  }

  private synchronized String generateReceiptNumber() {
    // Simple receipt number generation - can be enhanced
    long count = paymentReceiptRepository.count() + 1;
    return "REC-" + String.format("%06d", count);
  }

  // ==================== Reporting ====================

  public List<Client> getDebtors() {
    return clientRepository.findDebtorsOrderByDebtDesc();
  }

  public List<Client> getClientsNearCreditLimit(BigDecimal threshold) {
    // Find clients whose debt is within threshold of their credit limit
    return clientRepository.findByDeletedFalseOrderByCurrentDebtDesc().stream()
        .filter(
            client ->
                client.getCreditLimit().subtract(client.getCurrentDebt()).compareTo(threshold) <= 0)
        .toList();
  }
}
