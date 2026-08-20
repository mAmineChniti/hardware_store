package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock private ProductService productService;
  @Mock private SupplierService supplierService;
  @Mock private ProductBatchService productBatchService;
  @Mock private ProductVariantService productVariantService;

  private ProductController productController;

  @BeforeEach
  void setUp() {
    productController =
        new ProductController(
            productService, supplierService, productBatchService, productVariantService);
  }

  // ==================== Product CRUD ====================

  @Test
  void getAllProducts_ReturnsList() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getAllProducts()).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response = productController.getAllProducts();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("Hammer", response.getBody().getFirst().getName());
  }

  @Test
  void getProductById_Found_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));

    ResponseEntity<Product> response = productController.getProductById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Hammer", response.getBody().getName());
  }

  @Test
  void getProductById_NotFound_ReturnsNotFound() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseEntity<Product> response = productController.getProductById(999L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getProductByReference_Found_ReturnsOk() {
    Product product = createProduct(1L, "CIM-325", "Cement");
    when(productService.getProductByReference("CIM-325")).thenReturn(Optional.of(product));

    ResponseEntity<Product> response = productController.getProductByReference("CIM-325");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("CIM-325", response.getBody().getReference());
  }

  @Test
  void getProductByReference_NotFound_ReturnsNotFound() {
    when(productService.getProductByReference("NONEXISTENT")).thenReturn(Optional.empty());

    ResponseEntity<Product> response = productController.getProductByReference("NONEXISTENT");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void createProduct_ReturnsCreated() {
    Product saved = createProduct(1L, "PROD001", "Hammer");
    when(productService.createProductWithInitialBatch(
            any(Product.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenReturn(saved);

    CreateProductRequest request =
        new CreateProductRequest(
            "PROD001",
            "Hammer",
            null,
            null,
            "Tools",
            tn.inovexahub.hardware_store.enums.UnitType.UNITARY,
            null,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            null,
            null);

    ResponseEntity<Product> response = productController.createProduct(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
    assertEquals("Hammer", response.getBody().getName());
  }

  @Test
  void updateProduct_Success_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Updated Hammer");
    when(productService.updateProduct(1L, product)).thenReturn(product);

    ResponseEntity<Product> response = productController.updateProduct(1L, product);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Updated Hammer", response.getBody().getName());
  }

  @Test
  void updateProduct_NotFound_ThrowsNotFound() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.updateProduct(999L, product))
        .thenThrow(new RuntimeException("Product not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.updateProduct(999L, product));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void deleteProduct_ReturnsNoContent() {
    doNothing().when(productService).deleteProduct(1L);

    ResponseEntity<Void> response = productController.deleteProduct(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService).deleteProduct(1L);
  }

  // ==================== Search and Filter ====================

  @Test
  void searchProducts_ReturnsList() {
    Product product = createProduct(1L, "PROD001", "Cement Bag");
    when(productService.searchProducts("cement")).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response = productController.searchProducts("cement");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getProductsByCategory_ReturnsList() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getProductsByCategory("Tools")).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response = productController.getProductsByCategory("Tools");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getLowStockProducts_ReturnsList() {
    Product product = createProduct(1L, "PROD001", "Nails");
    product.setStockQuantity(new BigDecimal("5.000"));
    when(productService.getLowStockProducts(new BigDecimal("10.0"))).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response =
        productController.getLowStockProducts(new BigDecimal("10.0"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  // ==================== Product Conditionings ====================

  @Test
  void getProductConditionings_ReturnsList() {
    ProductConditioning conditioning = createConditioning(1L);
    when(productService.getProductConditionings(1L)).thenReturn(List.of(conditioning));

    ResponseEntity<List<ProductConditioning>> response =
        productController.getProductConditionings(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void addProductConditioning_Success_ReturnsCreated() {
    ProductConditioning conditioning = createConditioning(null);
    ProductConditioning saved = createConditioning(1L);
    when(productService.addProductConditioning(1L, conditioning)).thenReturn(saved);

    ResponseEntity<ProductConditioning> response =
        productController.addProductConditioning(1L, conditioning);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
  }

  @Test
  void addProductConditioning_ProductNotFound_ThrowsNotFound() {
    ProductConditioning conditioning = createConditioning(null);
    when(productService.addProductConditioning(999L, conditioning))
        .thenThrow(new RuntimeException("Product not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.addProductConditioning(999L, conditioning));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateProductConditioning_Success_ReturnsOk() {
    ProductConditioning conditioning = createConditioning(1L);
    when(productService.updateProductConditioning(1L, conditioning)).thenReturn(conditioning);

    ResponseEntity<ProductConditioning> response =
        productController.updateProductConditioning(1L, conditioning);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
  }

  @Test
  void createProduct_WithSupplier_SetsSupplierOnProduct() {
    Product saved = createProduct(1L, "PROD001", "Hammer");
    when(productService.createProductWithInitialBatch(
            any(Product.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenReturn(saved);

    Supplier supplier = new Supplier();
    supplier.setId(5L);
    supplier.setName("ABC Supplies");
    when(supplierService.getSupplierById(5L)).thenReturn(Optional.of(supplier));

    CreateProductRequest request =
        new CreateProductRequest(
            "PROD001",
            "Hammer",
            null,
            null,
            "Tools",
            tn.inovexahub.hardware_store.enums.UnitType.UNITARY,
            null,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            5L,
            "Bulk");

    ResponseEntity<Product> response = productController.createProduct(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(supplierService).getSupplierById(5L);
  }

  @Test
  void createProduct_SupplierNotFound_ThrowsNotFound() {
    when(supplierService.getSupplierById(999L)).thenReturn(Optional.empty());

    CreateProductRequest request =
        new CreateProductRequest(
            "PROD001",
            "Hammer",
            null,
            null,
            "Tools",
            tn.inovexahub.hardware_store.enums.UnitType.UNITARY,
            null,
            new BigDecimal("100"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            999L,
            null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.createProduct(request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateVariant_InvalidAttributes_ThrowsBadRequest() {
    UpdateVariantRequest request = new UpdateVariantRequest(null, null, "{bad json");
    when(productVariantService.updateVariant(1L, null, null, "{bad json"))
        .thenThrow(new IllegalArgumentException("Attributes must be valid JSON"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.updateVariant(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void updateProductConditioning_RuntimeException_ThrowsNotFound() {
    ProductConditioning conditioning = createConditioning(1L);
    when(productService.updateProductConditioning(1L, conditioning))
        .thenThrow(new RuntimeException("Product conditioning not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateProductConditioning(1L, conditioning));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateProductConditioning_IllegalArgumentException_ThrowsBadRequest() {
    ProductConditioning conditioning = createConditioning(1L);
    when(productService.updateProductConditioning(1L, conditioning))
        .thenThrow(new IllegalArgumentException("Invalid conditioning data"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateProductConditioning(1L, conditioning));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void deleteProductConditioning_ReturnsNoContent() {
    doNothing().when(productService).deleteProductConditioning(1L);

    ResponseEntity<Void> response = productController.deleteProductConditioning(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService).deleteProductConditioning(1L);
  }

  // ==================== Stock Management ====================

  @Test
  void updateStockQuantity_Success_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    product.setStockQuantity(new BigDecimal("60.000"));
    doNothing().when(productService).updateStockQuantity(1L, new BigDecimal("10.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));

    UpdateStockRequest request = new UpdateStockRequest(new BigDecimal("10.000"));
    ResponseEntity<Product> response = productController.updateStockQuantity(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("60.000"), response.getBody().getStockQuantity());
  }

  @Test
  void updateStockQuantity_BadRequest_ThrowsBadRequest() {
    doThrow(new RuntimeException("Insufficient stock"))
        .when(productService)
        .updateStockQuantity(1L, new BigDecimal("-100.000"));

    UpdateStockRequest request = new UpdateStockRequest(new BigDecimal("-100.000"));
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateStockQuantity(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void updateStockQuantity_ProductNotFoundException_ThrowsNotFound() {
    doThrow(new tn.inovexahub.hardware_store.exception.ProductNotFoundException(1L))
        .when(productService)
        .updateStockQuantity(1L, new BigDecimal("10.000"));

    UpdateStockRequest request = new UpdateStockRequest(new BigDecimal("10.000"));
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateStockQuantity(1L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== Product Variants ====================

  @Test
  void createVariant_Success_ReturnsCreated() {
    ProductVariant variant = createVariant(1L);
    when(productVariantService.createVariant(1L, "SCREW-6MM", "6mm Steel Screws", "{}"))
        .thenReturn(variant);

    CreateVariantRequest request = new CreateVariantRequest("SCREW-6MM", "6mm Steel Screws", "{}");
    ResponseEntity<ProductVariant> response = productController.createVariant(1L, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
  }

  @Test
  void createVariant_ProductNotFound_ThrowsNotFound() {
    when(productVariantService.createVariant(999L, "SCREW-6MM", null, "{}"))
        .thenThrow(new ProductNotFoundException(999L));

    CreateVariantRequest request = new CreateVariantRequest("SCREW-6MM", null, "{}");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.createVariant(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void createVariant_DuplicateSku_ThrowsConflict() {
    when(productVariantService.createVariant(1L, "SCREW-6MM", "6mm", "{}"))
        .thenThrow(new SkuAlreadyExistsException("SCREW-6MM"));

    CreateVariantRequest request = new CreateVariantRequest("SCREW-6MM", "6mm", "{}");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.createVariant(1L, request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void createVariant_InvalidAttributes_ThrowsBadRequest() {
    when(productVariantService.createVariant(1L, "SCREW-6MM", "6mm", "{bad"))
        .thenThrow(new IllegalArgumentException("Attributes must be valid JSON"));

    CreateVariantRequest request = new CreateVariantRequest("SCREW-6MM", "6mm", "{bad");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.createVariant(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getVariants_ReturnsList() {
    ProductVariant variant = createVariant(1L);
    when(productVariantService.getVariantsByProductId(1L)).thenReturn(List.of(variant));

    ResponseEntity<List<ProductVariant>> response = productController.getVariants(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getVariantById_Found_ReturnsOk() {
    ProductVariant variant = createVariant(1L);
    when(productVariantService.getVariantById(1L)).thenReturn(Optional.of(variant));

    ResponseEntity<ProductVariant> response = productController.getVariantById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("SCREW-6MM", response.getBody().getSku());
  }

  @Test
  void getVariantById_NotFound_ReturnsNotFound() {
    when(productVariantService.getVariantById(999L)).thenReturn(Optional.empty());

    ResponseEntity<ProductVariant> response = productController.getVariantById(999L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updateVariant_Success_ReturnsOk() {
    ProductVariant variant = createVariant(1L);
    when(productVariantService.updateVariant(1L, "SCREW-7MM", "7mm Steel Screws", "{}"))
        .thenReturn(variant);

    UpdateVariantRequest request = new UpdateVariantRequest("SCREW-7MM", "7mm Steel Screws", "{}");
    ResponseEntity<ProductVariant> response = productController.updateVariant(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void updateVariant_NotFound_ThrowsNotFound() {
    when(productVariantService.updateVariant(999L, null, null, null))
        .thenThrow(new ProductVariantNotFoundException(999L));

    UpdateVariantRequest request = new UpdateVariantRequest(null, null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.updateVariant(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateVariant_DuplicateSku_ThrowsConflict() {
    when(productVariantService.updateVariant(1L, "SCREW-6MM", null, null))
        .thenThrow(new SkuAlreadyExistsException("SCREW-6MM"));

    UpdateVariantRequest request = new UpdateVariantRequest("SCREW-6MM", null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.updateVariant(1L, request));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void deleteVariant_Success_ReturnsNoContent() {
    doNothing().when(productVariantService).deleteVariant(1L);

    ResponseEntity<Void> response = productController.deleteVariant(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productVariantService).deleteVariant(1L);
  }

  @Test
  void deleteVariant_NotFound_ThrowsNotFound() {
    doThrow(new ProductVariantNotFoundException(999L))
        .when(productVariantService)
        .deleteVariant(999L);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.deleteVariant(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void deleteVariant_HasBatches_ThrowsBadRequest() {
    doThrow(new IllegalArgumentException("Cannot delete variant with existing batches"))
        .when(productVariantService)
        .deleteVariant(1L);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.deleteVariant(1L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void addBatchToVariant_Success_ReturnsCreated() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.addBatchForVariant(
            eq(1L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenReturn(batch);

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseEntity<ProductBatch> response = productController.addBatchToVariant(1L, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
  }

  @Test
  void addBatchToVariant_VariantNotFound_ThrowsNotFound() {
    when(productBatchService.addBatchForVariant(
            eq(999L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenThrow(new ProductVariantNotFoundException(999L));

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.addBatchToVariant(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void addBatchToVariant_SupplierNotFound_ThrowsNotFound() {
    when(productBatchService.addBatchForVariant(
            eq(1L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            eq(99L),
            any()))
        .thenThrow(new SupplierNotFoundException(99L));

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), 99L, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addBatchToVariant(1L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void addBatchToVariant_InvalidQuantity_ThrowsBadRequest() {
    when(productBatchService.addBatchForVariant(
            eq(1L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenThrow(new IllegalArgumentException("Quantity must be positive"));

    BatchRequest request =
        new BatchRequest(
            BigDecimal.ZERO, new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addBatchToVariant(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getVariantBatches_ReturnsList() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.getBatchesByVariantId(1L)).thenReturn(List.of(batch));

    ResponseEntity<List<ProductBatch>> response = productController.getVariantBatches(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getAvailableVariantBatches_ReturnsList() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.getAvailableBatchesByVariantId(1L)).thenReturn(List.of(batch));

    ResponseEntity<List<ProductBatch>> response = productController.getAvailableVariantBatches(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  // ==================== Batch Tests ====================

  @Test
  void addBatch_Success_ReturnsCreated() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.addBatch(
            eq(1L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenReturn(batch);

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseEntity<ProductBatch> response = productController.addBatch(1L, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(1L, response.getBody().getId());
  }

  @Test
  void addBatch_RuntimeException_ThrowsBadRequest() {
    when(productBatchService.addBatch(
            eq(1L),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(BigDecimal.class),
            any(),
            any()))
        .thenThrow(new RuntimeException("Product not found"));

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.addBatch(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getBatches_ReturnsList() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.getBatchesByProductId(1L)).thenReturn(List.of(batch));

    ResponseEntity<List<ProductBatch>> response = productController.getBatches(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getAvailableBatches_ReturnsList() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.getAvailableBatchesByProductId(1L)).thenReturn(List.of(batch));

    ResponseEntity<List<ProductBatch>> response = productController.getAvailableBatches(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void updateBatchQuantity_Success_ReturnsOk() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    batch.setQuantity(new BigDecimal("20.000"));
    when(productBatchService.updateBatchQuantity(eq(1L), any(BigDecimal.class))).thenReturn(batch);

    UpdateBatchQuantityRequest request = new UpdateBatchQuantityRequest(new BigDecimal("20.000"));
    ResponseEntity<ProductBatch> response = productController.updateBatchQuantity(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("20.000"), response.getBody().getQuantity());
  }

  @Test
  void updateBatchQuantity_RuntimeException_ThrowsBadRequest() {
    when(productBatchService.updateBatchQuantity(eq(1L), any(BigDecimal.class)))
        .thenThrow(new RuntimeException("Quantity cannot be negative"));

    UpdateBatchQuantityRequest request = new UpdateBatchQuantityRequest(new BigDecimal("-5"));
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateBatchQuantity(1L, request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void updateBatchPricing_Success_ReturnsOk() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    batch.setUnitCost(new BigDecimal("16.00"));
    batch.setUnitPrice(new BigDecimal("22.00"));
    when(productBatchService.updateBatchPricing(
            eq(1L), any(BigDecimal.class), any(BigDecimal.class), any()))
        .thenReturn(batch);

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

    UpdateBatchPricingRequest request =
        new UpdateBatchPricingRequest(new BigDecimal("16.00"), new BigDecimal("22.00"), false);
    ResponseEntity<ProductBatch> response = productController.updateBatchPricing(1L, request, auth);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("16.00"), response.getBody().getUnitCost());
    assertEquals(new BigDecimal("22.00"), response.getBody().getUnitPrice());
  }

  @Test
  void updateBatchPricing_RuntimeException_ThrowsBadRequest() {
    when(productBatchService.updateBatchPricing(
            eq(1L), any(BigDecimal.class), any(BigDecimal.class), any()))
        .thenThrow(new RuntimeException("Unit cost cannot be negative"));

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

    UpdateBatchPricingRequest request =
        new UpdateBatchPricingRequest(new BigDecimal("-1.00"), new BigDecimal("22.00"), false);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateBatchPricing(1L, request, auth));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void deleteBatch_Success_ReturnsNoContent() {
    doNothing().when(productBatchService).deleteBatch(1L);

    ResponseEntity<Void> response = productController.deleteBatch(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productBatchService).deleteBatch(1L);
  }

  @Test
  void deleteBatch_RuntimeException_ThrowsBadRequest() {
    doThrow(new RuntimeException("Batch not found")).when(productBatchService).deleteBatch(999L);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.deleteBatch(999L));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  // ==================== Additional coverage tests ====================

  @Test
  void updateBatchPricing_NonAdmin_WithOverride_SetsEffectiveOverrideFalse() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.updateBatchPricing(eq(1L), any(), any(), eq(false))).thenReturn(batch);

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))).when(auth).getAuthorities();

    UpdateBatchPricingRequest request =
        new UpdateBatchPricingRequest(new BigDecimal("16.00"), new BigDecimal("22.00"), true);
    ResponseEntity<ProductBatch> response = productController.updateBatchPricing(1L, request, auth);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(productBatchService).updateBatchPricing(eq(1L), any(), any(), eq(false));
  }

  @Test
  void updateBatchQuantity_ProductBatchNotFoundException_ThrowsNotFound() {
    when(productBatchService.updateBatchQuantity(eq(999L), any(BigDecimal.class)))
        .thenThrow(new tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException(999L));

    UpdateBatchQuantityRequest request = new UpdateBatchQuantityRequest(new BigDecimal("20"));
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateBatchQuantity(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void deleteBatch_ProductBatchNotFoundException_ThrowsNotFound() {
    doThrow(new tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException(999L))
        .when(productBatchService)
        .deleteBatch(999L);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.deleteBatch(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void addBatch_ProductNotFoundException_ThrowsNotFound() {
    when(productBatchService.addBatch(eq(999L), any(), any(), any(), any(), any()))
        .thenThrow(new ProductNotFoundException(999L));

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addBatch(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateStockQuantity_ProductNotFound_ReturnsNotFound() {
    doNothing().when(productService).updateStockQuantity(1L, new BigDecimal("10.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.empty());

    UpdateStockRequest request = new UpdateStockRequest(new BigDecimal("10.000"));
    ResponseEntity<Product> response = productController.updateStockQuantity(1L, request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // ==================== Patch coverage: exception branches ====================

  @Test
  void addBatch_SupplierNotFoundException_ThrowsNotFound() {
    when(productBatchService.addBatch(eq(1L), any(), any(), any(), eq(99L), any()))
        .thenThrow(new SupplierNotFoundException(99L));

    BatchRequest request =
        new BatchRequest(
            new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), 99L, null);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> productController.addBatch(1L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateBatchPricing_ProductBatchNotFoundException_ThrowsNotFound() {
    when(productBatchService.updateBatchPricing(eq(999L), any(), any(), any()))
        .thenThrow(new tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException(999L));

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

    UpdateBatchPricingRequest request =
        new UpdateBatchPricingRequest(new BigDecimal("16.00"), new BigDecimal("22.00"), false);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateBatchPricing(999L, request, auth));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateBatchPricing_Admin_WithOverride_SetsEffectiveOverrideTrue() {
    ProductBatch batch = new ProductBatch();
    batch.setId(1L);
    when(productBatchService.updateBatchPricing(eq(1L), any(), any(), eq(true))).thenReturn(batch);

    Authentication auth = org.mockito.Mockito.mock(Authentication.class);
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

    UpdateBatchPricingRequest request =
        new UpdateBatchPricingRequest(new BigDecimal("16.00"), new BigDecimal("22.00"), true);
    ResponseEntity<ProductBatch> response = productController.updateBatchPricing(1L, request, auth);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(productBatchService).updateBatchPricing(eq(1L), any(), any(), eq(true));
  }

  private Product createProduct(Long id, String reference, String name) {
    Product product = new Product();
    product.setId(id);
    product.setReference(reference);
    product.setName(name);
    product.setCategory("Tools");
    product.setUnitType(tn.inovexahub.hardware_store.enums.UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("50.000"));
    product.setAveragePurchasePrice(BigDecimal.ZERO);
    return product;
  }

  private ProductConditioning createConditioning(Long id) {
    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setId(id);
    conditioning.setDescription("Roll of 100 meters");
    conditioning.setQuantityPerUnit(new BigDecimal("100.000"));
    conditioning.setUnitPrice(new BigDecimal("95.000"));
    return conditioning;
  }

  private ProductVariant createVariant(Long id) {
    ProductVariant variant = new ProductVariant();
    variant.setId(id);
    variant.setSku("SCREW-6MM");
    variant.setVariantName("6mm Steel Screws");
    variant.setAttributes("{\"calibre\": \"6mm\"}");
    return variant;
  }
}
