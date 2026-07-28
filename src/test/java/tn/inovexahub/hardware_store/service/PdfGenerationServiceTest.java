package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;

class PdfGenerationServiceTest {

  private PdfGenerationService pdfService;

  @BeforeEach
  void setUp() {
    pdfService = new PdfGenerationService();
  }

  @Test
  void generateDocumentPdf_Quote_ReturnsValidPdf() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    Product product1 = createTestProduct("Marteau");
    Product product2 = createTestProduct("Tournevis");
    doc.setLines(List.of(createTestLine(1, product1, null), createTestLine(2, product2, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("DEVIS"), "PDF should contain 'DEVIS'");
    assertTrue(text.contains("DEV-000001"), "PDF should contain document number");
    assertTrue(text.contains("Ahmed Ben Ali"), "PDF should contain client name");
  }

  @Test
  void generateDocumentPdf_DeliveryNote_ContainsTransportFee() throws IOException {
    Document doc = createTestDocument(DocumentType.DELIVERY_NOTE, true, false);
    Product product = createTestProduct("Vis M6");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("BON DE LIVRAISON"), "PDF should contain 'BON DE LIVRAISON'");
    assertTrue(text.contains("Frais Transport"), "PDF should contain 'Frais Transport'");
  }

  @Test
  void generateDocumentPdf_Invoice_ContainsStampDutyAndCreditSale() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, true);
    Product product = createTestProduct("Cable electrique");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("FACTURE"), "PDF should contain 'FACTURE'");
    assertTrue(text.contains("Droit Timbre"), "PDF should contain 'Droit Timbre'");
    assertTrue(text.contains("cr\u00e9dit"), "PDF should contain 'cr\u00e9dit'");
  }

  @Test
  void generateDocumentPdf_NullClient_SkipsClientSection() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Product product = createTestProduct("Perceuse");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    String text = extractTextFromPdf(pdfBytes);
    assertFalse(text.contains("Client:"), "PDF should not contain 'Client:' when client is null");
  }

  @Test
  void generateDocumentPdf_ClientWithAllFields_ShowsAllInfo() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    Product product = createTestProduct("Clou");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Client:"), "PDF should contain 'Client:'");
    assertTrue(text.contains("Ahmed Ben Ali"), "PDF should contain client name");
    assertTrue(text.contains("123 Rue de la Paix, Tunis"), "PDF should contain client address");
    assertTrue(text.contains("T\u00e9l: +216 20 123 456"), "PDF should contain phone");
    assertTrue(
        text.contains("Matricule Fiscal: 123456789"), "PDF should contain tax identification");
  }

  @Test
  void generateDocumentPdf_ClientWithPartialFields_ShowsPartialInfo() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Client client = new Client();
    client.setId(2L);
    client.setName("Societe ABC");
    doc.setClient(client);

    Product product = createTestProduct("Boulon");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Societe ABC"), "PDF should contain client name");
    assertFalse(
        text.contains("T\u00e9l:"), "PDF should not contain 'T\u00e9l:' when phone is null");
    assertFalse(
        text.contains("Matricule Fiscal:"),
        "PDF should not contain 'Matricule Fiscal:' when taxId is null");
  }

  @Test
  void generateDocumentPdf_NullProductLine_ShowsNA() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    DocumentLine line = createTestLine(1, null, null);
    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("N/A"), "PDF should contain 'N/A' when product is null");
  }

  @Test
  void generateDocumentPdf_NullProductLine_WithConditioningDescription_ShowsDescription()
      throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    DocumentLine line = createTestLine(1, null, "Rouleau");
    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Rouleau"), "PDF should contain conditioning description");
    assertFalse(text.contains("N/A"), "PDF should not contain 'N/A' when description is present");
  }

  @Test
  void generateDocumentPdf_ManyLines_NoPagination() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    List<DocumentLine> lines = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      Product product = createTestProduct("Produit " + i);
      lines.add(createTestLine(i, product, null));
    }
    doc.setLines(lines);

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    int pageCount = getPageCount(pdfBytes);
    assertEquals(1, pageCount);
  }

  @Test
  void generateDocumentPdf_ManyLines_PaginatesAcrossTwoPages() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    List<DocumentLine> lines = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      Product product = createTestProduct("Produit " + i);
      lines.add(createTestLine(i, product, null));
    }
    doc.setLines(lines);

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    int pageCount = getPageCount(pdfBytes);
    assertEquals(2, pageCount);
  }

  @Test
  void generateDocumentPdf_ManyLines_BoundarySeventeen_StaysOnOnePage() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    List<DocumentLine> lines = new ArrayList<>();
    for (int i = 1; i <= 17; i++) {
      Product product = createTestProduct("Produit " + i);
      lines.add(createTestLine(i, product, null));
    }
    doc.setLines(lines);

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    int pageCount = getPageCount(pdfBytes);
    assertEquals(1, pageCount);
  }

  @Test
  void generateDocumentPdf_Invoice_NullTransportAndStampDuty_StillGenerates() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setTransportFee(null);
    doc.setStampDuty(null);
    Product product = createTestProduct("Ampoule LED");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("FACTURE"), "PDF should still contain 'FACTURE'");
  }

  private Document createTestDocument(DocumentType type, boolean withClient, boolean creditSale) {
    Document doc = new Document();
    doc.setId(1L);
    doc.setDocumentNumber("DEV-000001");
    doc.setDocumentType(type);
    doc.setStatus(DocumentStatus.DRAFT);
    doc.setDate(LocalDateTime.of(2024, 6, 15, 10, 30));
    doc.setVatRate(new BigDecimal("0.19"));
    doc.setTransportFee(type != DocumentType.QUOTE ? new BigDecimal("10.000") : null);
    doc.setStampDuty(type == DocumentType.INVOICE ? new BigDecimal("1.000") : null);
    doc.setIsCreditSale(creditSale);
    doc.setTotalExcludingTax(new BigDecimal("1000.000"));
    doc.setTotalVat(new BigDecimal("190.000"));
    doc.setTotalIncludingTax(new BigDecimal("1191.000"));

    if (withClient) {
      Client client = new Client();
      client.setId(1L);
      client.setName("Ahmed Ben Ali");
      client.setAddress("123 Rue de la Paix, Tunis");
      client.setPhone("+216 20 123 456");
      client.setTaxIdentificationNumber("123456789");
      doc.setClient(client);
    }

    return doc;
  }

  private DocumentLine createTestLine(int lineNumber, Product product, String conditioningDesc) {
    DocumentLine line = new DocumentLine();
    line.setLineNumber(lineNumber);
    line.setProduct(product);
    line.setConditioningDescription(conditioningDesc);
    line.setQuantity(new BigDecimal("10.000"));
    line.setUnitPrice(new BigDecimal("100.000"));
    line.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line.setTotalLineIncludingTax(new BigDecimal("1190.000"));
    line.setIsDelivered(false);
    return line;
  }

  private Product createTestProduct(String name) {
    Product product = new Product();
    product.setId(1L);
    product.setName(name);
    return product;
  }

  private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      return new PDFTextStripper().getText(document);
    }
  }

  private int getPageCount(byte[] pdfBytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      return document.getNumberOfPages();
    }
  }

  @Test
  void generateDocumentPdf_LongProductName_TruncatesText() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    Product longNameProduct = createTestProduct("A".repeat(200));
    doc.setLines(List.of(createTestLine(1, longNameProduct, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    // Should not throw - truncation handles long names
  }

  @Test
  void generateDocumentPdf_ClientWithOnlyName_ShowsNameOnly() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Client client = new Client();
    client.setId(3L);
    client.setName("Minimal Client");
    client.setAddress(null);
    client.setPhone(null);
    client.setTaxIdentificationNumber(null);
    doc.setClient(client);

    Product product = createTestProduct("Simple Product");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Minimal Client"));
    assertFalse(text.contains("T\u00e9l:"));
    assertFalse(text.contains("Matricule Fiscal:"));
  }
}
