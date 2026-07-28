package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
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
import tn.inovexahub.hardware_store.entity.ProductCost;
import tn.inovexahub.hardware_store.enums.UnitType;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductCostRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @Mock private ProductConditioningRepository productConditioningRepository;

  @Mock private ProductCostRepository productCostRepository;

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
  void getHeavyMaterials_ReturnsHeavyMaterials() {
    testProduct.setIsHeavyMaterial(true);
    when(productRepository.findByIsHeavyMaterialTrue()).thenReturn(Arrays.asList(testProduct));

    List<Product> results = productService.getHeavyMaterials();

    assertNotNull(results);
    assertEquals(1, results.size());
    verify(productRepository).findByIsHeavyMaterialTrue();
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
  void addProductCost_PositiveCost_SavesCost() {
    ProductCost cost = new ProductCost();
    cost.setUnitCost(new BigDecimal("30.00"));
    cost.setEffectiveDate(LocalDate.now());

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.save(any(ProductCost.class))).thenReturn(cost);
    when(productCostRepository.findByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Arrays.asList(cost));

    ProductCost result =
        productService.addProductCost(1L, new BigDecimal("30.00"), LocalDate.now(), null, null);

    assertNotNull(result);
    verify(productCostRepository).save(any(ProductCost.class));
  }

  @Test
  void addProductCost_NegativeCost_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            productService.addProductCost(
                1L, new BigDecimal("-10.00"), LocalDate.now(), null, null));
  }

  @Test
  void updateStockQuantity_ValidUpdate_UpdatesStock() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productService.updateStockQuantity(1L, new BigDecimal("10.00"));

    assertEquals(new BigDecimal("110.000"), testProduct.getStockQuantity());
    verify(productRepository).save(testProduct);
  }

  @Test
  void updateStockQuantity_InsufficientStock_ThrowsException() {
    testProduct.setStockQuantity(new BigDecimal("5.00"));
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        RuntimeException.class,
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
  void getProductCostHistory_ReturnsCostsDescending() {
    ProductCost cost1 = new ProductCost();
    cost1.setUnitCost(new BigDecimal("40.00"));
    cost1.setEffectiveDate(LocalDate.of(2024, 3, 1));

    ProductCost cost2 = new ProductCost();
    cost2.setUnitCost(new BigDecimal("30.00"));
    cost2.setEffectiveDate(LocalDate.of(2024, 2, 1));

    ProductCost cost3 = new ProductCost();
    cost3.setUnitCost(new BigDecimal("20.00"));
    cost3.setEffectiveDate(LocalDate.of(2024, 1, 1));

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.findByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Arrays.asList(cost1, cost2, cost3));

    List<ProductCost> result = productService.getProductCostHistory(1L);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(new BigDecimal("40.00"), result.get(0).getUnitCost());
    assertEquals(new BigDecimal("30.00"), result.get(1).getUnitCost());
    assertEquals(new BigDecimal("20.00"), result.get(2).getUnitCost());
  }

  @Test
  void getProductCostHistory_NonExistingProduct_ThrowsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> productService.getProductCostHistory(999L));
  }

  @Test
  void getCurrentProductCost_ReturnsLatestCost() {
    ProductCost latestCost = new ProductCost();
    latestCost.setUnitCost(new BigDecimal("35.00"));
    latestCost.setEffectiveDate(LocalDate.of(2024, 3, 15));
    latestCost.setProduct(testProduct);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.findTopByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Optional.of(latestCost));

    Optional<ProductCost> result = productService.getCurrentProductCost(1L);

    assertNotNull(result);
    assertTrue(result.isPresent());
    assertEquals(new BigDecimal("35.00"), result.get().getUnitCost());
    assertEquals(LocalDate.of(2024, 3, 15), result.get().getEffectiveDate());
  }

  @Test
  void getCurrentProductCost_NonExistingProduct_ThrowsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> productService.getCurrentProductCost(999L));
  }

  @Test
  void addProductCost_ZeroCost_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () -> productService.addProductCost(1L, BigDecimal.ZERO, LocalDate.now(), null, null));
  }

  @Test
  void addProductCost_NullCost_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () -> productService.addProductCost(1L, null, LocalDate.now(), null, null));
  }

  @Test
  void addProductCost_MultipleCosts_UpdatesPAMPWithWeightedAverage() {
    ProductCost cost1 = new ProductCost();
    cost1.setUnitCost(new BigDecimal("40"));
    cost1.setEffectiveDate(LocalDate.of(2024, 3, 1));

    ProductCost cost2 = new ProductCost();
    cost2.setUnitCost(new BigDecimal("30"));
    cost2.setEffectiveDate(LocalDate.of(2024, 2, 1));

    ProductCost cost3 = new ProductCost();
    cost3.setUnitCost(new BigDecimal("20"));
    cost3.setEffectiveDate(LocalDate.of(2024, 1, 1));

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.save(any(ProductCost.class))).thenReturn(cost1);
    when(productCostRepository.findByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Arrays.asList(cost1, cost2, cost3));

    productService.addProductCost(1L, new BigDecimal("40"), LocalDate.of(2024, 3, 1), null, null);

    // limit = min(5, 3) = 3; weights = 3,2,1
    // PAMP = (40*3 + 30*2 + 20*1) / (3+2+1) = (120+60+20)/6 = 200/6 = 33.333
    BigDecimal expectedPamp = new BigDecimal("33.333");
    assertEquals(expectedPamp, testProduct.getAveragePurchasePrice());
    verify(productRepository).save(testProduct);
  }

  @Test
  void addProductCost_EmptyCostListAfterAdd_SetsPAMPToZero() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.save(any(ProductCost.class)))
        .thenAnswer(
            invocation -> {
              ProductCost pc = invocation.getArgument(0);
              pc.setId(1L);
              return pc;
            });
    when(productCostRepository.findByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Collections.emptyList());

    productService.addProductCost(1L, new BigDecimal("25.00"), LocalDate.now(), null, "test note");

    assertEquals(BigDecimal.ZERO, testProduct.getAveragePurchasePrice());
    verify(productRepository).save(testProduct);
  }

  @Test
  void getProductCostForDate_ReturnsMatchingCost() {
    ProductCost cost = new ProductCost();
    cost.setUnitCost(new BigDecimal("28.50"));
    cost.setEffectiveDate(LocalDate.of(2024, 6, 15));
    cost.setProduct(testProduct);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.findByProductAndEffectiveDate(
            testProduct, LocalDate.of(2024, 6, 15)))
        .thenReturn(Optional.of(cost));

    Optional<ProductCost> result =
        productService.getProductCostForDate(1L, LocalDate.of(2024, 6, 15));

    assertNotNull(result);
    assertTrue(result.isPresent());
    assertEquals(new BigDecimal("28.50"), result.get().getUnitCost());
    assertEquals(LocalDate.of(2024, 6, 15), result.get().getEffectiveDate());
  }

  @Test
  void getProductCostForDate_NoCost_ReturnsEmpty() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.findByProductAndEffectiveDate(
            testProduct, LocalDate.of(2024, 12, 25)))
        .thenReturn(Optional.empty());

    Optional<ProductCost> result =
        productService.getProductCostForDate(1L, LocalDate.of(2024, 12, 25));

    assertNotNull(result);
    assertFalse(result.isPresent());
  }

  @Test
  void getProductCostsBetweenDates_ReturnsCostsInRange() {
    ProductCost cost1 = new ProductCost();
    cost1.setUnitCost(new BigDecimal("20.00"));
    cost1.setEffectiveDate(LocalDate.of(2024, 2, 1));

    ProductCost cost2 = new ProductCost();
    cost2.setUnitCost(new BigDecimal("25.00"));
    cost2.setEffectiveDate(LocalDate.of(2024, 3, 1));

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productCostRepository.findByProductAndEffectiveDateBetween(
            testProduct, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 4, 1)))
        .thenReturn(Arrays.asList(cost1, cost2));

    List<ProductCost> result =
        productService.getProductCostsBetweenDates(
            1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 4, 1));

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(new BigDecimal("20.00"), result.get(0).getUnitCost());
    assertEquals(new BigDecimal("25.00"), result.get(1).getUnitCost());
  }

  @Test
  void deleteProductCost_RecalculatesPAMP() {
    ProductCost costToDelete = new ProductCost();
    costToDelete.setId(10L);
    costToDelete.setUnitCost(new BigDecimal("30.00"));
    costToDelete.setProduct(testProduct);

    ProductCost remainingCost = new ProductCost();
    remainingCost.setUnitCost(new BigDecimal("20.00"));
    remainingCost.setEffectiveDate(LocalDate.of(2024, 1, 1));

    when(productCostRepository.findById(10L)).thenReturn(Optional.of(costToDelete));
    when(productCostRepository.findByProductOrderByEffectiveDateDesc(testProduct))
        .thenReturn(Arrays.asList(remainingCost));

    productService.deleteProductCost(10L);

    verify(productCostRepository).deleteById(10L);
    // PAMP with single cost of 20: (20*1) / 1 = 20.000
    assertEquals(new BigDecimal("20.000"), testProduct.getAveragePurchasePrice());
    verify(productRepository).save(testProduct);
  }

  @Test
  void deleteProductCost_NonExistingCost_ThrowsException() {
    when(productCostRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> productService.deleteProductCost(999L));
  }

  @Test
  void updateStockQuantity_AddingStock_UpdatesCorrectly() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productService.updateStockQuantity(1L, new BigDecimal("50.5"));

    assertEquals(new BigDecimal("150.500"), testProduct.getStockQuantity());
    verify(productRepository).save(testProduct);
  }

  @Test
  void updateStockQuantity_NormalizesToScale3() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productService.updateStockQuantity(1L, new BigDecimal("0.12345"));

    assertEquals(new BigDecimal("100.123"), testProduct.getStockQuantity());
    verify(productRepository).save(testProduct);
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

  @Test
  void getProductCostForDate_NonExistingProduct_ThrowsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        RuntimeException.class, () -> productService.getProductCostForDate(999L, LocalDate.now()));
  }

  @Test
  void getProductCostsBetweenDates_NonExistingProduct_ThrowsException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        RuntimeException.class,
        () ->
            productService.getProductCostsBetweenDates(
                999L, LocalDate.now().minusDays(10), LocalDate.now()));
  }
}
