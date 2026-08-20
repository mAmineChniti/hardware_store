package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.exception.ProductNotFoundException;
import tn.inovexahub.hardware_store.exception.ProductVariantNotFoundException;
import tn.inovexahub.hardware_store.exception.SkuAlreadyExistsException;
import tn.inovexahub.hardware_store.repository.ProductBatchRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;
import tn.inovexahub.hardware_store.repository.ProductVariantRepository;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private ProductRepository productRepository;
  @Mock private ProductBatchRepository productBatchRepository;

  @InjectMocks private ProductVariantService productVariantService;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setReference("PROD001");
    testProduct.setName("Test Product");
  }

  private ProductVariant createVariant(Long id, String sku, String variantName) {
    ProductVariant variant = new ProductVariant();
    variant.setId(id);
    variant.setProduct(testProduct);
    variant.setSku(sku);
    variant.setVariantName(variantName);
    return variant;
  }

  // ==================== createVariant ====================

  @Test
  void createVariant_ValidInput_SavesNormalizedVariant() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    productVariantService.createVariant(
        1L, "  screw-6mm  ", "  6mm Steel Screws  ", "{\"calibre\": \"6mm\"}");

    ArgumentCaptor<ProductVariant> captor = ArgumentCaptor.forClass(ProductVariant.class);
    verify(productVariantRepository).saveAndFlush(captor.capture());
    ProductVariant saved = captor.getValue();

    assertEquals(testProduct, saved.getProduct());
    assertEquals("SCREW-6MM", saved.getSku());
    assertEquals("6mm Steel Screws", saved.getVariantName());
    assertEquals("{\"calibre\": \"6mm\"}", saved.getAttributes());
    verify(productVariantRepository).existsBySku("SCREW-6MM");
  }

  @Test
  void createVariant_NullVariantName_AllowsNullName() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ProductVariant result =
        productVariantService.createVariant(1L, "SCREW-6MM", null, "{\"btu\": 12000}");

    assertNull(result.getVariantName());
  }

  @Test
  void createVariant_BlankVariantName_StoresNullName() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ProductVariant result = productVariantService.createVariant(1L, "SCREW-6MM", "   ", null);

    assertNull(result.getVariantName());
    assertNull(result.getAttributes());
  }

  @Test
  void createVariant_NullProductId_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(null, "SCREW-6MM", "6mm", null));
  }

  @Test
  void createVariant_ProductNotFound_ThrowsProductNotFound() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    ProductNotFoundException ex =
        assertThrows(
            ProductNotFoundException.class,
            () -> productVariantService.createVariant(999L, "SCREW-6MM", "6mm", null));

    assertTrue(ex.getMessage().contains("999"));
  }

  @Test
  void createVariant_BlankSku_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(1L, "   ", "6mm", null));
  }

  @Test
  void createVariant_NullSku_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(1L, null, "6mm", null));
  }

  @Test
  void createVariant_SkuTooLong_ThrowsIllegalArgument() {
    String longSku = "A".repeat(51);
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(1L, longSku, "6mm", null));
  }

  @Test
  void createVariant_VariantNameTooLong_ThrowsIllegalArgument() {
    String longName = "N".repeat(101);
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(1L, "SCREW-6MM", longName, null));
  }

  @Test
  void createVariant_DuplicateSku_ThrowsSkuAlreadyExists() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(true);

    assertThrows(
        SkuAlreadyExistsException.class,
        () -> productVariantService.createVariant(1L, "screw-6mm", "6mm", null));
  }

  @Test
  void createVariant_ConcurrentDuplicateSku_TranslatesDataIntegrityViolation() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenThrow(new DataIntegrityViolationException("unique violation"));

    assertThrows(
        SkuAlreadyExistsException.class,
        () -> productVariantService.createVariant(1L, "SCREW-6MM", "6mm", null));
  }

  @Test
  void createVariant_InvalidAttributesJson_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            productVariantService.createVariant(
                1L, "SCREW-6MM", "6mm", "{calibre: not valid json"));
  }

  @Test
  void createVariant_NonObjectJsonAttributes_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.createVariant(1L, "SCREW-6MM", "6mm", "[\"a\", \"b\"]"));
  }

  @Test
  void createVariant_AttributesUnchangedWhenValid() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku("SCREW-6MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String attributes = "{\"calibre\": \"6mm\", \"material\": \"steel\"}";
    ProductVariant result = productVariantService.createVariant(1L, "SCREW-6MM", "6mm", attributes);

    assertEquals(attributes, result.getAttributes());
  }

  @Test
  void createVariant_BlankAttributes_NormalizesToNull() {
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productVariantRepository.existsBySku(anyString())).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProductVariant result =
        productVariantService.createVariant(1L, "SKU-NEW", "New Variant", "   ");

    assertNull(result.getAttributes());
    verify(productVariantRepository).saveAndFlush(any(ProductVariant.class));
  }

  // ==================== getVariantsByProductId ====================

  @Test
  void getVariantsByProductId_ReturnsVariants() {
    when(productVariantRepository.findByProductIdOrderByVariantNameAscIdAsc(1L))
        .thenReturn(Arrays.asList(createVariant(1L, "SCREW-6MM", "6mm")));

    List<ProductVariant> result = productVariantService.getVariantsByProductId(1L);

    assertEquals(1, result.size());
    assertEquals("SCREW-6MM", result.getFirst().getSku());
  }

  @Test
  void getVariantsByProductId_NullProductId_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class, () -> productVariantService.getVariantsByProductId(null));
  }

  // ==================== getVariantById ====================

  @Test
  void getVariantById_Existing_ReturnsVariant() {
    when(productVariantRepository.findById(1L))
        .thenReturn(Optional.of(createVariant(1L, "SCREW-6MM", "6mm")));

    Optional<ProductVariant> result = productVariantService.getVariantById(1L);

    assertTrue(result.isPresent());
    assertEquals("SCREW-6MM", result.get().getSku());
  }

  @Test
  void getVariantById_Missing_ReturnsEmpty() {
    when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<ProductVariant> result = productVariantService.getVariantById(999L);

    assertTrue(result.isEmpty());
  }

  @Test
  void getVariantById_NullId_ThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> productVariantService.getVariantById(null));
  }

  // ==================== getVariantBySku ====================

  @Test
  void getVariantBySku_NormalizesBeforeLookup() {
    when(productVariantRepository.findBySku("SCREW-6MM"))
        .thenReturn(Optional.of(createVariant(1L, "SCREW-6MM", "6mm")));

    Optional<ProductVariant> result = productVariantService.getVariantBySku("  screw-6mm  ");

    assertTrue(result.isPresent());
    verify(productVariantRepository).findBySku("SCREW-6MM");
  }

  @Test
  void getVariantBySku_NullSku_ThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> productVariantService.getVariantBySku(null));
  }

  // ==================== updateVariant ====================

  @Test
  void updateVariant_ValidFields_UpdatesAllFields() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm Steel Screws");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productVariantRepository.existsBySku("SCREW-7MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenReturn(existing);

    ProductVariant result =
        productVariantService.updateVariant(
            1L, " screw-7mm ", "7mm Steel Screws", "{\"calibre\": \"7mm\"}");

    assertEquals("SCREW-7MM", result.getSku());
    assertEquals("7mm Steel Screws", result.getVariantName());
    assertEquals("{\"calibre\": \"7mm\"}", result.getAttributes());
    verify(productVariantRepository).existsBySku("SCREW-7MM");
  }

  @Test
  void updateVariant_SameSkuCaseInsensitive_SkipsDuplicateCheck() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenReturn(existing);

    ProductVariant result = productVariantService.updateVariant(1L, "  screw-6mm  ", null, null);

    assertEquals("SCREW-6MM", result.getSku());
    verify(productVariantRepository, never()).existsBySku(anyString());
  }

  @Test
  void updateVariant_NotFound_ThrowsVariantNotFound() {
    when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

    ProductVariantNotFoundException ex =
        assertThrows(
            ProductVariantNotFoundException.class,
            () -> productVariantService.updateVariant(999L, "NEW-SKU", null, null));

    assertTrue(ex.getMessage().contains("999"));
  }

  @Test
  void updateVariant_NullId_ThrowsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.updateVariant(null, "NEW-SKU", null, null));
  }

  @Test
  void updateVariant_NewSkuAlreadyExists_ThrowsSkuAlreadyExists() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productVariantRepository.existsBySku("SCREW-7MM")).thenReturn(true);

    assertThrows(
        SkuAlreadyExistsException.class,
        () -> productVariantService.updateVariant(1L, "SCREW-7MM", null, null));
  }

  @Test
  void updateVariant_BlankSku_ThrowsIllegalArgument() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));

    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.updateVariant(1L, "   ", null, null));
  }

  @Test
  void updateVariant_BlankVariantName_StoresNullName() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class))).thenReturn(existing);

    ProductVariant result = productVariantService.updateVariant(1L, null, "   ", null);

    assertNull(result.getVariantName());
  }

  @Test
  void updateVariant_InvalidAttributes_ThrowsIllegalArgument() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));

    assertThrows(
        IllegalArgumentException.class,
        () -> productVariantService.updateVariant(1L, null, null, "{not json}"));
  }

  @Test
  void updateVariant_ConcurrentDuplicateSku_TranslatesDataIntegrityViolation() {
    ProductVariant existing = createVariant(1L, "SCREW-6MM", "6mm");
    when(productVariantRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(productVariantRepository.existsBySku("SCREW-7MM")).thenReturn(false);
    when(productVariantRepository.saveAndFlush(any(ProductVariant.class)))
        .thenThrow(new DataIntegrityViolationException("unique violation"));

    assertThrows(
        SkuAlreadyExistsException.class,
        () -> productVariantService.updateVariant(1L, "SCREW-7MM", null, null));
  }

  // ==================== deleteVariant ====================

  @Test
  void deleteVariant_NoBatches_Deletes() {
    when(productVariantRepository.existsById(1L)).thenReturn(true);
    when(productBatchRepository.existsByVariantId(1L)).thenReturn(false);

    productVariantService.deleteVariant(1L);

    verify(productVariantRepository).deleteById(1L);
  }

  @Test
  void deleteVariant_NotFound_ThrowsVariantNotFound() {
    when(productVariantRepository.existsById(999L)).thenReturn(false);

    assertThrows(
        ProductVariantNotFoundException.class, () -> productVariantService.deleteVariant(999L));
  }

  @Test
  void deleteVariant_HasBatches_ThrowsIllegalArgument() {
    when(productVariantRepository.existsById(1L)).thenReturn(true);
    when(productBatchRepository.existsByVariantId(1L)).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> productVariantService.deleteVariant(1L));

    verify(productVariantRepository, never()).deleteById(1L);
  }

  @Test
  void deleteVariant_NullId_ThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> productVariantService.deleteVariant(null));
  }

  @Test
  void deleteVariant_DataIntegrityViolation_TranslatesToIllegalArgument() {
    when(productVariantRepository.existsById(1L)).thenReturn(true);
    when(productBatchRepository.existsByVariantId(1L)).thenReturn(false);
    doThrow(new DataIntegrityViolationException("FK violation"))
        .when(productVariantRepository)
        .deleteById(1L);

    assertThrows(IllegalArgumentException.class, () -> productVariantService.deleteVariant(1L));
  }

  @Test
  void deleteVariant_FlushDataIntegrityViolation_TranslatesToIllegalArgument() {
    when(productVariantRepository.existsById(1L)).thenReturn(true);
    when(productBatchRepository.existsByVariantId(1L)).thenReturn(false);
    doThrow(new DataIntegrityViolationException("FK violation"))
        .when(productVariantRepository)
        .flush();

    assertThrows(IllegalArgumentException.class, () -> productVariantService.deleteVariant(1L));
  }
}
