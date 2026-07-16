package tn.inovexahub.hardware_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  public ResponseEntity<List<Product>> getAllProducts() {
    return ResponseEntity.ok(productService.getAllProducts());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID")
  public ResponseEntity<Product> getProductById(@PathVariable Long id) {
    return productService
        .getProductById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/reference/{reference}")
  @Operation(
      summary = "Get product by reference",
      description = "Retrieve a specific product by its reference code")
  public ResponseEntity<Product> getProductByReference(@PathVariable String reference) {
    return productService
        .getProductByReference(reference)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Create new product", description = "Create a new product")
  public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
    Product createdProduct = productService.createProduct(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(summary = "Update product", description = "Update an existing product")
  public ResponseEntity<Product> updateProduct(
      @PathVariable Long id, @Valid @RequestBody Product productDetails) {
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
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // ==================== Search and Filter ====================

  @GetMapping("/search")
  @Operation(
      summary = "Search products",
      description = "Search products by keyword in name or reference")
  public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
    return ResponseEntity.ok(productService.searchProducts(keyword));
  }

  @GetMapping("/category/{category}")
  @Operation(
      summary = "Get products by category",
      description = "Retrieve all products in a specific category")
  public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
    return ResponseEntity.ok(productService.getProductsByCategory(category));
  }

  @GetMapping("/heavy-materials")
  @Operation(
      summary = "Get heavy materials",
      description = "Retrieve all products marked as heavy materials (dual pricing)")
  public ResponseEntity<List<Product>> getHeavyMaterials() {
    return ResponseEntity.ok(productService.getHeavyMaterials());
  }

  @GetMapping("/low-stock")
  @Operation(
      summary = "Get low stock products",
      description = "Retrieve products with stock below threshold")
  public ResponseEntity<List<Product>> getLowStockProducts(
      @RequestParam(defaultValue = "10.0") BigDecimal threshold) {
    return ResponseEntity.ok(productService.getLowStockProducts(threshold));
  }

  // ==================== Product Conditionings ====================

  @GetMapping("/{productId}/conditionings")
  @Operation(
      summary = "Get product conditionings",
      description = "Retrieve all conditionings for a specific product")
  public ResponseEntity<List<ProductConditioning>> getProductConditionings(
      @PathVariable Long productId) {
    return ResponseEntity.ok(productService.getProductConditionings(productId));
  }

  @PostMapping("/{productId}/conditionings")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add product conditioning",
      description = "Add a new conditioning to a product")
  public ResponseEntity<ProductConditioning> addProductConditioning(
      @PathVariable Long productId, @Valid @RequestBody ProductConditioning conditioning) {
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
  public ResponseEntity<ProductConditioning> updateProductConditioning(
      @PathVariable Long id, @Valid @RequestBody ProductConditioning conditioningDetails) {
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
  public ResponseEntity<Void> deleteProductConditioning(@PathVariable Long id) {
    productService.deleteProductConditioning(id);
    return ResponseEntity.noContent().build();
  }

  // ==================== Product Costs ====================

  @GetMapping("/{productId}/costs")
  @Operation(
      summary = "Get product cost history",
      description = "Retrieve the complete cost history for a product")
  public ResponseEntity<List<ProductCost>> getProductCostHistory(@PathVariable Long productId) {
    return ResponseEntity.ok(productService.getProductCostHistory(productId));
  }

  @GetMapping("/{productId}/costs/current")
  @Operation(
      summary = "Get current product cost",
      description = "Retrieve the most recent cost for a product")
  public ResponseEntity<ProductCost> getCurrentProductCost(@PathVariable Long productId) {
    return productService
        .getCurrentProductCost(productId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/{productId}/costs")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Add product cost",
      description = "Add a new cost entry for a product (updates PAMP)")
  public ResponseEntity<ProductCost> addProductCost(
      @PathVariable Long productId,
      @RequestParam BigDecimal unitCost,
      @RequestParam LocalDate effectiveDate,
      @RequestParam(required = false) Long supplierId,
      @RequestParam(required = false) String notes) {

    Supplier supplier = null;
    if (supplierId != null) {
      supplier =
          supplierService
              .getSupplierById(supplierId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    try {
      ProductCost productCost =
          productService.addProductCost(productId, unitCost, effectiveDate, supplier, notes);
      return ResponseEntity.status(HttpStatus.CREATED).body(productCost);
    } catch (RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping("/{productId}/costs/{date}")
  @Operation(
      summary = "Get product cost for specific date",
      description = "Retrieve the cost for a product on a specific date")
  public ResponseEntity<ProductCost> getProductCostForDate(
      @PathVariable Long productId, @PathVariable LocalDate date) {
    return productService
        .getProductCostForDate(productId, date)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{productId}/costs/between")
  @Operation(
      summary = "Get product costs between dates",
      description = "Retrieve costs for a product within a date range")
  public ResponseEntity<List<ProductCost>> getProductCostsBetweenDates(
      @PathVariable Long productId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    return ResponseEntity.ok(
        productService.getProductCostsBetweenDates(productId, startDate, endDate));
  }

  @DeleteMapping("/costs/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete product cost", description = "Delete a product cost entry")
  public ResponseEntity<Void> deleteProductCost(@PathVariable Long id) {
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
  public ResponseEntity<Product> updateStockQuantity(
      @PathVariable Long productId, @RequestParam Double quantityChange) {
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
}
