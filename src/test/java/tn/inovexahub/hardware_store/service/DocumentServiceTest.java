package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Client;
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

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

  @Mock private DocumentRepository documentRepository;
  @Mock private DocumentLineRepository documentLineRepository;
  @Mock private ClientService clientService;
  @Mock private ProductConditioningRepository productConditioningRepository;
  @Mock private ProductBatchService productBatchService;

  @InjectMocks private DocumentService documentService;

  private Document testDocument;
  private Client testClient;
  private Product testProduct;

  @BeforeEach
  void setUp() {
    testClient = new Client();
    testClient.setId(1L);
    testClient.setName("Test Client");
    testClient.setCurrentDebt(BigDecimal.ZERO);
    testClient.setCreditLimit(new BigDecimal("10000.00"));

    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setName("Test Product");
    testProduct.setReference("PROD-001");
    testProduct.setStockQuantity(new BigDecimal("100"));
    testProduct.setAveragePurchasePrice(new BigDecimal("15.00"));

    testDocument = createDraftDocument(DocumentType.QUOTE);
  }

  private Document createDraftDocument(DocumentType type) {
    Document doc = new Document();
    doc.setId(1L);
    doc.setDocumentNumber("TEST-001");
    doc.setDocumentType(type);
    doc.setStatus(DocumentStatus.DRAFT);
    doc.setDate(LocalDateTime.now());
    doc.setVatRate(new BigDecimal("19.00"));
    doc.setTransportFee(new BigDecimal("10.000"));
    doc.setStampDuty(new BigDecimal("1.000"));
    doc.setIsCreditSale(false);
    doc.setIsDelivery(true);
    doc.setTotalExcludingTax(BigDecimal.ZERO);
    doc.setTotalVat(BigDecimal.ZERO);
    doc.setTotalIncludingTax(BigDecimal.ZERO);
    return doc;
  }

  private Document createValidatedDocument(DocumentType type) {
    Document doc = createDraftDocument(type);
    doc.setStatus(DocumentStatus.VALIDATED);
    return doc;
  }

  private Product createProduct() {
    Product product = new Product();
    product.setId(2L);
    product.setName("Another Product");
    product.setReference("PROD-002");
    product.setStockQuantity(new BigDecimal("50"));
    product.setAveragePurchasePrice(new BigDecimal("20.00"));
    return product;
  }

  // ==================== CRUD Tests ====================

  @Test
  void getAllDocuments_ReturnsAllDocuments() {
    when(documentRepository.findAll()).thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getAllDocuments();

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findAll();
  }

  @Test
  void getDocumentById_ExistingDocument_ReturnsDocument() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    Optional<Document> result = documentService.getDocumentById(1L);

    assertTrue(result.isPresent());
    assertEquals("TEST-001", result.get().getDocumentNumber());
  }

  @Test
  void getDocumentById_NonExistingDocument_ReturnsEmpty() {
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Document> result = documentService.getDocumentById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  void getDocumentByNumber_ExistingDocument_ReturnsDocument() {
    when(documentRepository.findByDocumentNumber("TEST-001")).thenReturn(Optional.of(testDocument));

    Optional<Document> result = documentService.getDocumentByNumber("TEST-001");

    assertTrue(result.isPresent());
    assertEquals("TEST-001", result.get().getDocumentNumber());
  }

  @Test
  void getDocumentByNumber_NonExistingDocument_ReturnsEmpty() {
    when(documentRepository.findByDocumentNumber("NONEXISTENT")).thenReturn(Optional.empty());

    Optional<Document> result = documentService.getDocumentByNumber("NONEXISTENT");

    assertFalse(result.isPresent());
  }

  // ==================== Create Document Tests ====================

  @Test
  void createDocument_Quote_SetsDefaults() {
    Document quote = new Document();
    quote.setDocumentType(DocumentType.QUOTE);
    quote.setVatRate(null);
    quote.setDocumentNumber(null);

    when(documentRepository.getNextQuoteSequence()).thenReturn(1L);
    when(documentRepository.save(any(Document.class))).thenReturn(quote);

    Document result = documentService.createDocument(quote);

    assertNotNull(result);
    assertEquals(new BigDecimal("19.00"), quote.getVatRate());
    assertEquals(DocumentStatus.DRAFT, quote.getStatus());
    assertNotNull(quote.getDate());
    assertNotNull(quote.getDocumentNumber());
    assertTrue(quote.getDocumentNumber().startsWith("DEV-"));
    verify(documentRepository).save(quote);
  }

  @Test
  void createDocument_DeliveryNote_NullTransportFee_ThrowsException() {
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setVatRate(null);
    bl.setTransportFee(null);
    bl.setIsDelivery(true);
    bl.setDocumentNumber(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(bl));
    assertEquals("Delivery documents require a transport fee to be specified", ex.getMessage());
  }

  @Test
  void createDocument_Invoice_SetsDefaultStampDuty() {
    Document invoice = new Document();
    invoice.setDocumentType(DocumentType.INVOICE);
    invoice.setVatRate(null);
    invoice.setStampDuty(null);
    invoice.setDocumentNumber(null);

    when(documentRepository.getNextInvoiceSequence()).thenReturn(1L);
    when(documentRepository.save(any(Document.class))).thenReturn(invoice);

    Document result = documentService.createDocument(invoice);

    assertNotNull(result);
    assertEquals(new BigDecimal("1.000"), invoice.getStampDuty());
    assertEquals(new BigDecimal("19.00"), invoice.getVatRate());
    assertNotNull(invoice.getDocumentNumber());
    assertTrue(invoice.getDocumentNumber().startsWith("FAC-"));
    verify(documentRepository).save(invoice);
  }

  // ==================== Update Document Tests ====================

  @Test
  void updateDocument_ExistingDraftDocument_UpdatesFields() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    Document updatedDetails = new Document();
    updatedDetails.setDate(LocalDateTime.now());
    updatedDetails.setDocumentType(DocumentType.INVOICE);
    updatedDetails.setClient(testClient);
    updatedDetails.setIsCreditSale(true);

    Document result = documentService.updateDocument(1L, updatedDetails);

    assertNotNull(result);
    verify(documentRepository, atLeastOnce()).save(testDocument);
  }

  @Test
  void updateDocument_NonExistingDocument_ThrowsException() {
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    Document updatedDetails = new Document();
    updatedDetails.setDocumentType(DocumentType.QUOTE);

    assertThrows(
        RuntimeException.class, () -> documentService.updateDocument(999L, updatedDetails));
  }

  @Test
  void updateDocument_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    Document updatedDetails = new Document();
    updatedDetails.setDocumentType(DocumentType.QUOTE);

    assertThrows(RuntimeException.class, () -> documentService.updateDocument(1L, updatedDetails));
  }

  @Test
  void updateDocument_DeliveryWithNullTransportFee_ThrowsException() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));

    Document updatedDetails = new Document();
    updatedDetails.setDate(LocalDateTime.now());
    updatedDetails.setDocumentType(DocumentType.DELIVERY_NOTE);
    updatedDetails.setTransportFee(null);
    updatedDetails.setIsDelivery(true);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> documentService.updateDocument(1L, updatedDetails));
    assertEquals("Delivery documents require a transport fee to be specified", ex.getMessage());
  }

  @Test
  void updateDocument_ChangeToInvoice_SetsDefaultStampDuty() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    Document updatedDetails = new Document();
    updatedDetails.setDate(LocalDateTime.now());
    updatedDetails.setDocumentType(DocumentType.INVOICE);
    updatedDetails.setStampDuty(null);

    documentService.updateDocument(1L, updatedDetails);

    assertEquals(new BigDecimal("1.000"), invoiceDoc.getStampDuty());
  }

  // ==================== Delete Document Tests ====================

  @Test
  void deleteDocument_ExistingDraftDocument_Deletes() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());

    documentService.deleteDocument(1L);

    verify(documentLineRepository).deleteAll(Collections.emptyList());
    verify(documentRepository).delete(testDocument);
  }

  @Test
  void deleteDocument_NonExistingDocument_ThrowsException() {
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> documentService.deleteDocument(999L));
  }

  @Test
  void deleteDocument_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(RuntimeException.class, () -> documentService.deleteDocument(1L));
  }

  @Test
  void deleteDocument_WithLines_DeletesLines() {
    DocumentLine line1 = new DocumentLine();
    line1.setId(1L);
    line1.setDocument(testDocument);
    line1.setProduct(testProduct);
    line1.setQuantity(new BigDecimal("5"));

    DocumentLine line2 = new DocumentLine();
    line2.setId(2L);
    line2.setDocument(testDocument);
    line2.setProduct(testProduct);
    line2.setQuantity(new BigDecimal("3"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line1, line2));

    documentService.deleteDocument(1L);

    verify(documentLineRepository).deleteAll(Arrays.asList(line1, line2));
    verify(documentRepository).delete(testDocument);
  }

  // ==================== Query Tests ====================

  @Test
  void getDocumentsByClient_ReturnsDocuments() {
    when(documentRepository.findByClientId(1L)).thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getDocumentsByClient(1L);

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findByClientId(1L);
  }

  @Test
  void getDocumentsByUser_ReturnsDocuments() {
    when(documentRepository.findByUserId(1L)).thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getDocumentsByUser(1L);

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findByUserId(1L);
  }

  @Test
  void getDocumentsByType_ReturnsDocuments() {
    when(documentRepository.findByDocumentType(DocumentType.INVOICE))
        .thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getDocumentsByType(DocumentType.INVOICE);

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findByDocumentType(DocumentType.INVOICE);
  }

  @Test
  void getDocumentsByStatus_ReturnsDocuments() {
    when(documentRepository.findByStatus(DocumentStatus.DRAFT))
        .thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getDocumentsByStatus(DocumentStatus.DRAFT);

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findByStatus(DocumentStatus.DRAFT);
  }

  @Test
  void getCreditSalesByClient_ReturnsDocuments() {
    when(documentRepository.findCreditSalesByClient(1L)).thenReturn(Arrays.asList(testDocument));

    List<Document> documents = documentService.getCreditSalesByClient(1L);

    assertNotNull(documents);
    assertEquals(1, documents.size());
    verify(documentRepository).findCreditSalesByClient(1L);
  }

  // ==================== Validate Document Tests ====================

  @Test
  void validateDocument_DraftDocument_SetsValidatedStatus() {
    Document blDocument = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDocument.setClient(testClient);
    blDocument.setIsCreditSale(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.save(any(Document.class))).thenReturn(blDocument);

    Document result = documentService.validateDocument(1L);

    assertNotNull(result);
    assertEquals(DocumentStatus.VALIDATED, blDocument.getStatus());
    verify(documentRepository).save(blDocument);
  }

  @Test
  void validateDocument_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(RuntimeException.class, () -> documentService.validateDocument(1L));
  }

  @Test
  void validateDocument_NonExistingDocument_ThrowsException() {
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> documentService.validateDocument(999L));
  }

  @Test
  void validateDocument_CreditSale_ChecksCreditLimitAndAddsHistory() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(true);
    blDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setIsDelivered(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.validateDocument(1L);

    verify(clientService).validateCreditLimit(1L, new BigDecimal("500.00"));
    verify(clientService)
        .addCreditHistoryEntry(
            eq(testClient), eq(blDoc), eq(new BigDecimal("500.00")), any(TransactionType.class));
  }

  @Test
  void validateDocument_CreditSale_NullClient_ThrowsException() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setClient(null);
    blDoc.setIsCreditSale(true);
    blDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.validateDocument(1L));
  }

  @Test
  void validateDocument_Invoice_DeductsStock() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setClient(testClient);
    invoiceDoc.setIsCreditSale(false);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(productBatchService.allocateStock(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.validateDocument(1L);

    verify(productBatchService).allocateStock(1L, new BigDecimal("10"));
    assertTrue(line.getIsDelivered());
  }

  @Test
  void validateDocument_SkipStockDeduction_NoCreditCheck() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(true);
    blDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.validateDocument(1L, true);

    verify(clientService, never()).validateCreditLimit(any(), any());
    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
    verify(productBatchService, never()).allocateStock(any(), any());
  }

  @Test
  void validateDocument_WithLines_SetsLineDelivered() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setIsCreditSale(false);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setIsDelivered(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.validateDocument(1L);

    assertTrue(line.getIsDelivered());
    verify(documentLineRepository).save(line);
  }

  // ==================== Cancel Document Tests ====================

  @Test
  void cancelDocument_ValidatedDocument_SetsCancelledStatus() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    Document result = documentService.cancelDocument(1L);

    assertNotNull(result);
    assertEquals(DocumentStatus.CANCELLED, testDocument.getStatus());
    verify(documentRepository).save(testDocument);
  }

  @Test
  void cancelDocument_AlreadyCancelledDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.CANCELLED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(RuntimeException.class, () -> documentService.cancelDocument(1L));
  }

  @Test
  void cancelDocument_NonExistingDocument_ThrowsException() {
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> documentService.cancelDocument(999L));
  }

  @Test
  void cancelDocument_ValidatedBL_RestoresStock() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setIsDelivered(true);
    line.setBatchAllocations("{\"1\":10.000}");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.cancelDocument(1L);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<Long, BigDecimal>> captor = ArgumentCaptor.forClass(Map.class);
    verify(productBatchService).restoreBatches(captor.capture());
    Map<Long, BigDecimal> restored = captor.getValue();
    assertEquals(new BigDecimal("10.000"), restored.get(1L));
    assertFalse(line.getIsDelivered());
    assertNull(line.getBatchAllocations());
  }

  @Test
  void cancelDocument_ValidatedInvoice_NoSourceDN_RestoresStock() {
    Document invoiceDoc = createValidatedDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setSourceDeliveryNoteId(null);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations("{\"1\":5.000}");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    verify(productBatchService).restoreBatches(anyMap());
    assertFalse(line.getIsDelivered());
  }

  @Test
  void cancelDocument_ConvertedInvoice_SkipsStockRestore() {
    Document invoiceDoc = createValidatedDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setSourceDeliveryNoteId(5L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    verify(productBatchService, never()).restoreBatches(anyMap());
  }

  @Test
  void cancelDocument_CreditSale_AdjustsHistory() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setIsCreditSale(true);
    blDoc.setClient(testClient);
    blDoc.setSourceDeliveryNoteId(null);
    blDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.cancelDocument(1L);

    ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
    verify(clientService)
        .addCreditHistoryEntry(
            eq(testClient), eq(blDoc), amountCaptor.capture(), eq(TransactionType.ADJUSTMENT));
    assertEquals(new BigDecimal("-500.00"), amountCaptor.getValue());
  }

  @Test
  void cancelDocument_ConvertedInvoice_SkipsCreditHistory() {
    Document invoiceDoc = createValidatedDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setIsCreditSale(true);
    invoiceDoc.setClient(testClient);
    invoiceDoc.setSourceDeliveryNoteId(5L);
    invoiceDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
  }

  @Test
  void cancelDocument_DraftDocument_SetsCancelledStatus() {
    Document draftDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    draftDoc.setId(1L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(draftDoc);

    documentService.cancelDocument(1L);

    assertEquals(DocumentStatus.CANCELLED, draftDoc.getStatus());
    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
  }

  // ==================== Add Document Line Tests ====================

  @Test
  void addDocumentLine_NullQuantity_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, null, new BigDecimal("10.00"), null, false, null));
  }

  @Test
  void addDocumentLine_ZeroQuantity_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, BigDecimal.ZERO, new BigDecimal("10.00"), null, false, null));
  }

  @Test
  void addDocumentLine_NegativeQuantity_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("-1"), new BigDecimal("10.00"), null, false, null));
  }

  @Test
  void addDocumentLine_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null));
  }

  @Test
  void addDocumentLine_WithConditioning_AppliesConditioningPrice() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setId(1L);
    conditioning.setProduct(testProduct);
    conditioning.setUnitPrice(new BigDecimal("50.00"));
    conditioning.setDescription("Box of 10");
    when(productConditioningRepository.findById(1L)).thenReturn(Optional.of(conditioning));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5"),
            new BigDecimal("15.00"),
            new BigDecimal("50.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, 1L);

    assertEquals(new BigDecimal("50.00"), result.getUnitPrice());
    assertEquals("Box of 10", result.getConditioningDescription());
  }

  @Test
  void addDocumentLine_WithConditioning_WrongProduct_ThrowsException() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    Product otherProduct = createProduct();
    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setId(1L);
    conditioning.setProduct(otherProduct);
    conditioning.setUnitPrice(new BigDecimal("50.00"));
    conditioning.setDescription("Box of 10");
    when(productConditioningRepository.findById(1L)).thenReturn(Optional.of(conditioning));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, 1L));
  }

  @Test
  void addDocumentLine_WithConditioning_NotFound_ThrowsException() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(productConditioningRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, 999L));
  }

  @Test
  void addDocumentLine_NegativePrice_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), new BigDecimal("-1"), null, false, null));
  }

  @Test
  void addDocumentLine_WithExistingLines_SetsCorrectLineNumber() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(10L);
    existingLine.setLineNumber(3);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5"),
            new BigDecimal("15.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    assertEquals(4, result.getLineNumber());
  }

  // ==================== Convert Quote To Delivery Note Tests ====================

  @Test
  void convertQuoteToDeliveryNote_HappyPath() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(10L);
    quote.setClient(testClient);
    quote.setIsCreditSale(false);

    DocumentLine quoteLine = new DocumentLine();
    quoteLine.setId(100L);
    quoteLine.setDocument(quote);
    quoteLine.setProduct(testProduct);
    quoteLine.setQuantity(new BigDecimal("5"));
    quoteLine.setUnitPrice(new BigDecimal("20.00"));
    quoteLine.setConditioningDescription(null);
    quoteLine.setIsDelivered(false);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(10L)).thenReturn(Arrays.asList(quoteLine));

    Document savedBl = createDraftDocument(DocumentType.DELIVERY_NOTE);
    savedBl.setId(20L);
    when(documentRepository.save(any(Document.class))).thenReturn(savedBl);
    when(documentRepository.findById(20L)).thenReturn(Optional.of(savedBl));
    when(documentRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(savedBl));
    when(documentLineRepository.findByDocumentId(20L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);

    Document result = documentService.convertQuoteToDeliveryNote(10L);

    assertNotNull(result);
    verify(documentRepository, atLeastOnce()).save(any(Document.class));
  }

  @Test
  void convertQuoteToDeliveryNote_NotQuote_ThrowsException() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(10L);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.convertQuoteToDeliveryNote(10L));
  }

  // ==================== Convert Delivery Note To Invoice Tests ====================

  @Test
  void convertDeliveryNoteToInvoice_HappyPath() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(10L);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(false);
    blDoc.setTransportFee(new BigDecimal("10.000"));
    blDoc.setConvertedToInvoiceId(null);

    DocumentLine blLine = new DocumentLine();
    blLine.setId(100L);
    blLine.setDocument(blDoc);
    blLine.setProduct(testProduct);
    blLine.setQuantity(new BigDecimal("5"));
    blLine.setUnitPrice(new BigDecimal("20.00"));
    blLine.setConditioningDescription(null);
    blLine.setIsDelivered(true);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(10L)).thenReturn(Arrays.asList(blLine));

    Document savedInvoice = createDraftDocument(DocumentType.INVOICE);
    savedInvoice.setId(30L);
    savedInvoice.setClient(testClient);
    when(documentRepository.save(any(Document.class))).thenReturn(savedInvoice);
    when(documentRepository.findById(30L)).thenReturn(Optional.of(savedInvoice));
    when(documentRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(savedInvoice));
    when(documentLineRepository.findByDocumentId(30L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextInvoiceSequence()).thenReturn(1L);

    Document result = documentService.convertDeliveryNoteToInvoice(10L);

    assertNotNull(result);
    verify(documentRepository, atLeastOnce()).save(any(Document.class));
  }

  @Test
  void convertDeliveryNoteToInvoice_NotDeliveryNote_ThrowsException() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(10L);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(quote));

    assertThrows(RuntimeException.class, () -> documentService.convertDeliveryNoteToInvoice(10L));
  }

  @Test
  void convertDeliveryNoteToInvoice_NotValidated_ThrowsException() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(10L);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.convertDeliveryNoteToInvoice(10L));
  }

  @Test
  void convertDeliveryNoteToInvoice_AlreadyConverted_ThrowsException() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(10L);
    blDoc.setConvertedToInvoiceId(5L);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.convertDeliveryNoteToInvoice(10L));
  }

  @Test
  void addDocumentLine_Invoice_RecalculatesWithTransportFeeAndStampDuty() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setTransportFee(new BigDecimal("10.000"));
    invoiceDoc.setStampDuty(new BigDecimal("1.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(invoiceDoc));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("20.00"));
    line.setLineNumber(1);
    line.setTotalLineExcludingTax(new BigDecimal("200.000"));
    line.setTotalLineIncludingTax(new BigDecimal("238.000"));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.addDocumentLine(
        1L, testProduct, new BigDecimal("10"), new BigDecimal("20.00"), null, false, null);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, atLeastOnce()).save(captor.capture());

    Document savedDoc =
        captor.getAllValues().stream().filter(d -> d.getId().equals(1L)).findFirst().orElse(null);

    assertNotNull(savedDoc);
    BigDecimal expectedExcludingTax = new BigDecimal("200.000").add(new BigDecimal("10.000"));
    assertEquals(0, expectedExcludingTax.compareTo(savedDoc.getTotalExcludingTax()));

    BigDecimal lineVat = new BigDecimal("200.000").multiply(new BigDecimal("0.19"));
    BigDecimal expectedIncludingTax =
        expectedExcludingTax.add(lineVat).add(new BigDecimal("1.000"));
    assertEquals(0, expectedIncludingTax.compareTo(savedDoc.getTotalIncludingTax()));
  }

  // ==================== Update Document Line Tests ====================

  @Test
  void updateDocumentLine_DraftDocument_UpdatesLine() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningDescription("Updated conditioning");

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DocumentLine updatedLine = new DocumentLine();
    updatedLine.setId(1L);
    updatedLine.setDocument(testDocument);
    updatedLine.setProduct(testProduct);
    updatedLine.setQuantity(new BigDecimal("10"));
    updatedLine.setUnitPrice(new BigDecimal("20.00"));
    updatedLine.setLineNumber(1);
    updatedLine.setTotalLineExcludingTax(new BigDecimal("200.000"));
    updatedLine.setTotalLineIncludingTax(new BigDecimal("238.000"));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(updatedLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertNotNull(result);
    assertEquals(new BigDecimal("10"), result.getQuantity());
    assertEquals(new BigDecimal("20.00"), result.getUnitPrice());
    assertEquals("Updated conditioning", result.getConditioningDescription());
  }

  @Test
  void updateDocumentLine_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("5"));
    lineDetails.setUnitPrice(new BigDecimal("10.00"));

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_NullQuantity_ThrowsException() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(null);
    lineDetails.setUnitPrice(new BigDecimal("10.00"));

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_ZeroQuantity_ThrowsException() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(BigDecimal.ZERO);
    lineDetails.setUnitPrice(new BigDecimal("10.00"));

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_NegativeUnitPrice_ThrowsException() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("5"));
    lineDetails.setUnitPrice(new BigDecimal("-1"));

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_NullUnitPrice_ThrowsException() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("5"));
    lineDetails.setUnitPrice(null);

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_NonExistingLine_ThrowsException() {
    when(documentLineRepository.findById(999L)).thenReturn(Optional.empty());

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("5"));
    lineDetails.setUnitPrice(new BigDecimal("10.00"));

    assertThrows(
        RuntimeException.class, () -> documentService.updateDocumentLine(999L, lineDetails));
  }

  // ==================== Delete Document Line Tests ====================

  @Test
  void deleteDocumentLine_DraftDocument_DeletesLine() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    documentService.deleteDocumentLine(1L);

    verify(documentLineRepository).delete(existingLine);
  }

  @Test
  void deleteDocumentLine_NonDraftDocument_ThrowsException() {
    testDocument.setStatus(DocumentStatus.VALIDATED);
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    assertThrows(RuntimeException.class, () -> documentService.deleteDocumentLine(1L));
  }

  @Test
  void deleteDocumentLine_NonExistingLine_ThrowsException() {
    when(documentLineRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> documentService.deleteDocumentLine(999L));
  }

  // ==================== Cancel Document - Converted Delivery Note Tests ====================

  @Test
  void cancelDocument_ConvertedDeliveryNote_ThrowsException() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setConvertedToInvoiceId(5L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.cancelDocument(1L));
  }

  // ==================== Add Document Line - Variant Tests ====================

  @Test
  void addDocumentLine_WithVariant_ValidatesVariantBelongsToProduct() {
    Product otherProduct = createProduct();
    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(otherProduct);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L,
                testProduct,
                variant,
                new BigDecimal("5"),
                new BigDecimal("10.00"),
                null,
                false,
                null));
  }

  @Test
  void addDocumentLine_WithVariant_NullProductInVariant_ThrowsException() {
    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L,
                testProduct,
                variant,
                new BigDecimal("5"),
                new BigDecimal("10.00"),
                null,
                false,
                null));
  }

  // ==================== Update Document Line - Variant and Conditioning Tests ====================

  @Test
  void updateDocumentLine_WithVariantChange_RecomputesUnitCost() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setConditioningQuantityPerUnit(BigDecimal.ONE);

    ProductVariant newVariant = new ProductVariant();
    newVariant.setId(10L);
    newVariant.setProduct(testProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(newVariant);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocationFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    documentService.updateDocumentLine(1L, lineDetails);

    verify(productBatchService).estimateAllocationFromVariant(eq(10L), any(BigDecimal.class));
  }

  @Test
  void updateDocumentLine_VariantFromDifferentProduct_ThrowsException() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);

    Product otherProduct = createProduct();
    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(otherProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("5"));
    lineDetails.setUnitPrice(new BigDecimal("10.00"));
    lineDetails.setVariant(variant);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));

    assertThrows(RuntimeException.class, () -> documentService.updateDocumentLine(1L, lineDetails));
  }

  @Test
  void updateDocumentLine_ConditioningQuantityChange_RecomputesUnitCost() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setConditioningQuantityPerUnit(BigDecimal.ONE);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningQuantityPerUnit(new BigDecimal("10"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    documentService.updateDocumentLine(1L, lineDetails);

    verify(productBatchService).estimateAllocation(eq(1L), any(BigDecimal.class));
  }

  @Test
  void updateDocumentLine_NullProduct_SkipsUnitCostRecomputation() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(null);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningQuantityPerUnit(new BigDecimal("10"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    documentService.updateDocumentLine(1L, lineDetails);

    verify(productBatchService, never()).estimateAllocation(any(), any());
  }

  @Test
  void cancelDocument_MissingBatchAllocations_ThrowsException() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    assertThrows(IllegalArgumentException.class, () -> documentService.cancelDocument(1L));
  }

  @Test
  void cancelDocument_BlankBatchAllocations_ThrowsException() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations("   ");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    assertThrows(IllegalArgumentException.class, () -> documentService.cancelDocument(1L));
  }

  @Test
  void addDocumentLine_NoAvailableBatches_UsesProductAveragePrice() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    testProduct.setAveragePurchasePrice(new BigDecimal("18.00"));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    assertNotNull(result);
    assertEquals(new BigDecimal("18.000"), result.getUnitCost());
  }

  @Test
  void updateDocument_VatRateChange_RecomputesLineVat() {
    testDocument.setVatRate(new BigDecimal("19.00"));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(testDocument);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("10.00"));
    line.setTotalLineExcludingTax(new BigDecimal("100.000"));
    line.setTotalLineIncludingTax(new BigDecimal("119.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    Document updatedDetails = new Document();
    updatedDetails.setVatRate(new BigDecimal("20.00"));

    documentService.updateDocument(1L, updatedDetails);

    verify(documentLineRepository).save(line);
  }

  // ==================== Validate Quote Tests ====================

  @Test
  void validateDocument_Quote_NoStockDeduction() {
    Document quoteDoc = createDraftDocument(DocumentType.QUOTE);
    quoteDoc.setClient(testClient);
    quoteDoc.setIsCreditSale(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(quoteDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(quoteDoc);

    Document result = documentService.validateDocument(1L);

    assertNotNull(result);
    assertEquals(DocumentStatus.VALIDATED, quoteDoc.getStatus());
    verify(productBatchService, never()).allocateStock(any(), any());
  }

  // ==================== Cancel Document Quote Tests ====================

  @Test
  void cancelDocument_CancelledQuote_SetsCancelledStatus() {
    Document quoteDoc = createValidatedDocument(DocumentType.QUOTE);
    quoteDoc.setId(1L);
    quoteDoc.setIsCreditSale(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(quoteDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(quoteDoc);

    documentService.cancelDocument(1L);

    assertEquals(DocumentStatus.CANCELLED, quoteDoc.getStatus());
    verify(productBatchService, never()).restoreBatches(anyMap());
  }

  // ==================== Stock Lifecycle Tests ====================

  @Test
  void addDocumentLine_DoesNotDeductStock() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    documentService.addDocumentLine(
        1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    verify(productBatchService, never()).allocateStock(any(), any());
  }

  @Test
  void addDocumentLine_Conditioning_MultipliesQuantityAndCost() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setId(1L);
    conditioning.setProduct(testProduct);
    conditioning.setUnitPrice(new BigDecimal("95.00"));
    conditioning.setDescription("Roll of 100m");
    conditioning.setQuantityPerUnit(new BigDecimal("100"));
    when(productConditioningRepository.findById(1L)).thenReturn(Optional.of(conditioning));

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("1"), new BigDecimal("95.00"), null, false, 1L);

    assertEquals(new BigDecimal("95.00"), result.getUnitPrice());
    assertEquals(new BigDecimal("100"), result.getConditioningQuantityPerUnit());
    assertEquals(new BigDecimal("1500.000"), result.getUnitCost());
    verify(productBatchService).estimateAllocation(eq(1L), eq(new BigDecimal("100")));
  }

  @Test
  void createDocument_CreditSaleWithoutClient_ThrowsException() {
    Document doc = createDraftDocument(DocumentType.INVOICE);
    doc.setClient(null);
    doc.setIsCreditSale(true);

    assertThrows(RuntimeException.class, () -> documentService.createDocument(doc));
  }

  @Test
  void cancelDocument_ConvertedBL_ThrowsException() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setConvertedToInvoiceId(5L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));

    assertThrows(RuntimeException.class, () -> documentService.cancelDocument(1L));
  }

  @Test
  void convertQuoteToDeliveryNote_CancelledQuote_ThrowsException() {
    Document quote = createValidatedDocument(DocumentType.QUOTE);
    quote.setId(10L);
    quote.setStatus(DocumentStatus.CANCELLED);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(quote));

    assertThrows(RuntimeException.class, () -> documentService.convertQuoteToDeliveryNote(10L));
  }

  // ==================== getDocumentLines ====================

  @Test
  void getDocumentLines_ReturnsLines() {
    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(testDocument);
    line.setProduct(testProduct);
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    List<DocumentLine> result = documentService.getDocumentLines(1L);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(documentLineRepository).findByDocumentId(1L);
  }

  // ==================== addDocumentLine with Variant ====================

  @Test
  void addDocumentLine_WithVariant_EstimatesAllocationFromVariant() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(testProduct);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocationFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L,
            testProduct,
            variant,
            new BigDecimal("5"),
            new BigDecimal("20.00"),
            null,
            false,
            null);

    assertNotNull(result);
    assertEquals(variant, result.getVariant());
    verify(productBatchService).estimateAllocationFromVariant(eq(10L), any(BigDecimal.class));
  }

  @Test
  void addDocumentLine_WithVariant_WrongProduct_ThrowsException() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    Product otherProduct = createProduct();
    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(otherProduct);

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L,
                testProduct,
                variant,
                new BigDecimal("5"),
                new BigDecimal("20.00"),
                null,
                false,
                null));
  }

  // ==================== validateDocument with Lines ====================

  @Test
  void validateDocument_InvoiceWithLines_DeductsStockForEachLine() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setClient(testClient);
    invoiceDoc.setIsCreditSale(false);

    DocumentLine line1 = new DocumentLine();
    line1.setId(1L);
    line1.setDocument(invoiceDoc);
    line1.setProduct(testProduct);
    line1.setQuantity(new BigDecimal("10"));
    line1.setIsDelivered(false);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line1));
    when(productBatchService.allocateStock(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.validateDocument(1L);

    assertEquals(DocumentStatus.VALIDATED, invoiceDoc.getStatus());
    assertTrue(line1.getIsDelivered());
    assertNotNull(line1.getBatchAllocations());
  }

  // ==================== validateDocument with credit sale ====================

  @Test
  void validateDocument_CreditSaleBL_WithLines_ChecksCreditAndDeductsStock() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(true);
    blDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setIsDelivered(false);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(productBatchService.allocateStock(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.validateDocument(1L);

    verify(clientService).validateCreditLimit(1L, new BigDecimal("500.00"));
    verify(clientService)
        .addCreditHistoryEntry(
            eq(testClient), eq(blDoc), eq(new BigDecimal("500.00")), any(TransactionType.class));
    assertTrue(line.getIsDelivered());
  }

  // ==================== cancelDocument with restoreStock ====================

  // ==================== convertQuoteToDeliveryNote edge cases ====================

  @Test
  void convertQuoteToDeliveryNote_WithNullProductLine_SkipsLine() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(10L);
    quote.setClient(testClient);

    DocumentLine badLine = new DocumentLine();
    badLine.setId(100L);
    badLine.setDocument(quote);
    badLine.setProduct(null);
    badLine.setQuantity(new BigDecimal("5"));
    badLine.setUnitPrice(new BigDecimal("20.00"));
    badLine.setConditioningDescription(null);
    badLine.setIsDelivered(false);

    DocumentLine validLine = new DocumentLine();
    validLine.setId(101L);
    validLine.setDocument(quote);
    validLine.setProduct(testProduct);
    validLine.setQuantity(new BigDecimal("3"));
    validLine.setUnitPrice(new BigDecimal("15.00"));
    validLine.setConditioningDescription(null);
    validLine.setIsDelivered(false);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(10L))
        .thenReturn(Arrays.asList(badLine, validLine));

    Document savedBl = createDraftDocument(DocumentType.DELIVERY_NOTE);
    savedBl.setId(20L);
    when(documentRepository.save(any(Document.class))).thenReturn(savedBl);
    when(documentRepository.findById(20L)).thenReturn(Optional.of(savedBl));
    when(documentRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(savedBl));
    when(documentLineRepository.findByDocumentId(20L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);

    Document result = documentService.convertQuoteToDeliveryNote(10L);

    assertNotNull(result);
    verify(documentRepository, atLeastOnce()).save(any(Document.class));
  }

  // ==================== convertDeliveryNoteToInvoice with credit sale ====================

  @Test
  void convertDeliveryNoteToInvoice_CreditSale_MarksSourceDeliveryNote() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(10L);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(true);
    blDoc.setTransportFee(new BigDecimal("10.000"));
    blDoc.setConvertedToInvoiceId(null);

    DocumentLine blLine = new DocumentLine();
    blLine.setId(100L);
    blLine.setDocument(blDoc);
    blLine.setProduct(testProduct);
    blLine.setQuantity(new BigDecimal("5"));
    blLine.setUnitPrice(new BigDecimal("20.00"));
    blLine.setConditioningDescription(null);
    blLine.setIsDelivered(true);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(10L)).thenReturn(Arrays.asList(blLine));

    Document savedInvoice = createDraftDocument(DocumentType.INVOICE);
    savedInvoice.setId(30L);
    savedInvoice.setClient(testClient);
    when(documentRepository.save(any(Document.class))).thenReturn(savedInvoice);
    when(documentRepository.findById(30L)).thenReturn(Optional.of(savedInvoice));
    when(documentRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(savedInvoice));
    when(documentLineRepository.findByDocumentId(30L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextInvoiceSequence()).thenReturn(1L);

    Document result = documentService.convertDeliveryNoteToInvoice(10L);

    assertNotNull(result);
    verify(documentRepository, atLeastOnce()).save(any(Document.class));
  }

  // ==================== updateDocumentLine with variant ====================

  @Test
  void updateDocumentLine_WithVariant_SetsVariantOnLine() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(variant);
    lineDetails.setConditioningQuantityPerUnit(new BigDecimal("2"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertNotNull(result);
    assertEquals(variant, result.getVariant());
    assertEquals(new BigDecimal("2"), result.getConditioningQuantityPerUnit());
  }

  // ==================== deleteDocumentLine - recalculate after delete ====================

  @Test
  void deleteDocumentLine_DeletesLineAndRecalculatesTotals() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    documentService.deleteDocumentLine(1L);

    verify(documentLineRepository).delete(existingLine);
    verify(documentRepository).save(testDocument);
  }

  // ==================== createDocument defaults ====================

  @Test
  void createDocument_NullDateStatusDocNumber_AppliesDefaults() {
    Document doc = new Document();
    doc.setDocumentType(DocumentType.QUOTE);
    doc.setClient(testClient);

    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.getNextQuoteSequence()).thenReturn(1L);

    Document result = documentService.createDocument(doc);

    assertNotNull(result.getDate());
    assertEquals(DocumentStatus.DRAFT, result.getStatus());
    assertEquals("DEV-000001", result.getDocumentNumber());
  }

  // ==================== updateDocument credit sale without client ====================

  @Test
  void updateDocument_CreditSaleWithoutClient_Throws() {
    Document draftDoc = createDraftDocument(DocumentType.INVOICE);
    draftDoc.setId(1L);
    draftDoc.setIsCreditSale(true);
    draftDoc.setClient(null);

    Document details = createDraftDocument(DocumentType.INVOICE);
    details.setIsCreditSale(true);
    details.setClient(null);
    details.setVatRate(new BigDecimal("19.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));

    assertThrows(IllegalArgumentException.class, () -> documentService.updateDocument(1L, details));
  }

  // ==================== updateDocument null vatRate ====================

  @Test
  void updateDocument_NullVatRate_PreservesExistingRate() {
    Document draftDoc = createDraftDocument(DocumentType.QUOTE);
    draftDoc.setId(1L);
    draftDoc.setVatRate(new BigDecimal("19.00"));
    draftDoc.setIsCreditSale(false);

    Document details = createDraftDocument(DocumentType.QUOTE);
    details.setVatRate(null);
    details.setIsCreditSale(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());

    Document result = documentService.updateDocument(1L, details);

    assertEquals(new BigDecimal("19.00"), result.getVatRate());
  }

  // ==================== addDocumentLine null unitPrice paths ====================

  @Test
  void addDocumentLine_NullUnitPrice_UsesWeightedAveragePrice() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, null, new BigDecimal("5"), null, null, false, null);

    assertEquals(new BigDecimal("20.000"), result.getUnitPrice());
  }

  @Test
  void addDocumentLine_NullUnitPrice_NullWeightedAvg_FallsBackToProductPrice() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    testProduct.setUnitPrice(new BigDecimal("25.00"));

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L, new BigDecimal("10"), new BigDecimal("15.00"), null, LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, null, new BigDecimal("5"), null, null, false, null);

    assertEquals(new BigDecimal("25.00"), result.getUnitPrice());
  }

  @Test
  void addDocumentLine_NullUnitPrice_NullWeightedAvg_NullProductPrice_Throws() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    testProduct.setUnitPrice(null);

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L, new BigDecimal("10"), new BigDecimal("15.00"), null, LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, null, new BigDecimal("5"), null, null, false, null));
  }

  @Test
  void addDocumentLine_NullUnitCost_FallsBackToAvgPurchasePrice() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    testProduct.setAveragePurchasePrice(new BigDecimal("12.00"));

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L, new BigDecimal("10"), null, new BigDecimal("20.00"), LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, null, new BigDecimal("5"), new BigDecimal("20.00"), null, false, null);

    assertEquals(new BigDecimal("12.000"), result.getUnitCost());
  }

  @Test
  void addDocumentLine_NullUnitCost_NullAvgPurchasePrice_UsesZero() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    testProduct.setAveragePurchasePrice(null);

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L, new BigDecimal("10"), null, new BigDecimal("20.00"), LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, null, new BigDecimal("5"), new BigDecimal("20.00"), null, false, null);

    assertEquals(new BigDecimal("0.000"), result.getUnitCost());
  }

  // ==================== validateDocument credit paths ====================

  @Test
  void validateDocument_CreditSaleWithClient_ValidatesCreditLimit() {
    Document draftDoc = createDraftDocument(DocumentType.INVOICE);
    draftDoc.setId(1L);
    draftDoc.setClient(testClient);
    draftDoc.setIsCreditSale(true);
    draftDoc.setTransportFee(null);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(draftDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setUnitPrice(new BigDecimal("10.00"));
    line.setTotalLineExcludingTax(new BigDecimal("50.000"));
    line.setTotalLineIncludingTax(new BigDecimal("59.500"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(clientService).validateCreditLimit(eq(1L), any());
    verify(clientService).addCreditHistoryEntry(eq(testClient), eq(draftDoc), any(), any());
  }

  @Test
  void validateDocument_NonCreditSale_SkipsCreditCheck() {
    Document draftDoc = createDraftDocument(DocumentType.INVOICE);
    draftDoc.setId(1L);
    draftDoc.setClient(testClient);
    draftDoc.setIsCreditSale(false);
    draftDoc.setTransportFee(null);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(draftDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setUnitPrice(new BigDecimal("10.00"));
    line.setTotalLineExcludingTax(new BigDecimal("50.000"));
    line.setTotalLineIncludingTax(new BigDecimal("59.500"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(clientService, never()).validateCreditLimit(any(), any());
    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
  }

  // ==================== deductStock variant path ====================

  @Test
  void deductStock_VariantLine_CallsAllocateStockFromVariant() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setClient(testClient);

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setVariant(variant);
    line.setQuantity(new BigDecimal("5"));
    line.setUnitPrice(new BigDecimal("10.00"));
    line.setTotalLineExcludingTax(new BigDecimal("50.000"));
    line.setTotalLineIncludingTax(new BigDecimal("59.500"));
    line.setConditioningQuantityPerUnit(null);

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5"),
            new BigDecimal("8.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(productBatchService.allocateStockFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(productBatchService).allocateStockFromVariant(eq(10L), any(BigDecimal.class));
    verify(productBatchService, never()).allocateStock(any(), any());
  }

  // ==================== deductStock/restoreStock null guard ====================

  @Test
  void deductStock_NullProductLine_Skips() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setClient(testClient);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(null);
    line.setQuantity(new BigDecimal("5"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(productBatchService, never()).allocateStock(any(), any());
  }

  @Test
  void deductStock_NullQuantityLine_Skips() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setClient(testClient);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(productBatchService, never()).allocateStock(any(), any());
  }

  @Test
  void restoreStock_NullProductLine_Skips() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(null);
    line.setQuantity(new BigDecimal("5"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    documentService.cancelDocument(1L);

    verify(productBatchService).restoreBatches(Map.of());
  }

  @Test
  void restoreStock_NullQuantityLine_Skips() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    documentService.cancelDocument(1L);

    verify(productBatchService).restoreBatches(Map.of());
  }

  // ==================== weightedAveragePrice/cost via null-unitPrice allocations
  // ====================

  @Test
  void addDocumentLine_EmptyAllocations_NullUnitCost_UsesZero() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    testProduct.setAveragePurchasePrice(null);

    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, null, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    assertEquals(new BigDecimal("0.000"), result.getUnitCost());
  }

  // ==================== copyLineToDocument with null fields ====================

  @Test
  void copyLineToDocument_NullQuantity_Skips() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(1L);

    DocumentLine source = new DocumentLine();
    source.setId(1L);
    source.setDocument(quote);
    source.setProduct(testProduct);
    source.setQuantity(null);
    source.setUnitPrice(new BigDecimal("10.00"));
    source.setLineNumber(1);

    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(2L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(source));
    when(documentRepository.save(any(Document.class)))
        .thenAnswer(
            inv -> {
              Document doc = inv.getArgument(0);
              if (doc.getId() == null) {
                doc.setId(2L);
              }
              return doc;
            });
    when(documentRepository.findById(2L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(2L)).thenReturn(Collections.emptyList());

    documentService.convertQuoteToDeliveryNote(1L);

    verify(documentLineRepository, never()).save(any(DocumentLine.class));
  }

  // ==================== serializeBatchAllocations/deserializeBatchAllocations ====================

  @Test
  void cancelDocument_WithBatchAllocations_RestoresAndClears() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations("{\"1\":5.000}");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.cancelDocument(1L);

    assertNull(line.getBatchAllocations());
    assertFalse(line.getIsDelivered());
  }

  @Test
  void cancelDocument_NullBatchAllocations_Throws() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    assertThrows(IllegalArgumentException.class, () -> documentService.cancelDocument(1L));
  }

  // ==================== generateDocumentNumber explicit number ====================

  @Test
  void createDocument_ExplicitDocumentNumber_IsPreserved() {
    Document doc = new Document();
    doc.setDocumentType(DocumentType.QUOTE);
    doc.setVatRate(new BigDecimal("19.00"));
    doc.setDocumentNumber("CUSTOM-NUMBER");
    doc.setClient(testClient);
    doc.setIsCreditSale(false);

    when(documentRepository.save(any(Document.class))).thenReturn(doc);

    Document result = documentService.createDocument(doc);

    assertNotNull(result);
    assertEquals("CUSTOM-NUMBER", result.getDocumentNumber());
  }

  // ==================== cancelDocument Invoice with sourceDeliveryNoteId ====================

  @Test
  void cancelDocument_InvoiceWithSourceDeliveryNoteId_SkipsStockRestore() {
    Document invoiceDoc = createValidatedDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setSourceDeliveryNoteId(5L);
    invoiceDoc.setIsCreditSale(true);
    invoiceDoc.setClient(testClient);
    invoiceDoc.setTotalIncludingTax(new BigDecimal("500.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    assertEquals(DocumentStatus.CANCELLED, invoiceDoc.getStatus());
    verify(productBatchService, never()).restoreBatches(anyMap());
    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
  }

  // ==================== deductStock variant with conditioning ====================

  @Test
  void deductStock_VariantWithConditioning_CallsAllocateStockFromVariantWithMultiplier() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setClient(testClient);

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setVariant(variant);
    line.setQuantity(new BigDecimal("2"));
    line.setUnitPrice(new BigDecimal("50.00"));
    line.setTotalLineExcludingTax(new BigDecimal("100.000"));
    line.setTotalLineIncludingTax(new BigDecimal("119.000"));
    line.setConditioningQuantityPerUnit(new BigDecimal("50"));

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("100"),
            new BigDecimal("8.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(productBatchService.allocateStockFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(List.of(alloc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(productBatchService).allocateStockFromVariant(eq(10L), eq(new BigDecimal("100")));
    assertTrue(line.getIsDelivered());
    assertNotNull(line.getBatchAllocations());
  }

  // ==================== copyLineToDocument with null unitPrice ====================

  @Test
  void convertQuoteToDeliveryNote_LineWithNullUnitPrice_SkipsLine() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(10L);
    quote.setClient(testClient);

    DocumentLine badLine = new DocumentLine();
    badLine.setId(100L);
    badLine.setDocument(quote);
    badLine.setProduct(testProduct);
    badLine.setQuantity(new BigDecimal("5"));
    badLine.setUnitPrice(null);
    badLine.setConditioningDescription(null);
    badLine.setIsDelivered(false);

    when(documentRepository.findById(10L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(10L)).thenReturn(Arrays.asList(badLine));

    Document savedBl = createDraftDocument(DocumentType.DELIVERY_NOTE);
    savedBl.setId(20L);
    when(documentRepository.save(any(Document.class))).thenReturn(savedBl);
    when(documentRepository.findById(20L)).thenReturn(Optional.of(savedBl));
    when(documentLineRepository.findByDocumentId(20L)).thenReturn(Collections.emptyList());
    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);

    Document result = documentService.convertQuoteToDeliveryNote(10L);

    assertNotNull(result);
    verify(documentLineRepository, never()).save(any(DocumentLine.class));
  }

  // ==================== addDocumentLine null unitPrice empty allocations ====================

  @Test
  void addDocumentLine_NullUnitPrice_EmptyAllocations_NullProductUnitPrice_Throws() {
    testProduct.setUnitPrice(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), null, null, false, null));
  }

  // ==================== addDocumentLine null unitPrice product has unitPrice ====================

  @Test
  void addDocumentLine_NullUnitPrice_EmptyAllocations_ProductHasUnitPrice_UsesProductPrice() {
    testProduct.setUnitPrice(new BigDecimal("30.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), null, null, false, null);

    assertEquals(new BigDecimal("30.00"), result.getUnitPrice());
  }

  // ==================== validateDocument with variant line and conditioning ====================

  @Test
  void addDocumentLine_WithConditioningAndVariant_EstimatesFromVariant() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(testProduct);

    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setId(1L);
    conditioning.setProduct(testProduct);
    conditioning.setUnitPrice(new BigDecimal("50.00"));
    conditioning.setDescription("Box of 10");
    conditioning.setQuantityPerUnit(new BigDecimal("10"));
    when(productConditioningRepository.findById(1L)).thenReturn(Optional.of(conditioning));

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("50.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocationFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, variant, new BigDecimal("1"), null, null, false, 1L);

    assertEquals(new BigDecimal("50.00"), result.getUnitPrice());
    assertEquals("Box of 10", result.getConditioningDescription());
    verify(productBatchService).estimateAllocationFromVariant(eq(10L), eq(new BigDecimal("10")));
  }

  // ==================== deductStock with conditioning on non-variant line ====================

  @Test
  void deductStock_ConditioningLine_MultipliesQuantityForAllocation() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setClient(testClient);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("2"));
    line.setUnitPrice(new BigDecimal("50.00"));
    line.setTotalLineExcludingTax(new BigDecimal("100.000"));
    line.setTotalLineIncludingTax(new BigDecimal("119.000"));
    line.setConditioningQuantityPerUnit(new BigDecimal("50"));
    line.setVariant(null);

    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("100"),
            new BigDecimal("8.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(productBatchService.allocateStock(eq(1L), eq(new BigDecimal("100"))))
        .thenReturn(List.of(alloc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.validateDocument(1L);

    verify(productBatchService).allocateStock(eq(1L), eq(new BigDecimal("100")));
    assertTrue(line.getIsDelivered());
  }

  // ==================== updateDocument preserve vatRate ====================

  @Test
  void updateDocument_VatRateNotNull_UpdatesVatRate() {
    Document draftDoc = createDraftDocument(DocumentType.QUOTE);
    draftDoc.setId(1L);
    draftDoc.setVatRate(new BigDecimal("19.00"));
    draftDoc.setIsCreditSale(false);

    Document details = createDraftDocument(DocumentType.QUOTE);
    details.setVatRate(new BigDecimal("7.00"));
    details.setIsCreditSale(false);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());

    Document result = documentService.updateDocument(1L, details);

    assertEquals(new BigDecimal("7.00"), result.getVatRate());
  }

  // ==================== updateDocument VAT rate change recomputes lines ====================

  @Test
  void updateDocument_VatRateChange_RecomputesLineTotals() {
    Document draftDoc = createDraftDocument(DocumentType.QUOTE);
    draftDoc.setId(1L);
    draftDoc.setVatRate(new BigDecimal("19.00"));
    draftDoc.setIsCreditSale(false);

    Document details = createDraftDocument(DocumentType.QUOTE);
    details.setVatRate(new BigDecimal("7.00"));
    details.setIsCreditSale(false);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(draftDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("100.00"));
    line.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.updateDocument(1L, details);

    assertEquals(new BigDecimal("7.00"), draftDoc.getVatRate());
    assertEquals(new BigDecimal("1000.000"), line.getTotalLineExcludingTax());
    assertEquals(0, new BigDecimal("1070.000").compareTo(line.getTotalLineIncludingTax()));
  }

  @Test
  void updateDocument_SameVatRate_DoesNotRecomputeLines() {
    Document draftDoc = createDraftDocument(DocumentType.QUOTE);
    draftDoc.setId(1L);
    draftDoc.setVatRate(new BigDecimal("19.00"));
    draftDoc.setIsCreditSale(false);

    Document details = createDraftDocument(DocumentType.QUOTE);
    details.setVatRate(new BigDecimal("19.00"));
    details.setIsCreditSale(false);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(draftDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("100.00"));
    line.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(draftDoc));
    when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    documentService.updateDocument(1L, details);

    assertEquals(new BigDecimal("1190.000"), line.getTotalLineIncludingTax());
    verify(documentLineRepository, never()).save(any(DocumentLine.class));
  }

  // ==================== legacy fractional VAT rate normalization ====================

  @Test
  void addDocumentLine_LegacyFractionalVatRate_NormalizesToPercentage() {
    testDocument.setVatRate(new BigDecimal("0.19"));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testDocument));

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    assertEquals(0, new BigDecimal("59.500").compareTo(result.getTotalLineIncludingTax()));
  }

  // ==================== updateDocumentLine unit cost recompute ====================

  @Test
  void updateDocumentLine_ChangedVariant_RecomputesUnitCost() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(testProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(variant);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocationFromVariant(eq(10L), eq(new BigDecimal("10"))))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(variant, result.getVariant());
    assertEquals(new BigDecimal("15.000"), result.getUnitCost());
    verify(productBatchService).estimateAllocationFromVariant(eq(10L), eq(new BigDecimal("10")));
  }

  @Test
  void updateDocumentLine_UnchangedVariantAndConditioning_KeepsUnitCost() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(new BigDecimal("8.000"), result.getUnitCost());
    verify(productBatchService, never()).estimateAllocation(any(), any());
  }

  // ==================== restoreStock merges allocations ====================

  @Test
  void cancelDocument_MultipleLines_SingleRestoreBatchesCallWithMergedAllocations() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line1 = new DocumentLine();
    line1.setId(1L);
    line1.setDocument(blDoc);
    line1.setProduct(testProduct);
    line1.setQuantity(new BigDecimal("10"));
    line1.setIsDelivered(true);
    line1.setBatchAllocations("{\"1\":10.000}");

    DocumentLine line2 = new DocumentLine();
    line2.setId(2L);
    line2.setDocument(blDoc);
    line2.setProduct(testProduct);
    line2.setQuantity(new BigDecimal("5"));
    line2.setIsDelivered(true);
    line2.setBatchAllocations("{\"1\":3.000,\"2\":2.000}");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line1, line2));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    documentService.cancelDocument(1L);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<Long, BigDecimal>> captor = ArgumentCaptor.forClass(Map.class);
    verify(productBatchService).restoreBatches(captor.capture());
    Map<Long, BigDecimal> restored = captor.getValue();
    assertEquals(new BigDecimal("13.000"), restored.get(1L));
    assertEquals(new BigDecimal("2.000"), restored.get(2L));
  }

  @Test
  void cancelDocument_MalformedBatchAllocations_Throws() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations("not-valid-json");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    assertThrows(RuntimeException.class, () -> documentService.cancelDocument(1L));
  }

  // ==================== updateDocumentLine unit cost recompute edge cases ====================

  @Test
  void updateDocumentLine_LineWithoutProduct_SkipsUnitCostRecompute() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(null);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(null);
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(testProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(variant);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertNull(result.getUnitCost());
    verify(productBatchService, never()).estimateAllocationFromVariant(any(), any());
  }

  @Test
  void updateDocumentLine_VariantChangeWithNoAveragePrice_UsesZero() {
    Product noPriceProduct = new Product();
    noPriceProduct.setId(7L);
    noPriceProduct.setAveragePurchasePrice(null);

    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(noPriceProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(noPriceProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(variant);

    when(productBatchService.estimateAllocationFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(new BigDecimal("0.000"), result.getUnitCost());
  }

  @Test
  void updateDocumentLine_ReplaceVariant_RecomputesUnitCost() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant oldVariant = new ProductVariant();
    oldVariant.setId(10L);
    oldVariant.setProduct(testProduct);
    existingLine.setVariant(oldVariant);

    ProductVariant newVariant = new ProductVariant();
    newVariant.setId(11L);
    newVariant.setProduct(testProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(newVariant);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("10"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocationFromVariant(eq(11L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(newVariant, result.getVariant());
    assertEquals(new BigDecimal("15.000"), result.getUnitCost());
    verify(productBatchService).estimateAllocationFromVariant(eq(11L), any(BigDecimal.class));
  }

  @Test
  void updateDocumentLine_ConditioningChange_EstimatesFromProduct() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningQuantityPerUnit(new BigDecimal("2"));

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("20"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(new BigDecimal("30.000"), result.getUnitCost());
    verify(productBatchService).estimateAllocation(eq(1L), any(BigDecimal.class));
  }

  @Test
  void updateDocumentLine_VariantChangeWithEmptyEstimate_FallsBackToAveragePrice() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setUnitCost(new BigDecimal("8.000"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(testProduct);

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setVariant(variant);

    when(productBatchService.estimateAllocationFromVariant(eq(10L), any(BigDecimal.class)))
        .thenReturn(Collections.emptyList());

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(new BigDecimal("15.000"), result.getUnitCost());
  }

  // ==================== cancelDocument BL without credit sale ====================

  @Test
  void cancelDocument_ValidatedBL_NonCreditSale_NoCreditAdjustment() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setIsCreditSale(false);
    blDoc.setClient(testClient);
    blDoc.setSourceDeliveryNoteId(null);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setIsDelivered(true);
    line.setBatchAllocations("{\"1\":10.000}");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.cancelDocument(1L);

    assertEquals(DocumentStatus.CANCELLED, blDoc.getStatus());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<Long, BigDecimal>> captor = ArgumentCaptor.forClass(Map.class);
    verify(productBatchService).restoreBatches(captor.capture());
    Map<Long, BigDecimal> restored = captor.getValue();
    assertEquals(new BigDecimal("10.000"), restored.get(1L));
    verify(clientService, never()).addCreditHistoryEntry(any(), any(), any(), any());
  }

  // ==================== isDelivery / delivery fee feature ====================

  @Test
  void createDocument_IsDeliveryTrue_NullTransportFee_ThrowsException() {
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setIsDelivery(true);
    bl.setTransportFee(null);
    bl.setVatRate(null);
    bl.setDocumentNumber(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(bl));
    assertEquals("Delivery documents require a transport fee to be specified", ex.getMessage());
  }

  @Test
  void createDocument_IsDeliveryTrue_CustomTransportFee() {
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setIsDelivery(true);
    bl.setTransportFee(new BigDecimal("25.000"));
    bl.setVatRate(null);
    bl.setDocumentNumber(null);

    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);
    when(documentRepository.save(any(Document.class))).thenReturn(bl);

    documentService.createDocument(bl);

    assertEquals(new BigDecimal("25.000"), bl.getTransportFee());
  }

  @Test
  void createDocument_IsDeliveryFalse_SetsTransportFeeNull() {
    Document invoice = new Document();
    invoice.setDocumentType(DocumentType.INVOICE);
    invoice.setIsDelivery(false);
    invoice.setTransportFee(new BigDecimal("15.000"));
    invoice.setVatRate(null);
    invoice.setDocumentNumber(null);

    when(documentRepository.getNextInvoiceSequence()).thenReturn(1L);
    when(documentRepository.save(any(Document.class))).thenReturn(invoice);

    documentService.createDocument(invoice);

    assertNull(invoice.getTransportFee());
  }

  @Test
  void recalculateDocumentTotals_IsDeliveryTrue_IncludesTransportFee() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setId(5L);
    invoiceDoc.setIsDelivery(true);
    invoiceDoc.setTransportFee(new BigDecimal("20.000"));
    invoiceDoc.setStampDuty(new BigDecimal("1.000"));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("100.00"));
    line.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    when(documentRepository.findById(5L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(5L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    Document updatedDetails = new Document();
    updatedDetails.setDate(invoiceDoc.getDate());
    updatedDetails.setDocumentType(DocumentType.INVOICE);
    updatedDetails.setIsDelivery(true);
    updatedDetails.setTransportFee(new BigDecimal("20.000"));
    updatedDetails.setStampDuty(new BigDecimal("1.000"));
    updatedDetails.setIsCreditSale(false);

    documentService.updateDocument(5L, updatedDetails);

    assertEquals(new BigDecimal("1020.000"), invoiceDoc.getTotalExcludingTax());
  }

  @Test
  void recalculateDocumentTotals_IsDeliveryFalse_ExcludesTransportFee() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setId(6L);
    invoiceDoc.setIsDelivery(false);
    invoiceDoc.setTransportFee(null);
    invoiceDoc.setStampDuty(new BigDecimal("1.000"));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("100.00"));
    line.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    when(documentRepository.findById(6L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(6L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    Document updatedDetails = new Document();
    updatedDetails.setDate(invoiceDoc.getDate());
    updatedDetails.setDocumentType(DocumentType.INVOICE);
    updatedDetails.setIsDelivery(false);
    updatedDetails.setTransportFee(null);
    updatedDetails.setStampDuty(new BigDecimal("1.000"));
    updatedDetails.setIsCreditSale(false);

    documentService.updateDocument(6L, updatedDetails);

    assertEquals(new BigDecimal("1000.000"), invoiceDoc.getTotalExcludingTax());
  }

  @Test
  void convertQuoteToDeliveryNote_CarryIsDeliveryTrue() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(20L);
    quote.setClient(testClient);
    quote.setIsDelivery(true);
    quote.setTransportFee(new BigDecimal("25.000"));

    DocumentLine quoteLine = new DocumentLine();
    quoteLine.setId(200L);
    quoteLine.setDocument(quote);
    quoteLine.setProduct(testProduct);
    quoteLine.setQuantity(new BigDecimal("5"));
    quoteLine.setUnitPrice(new BigDecimal("20.00"));
    quoteLine.setConditioningDescription(null);

    when(documentRepository.findById(20L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(20L)).thenReturn(Arrays.asList(quoteLine));

    Document savedBl = createDraftDocument(DocumentType.DELIVERY_NOTE);
    savedBl.setId(30L);
    savedBl.setClient(testClient);
    when(documentRepository.save(any(Document.class))).thenReturn(savedBl);
    when(documentRepository.findById(30L)).thenReturn(Optional.of(savedBl));
    when(documentRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(savedBl));
    when(documentLineRepository.findByDocumentId(30L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);

    documentService.convertQuoteToDeliveryNote(20L);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, atLeastOnce()).save(captor.capture());
    Document capturedBl =
        captor.getAllValues().stream()
            .filter(d -> d.getDocumentType() == DocumentType.DELIVERY_NOTE)
            .findFirst()
            .orElse(null);
    assertNotNull(capturedBl);
    assertTrue(capturedBl.getIsDelivery());
    assertEquals(new BigDecimal("25.000"), capturedBl.getTransportFee());
  }

  @Test
  void convertQuoteToDeliveryNote_CarryIsDeliveryFalse() {
    Document quote = createDraftDocument(DocumentType.QUOTE);
    quote.setId(21L);
    quote.setClient(testClient);
    quote.setIsDelivery(false);
    quote.setTransportFee(null);

    DocumentLine quoteLine = new DocumentLine();
    quoteLine.setId(201L);
    quoteLine.setDocument(quote);
    quoteLine.setProduct(testProduct);
    quoteLine.setQuantity(new BigDecimal("5"));
    quoteLine.setUnitPrice(new BigDecimal("20.00"));
    quoteLine.setConditioningDescription(null);

    when(documentRepository.findById(21L)).thenReturn(Optional.of(quote));
    when(documentLineRepository.findByDocumentId(21L)).thenReturn(Arrays.asList(quoteLine));

    Document savedBl = createDraftDocument(DocumentType.DELIVERY_NOTE);
    savedBl.setId(31L);
    savedBl.setClient(testClient);
    when(documentRepository.save(any(Document.class))).thenReturn(savedBl);
    when(documentRepository.findById(31L)).thenReturn(Optional.of(savedBl));
    when(documentRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(savedBl));
    when(documentLineRepository.findByDocumentId(31L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);

    documentService.convertQuoteToDeliveryNote(21L);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, atLeastOnce()).save(captor.capture());
    Document capturedBl =
        captor.getAllValues().stream()
            .filter(d -> d.getDocumentType() == DocumentType.DELIVERY_NOTE)
            .findFirst()
            .orElse(null);
    assertNotNull(capturedBl);
    assertFalse(capturedBl.getIsDelivery());
    assertNull(capturedBl.getTransportFee());
  }

  @Test
  void convertDeliveryNoteToInvoice_CarryIsDeliveryTrue() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(15L);
    blDoc.setClient(testClient);
    blDoc.setIsCreditSale(false);
    blDoc.setIsDelivery(true);
    blDoc.setTransportFee(new BigDecimal("15.000"));
    blDoc.setConvertedToInvoiceId(null);

    DocumentLine blLine = new DocumentLine();
    blLine.setId(150L);
    blLine.setDocument(blDoc);
    blLine.setProduct(testProduct);
    blLine.setQuantity(new BigDecimal("5"));
    blLine.setUnitPrice(new BigDecimal("20.00"));
    blLine.setConditioningDescription(null);
    blLine.setIsDelivered(true);

    when(documentRepository.findById(15L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(15L)).thenReturn(Arrays.asList(blLine));

    Document savedInvoice = createDraftDocument(DocumentType.INVOICE);
    savedInvoice.setId(35L);
    savedInvoice.setClient(testClient);
    when(documentRepository.save(any(Document.class))).thenReturn(savedInvoice);
    when(documentRepository.findById(35L)).thenReturn(Optional.of(savedInvoice));
    when(documentRepository.findByIdForUpdate(35L)).thenReturn(Optional.of(savedInvoice));
    when(documentLineRepository.findByDocumentId(35L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.getNextInvoiceSequence()).thenReturn(1L);

    documentService.convertDeliveryNoteToInvoice(15L);

    verify(documentRepository, atLeastOnce()).save(any(Document.class));
  }

  // ==================== orElseThrow partial branch coverage ====================

  @Test
  void addDocumentLine_DocumentNotFound_ThrowsException() {
    when(documentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                99L, testProduct, new BigDecimal("1"), new BigDecimal("10.00"), null, false, null));
  }

  @Test
  void convertQuoteToDeliveryNote_DocumentNotFound_ThrowsException() {
    when(documentRepository.findById(99L)).thenReturn(Optional.empty());

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> documentService.convertQuoteToDeliveryNote(99L));
    assertEquals("Quote not found", ex.getMessage());
  }

  @Test
  void convertDeliveryNoteToInvoice_DocumentNotFound_ThrowsException() {
    when(documentRepository.findById(99L)).thenReturn(Optional.empty());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> documentService.convertDeliveryNoteToInvoice(99L));
    assertEquals("Delivery Note not found", ex.getMessage());
  }

  @Test
  void deleteDocumentLine_DocumentNotFoundInRecalculate_ThrowsException() {
    Document draftDoc = createDraftDocument(DocumentType.QUOTE);
    draftDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(10L);
    line.setDocument(draftDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("1"));
    line.setUnitPrice(new BigDecimal("10.000"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(line));
    when(documentRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> documentService.deleteDocumentLine(1L));
  }

  @Test
  void updateDocumentLine_NullVatRate_UsesDefaultVatRate() {
    Document docNoVat = createDraftDocument(DocumentType.QUOTE);
    docNoVat.setVatRate(null);

    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(docNoVat);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.000"));
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("50.000"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.000"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    org.mockito.ArgumentCaptor<DocumentLine> captor =
        org.mockito.ArgumentCaptor.forClass(DocumentLine.class);
    when(documentLineRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(docNoVat));
    when(documentRepository.save(any(Document.class))).thenReturn(docNoVat);

    documentService.updateDocumentLine(1L, lineDetails);

    DocumentLine saved = captor.getValue();
    assertEquals(0, new BigDecimal("200.000").compareTo(saved.getTotalLineExcludingTax()));
    assertEquals(0, new BigDecimal("238.000").compareTo(saved.getTotalLineIncludingTax()));
  }

  // ==================== Patch coverage: null / default branches ====================

  @Test
  void addDocumentLine_NullVatRate_UsesDefaultVatRate() {
    Document docNoVat = createDraftDocument(DocumentType.QUOTE);
    docNoVat.setVatRate(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(docNoVat));
    when(documentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(docNoVat));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(docNoVat);

    ProductBatchService.BatchAllocation allocation =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5"),
            new BigDecimal("15.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now());
    when(productBatchService.estimateAllocation(eq(1L), any(BigDecimal.class)))
        .thenReturn(List.of(allocation));

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    assertEquals(0, new BigDecimal("50.000").compareTo(result.getTotalLineExcludingTax()));
    assertEquals(0, new BigDecimal("59.500").compareTo(result.getTotalLineIncludingTax()));
  }

  @Test
  void updateDocumentLine_WithConditioningQuantityPerUnit_SetsIt() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningQuantityPerUnit(new BigDecimal("3"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(0, new BigDecimal("3").compareTo(result.getConditioningQuantityPerUnit()));
  }

  @Test
  void updateDocumentLine_NullConditioningQuantityPerUnit_KeepsExistingValue() {
    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setLineNumber(1);
    existingLine.setConditioningQuantityPerUnit(new BigDecimal("4"));
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));
    lineDetails.setConditioningQuantityPerUnit(null);

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result = documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(new BigDecimal("4"), result.getConditioningQuantityPerUnit());
  }

  @Test
  void updateDocumentLine_IsDeliveryTrue_NullTransportFee_NoTransportFeeAdded() {
    Document docNoFee = createDraftDocument(DocumentType.INVOICE);
    docNoFee.setId(1L);
    docNoFee.setIsDelivery(true);
    docNoFee.setTransportFee(null);
    docNoFee.setStampDuty(new BigDecimal("1.000"));

    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(1L);
    existingLine.setDocument(docNoFee);
    existingLine.setProduct(testProduct);
    existingLine.setQuantity(new BigDecimal("5"));
    existingLine.setUnitPrice(new BigDecimal("10.00"));
    existingLine.setLineNumber(1);
    existingLine.setTotalLineExcludingTax(new BigDecimal("50.000"));
    existingLine.setTotalLineIncludingTax(new BigDecimal("59.500"));

    DocumentLine lineDetails = new DocumentLine();
    lineDetails.setQuantity(new BigDecimal("10"));
    lineDetails.setUnitPrice(new BigDecimal("20.00"));

    when(documentLineRepository.findById(1L)).thenReturn(Optional.of(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentRepository.findById(1L)).thenReturn(Optional.of(docNoFee));
    when(documentRepository.save(any(Document.class))).thenReturn(docNoFee);

    documentService.updateDocumentLine(1L, lineDetails);

    assertEquals(0, new BigDecimal("200.000").compareTo(docNoFee.getTotalExcludingTax()));
  }

  @Test
  void cancelDocument_BlankBatchAllocations_Throws() {
    Document blDoc = createValidatedDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setIsDelivered(true);
    line.setBatchAllocations("");

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));

    assertThrows(IllegalArgumentException.class, () -> documentService.cancelDocument(1L));
  }
}
