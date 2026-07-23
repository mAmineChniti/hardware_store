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
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
import tn.inovexahub.hardware_store.dto.ProductCostRequest;
import tn.inovexahub.hardware_store.dto.ProductCostResponse;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductCost;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.service.ProductService;
import tn.inovexahub.hardware_store.service.SupplierService;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management including conditionings and costs")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

  private final ProductService productService;
  private final SupplierService supplierService;

  public ProductController(ProductService productService, SupplierService supplierService) {
    this.productService = productService;
    this.supplierService = supplierService;
  }

  // ==================== Product CRUD ====================

  @GetMapping
  @Operation(summary = "Get all products", description = "Retrieve all products")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of products retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Product>> getAllProducts() {
    return ResponseEntity.ok(productService.getAllProducts());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<Product> getProductById(
      @Parameter(description = "ID of product to retrieve", example = "1", required = true)
          @PathVariable
          Long id) {
    return productService
        .getProductById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/reference/{reference}")
  @Operation(
      summary = "Get product by reference",
      description = "Retrieve a specific product by its reference code")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<Product> getProductByReference(
      @Parameter(description = "Product reference code", example = "CIM-325", required = true)
          @PathVariable
          String reference) {
    return productService
        .getProductByReference(reference)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Create new product", description = "Create a new product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Product.class))),
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
  public ResponseEntity<Product> createProduct(
      @RequestBody(description = "Product creation payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Product product) {
    Product createdProduct = productService.createProduct(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update product", description = "Update an existing product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Product.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<Product> updateProduct(
      @Parameter(description = "ID of product to update", example = "1", required = true)
          @PathVariable
          Long id,
      @RequestBody(description = "Product update payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          Product productDetails) {
    try {
      Product updatedProduct = productService.updateProduct(id, productDetails);
      return ResponseEntity.ok(updatedProduct);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete product", description = "Delete a product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Product deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<Void> deleteProduct(
      @Parameter(description = "ID of product to delete", example = "1", required = true)
          @PathVariable
          Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // ==================== Search and Filter ====================

  @GetMapping("/search")
  @Operation(
      summary = "Search products",
      description = "Search products by keyword in name or reference")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Matching products retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Product>> searchProducts(
      @Parameter(
              description = "Search keyword for product name or reference",
              example = "ciment",
              required = true)
          @RequestParam
          String keyword) {
    return ResponseEntity.ok(productService.searchProducts(keyword));
  }

  @GetMapping("/category/{category}")
  @Operation(
      summary = "Get products by category",
      description = "Retrieve all products in a specific category")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products in category retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Product>> getProductsByCategory(
      @Parameter(description = "Product category name", example = "Matériaux", required = true)
          @PathVariable
          String category) {
    return ResponseEntity.ok(productService.getProductsByCategory(category));
  }

  @GetMapping("/heavy-materials")
  @Operation(
      summary = "Get heavy materials",
      description = "Retrieve all products marked as heavy materials (dual pricing)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Heavy material products retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Product>> getHeavyMaterials() {
    return ResponseEntity.ok(productService.getHeavyMaterials());
  }

  @GetMapping("/low-stock")
  @Operation(
      summary = "Get low stock products",
      description = "Retrieve products with stock below threshold")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Low stock products retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<Product>> getLowStockProducts(
      @Parameter(description = "Minimum stock threshold", example = "10.0")
          @RequestParam(defaultValue = "10.0")
          BigDecimal threshold) {
    return ResponseEntity.ok(productService.getLowStockProducts(threshold));
  }

  // ==================== Product Conditionings ====================

  @GetMapping("/{productId}/conditionings")
  @Operation(
      summary = "Get product conditionings",
      description = "Retrieve all conditionings for a specific product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product conditionings retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ProductConditioning.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductConditioning>> getProductConditionings(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    return ResponseEntity.ok(productService.getProductConditionings(productId));
  }

  @PostMapping("/{productId}/conditionings")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add product conditioning",
      description = "Add a new conditioning to a product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product conditioning created",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductConditioning.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<ProductConditioning> addProductConditioning(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @RequestBody(description = "Conditioning creation payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ProductConditioning conditioning) {
    try {
      ProductConditioning createdConditioning =
          productService.addProductConditioning(productId, conditioning);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdConditioning);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @PutMapping("/conditionings/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Update product conditioning",
      description = "Update an existing product conditioning")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product conditioning updated",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductConditioning.class))),
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
            description = "Conditioning not found",
            content = @Content)
      })
  public ResponseEntity<ProductConditioning> updateProductConditioning(
      @Parameter(description = "Conditioning ID", example = "1", required = true) @PathVariable
          Long id,
      @RequestBody(description = "Conditioning update payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ProductConditioning conditioningDetails) {
    try {
      ProductConditioning updatedConditioning =
          productService.updateProductConditioning(id, conditioningDetails);
      return ResponseEntity.ok(updatedConditioning);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping("/conditionings/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete product conditioning", description = "Delete a product conditioning")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Conditioning deleted",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content)
      })
  public ResponseEntity<Void> deleteProductConditioning(
      @Parameter(description = "Conditioning ID to delete", example = "1", required = true)
          @PathVariable
          Long id) {
    productService.deleteProductConditioning(id);
    return ResponseEntity.noContent().build();
  }

  // ==================== Product Costs ====================

  @GetMapping("/{productId}/costs")
  @Operation(
      summary = "Get product cost history",
      description = "Retrieve the complete cost history for a product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cost history retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ProductCostResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<List<ProductCostResponse>> getProductCostHistory(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    Product product = requireProduct(productId);
    List<ProductCostResponse> responses =
        productService.getProductCostHistory(productId).stream()
            .map(cost -> toResponse(cost, product))
            .toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{productId}/costs/current")
  @Operation(
      summary = "Get current product cost",
      description = "Retrieve the most recent cost for a product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Current product cost retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductCostResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found, or no cost entry found",
            content = @Content)
      })
  public ResponseEntity<ProductCostResponse> getCurrentProductCost(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    Product product = requireProduct(productId);
    return productService
        .getCurrentProductCost(productId)
        .map(cost -> toResponse(cost, product))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{productId}/costs")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add product cost",
      description = "Add a new cost entry for a product (updates PAMP)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Product cost created",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductCostResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Product or supplier not found",
            content = @Content)
      })
  public ResponseEntity<ProductCostResponse> addProductCost(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @RequestBody(description = "Product cost creation payload", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          ProductCostRequest productCostRequest) {
    Product product = requireProduct(productId);

    Supplier supplier = null;
    if (productCostRequest.getSupplierId() != null) {
      supplier =
          supplierService
              .getSupplierById(productCostRequest.getSupplierId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    try {
      ProductCost productCost =
          productService.addProductCost(
              productId,
              productCostRequest.getUnitCost(),
              productCostRequest.getEffectiveDate(),
              supplier,
              productCostRequest.getNotes());
      return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(productCost, product));
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping("/{productId}/costs/{date}")
  @Operation(
      summary = "Get product cost for specific date",
      description = "Retrieve the cost for a product on a specific date")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cost entry retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductCostResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found, or cost entry not found",
            content = @Content)
      })
  public ResponseEntity<ProductCostResponse> getProductCostForDate(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "Target date", example = "2024-01-01", required = true)
          @PathVariable
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    Product product = requireProduct(productId);
    return productService
        .getProductCostForDate(productId, date)
        .map(cost -> toResponse(cost, product))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{productId}/costs/between")
  @Operation(
      summary = "Get product costs between dates",
      description = "Retrieve costs for a product within a date range")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cost entries retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = ProductCostResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<List<ProductCostResponse>> getProductCostsBetweenDates(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(description = "Start date", example = "2024-01-01", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date", example = "2024-01-31", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    Product product = requireProduct(productId);
    List<ProductCostResponse> responses =
        productService.getProductCostsBetweenDates(productId, startDate, endDate).stream()
            .map(cost -> toResponse(cost, product))
            .toList();
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/costs/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete product cost", description = "Delete a product cost entry")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Product cost deleted",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin access required",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Product cost not found",
            content = @Content)
      })
  public ResponseEntity<Void> deleteProductCost(
      @Parameter(description = "Cost entry ID to delete", example = "1", required = true)
          @PathVariable
          Long id) {
    try {
      productService.deleteProductCost(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  // ==================== Stock Management ====================

  @PostMapping("/{productId}/stock")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Update stock quantity",
      description = "Update the stock quantity for a product (positive to add, negative to remove)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Stock updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Product.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid quantity change or insufficient stock",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient role privileges",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<Product> updateStockQuantity(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @Parameter(
              description = "Stock quantity change (+10 to add, -5 to subtract)",
              example = "10.0",
              required = true)
          @RequestParam
          BigDecimal quantityChange) {
    try {
      productService.updateStockQuantity(productId, quantityChange);
      return productService
          .getProductById(productId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private Product requireProduct(Long productId) {
    return productService
        .getProductById(productId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
  }

  private ProductCostResponse toResponse(ProductCost cost, Product product) {
    ProductCostResponse response = new ProductCostResponse();
    response.setId(cost.getId());
    response.setProductId(product.getId());
    response.setProductName(product.getName());
    response.setUnitCost(cost.getUnitCost());
    response.setEffectiveDate(cost.getEffectiveDate());
    if (cost.getSupplier() != null) {
      response.setSupplier(cost.getSupplier().getName());
    }
    response.setNotes(cost.getNotes());
    response.setCreatedAt(cost.getCreatedAt());
    return response;
  }
}
