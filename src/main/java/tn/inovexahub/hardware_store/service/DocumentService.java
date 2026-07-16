package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.enums.TransactionType;
import tn.inovexahub.hardware_store.repository.DocumentLineRepository;
import tn.inovexahub.hardware_store.repository.DocumentRepository;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;

@Service
@Transactional
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final DocumentLineRepository documentLineRepository;
  private final ClientService clientService;
  private final ProductService productService;
  private final ProductConditioningRepository productConditioningRepository;

  // Constants from spec
  private static final BigDecimal TVA_RATE = new BigDecimal("0.19");
  private static final BigDecimal STAMP_DUTY = new BigDecimal("1.000");
  private static final BigDecimal DEFAULT_TRANSPORT_FEE = new BigDecimal("10.000");

  public DocumentService(
      DocumentRepository documentRepository,
      DocumentLineRepository documentLineRepository,
      ClientService clientService,
      ProductService productService,
      ProductConditioningRepository productConditioningRepository) {
    this.documentRepository = documentRepository;
    this.documentLineRepository = documentLineRepository;
    this.clientService = clientService;
    this.productService = productService;
    this.productConditioningRepository = productConditioningRepository;
  }

  // ==================== Document CRUD ====================

  public List<Document> getAllDocuments() {
    return documentRepository.findAll();
  }

  public Optional<Document> getDocumentById(Long id) {
    return documentRepository.findById(id);
  }

  public Optional<Document> getDocumentByNumber(String documentNumber) {
    return documentRepository.findByDocumentNumber(documentNumber);
  }

  /**
   * Create a new document (Quote, BL, or Invoice).
   *
   * @param document Document to create
   * @return Created document
   */
  public Document createDocument(Document document) {
    // Set default values based on document type
    if (document.getVatRate() == null) {
      document.setVatRate(TVA_RATE); // 0.19 (19%)
    }

    if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
        && document.getTransportFee() == null) {
      document.setTransportFee(DEFAULT_TRANSPORT_FEE);
    }

    if (document.getDocumentType() == DocumentType.INVOICE && document.getStampDuty() == null) {
      document.setStampDuty(STAMP_DUTY);
    }

    if (document.getDate() == null) {
      document.setDate(LocalDateTime.now());
    }

    if (document.getStatus() == null) {
      document.setStatus(DocumentStatus.DRAFT);
    }

    // Generate document number if not provided
    if (document.getDocumentNumber() == null) {
      document.setDocumentNumber(generateDocumentNumber(document.getDocumentType()));
    }

    return documentRepository.save(document);
  }

  public Document updateDocument(Long id, Document documentDetails) {
    Document document =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can be updated");
    }

    // Do not update documentNumber - it should remain unchanged
    document.setDate(documentDetails.getDate());
    document.setDocumentType(documentDetails.getDocumentType());
    document.setClient(documentDetails.getClient());
    document.setUser(documentDetails.getUser());
    document.setTransportFee(documentDetails.getTransportFee());
    document.setStampDuty(documentDetails.getStampDuty());
    document.setIsCreditSale(documentDetails.getIsCreditSale());

    // Reapply document type defaults if type changed
    if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
        && document.getTransportFee() == null) {
      document.setTransportFee(DEFAULT_TRANSPORT_FEE);
    }

    if (document.getDocumentType() == DocumentType.INVOICE && document.getStampDuty() == null) {
      document.setStampDuty(STAMP_DUTY);
    }

    // Recalculate document totals after type/fee changes
    recalculateDocumentTotals(document.getId());

    return documentRepository.save(document);
  }

  public void deleteDocument(Long id) {
    Document document =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can be deleted");
    }

    // Delete associated lines
    List<DocumentLine> lines = documentLineRepository.findByDocumentId(id);
    documentLineRepository.deleteAll(lines);

    documentRepository.delete(document);
  }

  // ==================== Document Lines ====================

  public List<DocumentLine> getDocumentLines(Long documentId) {
    return documentLineRepository.findByDocumentId(documentId);
  }

  /**
   * Add a line to a document.
   *
   * @param documentId Document ID
   * @param product Product
   * @param quantity Quantity
   * @param unitPrice Unit price (optional, auto-calculated for heavy materials or conditioning)
   * @param conditioningDescription Conditioning description (optional)
   * @param isDelivered Whether the product is delivered (for dual pricing)
   * @param conditioningId Optional ProductConditioning ID for non-proportional pricing
   * @return Created document line
   */
  public DocumentLine addDocumentLine(
      Long documentId,
      Product product,
      Double quantity,
      BigDecimal unitPrice,
      String conditioningDescription,
      Boolean isDelivered,
      Long conditioningId) {
    if (quantity == null || quantity <= 0) {
      throw new RuntimeException("Quantity must be positive");
    }

    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can have lines added");
    }

    // Apply ProductConditioning pricing if specified
    if (conditioningId != null) {
      ProductConditioning conditioning =
          productConditioningRepository
              .findById(conditioningId)
              .orElseThrow(() -> new RuntimeException("Product conditioning not found"));
      if (!conditioning.getProduct().getId().equals(product.getId())) {
        throw new RuntimeException("Conditioning does not belong to this product");
      }
      unitPrice = conditioning.getUnitPrice();
      conditioningDescription = conditioning.getDescription();
    }

    // Auto-calculate unit price for heavy materials based on delivery mode
    if (product.getIsHeavyMaterial() && isDelivered != null && unitPrice == null) {
      if (isDelivered && product.getPriceDelivered() != null) {
        unitPrice = product.getPriceDelivered();
      } else if (!isDelivered && product.getPriceOnSite() != null) {
        unitPrice = product.getPriceOnSite();
      }
    }

    if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Unit price cannot be negative");
    }

    DocumentLine line = new DocumentLine();
    line.setDocument(document);
    line.setProduct(product);
    line.setQuantity(quantity);
    line.setUnitPrice(unitPrice);
    line.setConditioningDescription(conditioningDescription);
    line.setIsDelivered(isDelivered != null ? isDelivered : false);

    // Snapshot unit cost at sale time for margin calculation
    if (product != null) {
      line.setUnitCost(product.getAveragePurchasePrice());
    }

    // Calculate line totals
    BigDecimal lineExcludingTax = unitPrice.multiply(BigDecimal.valueOf(quantity));
    line.setTotalLineExcludingTax(lineExcludingTax);

    // Calculate line TTC (with document's VAT rate)
    BigDecimal lineVat = lineExcludingTax.multiply(document.getVatRate());
    BigDecimal lineIncludingTax = lineExcludingTax.add(lineVat);
    line.setTotalLineIncludingTax(lineIncludingTax);

    // Set line number with synchronization for concurrency safety
    synchronized (this) {
      List<DocumentLine> existingLines = documentLineRepository.findByDocumentId(documentId);
      int maxLineNumber =
          existingLines.stream().mapToInt(DocumentLine::getLineNumber).max().orElse(0);
      line.setLineNumber(maxLineNumber + 1);
    }

    DocumentLine savedLine = documentLineRepository.save(line);

    // Recalculate document totals
    recalculateDocumentTotals(documentId);

    return savedLine;
  }

  public DocumentLine updateDocumentLine(Long id, DocumentLine lineDetails) {
    DocumentLine line =
        documentLineRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Document line not found"));

    if (line.getDocument().getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can have lines updated");
    }

    // Validate quantity and unit price before updating
    if (lineDetails.getQuantity() == null || lineDetails.getQuantity() <= 0) {
      throw new RuntimeException("Quantity must be positive");
    }
    if (lineDetails.getUnitPrice() == null
        || lineDetails.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Unit price must be non-negative");
    }

    line.setQuantity(lineDetails.getQuantity());
    line.setUnitPrice(lineDetails.getUnitPrice());
    line.setConditioningDescription(lineDetails.getConditioningDescription());

    // Recalculate line totals
    BigDecimal lineExcludingTax =
        lineDetails.getUnitPrice().multiply(BigDecimal.valueOf(lineDetails.getQuantity()));
    line.setTotalLineExcludingTax(lineExcludingTax);

    BigDecimal lineVat = lineExcludingTax.multiply(line.getDocument().getVatRate());
    BigDecimal lineIncludingTax = lineExcludingTax.add(lineVat);
    line.setTotalLineIncludingTax(lineIncludingTax);

    DocumentLine savedLine = documentLineRepository.save(line);

    // Recalculate document totals
    recalculateDocumentTotals(line.getDocument().getId());

    return savedLine;
  }

  public void deleteDocumentLine(Long id) {
    DocumentLine line =
        documentLineRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Document line not found"));

    if (line.getDocument().getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can have lines deleted");
    }

    Long documentId = line.getDocument().getId();
    documentLineRepository.delete(line);

    // Recalculate document totals
    recalculateDocumentTotals(documentId);
  }

  // ==================== Business Logic ====================

  /**
   * Recalculate document totals based on lines.
   *
   * @param documentId Document ID
   */
  private void recalculateDocumentTotals(Long documentId) {
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    List<DocumentLine> lines = documentLineRepository.findByDocumentId(documentId);

    // Sum line totals
    BigDecimal totalExcludingTax =
        lines.stream()
            .map(DocumentLine::getTotalLineExcludingTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalVat =
        lines.stream()
            .map(line -> line.getTotalLineIncludingTax().subtract(line.getTotalLineExcludingTax()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Add transport fee for BL and Invoice
    BigDecimal transportFee =
        document.getTransportFee() != null ? document.getTransportFee() : BigDecimal.ZERO;
    if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
        || document.getDocumentType() == DocumentType.INVOICE) {
      totalExcludingTax = totalExcludingTax.add(transportFee);
    }

    // Calculate total TTC
    BigDecimal totalIncludingTax = totalExcludingTax.add(totalVat);

    // Add stamp duty for Invoice
    if (document.getDocumentType() == DocumentType.INVOICE) {
      BigDecimal stampDuty = document.getStampDuty() != null ? document.getStampDuty() : STAMP_DUTY;
      totalIncludingTax = totalIncludingTax.add(stampDuty);
    }

    document.setTotalExcludingTax(totalExcludingTax);
    document.setTotalVat(totalVat);
    document.setTotalIncludingTax(totalIncludingTax);

    documentRepository.save(document);
  }

  /**
   * Validate a document (change status from DRAFT to VALIDATED). For BL and Invoice, this will
   * deduct stock. For credit sales, this will add credit history entry.
   *
   * @param documentId Document ID
   * @return Validated document
   */
  public Document validateDocument(Long documentId) {
    return validateDocument(documentId, false);
  }

  /**
   * Validate a document (change status from DRAFT to VALIDATED). For BL and Invoice, this will
   * deduct stock. For credit sales, this will add credit history entry.
   *
   * @param documentId Document ID
   * @param skipStockDeduction If true, skip stock deduction (used when converting BL to Invoice)
   * @return Validated document
   */
  public Document validateDocument(Long documentId, boolean skipStockDeduction) {
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can be validated");
    }

    // Check credit limit for credit sales (unless skipped for BL->Invoice conversion)
    if (!skipStockDeduction && document.getIsCreditSale() && document.getClient() != null) {
      clientService.validateCreditLimit(
          document.getClient().getId(), document.getTotalIncludingTax());
    }

    // Deduct stock for BL and Invoice (unless skipped)
    if (!skipStockDeduction
        && (document.getDocumentType() == DocumentType.DELIVERY_NOTE
            || document.getDocumentType() == DocumentType.INVOICE)) {
      deductStock(documentId);
    }

    // Add credit history entry for credit sales (unless skipped)
    if (!skipStockDeduction && document.getIsCreditSale() && document.getClient() != null) {
      clientService.addCreditHistoryEntry(
          document.getClient(), document, document.getTotalIncludingTax(), TransactionType.SALE);
    }

    document.setStatus(DocumentStatus.VALIDATED);
    return documentRepository.save(document);
  }

  /**
   * Cancel a document.
   *
   * @param documentId Document ID
   * @return Cancelled document
   */
  public Document cancelDocument(Long documentId) {
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() == DocumentStatus.CANCELLED) {
      throw new RuntimeException("Document is already cancelled");
    }

    // Restore stock if document was validated
    // Note: Invoices created from delivery note conversion should not restore stock
    // since stock was already deducted when the delivery note was validated
    if (document.getStatus() == DocumentStatus.VALIDATED) {
      // Skip stock restoration for invoices converted from delivery notes
      if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
          || (document.getDocumentType() == DocumentType.INVOICE
              && document.getSourceDeliveryNoteId() == null)) {
        restoreStock(documentId);
      }

      // Skip credit history adjustment for invoices converted from delivery notes
      // since credit history was already added when the delivery note was validated
      if (document.getIsCreditSale()
          && document.getClient() != null
          && document.getSourceDeliveryNoteId() == null) {
        clientService.addCreditHistoryEntry(
            document.getClient(),
            document,
            document.getTotalIncludingTax().negate(),
            TransactionType.ADJUSTMENT);
      }
    }

    document.setStatus(DocumentStatus.CANCELLED);
    return documentRepository.save(document);
  }

  /**
   * Deduct stock for document lines.
   *
   * @param documentId Document ID
   */
  private void deductStock(Long documentId) {
    List<DocumentLine> lines = documentLineRepository.findByDocumentId(documentId);
    for (DocumentLine line : lines) {
      if (line.getProduct() != null && line.getQuantity() != null) {
        productService.updateStockQuantity(line.getProduct().getId(), -line.getQuantity());
        line.setIsDelivered(true);
        documentLineRepository.save(line);
      }
    }
  }

  /**
   * Restore stock for document lines.
   *
   * @param documentId Document ID
   */
  private void restoreStock(Long documentId) {
    List<DocumentLine> lines = documentLineRepository.findByDocumentId(documentId);
    for (DocumentLine line : lines) {
      if (line.getProduct() != null && line.getQuantity() != null) {
        productService.updateStockQuantity(line.getProduct().getId(), line.getQuantity());
        line.setIsDelivered(false);
        documentLineRepository.save(line);
      }
    }
  }

  /**
   * Convert a Quote to a Delivery Note.
   *
   * @param quoteId Quote ID
   * @return Created Delivery Note
   */
  public Document convertQuoteToDeliveryNote(Long quoteId) {
    Document quote =
        documentRepository
            .findById(quoteId)
            .orElseThrow(() -> new RuntimeException("Quote not found"));

    if (quote.getDocumentType() != DocumentType.QUOTE) {
      throw new RuntimeException("Only QUOTE can be converted to DELIVERY_NOTE");
    }

    // Create new BL
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setClient(quote.getClient());
    bl.setUser(quote.getUser());
    bl.setIsCreditSale(quote.getIsCreditSale());
    bl.setTransportFee(DEFAULT_TRANSPORT_FEE);

    Document savedBl = createDocument(bl);

    // Copy lines
    List<DocumentLine> quoteLines = documentLineRepository.findByDocumentId(quoteId);
    for (DocumentLine quoteLine : quoteLines) {
      addDocumentLine(
          savedBl.getId(),
          quoteLine.getProduct(),
          quoteLine.getQuantity(),
          quoteLine.getUnitPrice(),
          quoteLine.getConditioningDescription(),
          quoteLine.getIsDelivered(),
          null);
    }

    return savedBl;
  }

  /**
   * Convert a Delivery Note to an Invoice.
   *
   * @param deliveryNoteId Delivery Note ID
   * @return Created Invoice
   */
  public Document convertDeliveryNoteToInvoice(Long deliveryNoteId) {
    Document bl =
        documentRepository
            .findById(deliveryNoteId)
            .orElseThrow(() -> new RuntimeException("Delivery Note not found"));

    if (bl.getDocumentType() != DocumentType.DELIVERY_NOTE) {
      throw new RuntimeException("Only DELIVERY_NOTE can be converted to INVOICE");
    }

    if (bl.getStatus() != DocumentStatus.VALIDATED) {
      throw new RuntimeException("Only VALIDATED delivery notes can be converted to invoices");
    }

    if (bl.getConvertedToInvoiceId() != null) {
      throw new RuntimeException("Delivery note has already been converted to an invoice");
    }

    // Create new Invoice
    Document invoice = new Document();
    invoice.setDocumentType(DocumentType.INVOICE);
    invoice.setClient(bl.getClient());
    invoice.setUser(bl.getUser());
    invoice.setIsCreditSale(bl.getIsCreditSale());
    invoice.setTransportFee(bl.getTransportFee());
    invoice.setStampDuty(STAMP_DUTY);

    Document savedInvoice = createDocument(invoice);

    // Copy lines
    List<DocumentLine> blLines = documentLineRepository.findByDocumentId(deliveryNoteId);
    for (DocumentLine blLine : blLines) {
      addDocumentLine(
          savedInvoice.getId(),
          blLine.getProduct(),
          blLine.getQuantity(),
          blLine.getUnitPrice(),
          blLine.getConditioningDescription(),
          blLine.getIsDelivered(),
          null);
    }

    // Validate invoice (skip stock deduction since BL already applied it)
    validateDocument(savedInvoice.getId(), true);

    // Mark delivery note as converted
    bl.setConvertedToInvoiceId(savedInvoice.getId());
    documentRepository.save(bl);

    // Mark invoice as converted from delivery note
    savedInvoice.setSourceDeliveryNoteId(deliveryNoteId);
    documentRepository.save(savedInvoice);

    return savedInvoice;
  }

  // ==================== Helper Methods ====================

  private String generateDocumentNumber(DocumentType documentType) {
    String prefix;
    Long sequenceValue;

    switch (documentType) {
      case QUOTE:
        prefix = "DEV";
        sequenceValue = documentRepository.getNextQuoteSequence();
        break;
      case DELIVERY_NOTE:
        prefix = "BL";
        sequenceValue = documentRepository.getNextDeliveryNoteSequence();
        break;
      case INVOICE:
        prefix = "FAC";
        sequenceValue = documentRepository.getNextInvoiceSequence();
        break;
      default:
        prefix = "DOC";
        sequenceValue = documentRepository.getNextInvoiceSequence();
    }

    return String.format("%s-%06d", prefix, sequenceValue);
  }

  // ==================== Reporting ====================

  public List<Document> getDocumentsByClient(Long clientId) {
    return documentRepository.findByClientId(clientId);
  }

  public List<Document> getDocumentsByUser(Long userId) {
    return documentRepository.findByUserId(userId);
  }

  public List<Document> getDocumentsByType(DocumentType documentType) {
    return documentRepository.findByDocumentType(documentType);
  }

  public List<Document> getDocumentsByStatus(DocumentStatus status) {
    return documentRepository.findByStatus(status);
  }

  public List<Document> getCreditSalesByClient(Long clientId) {
    return documentRepository.findCreditSalesByClient(clientId);
  }
}
