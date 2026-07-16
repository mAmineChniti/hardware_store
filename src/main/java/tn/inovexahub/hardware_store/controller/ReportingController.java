package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.service.ReportingService;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reporting", description = "Reporting and analytics endpoints (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {

  private final ReportingService reportingService;

  public ReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  // ==================== Revenue Tracking ====================

  @GetMapping("/revenue")
  @Operation(
      summary = "Get revenue statistics",
      description = "Get revenue statistics for a date range (Admin only)")
  public ResponseEntity<Map<String, Object>> getRevenueStats(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    return ResponseEntity.ok(reportingService.getRevenueStats(startDate, endDate));
  }

  @GetMapping("/revenue/daily")
  @Operation(
      summary = "Get daily revenue",
      description = "Get daily revenue data for a date range (Admin only)")
  public ResponseEntity<Map<LocalDate, BigDecimal>> getDailyRevenue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    return ResponseEntity.ok(reportingService.getDailyRevenue(startDate, endDate));
  }

  // ==================== Margin and Profit Calculation ====================

  @GetMapping("/margin")
  @Operation(
      summary = "Get margin statistics",
      description = "Calculate margin statistics for a date range (Admin only)")
  public ResponseEntity<Map<String, Object>> getMarginStats(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    return ResponseEntity.ok(reportingService.getMarginStats(startDate, endDate));
  }

  // ==================== Risk Management (Debtors) ====================

  @GetMapping("/debtors")
  @Operation(summary = "Get debtor report", description = "Get debtor risk report (Admin only)")
  public ResponseEntity<Map<String, Object>> getDebtorReport() {
    return ResponseEntity.ok(reportingService.getDebtorReport());
  }

  @GetMapping("/debtors/near-limit")
  @Operation(
      summary = "Get clients near credit limit",
      description = "Get clients within threshold of their credit limit (Admin only)")
  public ResponseEntity<List<Client>> getClientsNearCreditLimit(
      @RequestParam(defaultValue = "100.0") BigDecimal threshold) {
    return ResponseEntity.ok(reportingService.getClientsNearCreditLimit(threshold));
  }

  // ==================== Top Products and Rotation ====================

  @GetMapping("/products/top-revenue")
  @Operation(
      summary = "Get top products by revenue",
      description = "Get top selling products by revenue (Admin only)")
  public ResponseEntity<List<Map<String, Object>>> getTopProductsByRevenue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "10") int limit) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    if (limit < 1) {
      limit = 1;
    }
    if (limit > 100) {
      limit = 100;
    }
    return ResponseEntity.ok(reportingService.getTopProductsByRevenue(startDate, endDate, limit));
  }

  @GetMapping("/products/top-margin")
  @Operation(
      summary = "Get top products by margin",
      description = "Get top products by margin (Admin only)")
  public ResponseEntity<List<Map<String, Object>>> getTopProductsByMargin(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "10") int limit) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    if (limit < 1) {
      limit = 1;
    }
    if (limit > 100) {
      limit = 100;
    }
    return ResponseEntity.ok(reportingService.getTopProductsByMargin(startDate, endDate, limit));
  }

  // ==================== Stock Report ====================

  @GetMapping("/stock")
  @Operation(
      summary = "Get stock report",
      description = "Get stock statistics and low stock products (Admin only)")
  public ResponseEntity<Map<String, Object>> getStockReport() {
    return ResponseEntity.ok(reportingService.getStockReport());
  }

  // ==================== Export Functionality ====================

  @GetMapping("/exports/sales-journal/csv")
  @Operation(
      summary = "Export sales journal to CSV",
      description = "Export sales journal for a date range to CSV format (Admin only)")
  public ResponseEntity<byte[]> exportSalesJournalToCsv(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
      throws IOException {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    byte[] csvBytes = reportingService.exportSalesJournalToCsv(startDate, endDate);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDisposition(
        ContentDisposition.builder("attachment")
            .filename(
                String.format(
                    "sales-journal-%s-to-%s.csv",
                    startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .build());

    return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
  }

  @GetMapping("/exports/sales-journal/excel")
  @Operation(
      summary = "Export sales journal to Excel",
      description = "Export sales journal for a date range to Excel format (Admin only)")
  public ResponseEntity<byte[]> exportSalesJournalToExcel(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
      throws IOException {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    byte[] excelBytes = reportingService.exportSalesJournalToExcel(startDate, endDate);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDisposition(
        ContentDisposition.builder("attachment")
            .filename(
                String.format(
                    "sales-journal-%s-to-%s.xlsx",
                    startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .build());

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }

  @GetMapping("/exports/stock/csv")
  @Operation(
      summary = "Export stock report to CSV",
      description = "Export stock report to CSV format (Admin only)")
  public ResponseEntity<byte[]> exportStockReportToCsv() throws IOException {
    byte[] csvBytes = reportingService.exportStockReportToCsv();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDisposition(
        ContentDisposition.builder("attachment").filename("stock-report.csv").build());

    return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
  }

  @GetMapping("/exports/stock/excel")
  @Operation(
      summary = "Export stock report to Excel",
      description = "Export stock report to Excel format (Admin only)")
  public ResponseEntity<byte[]> exportStockReportToExcel() throws IOException {
    byte[] excelBytes = reportingService.exportStockReportToExcel();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDisposition(
        ContentDisposition.builder("attachment").filename("stock-report.xlsx").build());

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }
}
