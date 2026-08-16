package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import tn.inovexahub.hardware_store.entity.PaymentReceipt;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.entity.User;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.enums.PaymentMethod;
import tn.inovexahub.hardware_store.enums.UserRole;

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
    assertTrue(
        text.contains("TVA (19.00%"), "PDF should contain the VAT label with the rate in percent");
  }

  @Test
  void generateDocumentPdf_NullVatRate_ShowsDefaultTvaLabel() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setVatRate(null);
    Product product = createTestProduct("Cable electrique");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(
        text.contains("TVA (19.00%"),
        "PDF should show the default 19.00% VAT label when the rate is null");
  }

  @Test
  void generateDocumentPdf_FractionalVatRate_NormalizesTvaLabel() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setVatRate(new BigDecimal("0.19"));
    Product product = createTestProduct("Cable electrique");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(
        text.contains("TVA (19.00%"), "PDF should normalize fractional 0.19 to a 19.00% VAT label");
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
    doc.setVatRate(new BigDecimal("19.00"));
    boolean isDelivery = type != DocumentType.QUOTE;
    doc.setIsDelivery(isDelivery);
    doc.setTransportFee(isDelivery ? new BigDecimal("10.000") : null);
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

  @Test
  void generateDocumentPdf_WithVariant_ShowsVariantName() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Product product = createTestProduct("Screw");

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setVariantName("6mm Stainless");

    DocumentLine line = createTestLine(1, product, null);
    line.setVariant(variant);
    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Screw - 6mm Stainless"));
  }

  @Test
  void generateDocumentPdf_ProductWithVariantNullVariantName_ShowsProductNameOnly()
      throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Product product = createTestProduct("Screw");

    ProductVariant variant = new ProductVariant();
    variant.setId(10L);
    variant.setVariantName(null);

    DocumentLine line = createTestLine(1, product, null);
    line.setVariant(variant);
    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Screw"), "PDF should contain product name without variant suffix");
    assertFalse(
        text.contains("Screw -"),
        "PDF should not append a variant separator when variantName is null");
  }

  @Test
  void generateDocumentPdf_ProductWithoutVariant_ShowsProductName() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    Product product = createTestProduct("Vis Inox");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Vis Inox"), "PDF should contain product name");
  }

  // --- generatePaymentReceiptPdf ---

  @Test
  void generatePaymentReceiptPdf_WithUser_ShowsProcessedBy() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("REÇU DE PAIEMENT"), "PDF should contain receipt title");
    assertTrue(text.contains("REC-000001"), "PDF should contain receipt number");
    assertTrue(text.contains("Ahmed Ben Ali"), "PDF should contain client name");
    assertTrue(text.contains("Karim"), "PDF should contain user first name");
    assertTrue(text.contains("Ben Ahmed"), "PDF should contain user last name");
    assertTrue(text.contains("Traité par:"), "PDF should contain 'Traité par:'");
    assertTrue(text.contains("Montant payé"), "PDF should contain 'Montant payé'");
    assertTrue(text.contains("Dette précédente"), "PDF should contain 'Dette précédente'");
    assertTrue(text.contains("Dette restante"), "PDF should contain 'Dette restante'");
  }

  @Test
  void generatePaymentReceiptPdf_NullUser_ShowsNA() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(false);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    assertNotNull(pdfBytes);
    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("N/A"), "PDF should show 'N/A' when user is null");
  }

  @Test
  void generatePaymentReceiptPdf_ClientWithAllFields_ShowsAllInfo() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("123 Rue de la Paix, Tunis"), "PDF should contain client address");
    assertTrue(text.contains("Tél: +216 20 123 456"), "PDF should contain phone");
    assertTrue(
        text.contains("Matricule Fiscal: 123456789"), "PDF should contain tax identification");
  }

  @Test
  void generatePaymentReceiptPdf_PaymentMethod_ShowsMethod() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("CASH"), "PDF should contain payment method");
  }

  @Test
  void generatePaymentReceiptPdf_FooterContainsCompanyAndTimestamp() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("INOVEXAHUB"), "PDF should contain company name in footer");
    assertTrue(
        text.contains("Document généré automatiquement"),
        "PDF should contain auto-generated notice");
  }

  @Test
  void generatePaymentReceiptPdf_NullClient_OmitsClientSection() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);
    receipt.setClient(null);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("REÇU DE PAIEMENT"), "PDF should still show title");
    assertFalse(text.contains("Client:"), "PDF should not show 'Client:' when client is null");
  }

  @Test
  void generatePaymentReceiptPdf_ClientWithOnlyName_OmitsOptionalFields() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);
    Client client = new Client();
    client.setId(2L);
    client.setName("Minimal Client");
    receipt.setClient(client);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    assertNotNull(pdfBytes);
    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Minimal Client"));
    assertFalse(text.contains("123 Rue de la Paix"), "address should be omitted when null");
    assertFalse(text.contains("Tél:"), "phone should be omitted when null");
    assertFalse(text.contains("Matricule Fiscal:"), "tax id should be omitted when null");
  }

  @Test
  void generatePaymentReceiptPdf_NullAmountPaid_ThrowsIllegalArgument() {
    PaymentReceipt receipt = createTestPaymentReceipt(true);
    receipt.setAmountPaid(null);

    assertThrows(
        IllegalArgumentException.class, () -> pdfService.generatePaymentReceiptPdf(receipt));
  }

  private PaymentReceipt createTestPaymentReceipt(boolean withUser) {
    PaymentReceipt receipt = new PaymentReceipt();
    receipt.setId(1L);
    receipt.setReceiptNumber("REC-000001");
    receipt.setAmountPaid(new BigDecimal("200.000"));
    receipt.setPaymentDate(LocalDateTime.of(2024, 6, 15, 10, 30));
    receipt.setPaymentMethod(PaymentMethod.CASH);
    receipt.setPreviousDebt(new BigDecimal("500.000"));
    receipt.setNewDebt(new BigDecimal("300.000"));

    Client client = new Client();
    client.setId(1L);
    client.setName("Ahmed Ben Ali");
    client.setAddress("123 Rue de la Paix, Tunis");
    client.setPhone("+216 20 123 456");
    client.setTaxIdentificationNumber("123456789");
    receipt.setClient(client);

    if (withUser) {
      User user = new User();
      user.setId(1L);
      user.setFirstName("Karim");
      user.setLastName("Ben Ahmed");
      user.setEmail("ahmed@example.com");
      user.setRole(UserRole.EMPLOYEE);
      receipt.setUser(user);
    }

    return receipt;
  }

  // ==================== isDelivery / delivery fee PDF rendering ====================

  @Test
  void generateDocumentPdf_Invoice_IsDeliveryTrue_ContainsFraisTransport() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setIsDelivery(true);
    doc.setTransportFee(new BigDecimal("15.000"));
    Product product = createTestProduct("Ciment");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Frais Transport"), "PDF should contain 'Frais Transport'");
  }

  @Test
  void generateDocumentPdf_Invoice_IsDeliveryFalse_NoFraisTransport() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setIsDelivery(false);
    doc.setTransportFee(null);
    Product product = createTestProduct("Ciment");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertFalse(
        text.contains("Frais Transport"),
        "PDF should NOT contain 'Frais Transport' for non-delivery");
  }

  @Test
  void generateDocumentPdf_DeliveryNote_IsDeliveryFalse_NoFraisTransport() throws IOException {
    Document doc = createTestDocument(DocumentType.DELIVERY_NOTE, true, false);
    doc.setIsDelivery(false);
    doc.setTransportFee(null);
    Product product = createTestProduct("Vis M6");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("BON DE LIVRAISON"), "PDF should contain 'BON DE LIVRAISON'");
    assertFalse(
        text.contains("Frais Transport"),
        "PDF should NOT contain 'Frais Transport' for non-delivery BL");
  }

  @Test
  void generateDocumentPdf_NullProductName_DoesNotRenderNull() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, false, false);
    Product product = new Product();
    product.setId(99L);
    product.setName(null);
    product.setReference("NULL-NAME");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    String text = extractTextFromPdf(pdfBytes);
    assertFalse(text.contains("null"), "PDF should not contain the literal text 'null'");
  }

  @Test
  void generateDocumentPdf_EmptyLines_GeneratesPdf() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, true, false);
    doc.setLines(List.of());

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("DEVIS"));
  }

  @Test
  void generateDocumentPdf_NullVariantInLine_ShowsProductNameOnly() throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);
    Product product = createTestProduct("Screw");

    DocumentLine line = createTestLine(1, product, null);
    line.setVariant(null);
    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Screw"));
    assertFalse(text.contains("Screw -"));
  }

  @Test
  void generateDocumentPdf_NullProductInLine_WithConditioningDescription_ShowsDescription()
      throws IOException {
    Document doc = createTestDocument(DocumentType.QUOTE, false, false);

    DocumentLine line = new DocumentLine();
    line.setLineNumber(1);
    line.setProduct(null);
    line.setConditioningDescription("Custom Item");
    line.setQuantity(new BigDecimal("5.000"));
    line.setUnitPrice(new BigDecimal("100.000"));
    line.setTotalLineExcludingTax(new BigDecimal("500.000"));
    line.setTotalLineIncludingTax(new BigDecimal("595.000"));
    line.setIsDelivered(false);

    doc.setLines(List.of(line));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Custom Item"));
    assertFalse(text.contains("N/A"));
  }

  @Test
  void generateDocumentPdf_NullStampDutyForInvoice_UsesDefault() throws IOException {
    Document doc = createTestDocument(DocumentType.INVOICE, true, false);
    doc.setStampDuty(null);
    Product product = createTestProduct("Item");
    doc.setLines(List.of(createTestLine(1, product, null)));

    byte[] pdfBytes = pdfService.generateDocumentPdf(doc);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Droit Timbre"));
  }

  @Test
  void generatePaymentReceiptPdf_NullPreviousDebt_ShowsZero() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);
    receipt.setPreviousDebt(null);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Dette précédente"));
    assertTrue(
        text.contains("0,000"), "PDF should contain formatted zero value for null previous debt");
  }

  @Test
  void generatePaymentReceiptPdf_NullNewDebt_ShowsZero() throws IOException {
    PaymentReceipt receipt = createTestPaymentReceipt(true);
    receipt.setNewDebt(null);

    byte[] pdfBytes = pdfService.generatePaymentReceiptPdf(receipt);

    String text = extractTextFromPdf(pdfBytes);
    assertTrue(text.contains("Dette restante"));
    assertTrue(text.contains("0,000"), "PDF should contain formatted zero value for null new debt");
  }
}
