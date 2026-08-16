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
import tn.inovexahub.hardware_store.dto.BatchRequest;
import tn.inovexahub.hardware_store.dto.CreateProductRequest;
import tn.inovexahub.hardware_store.dto.CreateVariantRequest;
import tn.inovexahub.hardware_store.dto.UpdateBatchPricingRequest;
import tn.inovexahub.hardware_store.dto.UpdateBatchQuantityRequest;
import tn.inovexahub.hardware_store.dto.UpdateStockRequest;
import tn.inovexahub.hardware_store.dto.UpdateVariantRequest;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductBatch;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.ProductNotFoundException;
import tn.inovexahub.hardware_store.exception.ProductVariantNotFoundException;
import tn.inovexahub.hardware_store.exception.SkuAlreadyExistsException;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.service.ProductBatchService;
import tn.inovexahub.hardware_store.service.ProductService;
import tn.inovexahub.hardware_store.service.ProductVariantService;
import tn.inovexahub.hardware_store.service.SupplierService;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management including conditionings and costs")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

  private final ProductService productService;
  private final SupplierService supplierService;
  private final ProductBatchService productBatchService;
  private final ProductVariantService productVariantService;

  public ProductController(
      ProductService productService,
      SupplierService supplierService,
      ProductBatchService productBatchService,
      ProductVariantService productVariantService) {
    this.productService = productService;
    this.supplierService = supplierService;
    this.productBatchService = productBatchService;
    this.productVariantService = productVariantService;
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
  @Operation(
      summary = "Create new product",
      description = "Create a new product with initial batch and optional default supplier")
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
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
      })
  public ResponseEntity<Product> createProduct(
      @RequestBody(description = "Product and initial batch details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          CreateProductRequest request) {

    Product product = new Product();
    product.setReference(request.getReference());
    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setImage(request.getImage());
    product.setCategory(request.getCategory());
    product.setUnitType(request.getUnitType());
    product.setBaseUnit(request.getBaseUnit());

    if (request.getSupplierId() != null) {
      Supplier supplier =
          supplierService
              .getSupplierById(request.getSupplierId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
      product.setSupplier(supplier);
    }

    Product createdProduct =
        productService.createProductWithInitialBatch(
            product,
            request.getInitialQuantity(),
            request.getInitialUnitCost(),
            request.getInitialUnitPrice(),
            request.getSupplierId(),
            request.getNotes());
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
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
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
      @RequestBody(description = "Stock update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateStockRequest stockRequest) {
    try {
      productService.updateStockQuantity(productId, stockRequest.getQuantityChange());
      return productService
          .getProductById(productId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
    } catch (tn.inovexahub.hardware_store.exception.ProductNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  // ==================== Product Batch Management ====================

  @PostMapping("/{productId}/batches")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add inventory batch",
      description = "Add a new inventory batch for FIFO cost tracking")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Batch created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductBatch.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
      })
  public ResponseEntity<ProductBatch> addBatch(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @RequestBody(description = "Batch details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          BatchRequest batchRequest) {
    try {
      ProductBatch batch =
          productBatchService.addBatch(
              productId,
              batchRequest.getQuantity(),
              batchRequest.getUnitCost(),
              batchRequest.getUnitPrice(),
              batchRequest.getSupplierId(),
              batchRequest.getNotes());
      return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    } catch (tn.inovexahub.hardware_store.exception.ProductNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (tn.inovexahub.hardware_store.exception.SupplierNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping("/{productId}/batches")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get product batches",
      description = "Get all batches for a product ordered by creation date (oldest first)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batches retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductBatch.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductBatch>> getBatches(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    return ResponseEntity.ok(productBatchService.getBatchesByProductId(productId));
  }

  @GetMapping("/{productId}/batches/available")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get available batches",
      description = "Get batches with remaining quantity for a product")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Available batches retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductBatch.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductBatch>> getAvailableBatches(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    return ResponseEntity.ok(productBatchService.getAvailableBatchesByProductId(productId));
  }

  @PutMapping("/batches/{batchId}/quantity")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Update batch quantity",
      description = "Update the quantity of a batch (for corrections)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch quantity updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductBatch.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quantity", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content)
      })
  public ResponseEntity<ProductBatch> updateBatchQuantity(
      @Parameter(description = "Batch ID", example = "1", required = true) @PathVariable
          Long batchId,
      @RequestBody(description = "Batch quantity update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateBatchQuantityRequest quantityRequest) {
    try {
      ProductBatch batch =
          productBatchService.updateBatchQuantity(batchId, quantityRequest.getQuantity());
      return ResponseEntity.ok(batch);
    } catch (tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PutMapping("/batches/{batchId}/pricing")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Update batch pricing",
      description =
          "Update the cost and selling price of a batch. Price must be >= cost unless admin override is used.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batch pricing updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductBatch.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pricing", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content)
      })
  public ResponseEntity<ProductBatch> updateBatchPricing(
      @Parameter(description = "Batch ID", example = "1", required = true) @PathVariable
          Long batchId,
      @RequestBody(description = "Batch pricing update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateBatchPricingRequest pricingRequest,
      @Parameter(hidden = true) org.springframework.security.core.Authentication authentication) {
    try {
      boolean isAdmin =
          authentication.getAuthorities().stream()
              .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
      boolean effectiveOverride = Boolean.TRUE.equals(pricingRequest.getAdminOverride()) && isAdmin;
      ProductBatch batch =
          productBatchService.updateBatchPricing(
              batchId,
              pricingRequest.getUnitCost(),
              pricingRequest.getUnitPrice(),
              effectiveOverride);
      return ResponseEntity.ok(batch);
    } catch (tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping("/batches/{batchId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Delete batch", description = "Delete a batch and update product stock")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Batch deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content)
      })
  public ResponseEntity<Void> deleteBatch(
      @Parameter(description = "Batch ID", example = "1", required = true) @PathVariable
          Long batchId) {
    try {
      productBatchService.deleteBatch(batchId);
      return ResponseEntity.noContent().build();
    } catch (tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  // ==================== Product Variants ====================

  @PostMapping("/{productId}/variants")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Create product variant",
      description = "Create a new variant for a product with flexible JSON attributes")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Variant created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductVariant.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content)
      })
  public ResponseEntity<ProductVariant> createVariant(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId,
      @RequestBody(description = "Variant creation details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          CreateVariantRequest variantRequest) {
    try {
      ProductVariant variant =
          productVariantService.createVariant(
              productId,
              variantRequest.getSku(),
              variantRequest.getVariantName(),
              variantRequest.getAttributes());
      return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    } catch (ProductNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (SkuAlreadyExistsException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping("/{productId}/variants")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get product variants",
      description = "Get all variants for a product ordered by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Variants retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductVariant.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductVariant>> getVariants(
      @Parameter(description = "Product ID", example = "1", required = true) @PathVariable
          Long productId) {
    return ResponseEntity.ok(productVariantService.getVariantsByProductId(productId));
  }

  @GetMapping("/variants/{variantId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Get variant by ID", description = "Get a specific variant by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Variant retrieved",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductVariant.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
      })
  public ResponseEntity<ProductVariant> getVariantById(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId) {
    return productVariantService
        .getVariantById(variantId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/variants/{variantId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update variant", description = "Update an existing variant")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Variant updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductVariant.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content)
      })
  public ResponseEntity<ProductVariant> updateVariant(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId,
      @RequestBody(description = "Variant update details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          UpdateVariantRequest variantRequest) {
    try {
      ProductVariant variant =
          productVariantService.updateVariant(
              variantId,
              variantRequest.getSku(),
              variantRequest.getVariantName(),
              variantRequest.getAttributes());
      return ResponseEntity.ok(variant);
    } catch (ProductVariantNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (SkuAlreadyExistsException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping("/variants/{variantId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Delete variant", description = "Delete a variant")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Variant deleted successfully",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
      })
  public ResponseEntity<Void> deleteVariant(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId) {
    try {
      productVariantService.deleteVariant(variantId);
      return ResponseEntity.noContent().build();
    } catch (ProductVariantNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping("/variants/{variantId}/batches")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add batch to variant",
      description = "Add a new inventory batch to a specific variant")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Batch added successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductBatch.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content)
      })
  public ResponseEntity<ProductBatch> addBatchToVariant(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId,
      @RequestBody(description = "Batch details", required = true)
          @Valid
          @org.springframework.web.bind.annotation.RequestBody
          BatchRequest batchRequest) {
    try {
      ProductBatch batch =
          productBatchService.addBatchForVariant(
              variantId,
              batchRequest.getQuantity(),
              batchRequest.getUnitCost(),
              batchRequest.getUnitPrice(),
              batchRequest.getSupplierId(),
              batchRequest.getNotes());
      return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    } catch (ProductVariantNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (SupplierNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping("/variants/{variantId}/batches")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get variant batches",
      description = "Get all batches for a variant ordered by creation date (oldest first)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Batches retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductBatch.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductBatch>> getVariantBatches(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId) {
    return ResponseEntity.ok(productBatchService.getBatchesByVariantId(variantId));
  }

  @GetMapping("/variants/{variantId}/batches/available")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get available variant batches",
      description = "Get batches with remaining quantity for a variant")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Available batches retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductBatch.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content)
      })
  public ResponseEntity<List<ProductBatch>> getAvailableVariantBatches(
      @Parameter(description = "Variant ID", example = "1", required = true) @PathVariable
          Long variantId) {
    return ResponseEntity.ok(productBatchService.getAvailableBatchesByVariantId(variantId));
  }
}
