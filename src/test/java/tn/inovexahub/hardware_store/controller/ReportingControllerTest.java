package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.service.ReportingService;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTest {

  @Mock private ReportingService reportingService;

  private ReportingController reportingController;

  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 1, 31);

  @BeforeEach
  void setUp() {
    reportingController = new ReportingController(reportingService);
  }

  // ==================== getRevenueStats ====================

  @Test
  void getRevenueStats_ValidDates_ReturnsOk() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalRevenue", new BigDecimal("50000"));
    when(reportingService.getRevenueStats(START_DATE, END_DATE)).thenReturn(stats);

    ResponseEntity<Map<String, Object>> response =
        reportingController.getRevenueStats(START_DATE, END_DATE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(stats, response.getBody());
    verify(reportingService).getRevenueStats(START_DATE, END_DATE);
  }

  @Test
  void getRevenueStats_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.getRevenueStats(END_DATE, START_DATE));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== getDailyRevenue ====================

  @Test
  void getDailyRevenue_ValidDates_ReturnsOk() {
    Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();
    dailyRevenue.put(START_DATE, new BigDecimal("1500"));
    when(reportingService.getDailyRevenue(START_DATE, END_DATE)).thenReturn(dailyRevenue);

    ResponseEntity<Map<LocalDate, BigDecimal>> response =
        reportingController.getDailyRevenue(START_DATE, END_DATE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(dailyRevenue, response.getBody());
    verify(reportingService).getDailyRevenue(START_DATE, END_DATE);
  }

  @Test
  void getDailyRevenue_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.getDailyRevenue(END_DATE, START_DATE));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== getMarginStats ====================

  @Test
  void getMarginStats_ValidDates_ReturnsOk() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("grossMargin", new BigDecimal("12000"));
    when(reportingService.getMarginStats(START_DATE, END_DATE)).thenReturn(stats);

    ResponseEntity<Map<String, Object>> response =
        reportingController.getMarginStats(START_DATE, END_DATE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(stats, response.getBody());
    verify(reportingService).getMarginStats(START_DATE, END_DATE);
  }

  @Test
  void getMarginStats_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.getMarginStats(END_DATE, START_DATE));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== getDebtorReport ====================

  @Test
  void getDebtorReport_ReturnsOk() {
    Map<String, Object> report = new HashMap<>();
    report.put("debtorCount", 5);
    when(reportingService.getDebtorReport()).thenReturn(report);

    ResponseEntity<Map<String, Object>> response = reportingController.getDebtorReport();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(report, response.getBody());
    verify(reportingService).getDebtorReport();
  }

  // ==================== getClientsNearCreditLimit ====================

  @Test
  void getClientsNearCreditLimit_DefaultThreshold_ReturnsOk() {
    Client client = new Client();
    client.setName("Test Client");
    List<Client> clients = List.of(client);
    when(reportingService.getClientsNearCreditLimit(new BigDecimal("100.0"))).thenReturn(clients);

    ResponseEntity<List<Client>> response = reportingController.getClientsNearCreditLimit(null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(reportingService).getClientsNearCreditLimit(new BigDecimal("100.0"));
  }

  @Test
  void getClientsNearCreditLimit_CustomThreshold_ReturnsOk() {
    Client client = new Client();
    client.setName("Near Limit Client");
    List<Client> clients = List.of(client);
    BigDecimal threshold = new BigDecimal("500.0");
    when(reportingService.getClientsNearCreditLimit(threshold)).thenReturn(clients);

    ResponseEntity<List<Client>> response =
        reportingController.getClientsNearCreditLimit(threshold);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(reportingService).getClientsNearCreditLimit(threshold);
  }

  // ==================== getTopProductsByRevenue ====================

  @Test
  void getTopProductsByRevenue_ValidParams_ReturnsOk() {
    List<Map<String, Object>> products = List.of(Map.of("revenue", new BigDecimal("10000")));
    when(reportingService.getTopProductsByRevenue(START_DATE, END_DATE, 10)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByRevenue(START_DATE, END_DATE, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(reportingService).getTopProductsByRevenue(START_DATE, END_DATE, 10);
  }

  @Test
  void getTopProductsByRevenue_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.getTopProductsByRevenue(END_DATE, START_DATE, 10));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getTopProductsByRevenue_LimitBelowOne_ClampedToOne() {
    List<Map<String, Object>> products = List.of();
    when(reportingService.getTopProductsByRevenue(START_DATE, END_DATE, 1)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByRevenue(START_DATE, END_DATE, 0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(reportingService).getTopProductsByRevenue(START_DATE, END_DATE, 1);
  }

  @Test
  void getTopProductsByRevenue_LimitAbove100_ClampedTo100() {
    List<Map<String, Object>> products = List.of();
    when(reportingService.getTopProductsByRevenue(START_DATE, END_DATE, 100)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByRevenue(START_DATE, END_DATE, 200);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(reportingService).getTopProductsByRevenue(START_DATE, END_DATE, 100);
  }

  // ==================== getTopProductsByMargin ====================

  @Test
  void getTopProductsByMargin_ValidParams_ReturnsOk() {
    List<Map<String, Object>> products = List.of(Map.of("margin", new BigDecimal("5000")));
    when(reportingService.getTopProductsByMargin(START_DATE, END_DATE, 10)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByMargin(START_DATE, END_DATE, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    verify(reportingService).getTopProductsByMargin(START_DATE, END_DATE, 10);
  }

  @Test
  void getTopProductsByMargin_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.getTopProductsByMargin(END_DATE, START_DATE, 10));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getTopProductsByMargin_LimitBelowOne_ClampedToOne() {
    List<Map<String, Object>> products = List.of();
    when(reportingService.getTopProductsByMargin(START_DATE, END_DATE, 1)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByMargin(START_DATE, END_DATE, 0);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(reportingService).getTopProductsByMargin(START_DATE, END_DATE, 1);
  }

  @Test
  void getTopProductsByMargin_LimitAbove100_ClampedTo100() {
    List<Map<String, Object>> products = List.of();
    when(reportingService.getTopProductsByMargin(START_DATE, END_DATE, 100)).thenReturn(products);

    ResponseEntity<List<Map<String, Object>>> response =
        reportingController.getTopProductsByMargin(START_DATE, END_DATE, 200);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(reportingService).getTopProductsByMargin(START_DATE, END_DATE, 100);
  }

  // ==================== getStockReport ====================

  @Test
  void getStockReport_ReturnsOk() {
    Map<String, Object> report = new HashMap<>();
    report.put("totalProducts", 50);
    when(reportingService.getStockReport()).thenReturn(report);

    ResponseEntity<Map<String, Object>> response = reportingController.getStockReport();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(report, response.getBody());
    verify(reportingService).getStockReport();
  }

  // ==================== exportSalesJournalToCsv ====================

  @Test
  void exportSalesJournalToCsv_ValidDates_ReturnsOk() throws IOException {
    byte[] csvBytes = "col1,col2\nval1,val2".getBytes();
    when(reportingService.exportSalesJournalToCsv(START_DATE, END_DATE)).thenReturn(csvBytes);

    ResponseEntity<byte[]> response =
        reportingController.exportSalesJournalToCsv(START_DATE, END_DATE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(csvBytes, response.getBody());
    assertNotNull(response.getHeaders().getContentType());
    assertEquals("text/csv", response.getHeaders().getContentType().toString());
    assertNotNull(response.getHeaders().getContentDisposition());
    assertEquals(
        "attachment; filename=\"sales-journal-2024-01-01-to-2024-01-31.csv\"",
        response.getHeaders().getContentDisposition().toString());
    verify(reportingService).exportSalesJournalToCsv(START_DATE, END_DATE);
  }

  @Test
  void exportSalesJournalToCsv_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.exportSalesJournalToCsv(END_DATE, START_DATE));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== exportSalesJournalToExcel ====================

  @Test
  void exportSalesJournalToExcel_ValidDates_ReturnsOk() throws IOException {
    byte[] excelBytes = new byte[] {0x50, 0x4B};
    when(reportingService.exportSalesJournalToExcel(START_DATE, END_DATE)).thenReturn(excelBytes);

    ResponseEntity<byte[]> response =
        reportingController.exportSalesJournalToExcel(START_DATE, END_DATE);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(excelBytes, response.getBody());
    assertNotNull(response.getHeaders().getContentType());
    assertEquals(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        response.getHeaders().getContentType().toString());
    assertNotNull(response.getHeaders().getContentDisposition());
    assertEquals(
        "attachment; filename=\"sales-journal-2024-01-01-to-2024-01-31.xlsx\"",
        response.getHeaders().getContentDisposition().toString());
    verify(reportingService).exportSalesJournalToExcel(START_DATE, END_DATE);
  }

  @Test
  void exportSalesJournalToExcel_StartDateAfterEndDate_ThrowsBadRequest() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reportingController.exportSalesJournalToExcel(END_DATE, START_DATE));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== exportStockReportToCsv ====================

  @Test
  void exportStockReportToCsv_ReturnsOk() throws IOException {
    byte[] csvBytes = "col1,col2\nval1,val2".getBytes();
    when(reportingService.exportStockReportToCsv()).thenReturn(csvBytes);

    ResponseEntity<byte[]> response = reportingController.exportStockReportToCsv();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(csvBytes, response.getBody());
    assertNotNull(response.getHeaders().getContentType());
    assertEquals("text/csv", response.getHeaders().getContentType().toString());
    assertNotNull(response.getHeaders().getContentDisposition());
    assertEquals(
        "attachment; filename=\"stock-report.csv\"",
        response.getHeaders().getContentDisposition().toString());
    verify(reportingService).exportStockReportToCsv();
  }

  // ==================== exportStockReportToExcel ====================

  @Test
  void exportStockReportToExcel_ReturnsOk() throws IOException {
    byte[] excelBytes = new byte[] {0x50, 0x4B};
    when(reportingService.exportStockReportToExcel()).thenReturn(excelBytes);

    ResponseEntity<byte[]> response = reportingController.exportStockReportToExcel();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(excelBytes, response.getBody());
    assertNotNull(response.getHeaders().getContentType());
    assertEquals(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        response.getHeaders().getContentType().toString());
    assertNotNull(response.getHeaders().getContentDisposition());
    assertEquals(
        "attachment; filename=\"stock-report.xlsx\"",
        response.getHeaders().getContentDisposition().toString());
    verify(reportingService).exportStockReportToExcel();
  }
}
