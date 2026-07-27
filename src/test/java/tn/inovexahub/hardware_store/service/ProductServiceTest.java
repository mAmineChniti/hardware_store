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
}
