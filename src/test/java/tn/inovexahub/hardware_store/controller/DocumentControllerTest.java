package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.service.DocumentService;
import tn.inovexahub.hardware_store.service.PdfGenerationService;
import tn.inovexahub.hardware_store.service.ProductService;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

  @Mock private DocumentService documentService;
  @Mock private ProductService productService;
  @Mock private PdfGenerationService pdfGenerationService;

  private DocumentController documentController;

  @BeforeEach
  void setUp() {
    documentController =
        new DocumentController(documentService, productService, pdfGenerationService);
  }

  private Document createDocument(
      Long id, String number, DocumentType type, DocumentStatus status) {
    Document doc = new Document();
    doc.setId(id);
    doc.setDocumentNumber(number);
    doc.setDate(LocalDateTime.of(2024, 6, 15, 10, 0));
    doc.setDocumentType(type);
    doc.setStatus(status);
    doc.setVatRate(new BigDecimal("0.19"));
    doc.setTotalExcludingTax(new BigDecimal("100.000"));
    doc.setTotalVat(new BigDecimal("19.000"));
    doc.setTotalIncludingTax(new BigDecimal("119.000"));
    doc.setTransportFee(new BigDecimal("10.000"));
    doc.setStampDuty(new BigDecimal("1.000"));
    doc.setIsCreditSale(false);
    return doc;
  }

  private DocumentLine createDocumentLine(Long id, Integer lineNumber) {
    DocumentLine line = new DocumentLine();
    line.setId(id);
    line.setLineNumber(lineNumber);
    line.setQuantity(new BigDecimal("5.000"));
    line.setUnitPrice(new BigDecimal("20.000"));
    line.setTotalLineExcludingTax(new BigDecimal("100.000"));
    line.setTotalLineIncludingTax(new BigDecimal("119.000"));
    line.setIsDelivered(false);
    return line;
  }

  private Product createProduct(Long id, String name) {
    Product product = new Product();
    product.setId(id);
    product.setName(name);
    product.setReference("REF-" + id);
    product.setIsHeavyMaterial(false);
    product.setAveragePurchasePrice(new BigDecimal("15.000"));
    product.setStockQuantity(new BigDecimal("100.000"));
    return product;
  }

  // ==================== getAllDocuments ====================

  @Test
  void getAllDocuments_ReturnsOk() {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    when(documentService.getAllDocuments()).thenReturn(List.of(doc));

    ResponseEntity<List<Document>> response = documentController.getAllDocuments();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("DEV-000001", response.getBody().get(0).getDocumentNumber());
  }

  @Test
  void getAllDocuments_EmptyList_ReturnsOk() {
    when(documentService.getAllDocuments()).thenReturn(List.of());

    ResponseEntity<List<Document>> response = documentController.getAllDocuments();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0, response.getBody().size());
  }

  // ==================== getDocumentById ====================

  @Test
  void getDocumentById_Found_ReturnsOk() {
    Document doc =
        createDocument(1L, "BL-000001", DocumentType.DELIVERY_NOTE, DocumentStatus.DRAFT);
    when(documentService.getDocumentById(1L)).thenReturn(Optional.of(doc));

    ResponseEntity<Document> response = documentController.getDocumentById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("BL-000001", response.getBody().getDocumentNumber());
  }

  @Test
  void getDocumentById_NotFound_ReturnsNotFound() {
    when(documentService.getDocumentById(999L)).thenReturn(Optional.empty());

    ResponseEntity<Document> response = documentController.getDocumentById(999L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  // ==================== getDocumentByNumber ====================

  @Test
  void getDocumentByNumber_Found_ReturnsOk() {
    Document doc = createDocument(1L, "FAC-000001", DocumentType.INVOICE, DocumentStatus.VALIDATED);
    when(documentService.getDocumentByNumber("FAC-000001")).thenReturn(Optional.of(doc));

    ResponseEntity<Document> response = documentController.getDocumentByNumber("FAC-000001");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("FAC-000001", response.getBody().getDocumentNumber());
    assertEquals(DocumentType.INVOICE, response.getBody().getDocumentType());
  }

  @Test
  void getDocumentByNumber_NotFound_ReturnsNotFound() {
    when(documentService.getDocumentByNumber("NONEXISTENT")).thenReturn(Optional.empty());

    ResponseEntity<Document> response = documentController.getDocumentByNumber("NONEXISTENT");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  // ==================== createDocument ====================

  @Test
  void createDocument_Valid_ReturnsCreated() {
    Document input = createDocument(null, null, DocumentType.QUOTE, null);
    Document saved = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    when(documentService.createDocument(any(Document.class))).thenReturn(saved);

    ResponseEntity<Document> response = documentController.createDocument(input);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
    assertEquals("DEV-000001", response.getBody().getDocumentNumber());
    verify(documentService).createDocument(input);
  }

  // ==================== updateDocument ====================

  @Test
  void updateDocument_Success_ReturnsOk() {
    Document details = createDocument(null, null, DocumentType.QUOTE, DocumentStatus.DRAFT);
    Document updated = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    when(documentService.updateDocument(eq(1L), any(Document.class))).thenReturn(updated);

    ResponseEntity<Document> response = documentController.updateDocument(1L, details);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("DEV-000001", response.getBody().getDocumentNumber());
  }

  @Test
  void updateDocument_NotFound_ThrowsNotFound() {
    Document details = createDocument(null, null, DocumentType.QUOTE, DocumentStatus.DRAFT);
    when(documentService.updateDocument(eq(999L), any(Document.class)))
        .thenThrow(new RuntimeException("Document not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> documentController.updateDocument(999L, details));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== deleteDocument ====================

  @Test
  void deleteDocument_Success_ReturnsNoContent() {
    ResponseEntity<Void> response = documentController.deleteDocument(1L);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(documentService).deleteDocument(1L);
  }

  @Test
  void deleteDocument_NotFound_ThrowsNotFound() {
    doThrow(new RuntimeException("Document not found")).when(documentService).deleteDocument(999L);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> documentController.deleteDocument(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== getDocumentLines ====================

  @Test
  void getDocumentLines_ReturnsOk() {
    DocumentLine line1 = createDocumentLine(1L, 1);
    DocumentLine line2 = createDocumentLine(2L, 2);
    when(documentService.getDocumentLines(1L)).thenReturn(List.of(line1, line2));

    ResponseEntity<List<DocumentLine>> response = documentController.getDocumentLines(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    assertEquals(1, response.getBody().get(0).getLineNumber());
    assertEquals(2, response.getBody().get(1).getLineNumber());
  }

  // ==================== addDocumentLine ====================

  @Test
  void addDocumentLine_Success_ReturnsCreated() {
    Product product = createProduct(10L, "Hammer");
    DocumentLine savedLine = createDocumentLine(1L, 1);
    savedLine.setProduct(product);

    when(productService.getProductById(10L)).thenReturn(Optional.of(product));
    when(documentService.addDocumentLine(
            eq(1L), eq(product), eq(new BigDecimal("5")), any(), any(), any(), any()))
        .thenReturn(savedLine);

    ResponseEntity<DocumentLine> response =
        documentController.addDocumentLine(
            1L, 10L, new BigDecimal("5"), new BigDecimal("20.000"), "Box", true, null);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Hammer", response.getBody().getProduct().getName());
  }

  @Test
  void addDocumentLine_ProductNotFound_ThrowsBadRequest() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                documentController.addDocumentLine(
                    1L, 999L, new BigDecimal("5"), new BigDecimal("20.000"), null, null, null));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void addDocumentLine_ServiceThrows_ThrowsBadRequest() {
    Product product = createProduct(10L, "Hammer");
    when(productService.getProductById(10L)).thenReturn(Optional.of(product));
    when(documentService.addDocumentLine(
            eq(1L), eq(product), eq(new BigDecimal("5")), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Only DRAFT documents can have lines added"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                documentController.addDocumentLine(
                    1L, 10L, new BigDecimal("5"), new BigDecimal("20.000"), null, null, null));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Only DRAFT documents can have lines added", ex.getReason());
  }

  // ==================== updateDocumentLine ====================

  @Test
  void updateDocumentLine_Success_ReturnsOk() {
    DocumentLine details = createDocumentLine(null, null);
    DocumentLine updatedLine = createDocumentLine(1L, 1);
    when(documentService.updateDocumentLine(eq(1L), any(DocumentLine.class)))
        .thenReturn(updatedLine);

    ResponseEntity<DocumentLine> response = documentController.updateDocumentLine(1L, details);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().getId());
  }

  @Test
  void updateDocumentLine_NotFound_ThrowsNotFound() {
    DocumentLine details = createDocumentLine(null, null);
    when(documentService.updateDocumentLine(eq(999L), any(DocumentLine.class)))
        .thenThrow(new RuntimeException("Document line not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> documentController.updateDocumentLine(999L, details));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== deleteDocumentLine ====================

  @Test
  void deleteDocumentLine_Success_ReturnsNoContent() {
    ResponseEntity<Void> response = documentController.deleteDocumentLine(1L);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(documentService).deleteDocumentLine(1L);
  }

  @Test
  void deleteDocumentLine_NotFound_ThrowsNotFound() {
    doThrow(new RuntimeException("Document line not found"))
        .when(documentService)
        .deleteDocumentLine(999L);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> documentController.deleteDocumentLine(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== validateDocument ====================

  @Test
  void validateDocument_Success_ReturnsOk() {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.VALIDATED);
    when(documentService.validateDocument(1L)).thenReturn(doc);

    ResponseEntity<Document> response = documentController.validateDocument(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(DocumentStatus.VALIDATED, response.getBody().getStatus());
  }

  @Test
  void validateDocument_BadRequest_ThrowsBadRequest() {
    when(documentService.validateDocument(1L))
        .thenThrow(new RuntimeException("Only DRAFT documents can be validated"));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> documentController.validateDocument(1L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== cancelDocument ====================

  @Test
  void cancelDocument_Success_ReturnsOk() {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.CANCELLED);
    when(documentService.cancelDocument(1L)).thenReturn(doc);

    ResponseEntity<Document> response = documentController.cancelDocument(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(DocumentStatus.CANCELLED, response.getBody().getStatus());
  }

  @Test
  void cancelDocument_BadRequest_ThrowsBadRequest() {
    when(documentService.cancelDocument(1L))
        .thenThrow(new RuntimeException("Document is already cancelled"));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> documentController.cancelDocument(1L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== convertQuoteToDeliveryNote ====================

  @Test
  void convertQuoteToDeliveryNote_Success_ReturnsCreated() {
    Document bl = createDocument(2L, "BL-000001", DocumentType.DELIVERY_NOTE, DocumentStatus.DRAFT);
    when(documentService.convertQuoteToDeliveryNote(1L)).thenReturn(bl);

    ResponseEntity<Document> response = documentController.convertQuoteToDeliveryNote(1L);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("BL-000001", response.getBody().getDocumentNumber());
    assertEquals(DocumentType.DELIVERY_NOTE, response.getBody().getDocumentType());
  }

  @Test
  void convertQuoteToDeliveryNote_BadRequest_ThrowsBadRequest() {
    when(documentService.convertQuoteToDeliveryNote(999L))
        .thenThrow(new RuntimeException("Quote not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> documentController.convertQuoteToDeliveryNote(999L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== convertDeliveryNoteToInvoice ====================

  @Test
  void convertDeliveryNoteToInvoice_Success_ReturnsCreated() {
    Document invoice =
        createDocument(3L, "FAC-000001", DocumentType.INVOICE, DocumentStatus.VALIDATED);
    when(documentService.convertDeliveryNoteToInvoice(2L)).thenReturn(invoice);

    ResponseEntity<Document> response = documentController.convertDeliveryNoteToInvoice(2L);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("FAC-000001", response.getBody().getDocumentNumber());
    assertEquals(DocumentType.INVOICE, response.getBody().getDocumentType());
  }

  @Test
  void convertDeliveryNoteToInvoice_BadRequest_ThrowsBadRequest() {
    when(documentService.convertDeliveryNoteToInvoice(999L))
        .thenThrow(new RuntimeException("Delivery Note not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> documentController.convertDeliveryNoteToInvoice(999L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== getDocumentsByClient ====================

  @Test
  void getDocumentsByClient_ReturnsOk() {
    Document doc =
        createDocument(1L, "BL-000001", DocumentType.DELIVERY_NOTE, DocumentStatus.VALIDATED);
    when(documentService.getDocumentsByClient(1L)).thenReturn(List.of(doc));

    ResponseEntity<List<Document>> response = documentController.getDocumentsByClient(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(documentService).getDocumentsByClient(1L);
  }

  // ==================== getDocumentsByUser ====================

  @Test
  void getDocumentsByUser_ReturnsOk() {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    when(documentService.getDocumentsByUser(1L)).thenReturn(List.of(doc));

    ResponseEntity<List<Document>> response = documentController.getDocumentsByUser(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(documentService).getDocumentsByUser(1L);
  }

  // ==================== getDocumentsByType ====================

  @Test
  void getDocumentsByType_ReturnsOk() {
    Document doc1 = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    Document doc2 = createDocument(2L, "DEV-000002", DocumentType.QUOTE, DocumentStatus.VALIDATED);
    when(documentService.getDocumentsByType(DocumentType.QUOTE)).thenReturn(List.of(doc1, doc2));

    ResponseEntity<List<Document>> response =
        documentController.getDocumentsByType(DocumentType.QUOTE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
  }

  // ==================== getDocumentsByStatus ====================

  @Test
  void getDocumentsByStatus_ReturnsOk() {
    Document doc =
        createDocument(1L, "BL-000001", DocumentType.DELIVERY_NOTE, DocumentStatus.VALIDATED);
    when(documentService.getDocumentsByStatus(DocumentStatus.VALIDATED)).thenReturn(List.of(doc));

    ResponseEntity<List<Document>> response =
        documentController.getDocumentsByStatus(DocumentStatus.VALIDATED);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(DocumentStatus.VALIDATED, response.getBody().get(0).getStatus());
  }

  // ==================== getCreditSalesByClient ====================

  @Test
  void getCreditSalesByClient_ReturnsOk() {
    Document doc = createDocument(1L, "FAC-000001", DocumentType.INVOICE, DocumentStatus.VALIDATED);
    doc.setIsCreditSale(true);
    when(documentService.getCreditSalesByClient(1L)).thenReturn(List.of(doc));

    ResponseEntity<List<Document>> response = documentController.getCreditSalesByClient(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(true, response.getBody().get(0).getIsCreditSale());
  }

  // ==================== generateDocumentPdf ====================

  @Test
  void generateDocumentPdf_Success_ReturnsPdfBytes() throws IOException {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);
    byte[] pdfBytes = new byte[] {1, 2, 3, 4, 5};

    when(documentService.getDocumentById(1L)).thenReturn(Optional.of(doc));
    when(pdfGenerationService.generateDocumentPdf(doc)).thenReturn(pdfBytes);

    ResponseEntity<byte[]> response = documentController.generateDocumentPdf(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
    assertArrayEquals(pdfBytes, response.getBody());
    assertNotNull(response.getHeaders().getContentDisposition());
  }

  @Test
  void generateDocumentPdf_DocumentNotFound_ThrowsNotFound() {
    when(documentService.getDocumentById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> documentController.generateDocumentPdf(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void generateDocumentPdf_PdfFails_ThrowsInternalServerError() throws IOException {
    Document doc = createDocument(1L, "DEV-000001", DocumentType.QUOTE, DocumentStatus.DRAFT);

    when(documentService.getDocumentById(1L)).thenReturn(Optional.of(doc));
    when(pdfGenerationService.generateDocumentPdf(doc))
        .thenThrow(new IOException("PDF generation failed"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> documentController.generateDocumentPdf(1L));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    assertEquals("Failed to generate PDF", ex.getReason());
  }
}
