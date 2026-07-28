package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Revenue statistics calculated",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "RevenueStats",
                            value =
                                """
                                {
                                  "totalRevenue": 125000.000,
                                  "totalRevenueExcludingTax": 108695.652,
                                  "totalVat": 16304.348,
                                  "documentCount": 42,
                                  "averageRevenue": 2976.190
                                }"""))),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getRevenueStats(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Daily revenue data retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "DailyRevenue",
                            value =
                                """
                                {
                                  "2024-01-01": 5200.000,
                                  "2024-01-02": 3800.500,
                                  "2024-01-03": 0.000
                                }"""))),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Map<LocalDate, BigDecimal>> getDailyRevenue(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Margin statistics calculated",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "MarginStats",
                            value =
                                """
                                {
                                  "totalRevenue": 108695.652,
                                  "totalCost": 72450.000,
                                  "grossMargin": 36245.652,
                                  "marginPercentage": 33.3500
                                }"""))),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getMarginStats(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    if (startDate.isAfter(endDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
    }
    return ResponseEntity.ok(reportingService.getMarginStats(startDate, endDate));
  }

  // ==================== Risk Management (Debtors) ====================

  @GetMapping("/debtors")
  @Operation(summary = "Get debtor report", description = "Get debtor risk report (Admin only)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Debtor risk report retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "DebtorReport",
                            value =
                                """
                                {
                                  "debtorCount": 3,
                                  "totalOutstandingDebt": 45000.000,
                                  "totalCreditLimit": 80000.000,
                                  "creditUtilization": 56.2500,
                                  "debtors": [
                                    {
                                      "id": 2,
                                      "name": "Entreprise XYZ",
                                      "creditLimit": 30000.000,
                                      "currentDebt": 22500.000,
                                      "deleted": false
                                    }
                                  ]
                                }"""))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getDebtorReport() {
    return ResponseEntity.ok(reportingService.getDebtorReport());
  }

  @GetMapping("/debtors/near-limit")
  @Operation(
      summary = "Get clients near credit limit",
      description = "Get clients within threshold of their credit limit (Admin only)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clients near credit limit retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "ClientsNearCreditLimit",
                            value =
                                """
                                [
                                  {
                                    "id": 2,
                                    "name": "Entreprise XYZ",
                                    "creditLimit": 30000.000,
                                    "currentDebt": 29500.000,
                                    "deleted": false
                                  }
                                ]"""))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<List<Client>> getClientsNearCreditLimit(
      @Parameter(description = "Credit margin threshold", example = "100.0")
          @RequestParam(defaultValue = "100.0")
          BigDecimal threshold) {
    if (threshold == null) {
      threshold = new BigDecimal("100.0");
    }
    return ResponseEntity.ok(reportingService.getClientsNearCreditLimit(threshold));
  }

  // ==================== Top Products and Rotation ====================

  @GetMapping("/products/top-revenue")
  @Operation(
      summary = "Get top products by revenue",
      description = "Get top selling products by revenue (Admin only)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Top products by revenue retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "TopProductsByRevenue",
                            value =
                                """
                                [
                                  {
                                    "product": {"id": 1, "reference": "PROD001", "name": "Ciment Portland"},
                                    "revenue": 45000.00,
                                    "quantitySold": 500.000
                                  },
                                  {
                                    "product": {"id": 2, "reference": "PROD002", "name": "Fer à Béton"},
                                    "revenue": 32000.00,
                                    "quantitySold": 1000.000
                                  }
                                ]"""))),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<List<Map<String, Object>>> getTopProductsByRevenue(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @Parameter(description = "Maximum number of products to return (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Top products by margin retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "TopProductsByMargin",
                            value =
                                """
                                [
                                  {
                                    "product": {"id": 1, "reference": "PROD001", "name": "Ciment Portland"},
                                    "margin": 18000.00,
                                    "quantitySold": 500.000
                                  },
                                  {
                                    "product": {"id": 3, "reference": "PROD003", "name": "Carrelage"},
                                    "margin": 12500.00,
                                    "quantitySold": 200.000
                                  }
                                ]"""))),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<List<Map<String, Object>>> getTopProductsByMargin(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @Parameter(description = "Maximum number of products to return (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Stock report retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "StockReport",
                            value =
                                """
                                {
                                  "totalProducts": 150,
                                  "lowStockProducts": 12,
                                  "totalStockValue": 285000.000,
                                  "lowStockProductsList": [
                                    {
                                      "id": 5,
                                      "reference": "PROD005",
                                      "name": "Peinture Blanche",
                                      "stockQuantity": 3.000,
                                      "averagePurchasePrice": 45.000
                                    }
                                  ]
                                }"""))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getStockReport() {
    return ResponseEntity.ok(reportingService.getStockReport());
  }

  // ==================== Export Functionality ====================

  @GetMapping("/exports/sales-journal/csv")
  @Operation(
      summary = "Export sales journal to CSV",
      description = "Export sales journal for a date range to CSV format (Admin only)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file generated",
            content = @Content(mediaType = "text/csv")),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<byte[]> exportSalesJournalToCsv(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate)
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Excel file generated",
            content =
                @Content(
                    mediaType =
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<byte[]> exportSalesJournalToExcel(
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate)
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV file generated",
            content = @Content(mediaType = "text/csv")),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Excel file generated",
            content =
                @Content(
                    mediaType =
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
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
