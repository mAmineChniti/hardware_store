package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.ProductCostRequest;
import tn.inovexahub.hardware_store.dto.ProductCostResponse;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductCost;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.service.ProductService;
import tn.inovexahub.hardware_store.service.SupplierService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock private ProductService productService;
  @Mock private SupplierService supplierService;

  private ProductController productController;

  @BeforeEach
  void setUp() {
    productController = new ProductController(productService, supplierService);
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
    Product product = createProduct(null, "PROD001", "Hammer");
    Product saved = createProduct(1L, "PROD001", "Hammer");
    when(productService.createProduct(product)).thenReturn(saved);

    ResponseEntity<Product> response = productController.createProduct(product);

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
  void getHeavyMaterials_ReturnsList() {
    Product product = createProduct(1L, "PROD001", "Gravel");
    product.setIsHeavyMaterial(true);
    when(productService.getHeavyMaterials()).thenReturn(List.of(product));

    ResponseEntity<List<Product>> response = productController.getHeavyMaterials();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertTrue(response.getBody().getFirst().getIsHeavyMaterial());
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
  void updateProductConditioning_NotFound_ThrowsNotFound() {
    ProductConditioning conditioning = createConditioning(1L);
    when(productService.updateProductConditioning(999L, conditioning))
        .thenThrow(new RuntimeException("Product conditioning not found"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateProductConditioning(999L, conditioning));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void deleteProductConditioning_ReturnsNoContent() {
    doNothing().when(productService).deleteProductConditioning(1L);

    ResponseEntity<Void> response = productController.deleteProductConditioning(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService).deleteProductConditioning(1L);
  }

  // ==================== Product Costs ====================

  @Test
  void getProductCostHistory_Found_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    ProductCost cost = createCost(1L, product, new BigDecimal("25.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getProductCostHistory(1L)).thenReturn(List.of(cost));

    ResponseEntity<List<ProductCostResponse>> response =
        productController.getProductCostHistory(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(1L, response.getBody().getFirst().getProductId());
    assertEquals("Hammer", response.getBody().getFirst().getProductName());
  }

  @Test
  void getProductCostHistory_ProductNotFound_ThrowsNotFound() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.getProductCostHistory(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void getCurrentProductCost_Found_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    ProductCost cost = createCost(1L, product, new BigDecimal("25.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getCurrentProductCost(1L)).thenReturn(Optional.of(cost));

    ResponseEntity<ProductCostResponse> response = productController.getCurrentProductCost(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("25.000"), response.getBody().getUnitCost());
  }

  @Test
  void getCurrentProductCost_NoCost_ReturnsNotFound() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getCurrentProductCost(1L)).thenReturn(Optional.empty());

    ResponseEntity<ProductCostResponse> response = productController.getCurrentProductCost(1L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getCurrentProductCost_ProductNotFound_ThrowsNotFound() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.getCurrentProductCost(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void addProductCost_WithoutSupplier_ReturnsCreated() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    ProductCost cost = createCost(1L, product, new BigDecimal("30.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.addProductCost(
            eq(1L), any(BigDecimal.class), any(LocalDate.class), isNull(), any()))
        .thenReturn(cost);

    ProductCostRequest request = new ProductCostRequest();
    request.setUnitCost(new BigDecimal("30.000"));
    request.setEffectiveDate(LocalDate.of(2024, 1, 1));
    request.setNotes("Test invoice");

    ResponseEntity<ProductCostResponse> response = productController.addProductCost(1L, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(new BigDecimal("30.000"), response.getBody().getUnitCost());
    assertNull(response.getBody().getSupplier());
  }

  @Test
  void addProductCost_WithSupplier_ReturnsCreated() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    Supplier supplier = createSupplier(1L, "Supplier A");
    ProductCost cost = createCostWithSupplier(1L, product, new BigDecimal("30.000"), supplier);

    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(supplierService.getSupplierById(1L)).thenReturn(Optional.of(supplier));
    when(productService.addProductCost(
            eq(1L), any(BigDecimal.class), any(LocalDate.class), eq(supplier), any()))
        .thenReturn(cost);

    ProductCostRequest request = new ProductCostRequest();
    request.setUnitCost(new BigDecimal("30.000"));
    request.setEffectiveDate(LocalDate.of(2024, 1, 1));
    request.setSupplierId(1L);
    request.setNotes("Test invoice");

    ResponseEntity<ProductCostResponse> response = productController.addProductCost(1L, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Supplier A", response.getBody().getSupplier());
  }

  @Test
  void addProductCost_SupplierNotFound_ThrowsNotFound() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(supplierService.getSupplierById(999L)).thenReturn(Optional.empty());

    ProductCostRequest request = new ProductCostRequest();
    request.setUnitCost(new BigDecimal("30.000"));
    request.setEffectiveDate(LocalDate.of(2024, 1, 1));
    request.setSupplierId(999L);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addProductCost(1L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Supplier not found", ex.getReason());
  }

  @Test
  void addProductCost_ProductNotFound_ThrowsNotFound() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ProductCostRequest request = new ProductCostRequest();
    request.setUnitCost(new BigDecimal("30.000"));
    request.setEffectiveDate(LocalDate.of(2024, 1, 1));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addProductCost(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void addProductCost_RuntimeException_ThrowsNotFound() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.addProductCost(
            eq(1L), any(BigDecimal.class), any(LocalDate.class), isNull(), any()))
        .thenThrow(new RuntimeException("Invalid cost"));

    ProductCostRequest request = new ProductCostRequest();
    request.setUnitCost(new BigDecimal("30.000"));
    request.setEffectiveDate(LocalDate.of(2024, 1, 1));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.addProductCost(1L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void getProductCostForDate_Found_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    ProductCost cost = createCost(1L, product, new BigDecimal("25.000"));
    LocalDate date = LocalDate.of(2024, 1, 1);
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getProductCostForDate(1L, date)).thenReturn(Optional.of(cost));

    ResponseEntity<ProductCostResponse> response =
        productController.getProductCostForDate(1L, date);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(date, response.getBody().getEffectiveDate());
  }

  @Test
  void getProductCostForDate_NotFound_ReturnsNotFound() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    LocalDate date = LocalDate.of(2024, 6, 15);
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getProductCostForDate(1L, date)).thenReturn(Optional.empty());

    ResponseEntity<ProductCostResponse> response =
        productController.getProductCostForDate(1L, date);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getProductCostForDate_ProductNotFound_ThrowsNotFound() {
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.getProductCostForDate(999L, LocalDate.of(2024, 1, 1)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void getProductCostsBetweenDates_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    ProductCost cost = createCost(1L, product, new BigDecimal("25.000"));
    LocalDate start = LocalDate.of(2024, 1, 1);
    LocalDate end = LocalDate.of(2024, 12, 31);
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));
    when(productService.getProductCostsBetweenDates(1L, start, end)).thenReturn(List.of(cost));

    ResponseEntity<List<ProductCostResponse>> response =
        productController.getProductCostsBetweenDates(1L, start, end);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void deleteProductCost_Success_ReturnsNoContent() {
    doNothing().when(productService).deleteProductCost(1L);

    ResponseEntity<Void> response = productController.deleteProductCost(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService).deleteProductCost(1L);
  }

  @Test
  void deleteProductCost_NotFound_ThrowsNotFound() {
    doThrow(new RuntimeException("Product cost not found"))
        .when(productService)
        .deleteProductCost(999L);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> productController.deleteProductCost(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // ==================== Stock Management ====================

  @Test
  void updateStockQuantity_Success_ReturnsOk() {
    Product product = createProduct(1L, "PROD001", "Hammer");
    product.setStockQuantity(new BigDecimal("60.000"));
    doNothing().when(productService).updateStockQuantity(1L, new BigDecimal("10.000"));
    when(productService.getProductById(1L)).thenReturn(Optional.of(product));

    ResponseEntity<Product> response =
        productController.updateStockQuantity(1L, new BigDecimal("10.000"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new BigDecimal("60.000"), response.getBody().getStockQuantity());
  }

  @Test
  void updateStockQuantity_BadRequest_ThrowsBadRequest() {
    doThrow(new RuntimeException("Insufficient stock"))
        .when(productService)
        .updateStockQuantity(1L, new BigDecimal("-100.000"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> productController.updateStockQuantity(1L, new BigDecimal("-100.000")));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void updateStockQuantity_ProductNotFound_ReturnsNotFound() {
    doNothing().when(productService).updateStockQuantity(999L, new BigDecimal("10.000"));
    when(productService.getProductById(999L)).thenReturn(Optional.empty());

    ResponseEntity<Product> response =
        productController.updateStockQuantity(999L, new BigDecimal("10.000"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // ==================== Helpers ====================

  private Product createProduct(Long id, String reference, String name) {
    Product product = new Product();
    product.setId(id);
    product.setReference(reference);
    product.setName(name);
    product.setCategory("Tools");
    product.setUnitType(tn.inovexahub.hardware_store.enums.UnitType.UNITARY);
    product.setIsHeavyMaterial(false);
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

  private ProductCost createCost(Long id, Product product, BigDecimal unitCost) {
    ProductCost cost = new ProductCost();
    cost.setId(id);
    cost.setProduct(product);
    cost.setUnitCost(unitCost);
    cost.setEffectiveDate(LocalDate.of(2024, 1, 1));
    cost.setNotes("Test cost");
    cost.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
    return cost;
  }

  private ProductCost createCostWithSupplier(
      Long id, Product product, BigDecimal unitCost, Supplier supplier) {
    ProductCost cost = createCost(id, product, unitCost);
    cost.setSupplier(supplier);
    return cost;
  }

  private Supplier createSupplier(Long id, String name) {
    Supplier supplier = new Supplier();
    supplier.setId(id);
    supplier.setName(name);
    supplier.setDeleted(false);
    return supplier;
  }
}
