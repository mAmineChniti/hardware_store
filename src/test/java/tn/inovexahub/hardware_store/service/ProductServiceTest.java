package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.enums.UnitType;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @Mock private ProductConditioningRepository productConditioningRepository;

  @Mock private ProductBatchService productBatchService;

  @InjectMocks private ProductService productService;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setReference("PROD001");
    testProduct.setName("Test Product");
    testProduct.setUnitType(UnitType.UNITARY);
    testProduct.setStockQuantity(new BigDecimal("100.00"));
    testProduct.setAveragePurchasePrice(new BigDecimal("25.00"));
  }

  @Test
  void getAllProducts_ReturnsAllProducts() {
    when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    List<Product> products = productService.getAllProducts();

    assertNotNull(products);
    assertEquals(1, products.size());
    verify(productRepository).findAll();
  }

  @Test
  void getProductById_ExistingProduct_ReturnsProduct() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    Optional<Product> result = productService.getProductById(1L);

    assertNotNull(result);
    assertTrue(result.isPresent());
    assertEquals("Test Product", result.get().getName());
  }

  @Test
  void getProductById_NonExistingProduct_ReturnsEmpty() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Product> result = productService.getProductById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  void createProduct_SavesProduct() {
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    Product saved = productService.createProduct(testProduct);

    assertNotNull(saved);
    verify(productRepository).save(testProduct);
  }

  @Test
  void updateProduct_ExistingProduct_UpdatesFields() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    Product updatedDetails = new Product();
    updatedDetails.setName("Updated Product");
    updatedDetails.setCategory("Tools");

    Product result = productService.updateProduct(1L, updatedDetails);

    assertNotNull(result);
    verify(productRepository).save(testProduct);
  }

  @Test
  void updateProduct_NonExistingProduct_InsertsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    Product updatedDetails = new Product();
    updatedDetails.setName("Updated Product");

    assertThrows(RuntimeException.class, () -> productService.updateProduct(999L, updatedDetails));
  }

  @Test
  void searchProducts_ReturnsMatchingProducts() {
    when(productRepository.searchByKeyword("hammer")).thenReturn(Arrays.asList(testProduct));

    List<Product> results = productService.searchProducts("hammer");

    assertNotNull(results);
    assertEquals(1, results.size());
    verify(productRepository).searchByKeyword("hammer");
  }

  @Test
  void getProductsByCategory_ReturnsCategoryProducts() {
    when(productRepository.findByCategory("Tools")).thenReturn(Arrays.asList(testProduct));

    List<Product> results = productService.getProductsByCategory("Tools");

    assertNotNull(results);
    assertEquals(1, results.size());
    verify(productRepository).findByCategory("Tools");
  }

  @Test
  void getLowStockProducts_ReturnsLowStockItems() {
    testProduct.setStockQuantity(new BigDecimal("5.00"));
    when(productRepository.findLowStock(new BigDecimal("10.00")))
        .thenReturn(Arrays.asList(testProduct));

    List<Product> results = productService.getLowStockProducts(new BigDecimal("10.00"));

    assertNotNull(results);
    assertEquals(1, results.size());
    verify(productRepository).findLowStock(new BigDecimal("10.00"));
  }

  @Test
  void addProductConditioning_SavesConditioning() {
    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setDescription("Box of 10");
    conditioning.setUnitPrice(new BigDecimal("250.00"));

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productConditioningRepository.save(any(ProductConditioning.class)))
        .thenReturn(conditioning);

    ProductConditioning result = productService.addProductConditioning(1L, conditioning);

    assertNotNull(result);
    assertEquals(testProduct, conditioning.getProduct());
    verify(productConditioningRepository).save(conditioning);
  }

  @Test
  void updateStockQuantity_ValidUpdate_DelegatesToBatchLedger() {
    productService.updateStockQuantity(1L, new BigDecimal("10.00"));

    verify(productBatchService).applyStockAdjustment(1L, new BigDecimal("10.00"));
  }

  @Test
  void updateStockQuantity_InsufficientStock_ThrowsException() {
    doThrow(new IllegalArgumentException("Insufficient stock"))
        .when(productBatchService)
        .applyStockAdjustment(1L, new BigDecimal("-10.00"));

    assertThrows(
        IllegalArgumentException.class,
        () -> productService.updateStockQuantity(1L, new BigDecimal("-10.00")));
  }

  // ==================== New Tests ====================

  @Test
  void updateProductConditioning_ExistingConditioning_UpdatesFields() {
    ProductConditioning existing = new ProductConditioning();
    existing.setId(1L);
    existing.setDescription("Old description");
    existing.setQuantityPerUnit(new BigDecimal("10"));
    existing.setUnitPrice(new BigDecimal("50.00"));

    ProductConditioning details = new ProductConditioning();
    details.setDescription("New description");
    details.setQuantityPerUnit(new BigDecimal("20"));
    details.setUnitPrice(new BigDecimal("100.00"));

    when(productConditioningRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productConditioningRepository.save(any(ProductConditioning.class))).thenReturn(existing);

    ProductConditioning result = productService.updateProductConditioning(1L, details);

    assertNotNull(result);
    assertEquals("New description", existing.getDescription());
    assertEquals(new BigDecimal("20"), existing.getQuantityPerUnit());
    assertEquals(new BigDecimal("100.00"), existing.getUnitPrice());
    verify(productConditioningRepository).save(existing);
  }

  @Test
  void deleteProductConditioning_CallsDeleteById() {
    productService.deleteProductConditioning(1L);

    verify(productConditioningRepository).deleteById(1L);
  }

  @Test
  void updateStockQuantity_AddingStock_DelegatesWithDelta() {
    productService.updateStockQuantity(1L, new BigDecimal("50.5"));

    verify(productBatchService).applyStockAdjustment(1L, new BigDecimal("50.5"));
  }

  @Test
  void updateStockQuantity_ForwardsValueUnchanged() {
    // Scale-3 normalization is handled by ProductBatchService.applyStockAdjustment
    productService.updateStockQuantity(1L, new BigDecimal("0.12345"));

    verify(productBatchService).applyStockAdjustment(1L, new BigDecimal("0.12345"));
  }

  @Test
  void deleteProduct_CallsDeleteById() {
    productService.deleteProduct(1L);
    verify(productRepository).deleteById(1L);
  }

  @Test
  void getProductConditionings_ReturnsConditionings() {
    ProductConditioning c1 = new ProductConditioning();
    c1.setDescription("Box of 10");
    c1.setUnitPrice(new BigDecimal("250.00"));

    when(productConditioningRepository.findByProductId(1L)).thenReturn(Arrays.asList(c1));

    List<ProductConditioning> result = productService.getProductConditionings(1L);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(productConditioningRepository).findByProductId(1L);
  }

  @Test
  void addProductConditioning_NonExistingProduct_ThrowsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setDescription("Box of 10");

    assertThrows(
        RuntimeException.class, () -> productService.addProductConditioning(999L, conditioning));
  }

  @Test
  void updateProductConditioning_NonExistingConditioning_ThrowsException() {
    when(productConditioningRepository.findById(999L)).thenReturn(Optional.empty());

    ProductConditioning details = new ProductConditioning();
    details.setDescription("Updated");

    assertThrows(
        RuntimeException.class, () -> productService.updateProductConditioning(999L, details));
  }

  // ==================== createProductWithInitialBatch ====================

  @Test
  void createProductWithInitialBatch_SavesProduct_CallsAddBatch_RefreshesProduct() {
    Product saved = new Product();
    saved.setId(1L);
    saved.setReference("PROD001");
    saved.setName("New Product");

    when(productRepository.save(any(Product.class))).thenReturn(saved);
    when(productRepository.findById(1L)).thenReturn(Optional.of(saved));

    Product result =
        productService.createProductWithInitialBatch(
            saved,
            new BigDecimal("50"),
            new BigDecimal("10.00"),
            new BigDecimal("15.00"),
            1L,
            "Initial stock");

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(productRepository).save(saved);
    verify(productBatchService)
        .addBatch(
            1L,
            new BigDecimal("50"),
            new BigDecimal("10.00"),
            new BigDecimal("15.00"),
            1L,
            "Initial stock");
    verify(productRepository).findById(1L);
  }

  @Test
  void createProductWithInitialBatch_ValidParams_CallsAddBatch() {
    Product saved = new Product();
    saved.setId(2L);

    when(productRepository.save(any(Product.class))).thenReturn(saved);
    when(productRepository.findById(2L)).thenReturn(Optional.of(saved));

    Product result =
        productService.createProductWithInitialBatch(
            saved,
            new BigDecimal("100.000"),
            new BigDecimal("5.000"),
            new BigDecimal("8.000"),
            1L,
            "Initial stock");

    assertNotNull(result);
    verify(productBatchService)
        .addBatch(
            2L,
            new BigDecimal("100.000"),
            new BigDecimal("5.000"),
            new BigDecimal("8.000"),
            1L,
            "Initial stock");
  }

  @Test
  void createProductWithInitialBatch_BatchServiceThrows_PropagatesException() {
    Product saved = new Product();
    saved.setId(1L);

    when(productRepository.save(any(Product.class))).thenReturn(saved);
    when(productBatchService.addBatch(eq(1L), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Batch creation failed"));

    assertThrows(
        RuntimeException.class,
        () ->
            productService.createProductWithInitialBatch(
                saved,
                new BigDecimal("50"),
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                null,
                null));
  }
}
