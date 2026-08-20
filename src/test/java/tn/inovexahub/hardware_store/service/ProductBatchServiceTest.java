package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductBatch;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.ProductBatchNotFoundException;
import tn.inovexahub.hardware_store.exception.ProductNotFoundException;
import tn.inovexahub.hardware_store.exception.ProductVariantNotFoundException;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.repository.ProductBatchRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;
import tn.inovexahub.hardware_store.repository.ProductVariantRepository;
import tn.inovexahub.hardware_store.repository.SupplierRepository;
import tn.inovexahub.hardware_store.service.ProductBatchService.BatchAllocation;

@ExtendWith(MockitoExtension.class)
class ProductBatchServiceTest {

  @Mock private ProductBatchRepository productBatchRepository;
  @Mock private ProductRepository productRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private SupplierRepository supplierRepository;

  @InjectMocks private ProductBatchService productBatchService;

  private Product testProduct;
  private ProductVariant testVariant;

  @BeforeEach
  void setUp() {
    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setReference("PROD001");
    testProduct.setName("Test Product");

    testVariant = new ProductVariant();
    testVariant.setId(10L);
    testVariant.setProduct(testProduct);
    testVariant.setSku("SCREW-6MM");
    testVariant.setVariantName("6mm Steel Screws");
  }

  private ProductBatch createBatch(
      Long id, BigDecimal quantity, String cost, String price, ProductVariant variant) {
    ProductBatch batch = new ProductBatch();
    batch.setId(id);
    batch.setProduct(testProduct);
    batch.setVariant(variant);
    batch.setQuantity(quantity);
    batch.setUnitCost(new BigDecimal(cost));
    batch.setUnitPrice(new BigDecimal(price));
    batch.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
    batch.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
    return batch;
  }

  private void stubStockUpdate(BigDecimal remainingStock) {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.sumAllAvailableQuantityByProductId(1L))
        .thenReturn(Optional.of(remainingStock));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);
  }

  // ==================== addBatchForVariant ====================

  @Test
  void addBatchForVariant_ValidInput_SavesBatchAndUpdatesStock() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("100.000"));

    ProductBatch result =
        productBatchService.addBatchForVariant(
            10L,
            new BigDecimal("25.000"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            null,
            "Bulk purchase");

    assertNotNull(result);
    assertEquals(testProduct, result.getProduct());
    assertEquals(testVariant, result.getVariant());
    assertEquals(new BigDecimal("25.000"), result.getQuantity());
    assertEquals(new BigDecimal("15.00"), result.getUnitCost());
    assertEquals(new BigDecimal("20.00"), result.getUnitPrice());
    assertEquals("Bulk purchase", result.getNotes());
    verify(productBatchRepository).save(result);
  }

  @Test
  void addBatchForVariant_WithSupplier_ResolvesSupplier() {
    Supplier supplier = new Supplier();
    supplier.setId(5L);
    supplier.setName("ABC Supplies");

    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("100.000"));

    ProductBatch result =
        productBatchService.addBatchForVariant(
            10L,
            new BigDecimal("10.000"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            5L,
            null);

    assertEquals(supplier, result.getSupplier());
    verify(supplierRepository).findById(5L);
  }

  @Test
  void addBatchForVariant_SupplierNotFound_ThrowsSupplierNotFound() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        SupplierNotFoundException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                999L,
                null));
  }

  @Test
  void addBatchForVariant_VariantNotFound_ThrowsVariantNotFound() {
    when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductVariantNotFoundException.class,
        () ->
            productBatchService.addBatchForVariant(
                999L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatchForVariant_NullVariantId_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                null,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatchForVariant_NullQuantity_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L, null, new BigDecimal("15.00"), new BigDecimal("20.00"), null, null));
  }

  @Test
  void addBatchForVariant_NullUnitCost_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L, new BigDecimal("10.000"), null, new BigDecimal("20.00"), null, null));
  }

  @Test
  void addBatchForVariant_NullUnitPrice_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L, new BigDecimal("10.000"), new BigDecimal("15.00"), null, null, null));
  }

  @Test
  void addBatchForVariant_ZeroQuantity_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L,
                BigDecimal.ZERO,
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatchForVariant_NegativeUnitCost_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L,
                new BigDecimal("10.000"),
                new BigDecimal("-1.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatchForVariant_NegativeUnitPrice_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatchForVariant(
                10L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("-1.00"),
                null,
                null));
  }

  // ==================== allocateStockFromVariant ====================

  @Test
  void allocateStockFromVariant_FifoAcrossBatches_ReturnsAllocations() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("5.000"), "6.00", "12.00", testVariant);

    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(productBatchRepository.lockAvailableBatchesByVariantId(10L))
        .thenReturn(Arrays.asList(batch1, batch2));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("3.000"));

    List<BatchAllocation> allocations =
        productBatchService.allocateStockFromVariant(10L, new BigDecimal("12.000"));

    assertEquals(2, allocations.size());
    assertEquals(1L, allocations.get(0).getBatchId());
    assertEquals(new BigDecimal("10.000"), allocations.get(0).getQuantity());
    assertEquals(new BigDecimal("5.00"), allocations.get(0).getUnitCost());
    assertEquals(new BigDecimal("10.00"), allocations.get(0).getUnitPrice());
    assertEquals(2L, allocations.get(1).getBatchId());
    assertEquals(new BigDecimal("2.000"), allocations.get(1).getQuantity());
    assertEquals(new BigDecimal("0.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("3.000"), batch2.getQuantity());
    verify(productBatchRepository).sumAllAvailableBatchesCost(1L);
  }

  @Test
  void allocateStockFromVariant_ExactFit_UsesSingleBatch() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(productBatchRepository.lockAvailableBatchesByVariantId(10L))
        .thenReturn(Arrays.asList(batch1));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(BigDecimal.ZERO);

    List<BatchAllocation> allocations =
        productBatchService.allocateStockFromVariant(10L, new BigDecimal("10.000"));

    assertEquals(1, allocations.size());
    assertEquals(new BigDecimal("10.000"), allocations.get(0).getQuantity());
    assertEquals(new BigDecimal("0.000"), batch1.getQuantity());
  }

  @Test
  void allocateStockFromVariant_VariantNotFound_ThrowsVariantNotFound() {
    when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductVariantNotFoundException.class,
        () -> productBatchService.allocateStockFromVariant(999L, new BigDecimal("5.000")));
  }

  @Test
  void allocateStockFromVariant_NoAvailableStock_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(productBatchRepository.lockAvailableBatchesByVariantId(10L))
        .thenReturn(Collections.emptyList());

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStockFromVariant(10L, new BigDecimal("5.000")));
  }

  @Test
  void allocateStockFromVariant_InsufficientStock_ThrowsIllegalArgument() {
    when(productVariantRepository.findById(10L)).thenReturn(Optional.of(testVariant));
    when(productBatchRepository.lockAvailableBatchesByVariantId(10L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant)));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStockFromVariant(10L, new BigDecimal("12.000")));
  }

  @Test
  void allocateStockFromVariant_NullVariantId_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStockFromVariant(null, new BigDecimal("5.000")));
  }

  @Test
  void allocateStockFromVariant_ZeroQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStockFromVariant(10L, BigDecimal.ZERO));
  }

  @Test
  void allocateStockFromVariant_NegativeQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStockFromVariant(10L, new BigDecimal("-5.000")));
  }

  // ==================== batch queries for variants ====================

  @Test
  void getBatchesByVariantId_ReturnsBatches() {
    when(productBatchRepository.findByVariantIdOrderByCreatedAtAscIdAsc(10L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant)));

    List<ProductBatch> result = productBatchService.getBatchesByVariantId(10L);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(productBatchRepository).findByVariantIdOrderByCreatedAtAscIdAsc(10L);
  }

  @Test
  void getAvailableBatchesByVariantId_ReturnsAvailableBatches() {
    when(productBatchRepository.findAvailableBatchesByVariantId(10L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant)));

    List<ProductBatch> result = productBatchService.getAvailableBatchesByVariantId(10L);

    assertEquals(1, result.size());
    verify(productBatchRepository).findAvailableBatchesByVariantId(10L);
  }

  // ==================== addBatch (product-level, shared hardening) ====================

  @Test
  void addBatch_ValidInput_SavesBatchAndUpdatesStock() {
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("25.000"));

    ProductBatch result =
        productBatchService.addBatch(
            1L,
            new BigDecimal("25.000"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            null,
            null);

    assertNotNull(result);
    assertEquals(testProduct, result.getProduct());
    verify(productBatchRepository).save(result);
  }

  @Test
  void addBatch_NullQuantity_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L, null, new BigDecimal("15.00"), new BigDecimal("20.00"), null, null));
  }

  @Test
  void addBatch_SupplierNotFound_ThrowsSupplierNotFound() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        SupplierNotFoundException.class,
        () ->
            productBatchService.addBatch(
                1L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                999L,
                null));
  }

  @Test
  void addBatch_UpdatesProductStockQuantity() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(productBatchRepository.sumAllAvailableQuantityByProductId(1L))
        .thenReturn(Optional.of(new BigDecimal("100.000")));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productBatchService.addBatch(
        1L, new BigDecimal("10.000"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null);

    verify(productBatchRepository).sumAllAvailableQuantityByProductId(1L);
    verify(productRepository).save(testProduct);
  }

  @Test
  void addBatch_ProductNotFound_ThrowsProductNotFoundException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductNotFoundException.class,
        () ->
            productBatchService.addBatch(
                999L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatch_NullUnitCost_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L, new BigDecimal("10.000"), null, new BigDecimal("20.00"), null, null));
  }

  @Test
  void addBatch_NullUnitPrice_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L, new BigDecimal("10.000"), new BigDecimal("15.00"), null, null, null));
  }

  @Test
  void addBatch_ZeroQuantity_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L, BigDecimal.ZERO, new BigDecimal("15.00"), new BigDecimal("20.00"), null, null));
  }

  @Test
  void addBatch_NegativeUnitCost_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L,
                new BigDecimal("10.000"),
                new BigDecimal("-1.00"),
                new BigDecimal("20.00"),
                null,
                null));
  }

  @Test
  void addBatch_NegativeUnitPrice_ThrowsIllegalArgument() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.addBatch(
                1L,
                new BigDecimal("10.000"),
                new BigDecimal("15.00"),
                new BigDecimal("-1.00"),
                null,
                null));
  }

  @Test
  void addBatch_WithSupplier_ResolvesSupplier() {
    Supplier supplier = new Supplier();
    supplier.setId(5L);
    supplier.setName("ABC Supplies");

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("100.000"));

    ProductBatch result =
        productBatchService.addBatch(
            1L,
            new BigDecimal("10.000"),
            new BigDecimal("15.00"),
            new BigDecimal("20.00"),
            5L,
            "Test notes");

    assertEquals(supplier, result.getSupplier());
    assertEquals("Test notes", result.getNotes());
  }

  // ==================== estimateAllocation ====================

  @Test
  void estimateAllocation_WithAvailableBatches_ReturnsEstimates() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("5.000"), "6.00", "12.00", testVariant);

    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(batch1, batch2));

    List<BatchAllocation> allocations =
        productBatchService.estimateAllocation(1L, new BigDecimal("12.000"));

    assertEquals(2, allocations.size());
    assertEquals(new BigDecimal("10.000"), allocations.get(0).getQuantity());
    assertEquals(new BigDecimal("2.000"), allocations.get(1).getQuantity());
    assertEquals(new BigDecimal("10.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("5.000"), batch2.getQuantity());
  }

  @Test
  void estimateAllocation_InsufficientStock_ReturnsPartialAllocation() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("5.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(batch1));

    List<BatchAllocation> allocations =
        productBatchService.estimateAllocation(1L, new BigDecimal("12.000"));

    assertEquals(1, allocations.size());
    assertEquals(new BigDecimal("5.000"), allocations.get(0).getQuantity());
    assertEquals(new BigDecimal("5.000"), batch1.getQuantity());
  }

  @Test
  void estimateAllocation_NoProductLevelBatches_ThrowsIllegalArgument() {
    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(Collections.emptyList());
    when(productBatchRepository.existsByProductIdAndVariantIsNull(1L)).thenReturn(false);

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.estimateAllocation(1L, new BigDecimal("5.000")));
  }

  @Test
  void estimateAllocation_DepletedProductLevelBatches_ReturnsEmptyAllocations() {
    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(Collections.emptyList());
    when(productBatchRepository.existsByProductIdAndVariantIsNull(1L)).thenReturn(true);

    List<BatchAllocation> allocations =
        productBatchService.estimateAllocation(1L, new BigDecimal("5.000"));

    assertTrue(allocations.isEmpty());
  }

  @Test
  void estimateAllocation_NullQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class, () -> productBatchService.estimateAllocation(1L, null));
  }

  @Test
  void estimateAllocation_ZeroQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.estimateAllocation(1L, BigDecimal.ZERO));
  }

  @Test
  void estimateAllocation_NegativeQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.estimateAllocation(1L, new BigDecimal("-5")));
  }

  // ==================== estimateAllocationFromVariant ====================

  @Test
  void estimateAllocationFromVariant_WithAvailableBatches_ReturnsEstimates() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findAvailableBatchesByVariantId(10L))
        .thenReturn(Arrays.asList(batch1));

    List<BatchAllocation> allocations =
        productBatchService.estimateAllocationFromVariant(10L, new BigDecimal("8.000"));

    assertEquals(1, allocations.size());
    assertEquals(new BigDecimal("8.000"), allocations.get(0).getQuantity());
    assertEquals(new BigDecimal("10.000"), batch1.getQuantity());
  }

  @Test
  void estimateAllocationFromVariant_NullQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.estimateAllocationFromVariant(10L, null));
  }

  @Test
  void estimateAllocationFromVariant_ZeroQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.estimateAllocationFromVariant(10L, BigDecimal.ZERO));
  }

  // ==================== restoreBatches ====================

  @Test
  void restoreBatches_ValidAllocations_RestoresStock() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("5.000"), "5.00", "10.00", testVariant);

    Map<Long, BigDecimal> allocations = new HashMap<>();
    allocations.put(1L, new BigDecimal("5.000"));

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch1));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch1);
    stubStockUpdate(new BigDecimal("10.000"));

    productBatchService.restoreBatches(allocations);

    assertEquals(new BigDecimal("10.000"), batch1.getQuantity());
  }

  @Test
  void restoreBatches_EmptyMap_DoesNothing() {
    productBatchService.restoreBatches(new java.util.HashMap<>());
    verifyNoInteractions(productBatchRepository);
  }

  @Test
  void restoreBatches_NullMap_DoesNothing() {
    productBatchService.restoreBatches(null);
    verifyNoInteractions(productBatchRepository);
  }

  @Test
  void restoreBatches_BatchNotFound_SkipsMissingBatch() {
    Map<Long, BigDecimal> allocations = new HashMap<>();
    allocations.put(999L, new BigDecimal("5.000"));

    when(productBatchRepository.findById(999L)).thenReturn(Optional.empty());

    productBatchService.restoreBatches(allocations);

    verify(productBatchRepository).findById(999L);
    verify(productBatchRepository, never()).save(any(ProductBatch.class));
  }

  // ==================== allocateStock (product-level) ====================

  @Test
  void allocateStock_FifoAcrossBatches_ReturnsAllocations() {
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("5.000"), "6.00", "12.00", testVariant);

    when(productBatchRepository.lockAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(batch1, batch2));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("3.000"));

    List<BatchAllocation> allocations =
        productBatchService.allocateStock(1L, new BigDecimal("12.000"));

    assertEquals(2, allocations.size());
    assertEquals(new BigDecimal("0.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("3.000"), batch2.getQuantity());
    verify(productBatchRepository).sumAllAvailableBatchesCost(1L);
  }

  @Test
  void allocateStock_NoAvailableStock_ThrowsIllegalArgument() {
    when(productBatchRepository.lockAvailableBatchesByProductId(1L))
        .thenReturn(Collections.emptyList());

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStock(1L, new BigDecimal("5.000")));
  }

  @Test
  void allocateStock_InsufficientStock_ThrowsIllegalArgument() {
    when(productBatchRepository.lockAvailableBatchesByProductId(1L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("5.000"), "5.00", "10.00", testVariant)));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStock(1L, new BigDecimal("12.000")));
  }

  @Test
  void allocateStock_NullQuantity_ThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> productBatchService.allocateStock(1L, null));
  }

  @Test
  void allocateStock_ZeroQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStock(1L, BigDecimal.ZERO));
  }

  @Test
  void allocateStock_NegativeQuantity_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.allocateStock(1L, new BigDecimal("-5")));
  }

  // ==================== getBatchesByProductId / getAvailableBatchesByProductId
  // ====================

  @Test
  void getBatchesByProductId_ReturnsBatches() {
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant)));

    List<ProductBatch> result = productBatchService.getBatchesByProductId(1L);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(productBatchRepository).findByProductIdOrderByCreatedAtAscIdAsc(1L);
  }

  @Test
  void getAvailableBatchesByProductId_ReturnsAvailableBatches() {
    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(
            Arrays.asList(createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant)));

    List<ProductBatch> result = productBatchService.getAvailableBatchesByProductId(1L);

    assertEquals(1, result.size());
    verify(productBatchRepository).findAvailableBatchesByProductId(1L);
  }

  // ==================== deleteBatch ====================

  @Test
  void deleteBatch_ExistingBatch_Deletes() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    stubStockUpdate(new BigDecimal("0.000"));

    productBatchService.deleteBatch(1L);

    verify(productBatchRepository).deleteById(1L);
  }

  @Test
  void deleteBatch_BatchNotFound_ThrowsProductBatchNotFoundException() {
    when(productBatchRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ProductBatchNotFoundException.class, () -> productBatchService.deleteBatch(999L));
  }

  // ==================== updateBatchQuantity ====================

  @Test
  void updateBatchQuantity_ValidQuantity_UpdatesBatch() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    stubStockUpdate(new BigDecimal("20.000"));

    ProductBatch result = productBatchService.updateBatchQuantity(1L, new BigDecimal("20.000"));

    assertNotNull(result);
    assertEquals(new BigDecimal("20.000"), result.getQuantity());
  }

  @Test
  void updateBatchQuantity_NegativeQuantity_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.updateBatchQuantity(1L, new BigDecimal("-5")));
  }

  @Test
  void updateBatchQuantity_BatchNotFound_ThrowsProductBatchNotFoundException() {
    when(productBatchRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductBatchNotFoundException.class,
        () -> productBatchService.updateBatchQuantity(999L, new BigDecimal("10")));
  }

  @Test
  void updateBatchQuantity_ZeroQuantity_UpdatesBatch() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(BigDecimal.ZERO));
    stubStockUpdate(new BigDecimal("0.000"));

    ProductBatch result = productBatchService.updateBatchQuantity(1L, BigDecimal.ZERO);

    assertEquals(0, BigDecimal.ZERO.compareTo(result.getQuantity()));
  }

  // ==================== updateBatchPricing ====================

  @Test
  void updateBatchPricing_ValidPricing_UpdatesBatch() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("50.000")));
    stubStockUpdate(new BigDecimal("10.000"));

    ProductBatch result =
        productBatchService.updateBatchPricing(
            1L, new BigDecimal("6.00"), new BigDecimal("12.00"), false);

    assertNotNull(result);
    assertEquals(new BigDecimal("6.00"), result.getUnitCost());
    assertEquals(new BigDecimal("12.00"), result.getUnitPrice());
  }

  @Test
  void updateBatchPricing_NullUnitCost_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.updateBatchPricing(1L, null, new BigDecimal("12.00"), false));
  }

  @Test
  void updateBatchPricing_NullUnitPrice_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.updateBatchPricing(1L, new BigDecimal("6.00"), null, false));
  }

  @Test
  void updateBatchPricing_NegativeUnitCost_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.updateBatchPricing(
                1L, new BigDecimal("-1.00"), new BigDecimal("12.00"), false));
  }

  @Test
  void updateBatchPricing_NegativeUnitPrice_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.updateBatchPricing(
                1L, new BigDecimal("6.00"), new BigDecimal("-1.00"), false));
  }

  @Test
  void updateBatchPricing_PriceBelowCost_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.updateBatchPricing(
                1L, new BigDecimal("10.00"), new BigDecimal("5.00"), false));
  }

  @Test
  void updateBatchPricing_PriceBelowCostWithAdminOverride_UpdatesBatch() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("100.000")));
    stubStockUpdate(new BigDecimal("10.000"));

    ProductBatch result =
        productBatchService.updateBatchPricing(
            1L, new BigDecimal("10.00"), new BigDecimal("5.00"), true);

    assertEquals(new BigDecimal("5.00"), result.getUnitPrice());
  }

  @Test
  void updateBatchPricing_NullAdminOverride_PriceBelowCost_ThrowsIllegalArgument() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            productBatchService.updateBatchPricing(
                1L, new BigDecimal("10.00"), new BigDecimal("5.00"), null));
  }

  // ==================== applyStockAdjustment ====================

  @Test
  void applyStockAdjustment_PositiveAdjustment_AddsToNewestBatch() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));

    ProductBatch batch1 = createBatch(1L, new BigDecimal("50.000"), "5.00", "10.00", null);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("30.000"), "6.00", "12.00", null);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(batch1, batch2));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(productBatchRepository.sumAllAvailableQuantityByProductId(1L))
        .thenReturn(Optional.of(new BigDecimal("135.000")));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("810.000")));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productBatchService.applyStockAdjustment(1L, new BigDecimal("5.000"));

    assertEquals(new BigDecimal("35.000"), batch2.getQuantity());
    verify(productBatchRepository).save(batch2);
  }

  @Test
  void applyStockAdjustment_NegativeAdjustment_DeductsFifo() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));

    ProductBatch batch1 = createBatch(1L, new BigDecimal("50.000"), "5.00", "10.00", null);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("30.000"), "6.00", "12.00", null);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(batch1, batch2));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(productBatchRepository.sumAllAvailableQuantityByProductId(1L))
        .thenReturn(Optional.of(new BigDecimal("70.000")));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("420.000")));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    productBatchService.applyStockAdjustment(1L, new BigDecimal("-10.000"));

    assertEquals(new BigDecimal("40.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("30.000"), batch2.getQuantity());
  }

  @Test
  void applyStockAdjustment_NullQuantityChange_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> productBatchService.applyStockAdjustment(1L, null));
  }

  @Test
  void applyStockAdjustment_InsufficientStock_ThrowsException() {
    testProduct.setStockQuantity(new BigDecimal("10.000"));

    ProductBatch batch1 = createBatch(1L, new BigDecimal("5.000"), "5.00", "10.00", null);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(batch1));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.applyStockAdjustment(1L, new BigDecimal("-10.000")));
  }

  @Test
  void applyStockAdjustment_NoBatches_ThrowsException() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Collections.emptyList());

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.applyStockAdjustment(1L, new BigDecimal("5.000")));
  }

  @Test
  void applyStockAdjustment_NoProductLevelBatches_ThrowsException() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));

    ProductBatch batch1 = createBatch(1L, new BigDecimal("50.000"), "5.00", "10.00", testVariant);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("30.000"), "6.00", "12.00", testVariant);

    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(batch1, batch2));

    assertThrows(
        IllegalArgumentException.class,
        () -> productBatchService.applyStockAdjustment(1L, new BigDecimal("5.000")));
  }

  @Test
  void updateBatchQuantity_NullQuantity_ThrowsException() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);

    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));

    assertThrows(
        IllegalArgumentException.class, () -> productBatchService.updateBatchQuantity(1L, null));
  }

  @Test
  void updateBatchPricing_BatchNotFound_ThrowsProductBatchNotFoundException() {
    when(productBatchRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductBatchNotFoundException.class,
        () ->
            productBatchService.updateBatchPricing(
                999L, new BigDecimal("6.00"), new BigDecimal("12.00"), false));
  }

  @Test
  void updateBatchPricing_PriceEqualsCost_UpdatesBatch() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("100.000")));
    stubStockUpdate(new BigDecimal("10.000"));

    ProductBatch result =
        productBatchService.updateBatchPricing(
            1L, new BigDecimal("10.00"), new BigDecimal("10.00"), false);

    assertEquals(new BigDecimal("10.00"), result.getUnitCost());
    assertEquals(new BigDecimal("10.00"), result.getUnitPrice());
  }

  // ==================== computeAllocations early break ====================

  @Test
  void allocateStock_FirstBatchSatisfies_DoesNotTouchSecondBatch() {
    BigDecimal requested = new BigDecimal("5.000");
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", null);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("10.000"), "6.00", "12.00", null);
    when(productBatchRepository.lockAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(batch1, batch2));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("110.000")));
    stubStockUpdate(new BigDecimal("15.000"));

    List<ProductBatchService.BatchAllocation> result =
        productBatchService.allocateStock(1L, requested);

    assertEquals(1, result.size());
    assertEquals(new BigDecimal("5.000"), result.get(0).getQuantity());
    assertEquals(new BigDecimal("5.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("10.000"), batch2.getQuantity());
  }

  // ==================== estimateAllocations early break ====================

  @Test
  void estimateAllocation_FirstBatchSatisfies_DoesNotTouchSecondBatch() {
    BigDecimal requested = new BigDecimal("5.000");
    ProductBatch batch1 = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    ProductBatch batch2 = createBatch(2L, new BigDecimal("10.000"), "6.00", "12.00", testVariant);
    when(productBatchRepository.findAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(batch1, batch2));

    List<ProductBatchService.BatchAllocation> result =
        productBatchService.estimateAllocation(1L, requested);

    assertEquals(1, result.size());
    assertEquals(new BigDecimal("5.000"), result.get(0).getQuantity());
    assertEquals(new BigDecimal("10.000"), batch1.getQuantity());
    assertEquals(new BigDecimal("10.000"), batch2.getQuantity());
  }

  // ==================== BatchAllocation getter ====================

  @Test
  void batchAllocation_GetCreatedAt_ReturnsCreatedAt() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 9, 30);
    ProductBatchService.BatchAllocation alloc =
        new ProductBatchService.BatchAllocation(
            1L,
            new BigDecimal("5.000"),
            new BigDecimal("5.00"),
            new BigDecimal("10.00"),
            createdAt);

    assertEquals(createdAt, alloc.getBatchCreatedAt());
  }

  // ==================== computeAllocations edge cases ====================

  @Test
  void allocateStock_ZeroQuantityBatch_Skipped() {
    BigDecimal requested = new BigDecimal("5.000");
    ProductBatch emptyBatch = createBatch(1L, BigDecimal.ZERO, "5.00", "10.00", null);
    ProductBatch goodBatch = createBatch(2L, new BigDecimal("10.000"), "5.00", "10.00", null);
    when(productBatchRepository.lockAvailableBatchesByProductId(1L))
        .thenReturn(Arrays.asList(emptyBatch, goodBatch));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("50.000")));
    stubStockUpdate(new BigDecimal("10.000"));

    List<ProductBatchService.BatchAllocation> result =
        productBatchService.allocateStock(1L, requested);

    assertEquals(1, result.size());
    assertEquals(2L, result.get(0).getBatchId());
    assertEquals(new BigDecimal("5.000"), result.get(0).getQuantity());
  }

  // ==================== Patch coverage: null / missing branches ====================

  @Test
  void updateBatchQuantity_ProductNotFound_ThrowsProductNotFoundException() {
    ProductBatch batch = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", testVariant);
    when(productBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
    when(productBatchRepository.save(any(ProductBatch.class))).thenReturn(batch);
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        ProductNotFoundException.class,
        () -> productBatchService.updateBatchQuantity(1L, new BigDecimal("20.000")));
  }

  // ==================== applyStockAdjustment (stock adjustments through the batch ledger)
  // ====================

  @Test
  void applyStockAdjustment_ZeroDelta_DoesNotModifyBatches() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));
    ProductBatch oldest = createBatch(1L, new BigDecimal("10.000"), "5.00", "10.00", null);
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(oldest));
    stubStockUpdate(new BigDecimal("10.000"));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("50.000")));

    productBatchService.applyStockAdjustment(1L, new BigDecimal("0.000"));

    assertEquals(new BigDecimal("10.000"), oldest.getQuantity());
  }

  @Test
  void applyStockAdjustment_ProductNotFound_ThrowsProductNotFoundException() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
        ProductNotFoundException.class,
        () -> productBatchService.applyStockAdjustment(999L, new BigDecimal("10.000")));
  }

  @Test
  void applyStockAdjustment_NegativeDelta_SkipsDepletedBatch() {
    testProduct.setStockQuantity(new BigDecimal("100.000"));
    ProductBatch depleted = createBatch(1L, new BigDecimal("0.000"), "5.00", "10.00", null);
    ProductBatch available = createBatch(2L, new BigDecimal("5.000"), "6.00", "11.00", null);
    when(productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(1L))
        .thenReturn(Arrays.asList(depleted, available));
    when(productBatchRepository.save(any(ProductBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubStockUpdate(new BigDecimal("2.000"));
    when(productBatchRepository.sumAllAvailableBatchesCost(1L))
        .thenReturn(Optional.of(new BigDecimal("12.000")));

    productBatchService.applyStockAdjustment(1L, new BigDecimal("-3.000"));

    assertEquals(new BigDecimal("0.000"), depleted.getQuantity());
    assertEquals(new BigDecimal("2.000"), available.getQuantity());
  }
}
