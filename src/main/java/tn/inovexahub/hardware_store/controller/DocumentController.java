package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of documents retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getAllDocuments() {
    return ResponseEntity.ok(documentService.getAllDocuments());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get document by ID", description = "Retrieve a specific document by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
      })
  public ResponseEntity<Document> getDocumentById(
      @Parameter(description = "ID of document to retrieve", example = "1", required = true)
          @PathVariable
          Long id) {
    return documentService
        .getDocumentById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/number/{documentNumber}")
  @Operation(
      summary = "Get document by number",
      description = "Retrieve a document by its document number")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
      })
  public ResponseEntity<Document> getDocumentByNumber(
      @Parameter(
              description = "Document number (e.g. FACT-2024-001)",
              example = "FACT-2024-001",
              required = true)
          @PathVariable
          String documentNumber) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Document created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Document> createDocument(
      @RequestBody(description = "Document creation payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Document document) {
    Document createdDocument = documentService.createDocument(document);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdDocument);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update document", description = "Update an existing document")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
      })
  public ResponseEntity<Document> updateDocument(
      @Parameter(description = "ID of document to update", example = "1", required = true)
          @PathVariable
          Long id,
      @RequestBody(description = "Updated document details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Document documentDetails) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Document deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
      })
  public ResponseEntity<Void> deleteDocument(
      @Parameter(description = "ID of document to delete", example = "1", required = true)
          @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document lines retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = DocumentLine.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<DocumentLine>> getDocumentLines(
      @Parameter(description = "Document ID", example = "1", required = true) @PathVariable
          Long id) {
    return ResponseEntity.ok(documentService.getDocumentLines(id));
  }

  @PostMapping("/{id}/lines")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Add document line", description = "Add a line to a document")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Document line added successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DocumentLine.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid line item details or missing product",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<DocumentLine> addDocumentLine(
      @Parameter(description = "Document ID", example = "1", required = true) @PathVariable Long id,
      @Parameter(description = "Product ID", example = "10", required = true) @RequestParam
          Long productId,
      @Parameter(description = "Line item quantity", example = "5.0", required = true) @RequestParam
          BigDecimal quantity,
      @Parameter(description = "Custom unit price (optional)", example = "15.500")
          @RequestParam(required = false)
          BigDecimal unitPrice,
      @Parameter(description = "Conditioning unit description (optional)", example = "Boîte de 10")
          @RequestParam(required = false)
          String conditioningDescription,
      @Parameter(description = "Delivered status (optional)", example = "true")
          @RequestParam(required = false)
          Boolean isDelivered,
      @Parameter(description = "Product conditioning ID (optional)", example = "2")
          @RequestParam(required = false)
          Long conditioningId) {

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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document line updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DocumentLine.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Document line not found",
            content = @Content)
      })
  public ResponseEntity<DocumentLine> updateDocumentLine(
      @Parameter(description = "Line ID", example = "1", required = true) @PathVariable Long lineId,
      @RequestBody(description = "Updated document line details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          DocumentLine lineDetails) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Document line deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Document line not found",
            content = @Content)
      })
  public ResponseEntity<Void> deleteDocumentLine(
      @Parameter(description = "Line ID to delete", example = "1", required = true) @PathVariable
          Long lineId) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document validated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Document cannot be validated or invalid status",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Document> validateDocument(
      @Parameter(description = "ID of document to validate", example = "1", required = true)
          @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document cancelled successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Document cannot be cancelled",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Document> cancelDocument(
      @Parameter(description = "ID of document to cancel", example = "1", required = true)
          @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Quote converted to Delivery Note successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Document is not a Quote or cannot be converted",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Document> convertQuoteToDeliveryNote(
      @Parameter(description = "Quote ID to convert", example = "1", required = true) @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Delivery Note converted to Invoice successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Document is not a BL or cannot be converted",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content)
      })
  public ResponseEntity<Document> convertDeliveryNoteToInvoice(
      @Parameter(description = "Delivery Note ID to convert", example = "1", required = true)
          @PathVariable
          Long id) {
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
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documents for client retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getDocumentsByClient(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable
          Long clientId) {
    return ResponseEntity.ok(documentService.getDocumentsByClient(clientId));
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get documents by user",
      description = "Retrieve all documents created by a specific user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documents created by user retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getDocumentsByUser(
      @Parameter(description = "User ID", example = "1", required = true) @PathVariable
          Long userId) {
    return ResponseEntity.ok(documentService.getDocumentsByUser(userId));
  }

  @GetMapping("/type/{documentType}")
  @Operation(
      summary = "Get documents by type",
      description = "Retrieve all documents of a specific type")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documents of type retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getDocumentsByType(
      @Parameter(
              description = "Document type (QUOTE, DELIVERY_NOTE, INVOICE, CREDIT_NOTE)",
              required = true)
          @PathVariable
          DocumentType documentType) {
    return ResponseEntity.ok(documentService.getDocumentsByType(documentType));
  }

  @GetMapping("/status/{status}")
  @Operation(
      summary = "Get documents by status",
      description = "Retrieve all documents with a specific status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documents with status retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getDocumentsByStatus(
      @Parameter(
              description = "Document status (DRAFT, VALIDATED, CANCELLED, PAID)",
              required = true)
          @PathVariable
          DocumentStatus status) {
    return ResponseEntity.ok(documentService.getDocumentsByStatus(status));
  }

  @GetMapping("/client/{clientId}/credit-sales")
  @Operation(
      summary = "Get credit sales by client",
      description = "Retrieve all credit sales for a specific client")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credit sales retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Document.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Document>> getCreditSalesByClient(
      @Parameter(description = "Client ID", example = "1", required = true) @PathVariable
          Long clientId) {
    return ResponseEntity.ok(documentService.getCreditSalesByClient(clientId));
  }

  // ==================== PDF Generation ====================

  @GetMapping("/{id}/pdf")
  @Operation(
      summary = "Generate document PDF",
      description = "Generate a PDF for the document (Quote, BL, or Invoice)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF document generated successfully",
            content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to generate PDF",
            content = @Content)
      })
  public ResponseEntity<byte[]> generateDocumentPdf(
      @Parameter(description = "Document ID to render as PDF", example = "1", required = true)
          @PathVariable
          Long id) {
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
