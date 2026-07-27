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
import java.util.List;
import org.springframework.http.HttpStatus;
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
import tn.inovexahub.hardware_store.dto.SupplierRequest;
import tn.inovexahub.hardware_store.dto.SupplierResponse;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.service.SupplierService;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Supplier management")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

  private final SupplierService supplierService;

  public SupplierController(SupplierService supplierService) {
    this.supplierService = supplierService;
  }

  @GetMapping
  @Operation(summary = "Get all suppliers", description = "Retrieve all active suppliers")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of suppliers retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(schema = @Schema(implementation = SupplierResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
    return ResponseEntity.ok(
        supplierService.getAllSuppliers().stream().map(this::toResponse).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get supplier by ID", description = "Retrieve a specific supplier by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Supplier retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SupplierResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<SupplierResponse> getSupplierById(
      @Parameter(description = "Supplier ID", example = "1", required = true) @PathVariable
          Long id) {
    return supplierService
        .getSupplierById(id)
        .map(this::toResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/tax-id/{taxId}")
  @Operation(
      summary = "Get supplier by tax ID",
      description = "Retrieve a supplier by tax identification number")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Supplier retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SupplierResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<SupplierResponse> getSupplierByTaxId(
      @Parameter(
              description = "Tax Identification Number (Matricule Fiscal)",
              example = "123456789",
              required = true)
          @PathVariable
          String taxId) {
    return supplierService
        .getSupplierByTaxId(taxId)
        .map(this::toResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/search")
  @Operation(summary = "Search suppliers", description = "Search suppliers by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of matching suppliers retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(schema = @Schema(implementation = SupplierResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<SupplierResponse>> searchSuppliers(
      @Parameter(description = "Supplier name keyword", example = "SOTUVER", required = true)
          @RequestParam
          String name) {
    return ResponseEntity.ok(
        supplierService.searchSuppliers(name).stream().map(this::toResponse).toList());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Create new supplier", description = "Create a new supplier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Supplier created",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SupplierResponse.class))),
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
  public ResponseEntity<SupplierResponse> createSupplier(
      @RequestBody(description = "Supplier details payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          SupplierRequest supplierRequest) {
    Supplier createdSupplier = supplierService.createSupplier(toEntity(supplierRequest));
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdSupplier));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update supplier", description = "Update an existing supplier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Supplier updated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SupplierResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<SupplierResponse> updateSupplier(
      @Parameter(description = "Supplier ID to update", example = "1", required = true)
          @PathVariable
          Long id,
      @RequestBody(description = "Updated supplier details payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          SupplierRequest supplierRequest) {
    try {
      Supplier updatedSupplier = supplierService.updateSupplier(id, toEntity(supplierRequest));
      return ResponseEntity.ok(toResponse(updatedSupplier));
    } catch (SupplierNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete supplier", description = "Soft delete a supplier")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Supplier deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Void> deleteSupplier(
      @Parameter(description = "Supplier ID to delete", example = "1", required = true)
          @PathVariable
          Long id) {
    try {
      supplierService.deleteSupplier(id);
      return ResponseEntity.noContent().build();
    } catch (SupplierNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  private Supplier toEntity(SupplierRequest request) {
    Supplier supplier = new Supplier();
    supplier.setName(request.getName());
    supplier.setPhone(request.getPhone());
    supplier.setEmail(request.getEmail());
    supplier.setAddress(request.getAddress());
    supplier.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
    supplier.setContactPerson(request.getContactPerson());
    supplier.setPaymentTerms(request.getPaymentTerms());
    supplier.setNotes(request.getNotes());
    return supplier;
  }

  private SupplierResponse toResponse(Supplier supplier) {
    SupplierResponse response = new SupplierResponse();
    response.setId(supplier.getId());
    response.setName(supplier.getName());
    response.setPhone(supplier.getPhone());
    response.setEmail(supplier.getEmail());
    response.setAddress(supplier.getAddress());
    response.setTaxIdentificationNumber(supplier.getTaxIdentificationNumber());
    response.setContactPerson(supplier.getContactPerson());
    response.setPaymentTerms(supplier.getPaymentTerms());
    response.setNotes(supplier.getNotes());
    response.setDeleted(supplier.getDeleted());
    response.setCreatedAt(supplier.getCreatedAt());
    response.setUpdatedAt(supplier.getUpdatedAt());
    return response;
  }
}
