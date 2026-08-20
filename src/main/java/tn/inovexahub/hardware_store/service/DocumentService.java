package tn.inovexahub.hardware_store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.enums.TransactionType;
import tn.inovexahub.hardware_store.repository.DocumentLineRepository;
import tn.inovexahub.hardware_store.repository.DocumentRepository;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;

@Service
@Transactional
public class DocumentService {

  private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

  private final DocumentRepository documentRepository;
  private final DocumentLineRepository documentLineRepository;
  private final ClientService clientService;
  private final ProductConditioningRepository productConditioningRepository;
  private final ProductBatchService productBatchService;

  // Constants from spec
  private static final BigDecimal STAMP_DUTY = new BigDecimal("1.000");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DocumentService(
      DocumentRepository documentRepository,
      DocumentLineRepository documentLineRepository,
      ClientService clientService,
      ProductConditioningRepository productConditioningRepository,
      ProductBatchService productBatchService) {
    this.documentRepository = documentRepository;
    this.documentLineRepository = documentLineRepository;
    this.clientService = clientService;
    this.productConditioningRepository = productConditioningRepository;
    this.productBatchService = productBatchService;
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
      document.setVatRate(VatRates.DEFAULT_RATE); // 19% (stored as a percentage)
    }

    if (Boolean.TRUE.equals(document.getIsCreditSale()) && document.getClient() == null) {
      throw new IllegalArgumentException("Credit sales require a client");
    }

    if (Boolean.TRUE.equals(document.getIsDelivery()) && document.getTransportFee() == null) {
      throw new IllegalArgumentException(
          "Delivery documents require a transport fee to be specified");
    }

    if (!Boolean.TRUE.equals(document.getIsDelivery())) {
      document.setTransportFee(null);
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
    if (documentDetails.getIsDelivery() != null) {
      document.setIsDelivery(documentDetails.getIsDelivery());
    }
    document.setTransportFee(documentDetails.getTransportFee());
    document.setStampDuty(documentDetails.getStampDuty());
    document.setIsCreditSale(documentDetails.getIsCreditSale());
    boolean vatRateChanged =
        documentDetails.getVatRate() != null
            && documentDetails.getVatRate().compareTo(document.getVatRate()) != 0;
    if (documentDetails.getVatRate() != null) {
      document.setVatRate(documentDetails.getVatRate());
    }

    if (Boolean.TRUE.equals(document.getIsCreditSale()) && document.getClient() == null) {
      throw new IllegalArgumentException("Credit sales require a client");
    }
    if (Boolean.TRUE.equals(document.getIsDelivery()) && document.getTransportFee() == null) {
      throw new IllegalArgumentException(
          "Delivery documents require a transport fee to be specified");
    }

    if (!Boolean.TRUE.equals(document.getIsDelivery())) {
      document.setTransportFee(null);
    }

    if (document.getDocumentType() == DocumentType.INVOICE && document.getStampDuty() == null) {
      document.setStampDuty(STAMP_DUTY);
    }

    // Recompute line VAT when the rate changed, then document totals from the updated lines
    if (vatRateChanged) {
      recomputeLineVat(document);
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
   * @param unitPrice Unit price (optional, uses batch price if not provided)
   * @param conditioningDescription Conditioning description (optional)
   * @param isDelivered Whether the product is delivered
   * @param conditioningId Optional ProductConditioning ID for non-proportional pricing
   * @return Created document line
   */
  public DocumentLine addDocumentLine(
      Long documentId,
      Product product,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String conditioningDescription,
      Boolean isDelivered,
      Long conditioningId) {
    return addDocumentLine(
        documentId,
        product,
        null,
        quantity,
        unitPrice,
        conditioningDescription,
        isDelivered,
        conditioningId);
  }

  /**
   * Add a line to a document (variant-aware).
   *
   * <p>Stock is NOT deducted here: it is only reserved (FIFO) when the document is validated. This
   * method snapshots unit price and cost from the available batches without mutating them.
   *
   * @param documentId Document ID
   * @param product Product
   * @param variant Optional product variant
   * @param quantity Quantity
   * @param unitPrice Unit price (optional, uses batch price if not provided)
   * @param conditioningDescription Conditioning description (optional)
   * @param isDelivered Whether the product is delivered
   * @param conditioningId Optional ProductConditioning ID for non-proportional pricing
   * @return Created document line
   */
  public DocumentLine addDocumentLine(
      Long documentId,
      Product product,
      ProductVariant variant,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String conditioningDescription,
      Boolean isDelivered,
      Long conditioningId) {
    if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Quantity must be positive");
    }
    if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Unit price cannot be negative");
    }

    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

    if (document.getStatus() != DocumentStatus.DRAFT) {
      throw new RuntimeException("Only DRAFT documents can have lines added");
    }

    if (variant != null
        && (variant.getProduct() == null
            || !variant.getProduct().getId().equals(product.getId()))) {
      throw new RuntimeException("Variant does not belong to this product");
    }

    BigDecimal multiplier = BigDecimal.ONE;
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
      if (conditioning.getQuantityPerUnit() != null) {
        multiplier = conditioning.getQuantityPerUnit();
      }
    }

    // Snapshot unit price and cost from available FIFO batches WITHOUT mutating stock.
    // Stock is deducted at validation, where the exact batches are allocated and stored.
    BigDecimal effectiveQuantity = quantity.multiply(multiplier);
    List<ProductBatchService.BatchAllocation> batchAllocations =
        variant != null
            ? productBatchService.estimateAllocationFromVariant(variant.getId(), effectiveQuantity)
            : productBatchService.estimateAllocation(product.getId(), effectiveQuantity);

    if (unitPrice == null) {
      BigDecimal weightedAveragePrice = weightedAveragePrice(batchAllocations);
      if (weightedAveragePrice != null) {
        unitPrice = weightedAveragePrice;
      } else if (product.getUnitPrice() != null) {
        unitPrice = product.getUnitPrice();
      } else {
        throw new RuntimeException("Cannot calculate unit price from batches");
      }
    }

    // Calculate cost per sale unit from allocated batches for margin
    BigDecimal baseUnitCost = weightedAverageCost(batchAllocations);
    if (baseUnitCost == null) {
      baseUnitCost =
          product.getAveragePurchasePrice() != null
              ? product.getAveragePurchasePrice()
              : BigDecimal.ZERO;
    }
    BigDecimal unitCost = baseUnitCost.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);

    DocumentLine line = new DocumentLine();
    line.setDocument(document);
    line.setProduct(product);
    line.setVariant(variant);
    line.setQuantity(quantity);
    line.setUnitPrice(unitPrice);
    line.setUnitCost(unitCost);
    line.setConditioningDescription(conditioningDescription);
    line.setConditioningQuantityPerUnit(multiplier);
    line.setIsDelivered(isDelivered != null ? isDelivered : false);

    // Calculate line totals
    BigDecimal lineExcludingTax = unitPrice.multiply(quantity).setScale(3, RoundingMode.HALF_UP);
    line.setTotalLineExcludingTax(lineExcludingTax);

    // Calculate line TTC (with document's VAT rate, stored as a percentage)
    BigDecimal vatRate = VatRates.normalize(document.getVatRate());
    BigDecimal lineVat =
        lineExcludingTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    BigDecimal lineIncludingTax = lineExcludingTax.add(lineVat).setScale(3, RoundingMode.HALF_UP);
    line.setTotalLineIncludingTax(lineIncludingTax);

    // Set line number with synchronization for concurrency safety
    line.setLineNumber(nextLineNumber(documentId));

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
    if (lineDetails.getQuantity() == null
        || lineDetails.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Quantity must be positive");
    }
    if (lineDetails.getUnitPrice() == null
        || lineDetails.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Unit price must be non-negative");
    }

    line.setQuantity(lineDetails.getQuantity());
    line.setUnitPrice(lineDetails.getUnitPrice());
    line.setConditioningDescription(lineDetails.getConditioningDescription());
    boolean conditioningQuantityChanged =
        lineDetails.getConditioningQuantityPerUnit() != null
            && !lineDetails
                .getConditioningQuantityPerUnit()
                .equals(line.getConditioningQuantityPerUnit());
    boolean variantChanged =
        lineDetails.getVariant() != null
            && !lineDetails
                .getVariant()
                .getId()
                .equals(line.getVariant() != null ? line.getVariant().getId() : null);
    if (lineDetails.getConditioningQuantityPerUnit() != null) {
      line.setConditioningQuantityPerUnit(lineDetails.getConditioningQuantityPerUnit());
    }
    if (lineDetails.getVariant() != null) {
      if (line.getProduct() != null
          && lineDetails.getVariant().getProduct() != null
          && !lineDetails.getVariant().getProduct().getId().equals(line.getProduct().getId())) {
        throw new RuntimeException("Variant does not belong to this product");
      }
      line.setVariant(lineDetails.getVariant());
    }

    // Recompute unit cost from the batch estimate when the costing inputs changed
    if (conditioningQuantityChanged || variantChanged) {
      recomputeLineUnitCost(line);
    }

    // Recalculate line totals
    BigDecimal lineExcludingTax =
        lineDetails
            .getUnitPrice()
            .multiply(lineDetails.getQuantity())
            .setScale(3, RoundingMode.HALF_UP);
    line.setTotalLineExcludingTax(lineExcludingTax);

    BigDecimal vatRate = VatRates.normalize(line.getDocument().getVatRate());
    BigDecimal lineVat =
        lineExcludingTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    BigDecimal lineIncludingTax = lineExcludingTax.add(lineVat).setScale(3, RoundingMode.HALF_UP);
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

    // Add transport fee only for deliveries
    if (Boolean.TRUE.equals(document.getIsDelivery()) && document.getTransportFee() != null) {
      totalExcludingTax = totalExcludingTax.add(document.getTransportFee());
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
   * deduct stock (FIFO batches are allocated and recorded on each line). For credit sales, this
   * will add credit history entry.
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

    if (Boolean.TRUE.equals(document.getIsCreditSale()) && document.getClient() == null) {
      throw new RuntimeException("Credit sales require a client");
    }

    // Check credit limit for credit sales (unless skipped for BL->Invoice conversion)
    if (!skipStockDeduction
        && Boolean.TRUE.equals(document.getIsCreditSale())
        && document.getClient() != null) {
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
    if (!skipStockDeduction
        && Boolean.TRUE.equals(document.getIsCreditSale())
        && document.getClient() != null) {
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

    if (document.getStatus() == DocumentStatus.VALIDATED) {
      // A delivery note that was converted to an invoice cannot be cancelled on its own.
      if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
          && document.getConvertedToInvoiceId() != null) {
        throw new RuntimeException(
            "Delivery note has been converted to an invoice and cannot be cancelled");
      }

      // Restore stock if document was validated.
      // Invoices created from delivery note conversion should not restore stock
      // since stock was already deducted when the delivery note was validated.
      if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
          || (document.getDocumentType() == DocumentType.INVOICE
              && document.getSourceDeliveryNoteId() == null)) {
        restoreStock(documentId);
      }

      // Skip credit history adjustment for invoices converted from delivery notes
      // since credit history was already added when the delivery note was validated
      if (Boolean.TRUE.equals(document.getIsCreditSale())
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
   * Deduct stock for document lines. Allocates FIFO batches and records the exact allocation per
   * line so it can be restored precisely on cancel.
   *
   * @param documentId Document ID
   */
  private void deductStock(Long documentId) {
    List<DocumentLine> lines = documentLineRepository.findByDocumentId(documentId);
    for (DocumentLine line : lines) {
      if (line.getProduct() == null || line.getQuantity() == null) {
        continue;
      }
      BigDecimal multiplier =
          line.getConditioningQuantityPerUnit() != null
              ? line.getConditioningQuantityPerUnit()
              : BigDecimal.ONE;
      BigDecimal effectiveQuantity = line.getQuantity().multiply(multiplier);

      List<ProductBatchService.BatchAllocation> allocations =
          line.getVariant() != null
              ? productBatchService.allocateStockFromVariant(
                  line.getVariant().getId(), effectiveQuantity)
              : productBatchService.allocateStock(line.getProduct().getId(), effectiveQuantity);

      line.setBatchAllocations(serializeBatchAllocations(allocations));
      line.setIsDelivered(true);
      documentLineRepository.save(line);
    }
  }

  /**
   * Restore stock for document lines using the recorded batch allocations.
   *
   * @param documentId Document ID
   */
  private void restoreStock(Long documentId) {
    List<DocumentLine> lines = documentLineRepository.findByDocumentId(documentId);
    Map<Long, BigDecimal> allocations = new LinkedHashMap<>();
    for (DocumentLine line : lines) {
      if (line.getProduct() == null || line.getQuantity() == null) {
        continue;
      }
      if (line.getBatchAllocations() == null || line.getBatchAllocations().isBlank()) {
        throw new IllegalArgumentException(
            "Cannot cancel document "
                + documentId
                + ": line "
                + line.getId()
                + " is missing batch allocation data, so stock cannot be restored");
      }
      for (Map.Entry<Long, BigDecimal> entry :
          deserializeBatchAllocations(line.getBatchAllocations()).entrySet()) {
        allocations.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
      }
      line.setBatchAllocations(null);
      line.setIsDelivered(false);
      documentLineRepository.save(line);
    }
    productBatchService.restoreBatches(allocations);
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

    if (quote.getStatus() == DocumentStatus.CANCELLED) {
      throw new RuntimeException("Cancelled quotes cannot be converted");
    }

    // Create new BL
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setClient(quote.getClient());
    bl.setUser(quote.getUser());
    bl.setIsCreditSale(quote.getIsCreditSale());
    bl.setIsDelivery(quote.getIsDelivery());
    bl.setTransportFee(quote.getTransportFee());
    bl.setVatRate(quote.getVatRate());

    Document savedBl = createDocument(bl);

    // Copy lines without touching stock (no allocation at conversion)
    List<DocumentLine> quoteLines = documentLineRepository.findByDocumentId(quoteId);
    for (DocumentLine quoteLine : quoteLines) {
      copyLineToDocument(quoteLine, savedBl, false);
    }

    // Recalculate totals using the copied lines
    recalculateDocumentTotals(savedBl.getId());

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
    invoice.setIsDelivery(bl.getIsDelivery());
    invoice.setTransportFee(bl.getTransportFee());
    invoice.setStampDuty(STAMP_DUTY);
    invoice.setVatRate(bl.getVatRate());

    Document savedInvoice = createDocument(invoice);

    // Copy lines without re-allocating stock (BL validation already deducted it)
    List<DocumentLine> blLines = documentLineRepository.findByDocumentId(deliveryNoteId);
    for (DocumentLine blLine : blLines) {
      copyLineToDocument(blLine, savedInvoice, true);
    }

    // Recalculate totals using the copied lines
    recalculateDocumentTotals(savedInvoice.getId());

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

  /**
   * Copy a line from one document to another without allocating stock. Line totals are recomputed
   * using the target document's VAT rate.
   */
  private void copyLineToDocument(DocumentLine source, Document target, boolean isDelivered) {
    if (source.getProduct() == null
        || source.getQuantity() == null
        || source.getUnitPrice() == null) {
      log.warn(
          "Skipping document line {}: missing product, quantity or unit price", source.getId());
      return;
    }

    DocumentLine line = new DocumentLine();
    line.setDocument(target);
    line.setProduct(source.getProduct());
    line.setVariant(source.getVariant());
    line.setQuantity(source.getQuantity());
    line.setUnitPrice(source.getUnitPrice());
    line.setUnitCost(source.getUnitCost());
    line.setConditioningDescription(source.getConditioningDescription());
    line.setConditioningQuantityPerUnit(source.getConditioningQuantityPerUnit());
    line.setIsDelivered(isDelivered);

    BigDecimal vatRate = VatRates.normalize(target.getVatRate());
    BigDecimal lineExcludingTax =
        line.getUnitPrice().multiply(line.getQuantity()).setScale(3, RoundingMode.HALF_UP);
    BigDecimal lineVat =
        lineExcludingTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    line.setTotalLineExcludingTax(lineExcludingTax);
    line.setTotalLineIncludingTax(lineExcludingTax.add(lineVat));

    line.setLineNumber(nextLineNumber(target.getId()));

    documentLineRepository.save(line);
  }

  /**
   * Assign the next line number for a document. The parent document row is locked for update so
   * concurrent line insertions on the same document are serialized and cannot produce duplicates.
   */
  private int nextLineNumber(Long documentId) {
    try {
      documentRepository
          .findByIdForUpdate(documentId)
          .orElseThrow(() -> new RuntimeException("Document not found"));
    } catch (PessimisticLockingFailureException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Document is currently locked by another operation. Please retry.");
    }
    List<DocumentLine> existingLines = documentLineRepository.findByDocumentId(documentId);
    return existingLines.stream()
            .filter(line -> line.getLineNumber() != null)
            .mapToInt(DocumentLine::getLineNumber)
            .max()
            .orElse(0)
        + 1;
  }

  /** Recompute every line's totalLineIncludingTax using the document's current VAT rate. */
  private void recomputeLineVat(Document document) {
    BigDecimal vatRate = VatRates.normalize(document.getVatRate());
    for (DocumentLine line : documentLineRepository.findByDocumentId(document.getId())) {
      BigDecimal lineVat =
          line.getTotalLineExcludingTax()
              .multiply(vatRate)
              .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
      line.setTotalLineIncludingTax(
          line.getTotalLineExcludingTax().add(lineVat).setScale(3, RoundingMode.HALF_UP));
      documentLineRepository.save(line);
    }
  }

  /**
   * Recompute a line's unit cost from the FIFO batch estimate using the same base-unit-cost
   * multiplied-by-conditioning-multiplier calculation as {@link #addDocumentLine}.
   */
  private void recomputeLineUnitCost(DocumentLine line) {
    if (line.getProduct() == null || line.getQuantity() == null) {
      return;
    }
    BigDecimal multiplier =
        line.getConditioningQuantityPerUnit() != null
            ? line.getConditioningQuantityPerUnit()
            : BigDecimal.ONE;
    BigDecimal effectiveQuantity = line.getQuantity().multiply(multiplier);

    List<ProductBatchService.BatchAllocation> batchAllocations =
        line.getVariant() != null
            ? productBatchService.estimateAllocationFromVariant(
                line.getVariant().getId(), effectiveQuantity)
            : productBatchService.estimateAllocation(line.getProduct().getId(), effectiveQuantity);

    BigDecimal baseUnitCost = weightedAverageCost(batchAllocations);
    if (baseUnitCost == null) {
      if (line.getProduct().getAveragePurchasePrice() != null) {
        baseUnitCost = line.getProduct().getAveragePurchasePrice();
      } else {
        baseUnitCost = BigDecimal.ZERO;
      }
    }
    line.setUnitCost(baseUnitCost.multiply(multiplier).setScale(3, RoundingMode.HALF_UP));
  }

  private BigDecimal weightedAveragePrice(List<ProductBatchService.BatchAllocation> allocations) {
    BigDecimal totalPrice = BigDecimal.ZERO;
    BigDecimal totalQty = BigDecimal.ZERO;
    for (ProductBatchService.BatchAllocation allocation : allocations) {
      if (allocation.getUnitPrice() != null) {
        totalPrice = totalPrice.add(allocation.getUnitPrice().multiply(allocation.getQuantity()));
        totalQty = totalQty.add(allocation.getQuantity());
      }
    }
    if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return totalPrice.divide(totalQty, 3, RoundingMode.HALF_UP);
  }

  private BigDecimal weightedAverageCost(List<ProductBatchService.BatchAllocation> allocations) {
    BigDecimal totalCost = BigDecimal.ZERO;
    BigDecimal totalQty = BigDecimal.ZERO;
    for (ProductBatchService.BatchAllocation allocation : allocations) {
      if (allocation.getUnitCost() != null) {
        totalCost = totalCost.add(allocation.getUnitCost().multiply(allocation.getQuantity()));
        totalQty = totalQty.add(allocation.getQuantity());
      }
    }
    if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return totalCost.divide(totalQty, 3, RoundingMode.HALF_UP);
  }

  private String serializeBatchAllocations(List<ProductBatchService.BatchAllocation> allocations) {
    Map<Long, BigDecimal> map = new LinkedHashMap<>();
    for (ProductBatchService.BatchAllocation allocation : allocations) {
      map.put(allocation.getBatchId(), allocation.getQuantity());
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize batch allocations", e);
    }
  }

  private Map<Long, BigDecimal> deserializeBatchAllocations(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<Map<Long, BigDecimal>>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize batch allocations", e);
    }
  }

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
