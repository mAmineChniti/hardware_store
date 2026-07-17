package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.entity.Document;
import tn.inovexahub.hardware_store.entity.DocumentLine;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.enums.DocumentStatus;
import tn.inovexahub.hardware_store.enums.DocumentType;
import tn.inovexahub.hardware_store.service.DocumentService;
import tn.inovexahub.hardware_store.service.PdfGenerationService;
import tn.inovexahub.hardware_store.service.ProductService;

@RestController
@RequestMapping("/api/documents")
@Tag(
    name = "Documents",
    description = "Document management including quotes, delivery notes, and invoices")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

  private final DocumentService documentService;
  private final ProductService productService;
  private final PdfGenerationService pdfGenerationService;

  public DocumentController(
      DocumentService documentService,
      ProductService productService,
      PdfGenerationService pdfGenerationService) {
    this.documentService = documentService;
    this.productService = productService;
    this.pdfGenerationService = pdfGenerationService;
  }

  // ==================== Document CRUD ====================

  @GetMapping
  @Operation(summary = "Get all documents", description = "Retrieve all documents")
  public ResponseEntity<List<Document>> getAllDocuments() {
    return ResponseEntity.ok(documentService.getAllDocuments());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get document by ID", description = "Retrieve a specific document by its ID")
  public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
    return documentService
        .getDocumentById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/number/{documentNumber}")
  @Operation(
      summary = "Get document by number",
      description = "Retrieve a document by its document number")
  public ResponseEntity<Document> getDocumentByNumber(@PathVariable String documentNumber) {
    return documentService
        .getDocumentByNumber(documentNumber)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Create new document",
      description = "Create a new document (Quote, BL, or Invoice)")
  public ResponseEntity<Document> createDocument(@Valid @RequestBody Document document) {
    Document createdDocument = documentService.createDocument(document);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdDocument);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update document", description = "Update an existing document")
  public ResponseEntity<Document> updateDocument(
      @PathVariable Long id, @Valid @RequestBody Document documentDetails) {
    try {
      Document updatedDocument = documentService.updateDocument(id, documentDetails);
      return ResponseEntity.ok(updatedDocument);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete document", description = "Delete a document and its lines")
  public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
    try {
      documentService.deleteDocument(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  // ==================== Document Lines ====================

  @GetMapping("/{id}/lines")
  @Operation(
      summary = "Get document lines",
      description = "Retrieve all lines for a specific document")
  public ResponseEntity<List<DocumentLine>> getDocumentLines(@PathVariable Long id) {
    return ResponseEntity.ok(documentService.getDocumentLines(id));
  }

  @PostMapping("/{id}/lines")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Add document line", description = "Add a line to a document")
  public ResponseEntity<DocumentLine> addDocumentLine(
      @PathVariable Long id,
      @RequestParam Long productId,
      @RequestParam Double quantity,
      @RequestParam(required = false) BigDecimal unitPrice,
      @RequestParam(required = false) String conditioningDescription,
      @RequestParam(required = false) Boolean isDelivered,
      @RequestParam(required = false) Long conditioningId) {

    try {
      Product product =
          productService
              .getProductById(productId)
              .orElseThrow(() -> new RuntimeException("Product not found"));

      DocumentLine line =
          documentService.addDocumentLine(
              id,
              product,
              quantity,
              unitPrice,
              conditioningDescription,
              isDelivered,
              conditioningId);
      return ResponseEntity.status(HttpStatus.CREATED).body(line);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PutMapping("/lines/{lineId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update document line", description = "Update an existing document line")
  public ResponseEntity<DocumentLine> updateDocumentLine(
      @PathVariable Long lineId, @Valid @RequestBody DocumentLine lineDetails) {
    try {
      DocumentLine updatedLine = documentService.updateDocumentLine(lineId, lineDetails);
      return ResponseEntity.ok(updatedLine);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/lines/{lineId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Delete document line", description = "Delete a document line")
  public ResponseEntity<Void> deleteDocumentLine(@PathVariable Long lineId) {
    try {
      documentService.deleteDocumentLine(lineId);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  // ==================== Document Workflow ====================

  @PostMapping("/{id}/validate")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Validate document",
      description =
          "Validate a document (DRAFT -> VALIDATED). Deducts stock for BL/Invoice"
              + " and adds credit history for credit sales.")
  public ResponseEntity<Document> validateDocument(@PathVariable Long id) {
    try {
      Document validatedDocument = documentService.validateDocument(id);
      return ResponseEntity.ok(validatedDocument);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Cancel document",
      description =
          "Cancel a document. Restores stock if validated and adds adjustment credit history for credit sales.")
  public ResponseEntity<Document> cancelDocument(@PathVariable Long id) {
    try {
      Document cancelledDocument = documentService.cancelDocument(id);
      return ResponseEntity.ok(cancelledDocument);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping("/{id}/convert-to-bl")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Convert Quote to Delivery Note",
      description = "Convert a Quote to a Delivery Note (BL)")
  public ResponseEntity<Document> convertQuoteToDeliveryNote(@PathVariable Long id) {
    try {
      Document bl = documentService.convertQuoteToDeliveryNote(id);
      return ResponseEntity.status(HttpStatus.CREATED).body(bl);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping("/{id}/convert-to-invoice")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Convert Delivery Note to Invoice",
      description = "Convert a Delivery Note to an Invoice")
  public ResponseEntity<Document> convertDeliveryNoteToInvoice(@PathVariable Long id) {
    try {
      Document invoice = documentService.convertDeliveryNoteToInvoice(id);
      return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  // ==================== Filtering and Reporting ====================

  @GetMapping("/client/{clientId}")
  @Operation(
      summary = "Get documents by client",
      description = "Retrieve all documents for a specific client")
  public ResponseEntity<List<Document>> getDocumentsByClient(@PathVariable Long clientId) {
    return ResponseEntity.ok(documentService.getDocumentsByClient(clientId));
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get documents by user",
      description = "Retrieve all documents created by a specific user")
  public ResponseEntity<List<Document>> getDocumentsByUser(@PathVariable Long userId) {
    return ResponseEntity.ok(documentService.getDocumentsByUser(userId));
  }

  @GetMapping("/type/{documentType}")
  @Operation(
      summary = "Get documents by type",
      description = "Retrieve all documents of a specific type")
  public ResponseEntity<List<Document>> getDocumentsByType(
      @PathVariable DocumentType documentType) {
    return ResponseEntity.ok(documentService.getDocumentsByType(documentType));
  }

  @GetMapping("/status/{status}")
  @Operation(
      summary = "Get documents by status",
      description = "Retrieve all documents with a specific status")
  public ResponseEntity<List<Document>> getDocumentsByStatus(@PathVariable DocumentStatus status) {
    return ResponseEntity.ok(documentService.getDocumentsByStatus(status));
  }

  @GetMapping("/client/{clientId}/credit-sales")
  @Operation(
      summary = "Get credit sales by client",
      description = "Retrieve all credit sales for a specific client")
  public ResponseEntity<List<Document>> getCreditSalesByClient(@PathVariable Long clientId) {
    return ResponseEntity.ok(documentService.getCreditSalesByClient(clientId));
  }

  // ==================== PDF Generation ====================

  @GetMapping("/{id}/pdf")
  @Operation(
      summary = "Generate document PDF",
      description = "Generate a PDF for the document (Quote, BL, or Invoice)")
  public ResponseEntity<byte[]> generateDocumentPdf(@PathVariable Long id) {
    Document document =
        documentService
            .getDocumentById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

    try {
      byte[] pdfBytes = pdfGenerationService.generateDocumentPdf(document);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(
          ContentDisposition.builder("attachment")
              .filename(document.getDocumentNumber() + ".pdf")
              .build());

      return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF");
    }
  }
}
