package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.enums.UnitType;
import tn.inovexahub.hardware_store.repository.DocumentLineRepository;
import tn.inovexahub.hardware_store.repository.DocumentRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

  @Mock private DocumentRepository documentRepository;
  @Mock private DocumentLineRepository documentLineRepository;
  @Mock private ProductRepository productRepository;
  @Mock private ClientService clientService;

  @InjectMocks private ReportingService reportingService;

  private Document testDocument;
  private DocumentLine testDocumentLine;
  private Product testProduct;
  private Client testClient;

  @BeforeEach
  void setUp() {
    testClient = new Client();
    testClient.setId(1L);
    testClient.setName("Test Client");
    testClient.setCurrentDebt(new BigDecimal("5000.00"));
    testClient.setCreditLimit(new BigDecimal("10000.00"));

    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setName("Test Product");
    testProduct.setReference("PROD-001");
    testProduct.setStockQuantity(new BigDecimal("50"));
    testProduct.setAveragePurchasePrice(new BigDecimal("15.00"));
    testProduct.setUnitType(UnitType.UNITARY);

    testDocument = new Document();
    testDocument.setId(1L);
    testDocument.setDocumentNumber("FAC-000001");
    testDocument.setDocumentType(DocumentType.INVOICE);
    testDocument.setStatus(DocumentStatus.VALIDATED);
    testDocument.setDate(LocalDateTime.now());
    testDocument.setClient(testClient);
    testDocument.setTotalExcludingTax(new BigDecimal("1000.000"));
    testDocument.setTotalVat(new BigDecimal("190.000"));
    testDocument.setTotalIncludingTax(new BigDecimal("1190.000"));
    testDocument.setIsCreditSale(false);
    testDocument.setTransportFee(new BigDecimal("10.000"));
    testDocument.setStampDuty(new BigDecimal("1.000"));

    testDocumentLine = new DocumentLine();
    testDocumentLine.setId(1L);
    testDocumentLine.setDocument(testDocument);
    testDocumentLine.setProduct(testProduct);
    testDocumentLine.setQuantity(new BigDecimal("10"));
    testDocumentLine.setUnitPrice(new BigDecimal("100.00"));
    testDocumentLine.setUnitCost(new BigDecimal("15.00"));
    testDocumentLine.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    testDocumentLine.setTotalLineIncludingTax(new BigDecimal("1190.000"));
    testDocumentLine.setLineNumber(1);
  }

  private void mockDocumentsForDateRange(List<Document> documents) {
    when(documentRepository.findByDateGreaterThanEqualAndDateLessThan(
            any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(documents);
  }

  private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
    assertEquals(0, expected.compareTo(actual), "Expected " + expected + " but was " + actual);
  }

  // ==================== Revenue Stats Tests ====================

  @Test
  void getRevenueStats_WithDocuments_ReturnsStats() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    Map<String, Object> stats =
        reportingService.getRevenueStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(stats);
    assertBigDecimalEquals(new BigDecimal("1190.000"), (BigDecimal) stats.get("totalRevenue"));
    assertBigDecimalEquals(
        new BigDecimal("1000.000"), (BigDecimal) stats.get("totalRevenueExcludingTax"));
    assertBigDecimalEquals(new BigDecimal("190.000"), (BigDecimal) stats.get("totalVat"));
    assertEquals(1L, stats.get("documentCount"));
    assertBigDecimalEquals(new BigDecimal("1190.00"), (BigDecimal) stats.get("averageRevenue"));
  }

  @Test
  void getRevenueStats_EmptyRange_ReturnsZeros() {
    mockDocumentsForDateRange(Collections.emptyList());

    Map<String, Object> stats =
        reportingService.getRevenueStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(stats);
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalRevenue"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalRevenueExcludingTax"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalVat"));
    assertEquals(0L, stats.get("documentCount"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("averageRevenue"));
  }

  // ==================== Daily Revenue Tests ====================

  @Test
  void getDailyRevenue_WithDocuments_ReturnsDailyData() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    Map<LocalDate, BigDecimal> dailyRevenue =
        reportingService.getDailyRevenue(LocalDate.now(), LocalDate.now());

    assertNotNull(dailyRevenue);
    assertFalse(dailyRevenue.isEmpty());
    assertTrue(dailyRevenue.containsKey(LocalDate.now()));
  }

  // ==================== Margin Stats Tests ====================

  @Test
  void getMarginStats_WithDocuments_ReturnsStats() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(testDocumentLine));

    Map<String, Object> stats =
        reportingService.getMarginStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(stats);
    assertBigDecimalEquals(new BigDecimal("1000.000"), (BigDecimal) stats.get("totalRevenue"));
    assertBigDecimalEquals(new BigDecimal("150.00"), (BigDecimal) stats.get("totalCost"));
    assertBigDecimalEquals(new BigDecimal("850.00"), (BigDecimal) stats.get("grossMargin"));
    assertBigDecimalEquals(new BigDecimal("85.0000"), (BigDecimal) stats.get("marginPercentage"));
  }

  @Test
  void getMarginStats_EmptyRange_ReturnsZeros() {
    mockDocumentsForDateRange(Collections.emptyList());

    Map<String, Object> stats =
        reportingService.getMarginStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(stats);
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalRevenue"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalCost"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("grossMargin"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("marginPercentage"));
  }

  @Test
  void getMarginStats_ZeroRevenue_ReturnsZeroMarginPercentage() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    DocumentLine lineWithZeroRevenue = new DocumentLine();
    lineWithZeroRevenue.setUnitCost(new BigDecimal("10.00"));
    lineWithZeroRevenue.setProduct(testProduct);
    lineWithZeroRevenue.setQuantity(new BigDecimal("1"));
    lineWithZeroRevenue.setTotalLineExcludingTax(BigDecimal.ZERO);
    when(documentLineRepository.findByDocumentId(1L))
        .thenReturn(Arrays.asList(lineWithZeroRevenue));

    Map<String, Object> stats =
        reportingService.getMarginStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(stats);
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalRevenue"));
    assertBigDecimalEquals(new BigDecimal("10.00"), (BigDecimal) stats.get("totalCost"));
    assertBigDecimalEquals(new BigDecimal("-10.00"), (BigDecimal) stats.get("grossMargin"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("marginPercentage"));
  }

  // ==================== Debtor Report Tests ====================

  @Test
  void getDebtorReport_WithDebtors_ReturnsReport() {
    when(clientService.getDebtors()).thenReturn(Arrays.asList(testClient));

    Map<String, Object> report = reportingService.getDebtorReport();

    assertNotNull(report);
    assertEquals(1, report.get("debtorCount"));
    assertBigDecimalEquals(
        new BigDecimal("5000.00"), (BigDecimal) report.get("totalOutstandingDebt"));
    assertBigDecimalEquals(new BigDecimal("10000.00"), (BigDecimal) report.get("totalCreditLimit"));
    assertBigDecimalEquals(new BigDecimal("50.0000"), (BigDecimal) report.get("creditUtilization"));
  }

  @Test
  void getDebtorReport_NoDebtors_ReturnsEmptyReport() {
    when(clientService.getDebtors()).thenReturn(Collections.emptyList());

    Map<String, Object> report = reportingService.getDebtorReport();

    assertNotNull(report);
    assertEquals(0, report.get("debtorCount"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) report.get("totalOutstandingDebt"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) report.get("totalCreditLimit"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) report.get("creditUtilization"));
  }

  @Test
  void getDebtorReport_ZeroCreditLimit_ReturnsZeroUtilization() {
    testClient.setCreditLimit(BigDecimal.ZERO);
    when(clientService.getDebtors()).thenReturn(Arrays.asList(testClient));

    Map<String, Object> report = reportingService.getDebtorReport();

    assertNotNull(report);
    assertEquals(1, report.get("debtorCount"));
    assertBigDecimalEquals(
        new BigDecimal("5000.00"), (BigDecimal) report.get("totalOutstandingDebt"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) report.get("totalCreditLimit"));
    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) report.get("creditUtilization"));
  }

  // ==================== Clients Near Credit Limit Tests ====================

  @Test
  void getClientsNearCreditLimit_ReturnsClients() {
    when(clientService.getClientsNearCreditLimit(new BigDecimal("1000.00")))
        .thenReturn(Arrays.asList(testClient));

    List<Client> clients = reportingService.getClientsNearCreditLimit(new BigDecimal("1000.00"));

    assertNotNull(clients);
    assertEquals(1, clients.size());
    verify(clientService).getClientsNearCreditLimit(new BigDecimal("1000.00"));
  }

  // ==================== Top Products By Revenue Tests ====================

  @Test
  void getTopProductsByRevenue_WithProducts_ReturnsProducts() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    when(documentLineRepository.findByDocumentIdIn(any()))
        .thenReturn(Arrays.asList(testDocumentLine));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByRevenue(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(testProduct, result.get(0).get("product"));
    assertBigDecimalEquals(new BigDecimal("1000.000"), (BigDecimal) result.get(0).get("revenue"));
  }

  @Test
  void getTopProductsByRevenue_EmptyRange_ReturnsEmptyList() {
    mockDocumentsForDateRange(Collections.emptyList());
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Collections.emptyList());

    List<Map<String, Object>> result =
        reportingService.getTopProductsByRevenue(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==================== Top Products By Margin Tests ====================

  @Test
  void getTopProductsByMargin_WithProducts_ReturnsProducts() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    when(documentLineRepository.findByDocumentIdIn(any()))
        .thenReturn(Arrays.asList(testDocumentLine));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByMargin(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(testProduct, result.get(0).get("product"));
    assertBigDecimalEquals(new BigDecimal("850.00"), (BigDecimal) result.get(0).get("margin"));
  }

  @Test
  void getTopProductsByMargin_EmptyRange_ReturnsEmptyList() {
    mockDocumentsForDateRange(Collections.emptyList());
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Collections.emptyList());

    List<Map<String, Object>> result =
        reportingService.getTopProductsByMargin(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==================== Stock Report Tests ====================

  @Test
  void getStockReport_WithProducts_ReturnsReport() {
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));
    when(productRepository.findLowStock(new BigDecimal("10.0")))
        .thenReturn(Collections.emptyList());

    Map<String, Object> report = reportingService.getStockReport();

    assertNotNull(report);
    assertEquals(1, report.get("totalProducts"));
    assertEquals(0, report.get("lowStockProducts"));
    assertNotNull(report.get("totalStockValue"));
  }

  // ==================== Export Sales Journal CSV Tests ====================

  @Test
  void exportSalesJournalToCsv_WithDocuments_ReturnsNonEmptyBytes() throws IOException {
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(csvBytes);
    assertTrue(csvBytes.length > 0);
    String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csvContent.contains("Document Number"));
    assertTrue(csvContent.contains("FAC-000001"));
  }

  // ==================== Export Sales Journal Excel Tests ====================

  @Test
  void exportSalesJournalToExcel_WithDocuments_ReturnsNonEmptyBytes() throws IOException {
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  // ==================== Export Stock Report CSV Tests ====================

  @Test
  void exportStockReportToCsv_WithProducts_ReturnsNonEmptyBytes() throws IOException {
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] csvBytes = reportingService.exportStockReportToCsv();

    assertNotNull(csvBytes);
    assertTrue(csvBytes.length > 0);
    String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csvContent.contains("Reference"));
    assertTrue(csvContent.contains("PROD-001"));
  }

  // ==================== Export Stock Report Excel Tests ====================

  @Test
  void exportStockReportToExcel_WithProducts_ReturnsNonEmptyBytes() throws IOException {
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] excelBytes = reportingService.exportStockReportToExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  // ==================== CSV/Excel Null Client and Fields Tests ====================

  @Test
  void exportSalesJournalToCsv_NullClient_ShowsNA() throws IOException {
    testDocument.setClient(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("N/A"));
  }

  @Test
  void exportSalesJournalToCsv_NullTransportFee_ShowsZero() throws IOException {
    testDocument.setTransportFee(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("0.0"));
  }

  @Test
  void exportSalesJournalToCsv_NullStampDuty_ShowsZero() throws IOException {
    testDocument.setStampDuty(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("0.0"));
  }

  @Test
  void exportSalesJournalToCsv_FormulaInjection_PrefixedWithQuote() throws IOException {
    testDocument.setDocumentNumber("=SUM(A1:A10)");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("'=SUM(A1:A10)"));
  }

  @Test
  void exportSalesJournalToCsv_PlusPrefix_FormulaInjection() throws IOException {
    testDocument.setDocumentNumber("+CMD");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("'+CMD"));
  }

  @Test
  void exportSalesJournalToCsv_MinusPrefix_FormulaInjection() throws IOException {
    testDocument.setDocumentNumber("-CMD");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("'-CMD"));
  }

  @Test
  void exportSalesJournalToCsv_AtPrefix_FormulaInjection() throws IOException {
    testDocument.setDocumentNumber("@SUM(1)");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("'@SUM(1)"));
  }

  @Test
  void exportSalesJournalToCsv_CommaInValue_Quoted() throws IOException {
    testDocument.setDocumentNumber("DEV,001");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("\"DEV,001\""));
  }

  @Test
  void exportSalesJournalToCsv_QuoteInValue_EscapedAndQuoted() throws IOException {
    testDocument.setDocumentNumber("DEV\"001");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("\"DEV\"\"001\""));
  }

  @Test
  void exportSalesJournalToCsv_NewlineInValue_Quoted() throws IOException {
    testDocument.setDocumentNumber("DEV\n001");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("\"DEV\n001\""));
  }

  @Test
  void exportSalesJournalToCsv_CarriageReturnInValue_Quoted() throws IOException {
    testDocument.setDocumentNumber("DEV\r001");
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("\"DEV\r001\""));
  }

  @Test
  void exportSalesJournalToExcel_NullClient_ShowsNA() throws IOException {
    testDocument.setClient(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportSalesJournalToExcel_NullTransportFee_ShowsZero() throws IOException {
    testDocument.setTransportFee(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportSalesJournalToExcel_NullStampDuty_ShowsZero() throws IOException {
    testDocument.setStampDuty(null);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportStockReportToCsv_NullCategory_ShowsNA() throws IOException {
    testProduct.setCategory(null);
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] csvBytes = reportingService.exportStockReportToCsv();

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("N/A"));
  }

  @Test
  void exportStockReportToCsv_NullPrices_ShowsZero() throws IOException {
    testProduct.setPriceOnSite(null);
    testProduct.setPriceDelivered(null);
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] csvBytes = reportingService.exportStockReportToCsv();

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("0.0"));
  }

  @Test
  void exportStockReportToExcel_NullCategory_ShowsNA() throws IOException {
    testProduct.setCategory(null);
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] excelBytes = reportingService.exportStockReportToExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportStockReportToExcel_NullPrices_ShowsZero() throws IOException {
    testProduct.setPriceOnSite(null);
    testProduct.setPriceDelivered(null);
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    byte[] excelBytes = reportingService.exportStockReportToExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  // ==================== Empty CSV Export Tests ====================

  @Test
  void exportSalesJournalToCsv_EmptyDocuments_ReturnsOnlyHeader() throws IOException {
    mockDocumentsForDateRange(Collections.emptyList());

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("Document Number"));
    assertTrue(!csv.contains("FAC-"));
  }

  @Test
  void exportSalesJournalToExcel_EmptyDocuments_ReturnsOnlyHeader() throws IOException {
    mockDocumentsForDateRange(Collections.emptyList());

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportStockReportToCsv_EmptyProducts_ReturnsOnlyHeader() throws IOException {
    when(productRepository.findAll()).thenReturn(Collections.emptyList());

    byte[] csvBytes = reportingService.exportStockReportToCsv();

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("Reference"));
  }

  @Test
  void exportStockReportToExcel_EmptyProducts_ReturnsOnlyHeader() throws IOException {
    when(productRepository.findAll()).thenReturn(Collections.emptyList());

    byte[] excelBytes = reportingService.exportStockReportToExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  // ==================== Margin Stats Null UnitCost Tests ====================

  @Test
  void getMarginStats_NullUnitCostInLine_SkipsLine() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    DocumentLine lineNoCost = new DocumentLine();
    lineNoCost.setUnitCost(null);
    lineNoCost.setProduct(testProduct);
    lineNoCost.setQuantity(new BigDecimal("5"));
    lineNoCost.setTotalLineExcludingTax(new BigDecimal("500.000"));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(lineNoCost));

    Map<String, Object> stats =
        reportingService.getMarginStats(LocalDate.now().minusDays(7), LocalDate.now());

    assertBigDecimalEquals(BigDecimal.ZERO, (BigDecimal) stats.get("totalCost"));
  }

  // ==================== Daily Revenue Date Boundary Tests ====================

  @Test
  void getDailyRevenue_MultipleDays_ReturnsAllDays() {
    Document doc2 = new Document();
    doc2.setId(2L);
    doc2.setDocumentType(DocumentType.INVOICE);
    doc2.setStatus(DocumentStatus.VALIDATED);
    doc2.setDate(LocalDateTime.now().minusDays(1));
    doc2.setTotalIncludingTax(new BigDecimal("500.000"));
    mockDocumentsForDateRange(Arrays.asList(testDocument, doc2));

    LocalDate start = LocalDate.now().minusDays(2);
    LocalDate end = LocalDate.now();
    Map<LocalDate, BigDecimal> dailyRevenue = reportingService.getDailyRevenue(start, end);

    assertEquals(3, dailyRevenue.size());
    assertTrue(dailyRevenue.containsKey(start));
    assertTrue(dailyRevenue.containsKey(start.plusDays(1)));
    assertTrue(dailyRevenue.containsKey(end));
  }

  // ==================== Top Products By Revenue WithNullProduct Tests ====================

  @Test
  void getTopProductsByRevenue_NullProductInLine_SkipsLine() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    DocumentLine lineNoProduct = new DocumentLine();
    lineNoProduct.setProduct(null);
    lineNoProduct.setTotalLineExcludingTax(new BigDecimal("100.000"));
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Arrays.asList(lineNoProduct));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByRevenue(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getTopProductsByMargin_NullUnitCost_SkipsLine() {
    mockDocumentsForDateRange(Arrays.asList(testDocument));
    DocumentLine lineNoCost = new DocumentLine();
    lineNoCost.setUnitCost(null);
    lineNoCost.setProduct(testProduct);
    lineNoCost.setTotalLineExcludingTax(new BigDecimal("500.000"));
    lineNoCost.setQuantity(new BigDecimal("10"));
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Arrays.asList(lineNoCost));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByMargin(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getDailyRevenue_DocumentOutsideRange_NotIncluded() {
    Document futureDoc = new Document();
    futureDoc.setId(2L);
    futureDoc.setDocumentType(DocumentType.INVOICE);
    futureDoc.setStatus(DocumentStatus.VALIDATED);
    futureDoc.setDate(LocalDateTime.now().plusDays(5));
    futureDoc.setTotalIncludingTax(new BigDecimal("500.000"));
    mockDocumentsForDateRange(Arrays.asList(testDocument, futureDoc));

    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    Map<LocalDate, BigDecimal> dailyRevenue = reportingService.getDailyRevenue(start, end);

    assertNotNull(dailyRevenue);
    assertTrue(dailyRevenue.containsKey(start));
  }

  @Test
  void getStockReport_WithLowStockProducts_ShowsLowStockList() {
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));
    when(productRepository.findLowStock(new BigDecimal("10.0")))
        .thenReturn(Arrays.asList(testProduct));

    Map<String, Object> report = reportingService.getStockReport();

    assertNotNull(report);
    assertEquals(1, report.get("totalProducts"));
    assertEquals(1, report.get("lowStockProducts"));
    assertNotNull(report.get("lowStockProductsList"));
  }

  @Test
  void exportSalesJournalToCsv_CreditSaleDocument_ShowsYes() throws IOException {
    testDocument.setIsCreditSale(true);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] csvBytes =
        reportingService.exportSalesJournalToCsv(LocalDate.now().minusDays(7), LocalDate.now());

    String csv = new String(csvBytes, StandardCharsets.UTF_8);
    assertTrue(csv.contains("Yes"));
  }

  @Test
  void exportSalesJournalToExcel_CreditSaleDocument_ShowsYes() throws IOException {
    testDocument.setIsCreditSale(true);
    mockDocumentsForDateRange(Arrays.asList(testDocument));

    byte[] excelBytes =
        reportingService.exportSalesJournalToExcel(LocalDate.now().minusDays(7), LocalDate.now());

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void getTopProductsByRevenue_WithMultipleProducts_SortsCorrectly() {
    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("Expensive Product");

    DocumentLine line2 = new DocumentLine();
    line2.setProduct(product2);
    line2.setQuantity(new BigDecimal("5"));
    line2.setUnitPrice(new BigDecimal("400.00"));
    line2.setUnitCost(new BigDecimal("100.00"));
    line2.setTotalLineExcludingTax(new BigDecimal("2000.000"));
    line2.setTotalLineIncludingTax(new BigDecimal("2380.000"));

    DocumentLine line1 = new DocumentLine();
    line1.setProduct(testProduct);
    line1.setQuantity(new BigDecimal("10"));
    line1.setUnitPrice(new BigDecimal("100.00"));
    line1.setUnitCost(new BigDecimal("15.00"));
    line1.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line1.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    mockDocumentsForDateRange(Arrays.asList(testDocument));
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Arrays.asList(line1, line2));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line1, line2));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByRevenue(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(product2, result.get(0).get("product"));
    assertEquals(new BigDecimal("2000.000"), result.get(0).get("revenue"));
  }

  @Test
  void getTopProductsByMargin_WithMultipleProducts_SortsCorrectly() {
    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("High Margin Product");

    DocumentLine line2 = new DocumentLine();
    line2.setProduct(product2);
    line2.setQuantity(new BigDecimal("5"));
    line2.setUnitPrice(new BigDecimal("200.00"));
    line2.setUnitCost(new BigDecimal("50.00"));
    line2.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line2.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    DocumentLine line1 = new DocumentLine();
    line1.setProduct(testProduct);
    line1.setQuantity(new BigDecimal("10"));
    line1.setUnitPrice(new BigDecimal("100.00"));
    line1.setUnitCost(new BigDecimal("15.00"));
    line1.setTotalLineExcludingTax(new BigDecimal("1000.000"));
    line1.setTotalLineIncludingTax(new BigDecimal("1190.000"));

    mockDocumentsForDateRange(Arrays.asList(testDocument));
    when(documentLineRepository.findByDocumentIdIn(any())).thenReturn(Arrays.asList(line1, line2));
    when(documentLineRepository.findByDocumentId(1L)).thenReturn(Arrays.asList(line1, line2));

    List<Map<String, Object>> result =
        reportingService.getTopProductsByMargin(LocalDate.now().minusDays(7), LocalDate.now(), 10);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(testProduct, result.get(0).get("product"));
  }
}
