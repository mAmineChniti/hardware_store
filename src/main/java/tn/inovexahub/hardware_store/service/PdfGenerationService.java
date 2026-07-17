package tn.inovexahub.hardware_store.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.enums.DocumentType;

@Service
public class PdfGenerationService {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final float MARGIN = 50;
  private static final float LINE_HEIGHT = 20;
  private static final float TABLE_ROW_HEIGHT = 25;

  private NumberFormat getCurrencyFormat() {
    return NumberFormat.getCurrencyInstance(Locale.FRANCE);
  }

  /**
   * Generate PDF for a document.
   *
   * @param document Document to generate PDF for
   * @return PDF as byte array
   * @throws IOException if PDF generation fails
   */
  public byte[] generateDocumentPdf(Document document) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);

      PDPageContentStream contentStream =
          new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

      float yPosition = PDRectangle.A4.getHeight() - MARGIN;

      // Draw header
      yPosition = drawHeader(contentStream, document, yPosition);

      // Draw client info
      yPosition = drawClientInfo(contentStream, document, yPosition);

      // Draw table header
      yPosition = drawTableHeader(contentStream, yPosition);

      // Draw document lines with page continuation support
      yPosition = drawDocumentLinesWithPagination(doc, contentStream, document, yPosition, page);

      // Draw totals on the current page
      yPosition = drawTotals(contentStream, document, yPosition);

      // Draw footer
      drawFooter(contentStream, document);

      contentStream.close();

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      doc.save(outputStream);
      return outputStream.toByteArray();
    }
  }

  private float drawHeader(PDPageContentStream contentStream, Document document, float yPosition)
      throws IOException {
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, yPosition);
    contentStream.showText(getDocumentTypeName(document.getDocumentType()));
    contentStream.endText();

    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
    yPosition -= LINE_HEIGHT;

    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, yPosition);
    contentStream.showText("Numéro: " + document.getDocumentNumber());
    contentStream.endText();

    yPosition -= LINE_HEIGHT;
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, yPosition);
    contentStream.showText("Date: " + document.getDate().format(DATE_FORMAT));
    contentStream.endText();

    yPosition -= LINE_HEIGHT;
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, yPosition);
    contentStream.showText("Statut: " + document.getStatus().name());
    contentStream.endText();

    return yPosition - LINE_HEIGHT * 2;
  }

  private float drawClientInfo(
      PDPageContentStream contentStream, Document document, float yPosition) throws IOException {
    if (document.getClient() != null) {
      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
      contentStream.beginText();
      contentStream.newLineAtOffset(MARGIN, yPosition);
      contentStream.showText("Client:");
      contentStream.endText();

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
      yPosition -= LINE_HEIGHT;

      contentStream.beginText();
      contentStream.newLineAtOffset(MARGIN, yPosition);
      contentStream.showText(document.getClient().getName());
      contentStream.endText();

      if (document.getClient().getAddress() != null) {
        yPosition -= LINE_HEIGHT;
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(document.getClient().getAddress());
        contentStream.endText();
      }

      if (document.getClient().getPhone() != null) {
        yPosition -= LINE_HEIGHT;
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Tél: " + document.getClient().getPhone());
        contentStream.endText();
      }

      if (document.getClient().getTaxIdentificationNumber() != null) {
        yPosition -= LINE_HEIGHT;
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(
            "Matricule Fiscal: " + document.getClient().getTaxIdentificationNumber());
        contentStream.endText();
      }
    }

    return yPosition - LINE_HEIGHT * 2;
  }

  private float drawTableHeader(PDPageContentStream contentStream, float yPosition)
      throws IOException {
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);

    // Draw table header line
    contentStream.setLineWidth(0.5f);
    contentStream.moveTo(MARGIN, yPosition);
    contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition);
    contentStream.stroke();

    yPosition -= LINE_HEIGHT;

    // Column headers
    float[] columnPositions = {MARGIN, MARGIN + 50, MARGIN + 200, MARGIN + 350, MARGIN + 450};
    String[] headers = {"#", "Désignation", "Quantité", "Prix Unitaire", "Total HT"};

    for (int i = 0; i < headers.length; i++) {
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[i], yPosition);
      contentStream.showText(headers[i]);
      contentStream.endText();
    }

    // Draw table header line
    contentStream.moveTo(MARGIN, yPosition - 5);
    contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition - 5);
    contentStream.stroke();

    return yPosition - TABLE_ROW_HEIGHT;
  }

  private float drawDocumentLinesWithPagination(
      PDDocument doc,
      PDPageContentStream contentStream,
      Document document,
      float yPosition,
      PDPage currentPage)
      throws IOException {
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

    float[] columnPositions = {MARGIN, MARGIN + 50, MARGIN + 200, MARGIN + 350, MARGIN + 450};

    for (DocumentLine line : document.getLines()) {
      // Check if we need a new page
      if (yPosition < MARGIN + TABLE_ROW_HEIGHT * 2) {
        // Close current content stream
        contentStream.close();

        // Add new page
        currentPage = new PDPage(PDRectangle.A4);
        doc.addPage(currentPage);

        // Create new content stream for the new page
        contentStream =
            new PDPageContentStream(doc, currentPage, PDPageContentStream.AppendMode.APPEND, true);

        // Reset y position
        yPosition = PDRectangle.A4.getHeight() - MARGIN;

        // Redraw table header on new page
        yPosition = drawTableHeader(contentStream, yPosition);

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
      }

      // Line number
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[0], yPosition);
      contentStream.showText(String.valueOf(line.getLineNumber()));
      contentStream.endText();

      // Product name
      String productName =
          line.getProduct() != null
              ? line.getProduct().getName()
              : (line.getConditioningDescription() != null
                  ? line.getConditioningDescription()
                  : "N/A");
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[1], yPosition);
      contentStream.showText(truncateText(productName, 140));
      contentStream.endText();

      // Quantity
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[2], yPosition);
      contentStream.showText(String.format("%.3f", line.getQuantity()));
      contentStream.endText();

      // Unit price
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[3], yPosition);
      contentStream.showText(getCurrencyFormat().format(line.getUnitPrice()));
      contentStream.endText();

      // Total
      contentStream.beginText();
      contentStream.newLineAtOffset(columnPositions[4], yPosition);
      contentStream.showText(getCurrencyFormat().format(line.getTotalLineExcludingTax()));
      contentStream.endText();

      yPosition -= TABLE_ROW_HEIGHT;
    }

    // Draw table bottom line
    contentStream.moveTo(MARGIN, yPosition);
    contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition);
    contentStream.stroke();

    return yPosition - LINE_HEIGHT;
  }

  private float drawTotals(PDPageContentStream contentStream, Document document, float yPosition)
      throws IOException {
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);

    float totalsX = PDRectangle.A4.getWidth() - MARGIN - 200;

    // Total HT
    yPosition -= LINE_HEIGHT;
    contentStream.beginText();
    contentStream.newLineAtOffset(totalsX, yPosition);
    contentStream.showText(
        "Total HT: " + getCurrencyFormat().format(document.getTotalExcludingTax()));
    contentStream.endText();

    // TVA
    yPosition -= LINE_HEIGHT;
    contentStream.beginText();
    contentStream.newLineAtOffset(totalsX, yPosition);
    BigDecimal vatPercentage = document.getVatRate().multiply(BigDecimal.valueOf(100));
    contentStream.showText(
        "TVA (" + vatPercentage + "%): " + getCurrencyFormat().format(document.getTotalVat()));
    contentStream.endText();

    // Transport fee (for BL and Invoice)
    if (document.getDocumentType() == DocumentType.DELIVERY_NOTE
        || document.getDocumentType() == DocumentType.INVOICE) {
      yPosition -= LINE_HEIGHT;
      contentStream.beginText();
      contentStream.newLineAtOffset(totalsX, yPosition);
      contentStream.showText(
          "Frais Transport: " + getCurrencyFormat().format(document.getTransportFee()));
      contentStream.endText();
    }

    // Stamp duty (for Invoice)
    if (document.getDocumentType() == DocumentType.INVOICE) {
      yPosition -= LINE_HEIGHT;
      contentStream.beginText();
      contentStream.newLineAtOffset(totalsX, yPosition);
      contentStream.showText(
          "Droit Timbre: " + getCurrencyFormat().format(document.getStampDuty()));
      contentStream.endText();
    }

    // Total TTC
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
    yPosition -= LINE_HEIGHT * 1.5f;
    contentStream.beginText();
    contentStream.newLineAtOffset(totalsX, yPosition);
    contentStream.showText(
        "Total TTC: " + getCurrencyFormat().format(document.getTotalIncludingTax()));
    contentStream.endText();

    // Credit sale indicator
    if (document.getIsCreditSale()) {
      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
      yPosition -= LINE_HEIGHT;
      contentStream.beginText();
      contentStream.newLineAtOffset(totalsX, yPosition);
      contentStream.showText("* Vente à crédit");
      contentStream.endText();
    }

    return yPosition - LINE_HEIGHT * 2;
  }

  private void drawFooter(PDPageContentStream contentStream, Document document) throws IOException {
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
    float footerY = MARGIN;

    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, footerY);
    contentStream.showText("INOVEXAHUB - Système de Gestion Commerciale");
    contentStream.endText();

    footerY -= LINE_HEIGHT;
    contentStream.beginText();
    contentStream.newLineAtOffset(MARGIN, footerY);
    contentStream.showText(
        "Document généré automatiquement - " + document.getDate().format(DATE_FORMAT));
    contentStream.endText();
  }

  private String getDocumentTypeName(DocumentType type) {
    switch (type) {
      case QUOTE:
        return "DEVIS";
      case DELIVERY_NOTE:
        return "BON DE LIVRAISON";
      case INVOICE:
        return "FACTURE";
      default:
        return "DOCUMENT";
    }
  }

  private String truncateText(String text, int maxLength) {
    if (text == null) {
      return "";
    }
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength - 3) + "...";
  }
}
