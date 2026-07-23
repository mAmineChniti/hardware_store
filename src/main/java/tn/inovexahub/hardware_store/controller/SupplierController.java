package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Supplier>> getAllSuppliers() {
    return ResponseEntity.ok(supplierService.getAllSuppliers());
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Supplier> getSupplierById(@PathVariable Long id) {
    return supplierService
        .getSupplierById(id)
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Supplier> getSupplierByTaxId(@PathVariable String taxId) {
    return supplierService
        .getSupplierByTaxId(taxId)
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Supplier>> searchSuppliers(@RequestParam String name) {
    return ResponseEntity.ok(supplierService.searchSuppliers(name));
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody Supplier supplier) {
    Supplier createdSupplier = supplierService.createSupplier(supplier);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSupplier);
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
                    schema = @Schema(implementation = Supplier.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Supplier> updateSupplier(
      @PathVariable Long id, @Valid @RequestBody Supplier supplierDetails) {
    try {
      Supplier updatedSupplier = supplierService.updateSupplier(id, supplierDetails);
      return ResponseEntity.ok(updatedSupplier);
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
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
    try {
      supplierService.deleteSupplier(id);
      return ResponseEntity.noContent().build();
    } catch (SupplierNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }
}
