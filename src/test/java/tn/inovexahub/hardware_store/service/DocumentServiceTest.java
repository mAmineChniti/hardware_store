package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
  @Mock private ProductService productService;
  @Mock private ProductConditioningRepository productConditioningRepository;

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
    testProduct.setIsHeavyMaterial(false);

    testDocument = createDraftDocument(DocumentType.QUOTE);
  }

  private Document createDraftDocument(DocumentType type) {
    Document doc = new Document();
    doc.setId(1L);
    doc.setDocumentNumber("TEST-001");
    doc.setDocumentType(type);
    doc.setStatus(DocumentStatus.DRAFT);
    doc.setDate(LocalDateTime.now());
    doc.setVatRate(new BigDecimal("0.19"));
    doc.setTransportFee(new BigDecimal("10.000"));
    doc.setStampDuty(new BigDecimal("1.000"));
    doc.setIsCreditSale(false);
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
    product.setIsHeavyMaterial(false);
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
    assertEquals(new BigDecimal("0.19"), quote.getVatRate());
    assertEquals(DocumentStatus.DRAFT, quote.getStatus());
    assertNotNull(quote.getDate());
    assertNotNull(quote.getDocumentNumber());
    assertTrue(quote.getDocumentNumber().startsWith("DEV-"));
    verify(documentRepository).save(quote);
  }

  @Test
  void createDocument_DeliveryNote_SetsDefaultTransportFee() {
    Document bl = new Document();
    bl.setDocumentType(DocumentType.DELIVERY_NOTE);
    bl.setVatRate(null);
    bl.setTransportFee(null);
    bl.setDocumentNumber(null);

    when(documentRepository.getNextDeliveryNoteSequence()).thenReturn(1L);
    when(documentRepository.save(any(Document.class))).thenReturn(bl);

    Document result = documentService.createDocument(bl);

    assertNotNull(result);
    assertEquals(new BigDecimal("10.000"), bl.getTransportFee());
    assertEquals(new BigDecimal("0.19"), bl.getVatRate());
    assertNotNull(bl.getDocumentNumber());
    assertTrue(bl.getDocumentNumber().startsWith("BL-"));
    verify(documentRepository).save(bl);
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
    assertEquals(new BigDecimal("0.19"), invoice.getVatRate());
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
  void updateDocument_ChangeToBL_SetsDefaultTransportFee() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    Document updatedDetails = new Document();
    updatedDetails.setDate(LocalDateTime.now());
    updatedDetails.setDocumentType(DocumentType.DELIVERY_NOTE);
    updatedDetails.setTransportFee(null);

    documentService.updateDocument(1L, updatedDetails);

    assertEquals(new BigDecimal("10.000"), blDoc.getTransportFee());
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
  void validateDocument_Invoice_DeductsStock() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setClient(testClient);
    invoiceDoc.setIsCreditSale(false);

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(invoiceDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.validateDocument(1L);

    verify(productService).updateStockQuantity(1L, new BigDecimal("-10"));
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
    verify(productService, never()).updateStockQuantity(any(), any());
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

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.cancelDocument(1L);

    verify(productService).updateStockQuantity(1L, new BigDecimal("10"));
    assertFalse(line.getIsDelivered());
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

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    verify(productService).updateStockQuantity(1L, new BigDecimal("5"));
  }

  @Test
  void cancelDocument_ConvertedInvoice_SkipsStockRestore() {
    Document invoiceDoc = createValidatedDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setSourceDeliveryNoteId(5L);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));
    when(documentRepository.save(any(Document.class))).thenReturn(invoiceDoc);

    documentService.cancelDocument(1L);

    verify(productService, never()).updateStockQuantity(any(), any());
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
    verify(productService, never()).updateStockQuantity(any(), any());
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
  void addDocumentLine_HeavyMaterial_Delivered_UsesPriceDelivered() {
    testProduct.setIsHeavyMaterial(true);
    testProduct.setPriceDelivered(new BigDecimal("40.00"));
    testProduct.setPriceOnSite(new BigDecimal("35.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), null, null, true, null);

    assertEquals(new BigDecimal("40.00"), result.getUnitPrice());
  }

  @Test
  void addDocumentLine_HeavyMaterial_OnSite_UsesPriceOnSite() {
    testProduct.setIsHeavyMaterial(true);
    testProduct.setPriceDelivered(new BigDecimal("40.00"));
    testProduct.setPriceOnSite(new BigDecimal("35.00"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

    DocumentLine result =
        documentService.addDocumentLine(
            1L, testProduct, new BigDecimal("5"), null, null, false, null);

    assertEquals(new BigDecimal("35.00"), result.getUnitPrice());
  }

  @Test
  void addDocumentLine_NegativePrice_ThrowsException() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), new BigDecimal("-1"), null, false, null));
  }

  @Test
  void addDocumentLine_NullPrice_NoHeavyMaterial_ThrowsException() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    assertThrows(
        RuntimeException.class,
        () ->
            documentService.addDocumentLine(
                1L, testProduct, new BigDecimal("5"), null, null, false, null));
  }

  @Test
  void addDocumentLine_WithExistingLines_SetsCorrectLineNumber() {
    when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

    DocumentLine existingLine = new DocumentLine();
    existingLine.setId(10L);
    existingLine.setLineNumber(3);
    existingLine.setDocument(testDocument);
    existingLine.setProduct(testProduct);

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(existingLine));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

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

  // ==================== Recalculate Document Totals Tests ====================

  @Test
  void addDocumentLine_BL_RecalculatesWithTransportFee() {
    Document blDoc = createDraftDocument(DocumentType.DELIVERY_NOTE);
    blDoc.setId(1L);
    blDoc.setTransportFee(new BigDecimal("10.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(blDoc));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(blDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("10"));
    line.setUnitPrice(new BigDecimal("20.00"));
    line.setLineNumber(1);
    line.setTotalLineExcludingTax(new BigDecimal("200.000"));
    line.setTotalLineIncludingTax(new BigDecimal("238.000"));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(blDoc);

    documentService.addDocumentLine(
        1L, testProduct, new BigDecimal("10"), new BigDecimal("20.00"), null, false, null);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, atLeastOnce()).save(captor.capture());

    Document savedDoc =
        captor.getAllValues().stream().filter(d -> d.getId().equals(1L)).findFirst().orElse(null);

    assertNotNull(savedDoc);
    BigDecimal expectedTotal = new BigDecimal("200.000").add(new BigDecimal("10.000"));
    assertEquals(0, expectedTotal.compareTo(savedDoc.getTotalExcludingTax()));
  }

  @Test
  void addDocumentLine_Invoice_RecalculatesWithTransportFeeAndStampDuty() {
    Document invoiceDoc = createDraftDocument(DocumentType.INVOICE);
    invoiceDoc.setId(1L);
    invoiceDoc.setTransportFee(new BigDecimal("10.000"));
    invoiceDoc.setStampDuty(new BigDecimal("1.000"));

    when(documentRepository.findById(1L)).thenReturn(Optional.of(invoiceDoc));

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
    verify(productService, never()).updateStockQuantity(any(), any());
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
    verify(productService, never()).updateStockQuantity(any(), any());
  }

  // ==================== Recalculate Totals Quote Tests ====================

  @Test
  void addDocumentLine_Quote_RecalculatesWithoutTransportFeeOrStampDuty() {
    Document quoteDoc = createDraftDocument(DocumentType.QUOTE);
    quoteDoc.setId(1L);
    quoteDoc.setTransportFee(null);
    quoteDoc.setStampDuty(null);

    when(documentRepository.findById(1L)).thenReturn(Optional.of(quoteDoc));

    DocumentLine line = new DocumentLine();
    line.setId(1L);
    line.setDocument(quoteDoc);
    line.setProduct(testProduct);
    line.setQuantity(new BigDecimal("5"));
    line.setUnitPrice(new BigDecimal("10.00"));
    line.setLineNumber(1);
    line.setTotalLineExcludingTax(new BigDecimal("50.000"));
    line.setTotalLineIncludingTax(new BigDecimal("59.500"));

    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line));
    when(documentLineRepository.save(any(DocumentLine.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(documentRepository.save(any(Document.class))).thenReturn(quoteDoc);

    documentService.addDocumentLine(
        1L, testProduct, new BigDecimal("5"), new BigDecimal("10.00"), null, false, null);

    ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, atLeastOnce()).save(captor.capture());

    Document savedDoc =
        captor.getAllValues().stream().filter(d -> d.getId().equals(1L)).findFirst().orElse(null);

    assertNotNull(savedDoc);
    assertEquals(0, BigDecimal.valueOf(50).compareTo(savedDoc.getTotalExcludingTax()));
  }
}
