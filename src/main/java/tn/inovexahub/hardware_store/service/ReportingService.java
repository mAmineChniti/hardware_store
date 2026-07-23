package tn.inovexahub.hardware_store.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import tn.inovexahub.hardware_store.entity.Client;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.repository.DocumentLineRepository;
import tn.inovexahub.hardware_store.repository.DocumentRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@Service
public class ReportingService {

  private final DocumentRepository documentRepository;
  private final DocumentLineRepository documentLineRepository;
  private final ProductRepository productRepository;
  private final ClientService clientService;

  public ReportingService(
      DocumentRepository documentRepository,
      DocumentLineRepository documentLineRepository,
      ProductRepository productRepository,
      ClientService clientService) {
    this.documentRepository = documentRepository;
    this.documentLineRepository = documentLineRepository;
    this.productRepository = productRepository;
    this.clientService = clientService;
  }

  // ==================== Revenue Tracking ====================

  /**
   * Get revenue statistics for a date range.
   *
   * @param startDate Start date
   * @param endDate End date
   * @return Revenue statistics
   */
  public Map<String, Object> getRevenueStats(LocalDate startDate, LocalDate endDate) {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    BigDecimal totalRevenue =
        documents.stream()
            .map(Document::getTotalIncludingTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalRevenueExcludingTax =
        documents.stream()
            .map(Document::getTotalExcludingTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalVat =
        documents.stream().map(Document::getTotalVat).reduce(BigDecimal.ZERO, BigDecimal::add);

    long documentCount = documents.size();

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalRevenue", totalRevenue);
    stats.put("totalRevenueExcludingTax", totalRevenueExcludingTax);
    stats.put("totalVat", totalVat);
    stats.put("documentCount", documentCount);
    stats.put(
        "averageRevenue",
        documentCount > 0
            ? totalRevenue.divide(BigDecimal.valueOf(documentCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);

    return stats;
  }

  /**
   * Get daily revenue for a date range.
   *
   * @param startDate Start date
   * @param endDate End date
   * @return Daily revenue data
   */
  public Map<LocalDate, BigDecimal> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
    Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();

    // Load all documents for the date range once (half-open range)
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    // Group by date in memory
    for (Document doc : documents) {
      LocalDate docDate = doc.getDate().toLocalDate();
      if (!docDate.isBefore(startDate) && !docDate.isBefore(endDate.plusDays(1))) {
        dailyRevenue.merge(docDate, doc.getTotalIncludingTax(), BigDecimal::add);
      }
    }

    // Ensure all dates in range are present (even with zero revenue)
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      dailyRevenue.putIfAbsent(date, BigDecimal.ZERO);
    }

    return dailyRevenue;
  }

  // ==================== Margin and Profit Calculation ====================

  /**
   * Calculate margin statistics for a date range.
   *
   * @param startDate Start date
   * @param endDate End date
   * @return Margin statistics
   */
  public Map<String, Object> getMarginStats(LocalDate startDate, LocalDate endDate) {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    BigDecimal totalRevenue = BigDecimal.ZERO;
    BigDecimal totalCost = BigDecimal.ZERO;

    for (Document document : documents) {
      List<DocumentLine> lines = documentLineRepository.findByDocumentId(document.getId());
      for (DocumentLine line : lines) {
        if (line.getUnitCost() != null) {
          BigDecimal lineRevenue = line.getTotalLineExcludingTax();
          BigDecimal unitCost = line.getUnitCost();
          BigDecimal lineCost = unitCost.multiply(line.getQuantity());

          totalRevenue = totalRevenue.add(lineRevenue);
          totalCost = totalCost.add(lineCost);
        }
      }
    }

    BigDecimal grossMargin = totalRevenue.subtract(totalCost);
    BigDecimal marginPercentage =
        totalRevenue.compareTo(BigDecimal.ZERO) > 0
            ? grossMargin
                .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalRevenue", totalRevenue);
    stats.put("totalCost", totalCost);
    stats.put("grossMargin", grossMargin);
    stats.put("marginPercentage", marginPercentage);

    return stats;
  }

  // ==================== Risk Management (Debtors) ====================

  /**
   * Get debtor risk report.
   *
   * @return Debtor statistics
   */
  public Map<String, Object> getDebtorReport() {
    List<Client> debtors = clientService.getDebtors();

    BigDecimal totalOutstandingDebt =
        debtors.stream().map(Client::getCurrentDebt).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalCreditLimit =
        debtors.stream().map(Client::getCreditLimit).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal creditUtilization =
        totalCreditLimit.compareTo(BigDecimal.ZERO) > 0
            ? totalOutstandingDebt
                .divide(totalCreditLimit, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

    Map<String, Object> report = new HashMap<>();
    report.put("debtorCount", debtors.size());
    report.put("totalOutstandingDebt", totalOutstandingDebt);
    report.put("totalCreditLimit", totalCreditLimit);
    report.put("creditUtilization", creditUtilization);
    report.put("debtors", debtors);

    return report;
  }

  /**
   * Get clients near their credit limit.
   *
   * @param threshold Threshold amount
   * @return List of clients near limit
   */
  public List<Client> getClientsNearCreditLimit(BigDecimal threshold) {
    return clientService.getClientsNearCreditLimit(threshold);
  }

  // ==================== Top Products and Rotation ====================

  /**
   * Get top selling products by revenue.
   *
   * @param startDate Start date
   * @param endDate End date
   * @param limit Number of top products to return
   * @return Top products
   */
  public List<Map<String, Object>> getTopProductsByRevenue(
      LocalDate startDate, LocalDate endDate, int limit) {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    // Load all lines for all documents once
    List<Long> documentIds = documents.stream().map(Document::getId).collect(Collectors.toList());
    List<DocumentLine> allLines = documentLineRepository.findByDocumentIdIn(documentIds);

    Map<Product, BigDecimal> productRevenue = new HashMap<>();

    for (DocumentLine line : allLines) {
      if (line.getProduct() != null) {
        Product product = line.getProduct();
        BigDecimal revenue = line.getTotalLineExcludingTax();
        productRevenue.merge(product, revenue, BigDecimal::add);
      }
    }

    return productRevenue.entrySet().stream()
        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
        .limit(limit)
        .map(
            entry -> {
              Map<String, Object> productData = new HashMap<>();
              productData.put("product", entry.getKey());
              productData.put("revenue", entry.getValue());
              productData.put(
                  "quantitySold",
                  getProductQuantitySold(entry.getKey(), startDateTime, endDateTime));
              return productData;
            })
        .collect(Collectors.toList());
  }

  /**
   * Get top products by margin.
   *
   * @param startDate Start date
   * @param endDate End date
   * @param limit Number of top products to return
   * @return Top products by margin
   */
  public List<Map<String, Object>> getTopProductsByMargin(
      LocalDate startDate, LocalDate endDate, int limit) {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    // Load all lines for all documents once
    List<Long> documentIds = documents.stream().map(Document::getId).collect(Collectors.toList());
    List<DocumentLine> allLines = documentLineRepository.findByDocumentIdIn(documentIds);

    Map<Product, BigDecimal> productMargin = new HashMap<>();

    for (DocumentLine line : allLines) {
      if (line.getUnitCost() != null && line.getProduct() != null) {
        Product product = line.getProduct();
        BigDecimal revenue = line.getTotalLineExcludingTax();
        BigDecimal cost = line.getUnitCost().multiply(line.getQuantity());
        BigDecimal margin = revenue.subtract(cost);
        productMargin.merge(product, margin, BigDecimal::add);
      }
    }

    return productMargin.entrySet().stream()
        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
        .limit(limit)
        .map(
            entry -> {
              Map<String, Object> productData = new HashMap<>();
              productData.put("product", entry.getKey());
              productData.put("margin", entry.getValue());
              productData.put(
                  "quantitySold",
                  getProductQuantitySold(entry.getKey(), startDateTime, endDateTime));
              return productData;
            })
        .collect(Collectors.toList());
  }

  private BigDecimal getProductQuantitySold(
      Product product, LocalDateTime startDate, LocalDateTime endDate) {
    List<Document> documents =
        documentRepository.findByDateGreaterThanEqualAndDateLessThan(startDate, endDate).stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    BigDecimal totalQuantity = BigDecimal.ZERO;
    for (Document document : documents) {
      List<DocumentLine> lines = documentLineRepository.findByDocumentId(document.getId());
      for (DocumentLine line : lines) {
        if (line.getProduct() != null && line.getProduct().getId().equals(product.getId())) {
          totalQuantity = totalQuantity.add(line.getQuantity());
        }
      }
    }
    return totalQuantity;
  }

  // ==================== Stock Report ====================

  /**
   * Get stock report.
   *
   * @return Stock statistics
   */
  public Map<String, Object> getStockReport() {
    List<Product> allProducts = productRepository.findAll();
    List<Product> lowStockProducts = productRepository.findLowStock(new BigDecimal("10.0"));

    BigDecimal totalStockValue =
        allProducts.stream()
            .map(p -> p.getAveragePurchasePrice().multiply(p.getStockQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<String, Object> report = new HashMap<>();
    report.put("totalProducts", allProducts.size());
    report.put("lowStockProducts", lowStockProducts.size());
    report.put("totalStockValue", totalStockValue);
    report.put("lowStockProductsList", lowStockProducts);

    return report;
  }

  // ==================== Export Functionality ====================

  // ==================== Helper Methods ====================

  /**
   * Encode a value for CSV export, handling special characters and formula injection.
   *
   * @param value Value to encode
   * @return Encoded CSV-safe string
   */
  private String encodeCsvValue(String value) {
    if (value == null) {
      return "";
    }

    // Neutralize spreadsheet formula prefixes
    if (value.startsWith("=")
        || value.startsWith("+")
        || value.startsWith("-")
        || value.startsWith("@")) {
      value = "'" + value;
    }

    // Quote if contains comma, quote, newline, or carriage return
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      // Escape quotes by doubling them
      value = value.replace("\"", "\"\"");
      return "\"" + value + "\"";
    }

    return value;
  }

  /**
   * Export sales journal to CSV.
   *
   * @param startDate Start date
   * @param endDate End date
   * @return CSV bytes
   * @throws IOException if export fails
   */
  public byte[] exportSalesJournalToCsv(LocalDate startDate, LocalDate endDate) throws IOException {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8));

    // CSV Header
    writer.println(
        "Document Number,Date,Client Name,Total HT,Total VAT,Total TTC,Transport Fee,Stamp Duty,Credit Sale");

    // CSV Data
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    for (Document doc : documents) {
      String clientName = doc.getClient() != null ? doc.getClient().getName() : "N/A";
      String dateStr = doc.getDate().format(dateFormatter);
      String creditSaleStr = doc.getIsCreditSale() ? "Yes" : "No";

      writer.println(
          String.join(
              ",",
              encodeCsvValue(doc.getDocumentNumber()),
              encodeCsvValue(dateStr),
              encodeCsvValue(clientName),
              encodeCsvValue(doc.getTotalExcludingTax().toString()),
              encodeCsvValue(doc.getTotalVat().toString()),
              encodeCsvValue(doc.getTotalIncludingTax().toString()),
              encodeCsvValue(
                  doc.getTransportFee() != null ? doc.getTransportFee().toString() : "0.0"),
              encodeCsvValue(doc.getStampDuty() != null ? doc.getStampDuty().toString() : "0.0"),
              encodeCsvValue(creditSaleStr)));
    }

    writer.flush();
    return outputStream.toByteArray();
  }

  /**
   * Export sales journal to Excel.
   *
   * @param startDate Start date
   * @param endDate End date
   * @return Excel bytes
   * @throws IOException if export fails
   */
  public byte[] exportSalesJournalToExcel(LocalDate startDate, LocalDate endDate)
      throws IOException {
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    List<Document> documents =
        documentRepository
            .findByDateGreaterThanEqualAndDateLessThan(startDateTime, endDateTime)
            .stream()
            .filter(doc -> doc.getStatus() == DocumentStatus.VALIDATED)
            .filter(doc -> doc.getDocumentType() == DocumentType.INVOICE)
            .collect(Collectors.toList());

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Sales Journal");

      // Header Row
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Document Number");
      headerRow.createCell(1).setCellValue("Date");
      headerRow.createCell(2).setCellValue("Client Name");
      headerRow.createCell(3).setCellValue("Total HT");
      headerRow.createCell(4).setCellValue("Total VAT");
      headerRow.createCell(5).setCellValue("Total TTC");
      headerRow.createCell(6).setCellValue("Transport Fee");
      headerRow.createCell(7).setCellValue("Stamp Duty");
      headerRow.createCell(8).setCellValue("Credit Sale");

      // Data Rows
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      int rowNum = 1;
      for (Document doc : documents) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(doc.getDocumentNumber());
        row.createCell(1).setCellValue(doc.getDate().format(dateFormatter));
        row.createCell(2).setCellValue(doc.getClient() != null ? doc.getClient().getName() : "N/A");
        row.createCell(3).setCellValue(doc.getTotalExcludingTax().doubleValue());
        row.createCell(4).setCellValue(doc.getTotalVat().doubleValue());
        row.createCell(5).setCellValue(doc.getTotalIncludingTax().doubleValue());
        row.createCell(6)
            .setCellValue(
                doc.getTransportFee() != null ? doc.getTransportFee().doubleValue() : 0.0);
        row.createCell(7)
            .setCellValue(doc.getStampDuty() != null ? doc.getStampDuty().doubleValue() : 0.0);
        row.createCell(8).setCellValue(doc.getIsCreditSale() ? "Yes" : "No");
      }

      // Auto-size columns
      for (int i = 0; i < 9; i++) {
        sheet.autoSizeColumn(i);
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  /**
   * Export stock report to CSV.
   *
   * @return CSV bytes
   * @throws IOException if export fails
   */
  public byte[] exportStockReportToCsv() throws IOException {
    List<Product> allProducts = productRepository.findAll();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8));

    // CSV Header
    writer.println(
        "Reference,Name,Category,Unit Type,Stock Quantity,"
            + "Average Purchase Price,Stock Value,Price On Site,Price Delivered");

    // CSV Data
    for (Product product : allProducts) {
      BigDecimal stockValue =
          product.getAveragePurchasePrice().multiply(product.getStockQuantity());

      writer.println(
          String.join(
              ",",
              encodeCsvValue(product.getReference()),
              encodeCsvValue(product.getName()),
              encodeCsvValue(product.getCategory() != null ? product.getCategory() : "N/A"),
              encodeCsvValue(product.getUnitType().toString()),
              encodeCsvValue(String.valueOf(product.getStockQuantity())),
              encodeCsvValue(product.getAveragePurchasePrice().toString()),
              encodeCsvValue(stockValue.toString()),
              encodeCsvValue(
                  product.getPriceOnSite() != null ? product.getPriceOnSite().toString() : "0.0"),
              encodeCsvValue(
                  product.getPriceDelivered() != null
                      ? product.getPriceDelivered().toString()
                      : "0.0")));
    }

    writer.flush();
    return outputStream.toByteArray();
  }

  /**
   * Export stock report to Excel.
   *
   * @return Excel bytes
   * @throws IOException if export fails
   */
  public byte[] exportStockReportToExcel() throws IOException {
    List<Product> allProducts = productRepository.findAll();

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Stock Report");

      // Header Row
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Reference");
      headerRow.createCell(1).setCellValue("Name");
      headerRow.createCell(2).setCellValue("Category");
      headerRow.createCell(3).setCellValue("Unit Type");
      headerRow.createCell(4).setCellValue("Stock Quantity");
      headerRow.createCell(5).setCellValue("Average Purchase Price");
      headerRow.createCell(6).setCellValue("Stock Value");
      headerRow.createCell(7).setCellValue("Price On Site");
      headerRow.createCell(8).setCellValue("Price Delivered");

      // Data Rows
      int rowNum = 1;
      for (Product product : allProducts) {
        BigDecimal stockValue =
            product.getAveragePurchasePrice().multiply(product.getStockQuantity());

        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(product.getReference());
        row.createCell(1).setCellValue(product.getName());
        row.createCell(2)
            .setCellValue(product.getCategory() != null ? product.getCategory() : "N/A");
        row.createCell(3).setCellValue(product.getUnitType().toString());
        row.createCell(4).setCellValue(product.getStockQuantity().doubleValue());
        row.createCell(5).setCellValue(product.getAveragePurchasePrice().doubleValue());
        row.createCell(6).setCellValue(stockValue.doubleValue());
        row.createCell(7)
            .setCellValue(
                product.getPriceOnSite() != null ? product.getPriceOnSite().doubleValue() : 0.0);
        row.createCell(8)
            .setCellValue(
                product.getPriceDelivered() != null
                    ? product.getPriceDelivered().doubleValue()
                    : 0.0);
      }

      // Auto-size columns
      for (int i = 0; i < 9; i++) {
        sheet.autoSizeColumn(i);
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }
}
