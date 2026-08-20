package tn.inovexahub.hardware_store.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductBatch;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.enums.UnitType;
import tn.inovexahub.hardware_store.exception.SkuAlreadyExistsException;
import tn.inovexahub.hardware_store.repository.ProductBatchRepository;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;
import tn.inovexahub.hardware_store.repository.ProductVariantRepository;
import tn.inovexahub.hardware_store.service.ProductVariantService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductIntegrationTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private ProductRepository productRepository;

  @Autowired private ProductConditioningRepository productConditioningRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired private ProductBatchRepository productBatchRepository;

  @Autowired private ProductVariantService productVariantService;

  @Test
  void whenCreateProduct_thenProductCanBeRetrieved() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Test Product");
    product.setDescription("Test description");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("100.00"));
    product.setAveragePurchasePrice(new BigDecimal("25.00"));

    entityManager.persist(product);
    entityManager.flush();
    entityManager.clear();

    Product found = productRepository.findById(product.getId()).orElse(null);

    assertNotNull(found);
    assertEquals("PROD001", found.getReference());
    assertEquals("Test Product", found.getName());
  }

  @Test
  void whenFindByReference_thenReturnProduct() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Test Product");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("50.00"));
    product.setAveragePurchasePrice(new BigDecimal("20.00"));
    entityManager.persist(product);

    entityManager.flush();

    var found = productRepository.findByReference("PROD001");

    assertTrue(found.isPresent());
    assertEquals("Test Product", found.get().getName());
  }

  @Test
  void whenFindByCategory_thenReturnCategoryProducts() {
    Product product1 = new Product();
    product1.setReference("PROD001");
    product1.setName("Hammer");
    product1.setCategory("Tools");
    product1.setUnitType(UnitType.UNITARY);
    product1.setStockQuantity(new BigDecimal("50.00"));
    product1.setAveragePurchasePrice(new BigDecimal("20.00"));
    entityManager.persist(product1);

    Product product2 = new Product();
    product2.setReference("PROD002");
    product2.setName("Screwdriver");
    product2.setCategory("Tools");
    product2.setUnitType(UnitType.UNITARY);
    product2.setStockQuantity(new BigDecimal("30.00"));
    product2.setAveragePurchasePrice(new BigDecimal("15.00"));
    entityManager.persist(product2);

    entityManager.flush();

    List<Product> tools = productRepository.findByCategory("Tools");

    assertNotNull(tools);
    assertEquals(2, tools.size());
  }

  @Test
  void whenAddProductConditioning_thenConditioningCanBeRetrieved() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Test Product");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("100.00"));
    product.setAveragePurchasePrice(new BigDecimal("25.00"));
    entityManager.persist(product);

    ProductConditioning conditioning = new ProductConditioning();
    conditioning.setProduct(product);
    conditioning.setDescription("Box of 10");
    conditioning.setQuantityPerUnit(new BigDecimal("10.00"));
    conditioning.setUnitPrice(new BigDecimal("250.00"));
    entityManager.persist(conditioning);

    entityManager.flush();

    List<ProductConditioning> conditionings =
        productConditioningRepository.findByProductId(product.getId());

    assertNotNull(conditionings);
    assertEquals(1, conditionings.size());
    assertEquals("Box of 10", conditionings.get(0).getDescription());
  }

  @Test
  void whenSearchByKeyword_thenReturnMatchingProducts() {
    Product product1 = new Product();
    product1.setReference("PROD001");
    product1.setName("Steel Hammer");
    product1.setUnitType(UnitType.UNITARY);
    product1.setStockQuantity(new BigDecimal("50.00"));
    product1.setAveragePurchasePrice(new BigDecimal("20.00"));
    entityManager.persist(product1);

    Product product2 = new Product();
    product2.setReference("PROD002");
    product2.setName("Rubber Hammer");
    product2.setUnitType(UnitType.UNITARY);
    product2.setStockQuantity(new BigDecimal("30.00"));
    product2.setAveragePurchasePrice(new BigDecimal("15.00"));
    entityManager.persist(product2);

    Product product3 = new Product();
    product3.setReference("PROD003");
    product3.setName("Screwdriver");
    product3.setUnitType(UnitType.UNITARY);
    product3.setStockQuantity(new BigDecimal("40.00"));
    product3.setAveragePurchasePrice(new BigDecimal("10.00"));
    entityManager.persist(product3);

    entityManager.flush();

    List<Product> results = productRepository.searchByKeyword("hammer");

    assertNotNull(results);
    assertEquals(2, results.size());
  }

  @Test
  void whenCreateVariant_thenVariantCanBeRetrievedBySku() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Screws");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("0.00"));
    product.setAveragePurchasePrice(new BigDecimal("0.00"));
    entityManager.persist(product);

    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setSku("SCREW-6MM");
    variant.setVariantName("6mm Steel Screws");
    variant.setAttributes("{\"calibre\": \"6mm\"}");
    entityManager.persist(variant);

    entityManager.flush();

    var found = productVariantRepository.findBySku("SCREW-6MM");

    assertTrue(found.isPresent());
    assertEquals("6mm Steel Screws", found.get().getVariantName());
    assertEquals(product.getId(), found.get().getProduct().getId());
    assertEquals(
        1,
        productVariantRepository.findByProductIdOrderByVariantNameAscIdAsc(product.getId()).size());
  }

  @Test
  void whenDuplicateVariantSku_thenConstraintRejected() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Screws");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("0.00"));
    product.setAveragePurchasePrice(new BigDecimal("0.00"));
    entityManager.persist(product);

    ProductVariant variant1 = new ProductVariant();
    variant1.setProduct(product);
    variant1.setSku("SCREW-6MM");
    variant1.setVariantName("6mm");
    entityManager.persist(variant1);

    ProductVariant variant2 = new ProductVariant();
    variant2.setProduct(product);
    variant2.setSku("SCREW-6MM");
    variant2.setVariantName("6mm duplicate");

    assertThrows(
        org.hibernate.exception.ConstraintViolationException.class,
        () -> {
          entityManager.persist(variant2);
          entityManager.flush();
        });
  }

  @Test
  void whenCreateVariantWithDuplicateSkuViaService_thenSkuAlreadyExistsException() {
    Product product = new Product();
    product.setReference("PROD003");
    product.setName("Screws");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("0.00"));
    product.setAveragePurchasePrice(new BigDecimal("0.00"));
    entityManager.persist(product);

    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setSku("SCREW-8MM");
    variant.setVariantName("8mm");
    entityManager.persist(variant);
    entityManager.flush();

    assertThrows(
        SkuAlreadyExistsException.class,
        () -> productVariantService.createVariant(product.getId(), "SCREW-8MM", "8mm dup", ""));
  }

  @Test
  void whenBlankSku_thenValidationRejected() {
    Product product = new Product();
    product.setReference("PROD002");
    product.setName("Screws");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("0.00"));
    product.setAveragePurchasePrice(new BigDecimal("0.00"));
    entityManager.persist(product);

    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setSku("   ");
    variant.setVariantName("Blank sku");

    assertThrows(
        ConstraintViolationException.class,
        () -> {
          entityManager.persist(variant);
          entityManager.flush();
        });
  }

  @Test
  void whenAddBatchToVariant_thenAvailableStockQueryWorks() {
    Product product = new Product();
    product.setReference("PROD003");
    product.setName("AC Units");
    product.setUnitType(UnitType.UNITARY);
    product.setStockQuantity(new BigDecimal("0.00"));
    product.setAveragePurchasePrice(new BigDecimal("0.00"));
    entityManager.persist(product);

    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setSku("AC-12000BTU");
    variant.setAttributes("{\"btu\": 12000}");
    entityManager.persist(variant);

    ProductBatch batch1 = new ProductBatch();
    batch1.setProduct(product);
    batch1.setVariant(variant);
    batch1.setQuantity(new BigDecimal("10.00"));
    batch1.setUnitCost(new BigDecimal("1000.00"));
    batch1.setUnitPrice(new BigDecimal("1200.00"));
    entityManager.persist(batch1);

    ProductBatch batch2 = new ProductBatch();
    batch2.setProduct(product);
    batch2.setVariant(variant);
    batch2.setQuantity(new BigDecimal("5.00"));
    batch2.setUnitCost(new BigDecimal("1050.00"));
    batch2.setUnitPrice(new BigDecimal("1250.00"));
    entityManager.persist(batch2);

    entityManager.flush();
    entityManager.clear();

    assertTrue(productBatchRepository.existsByVariantId(variant.getId()));
    assertEquals(
        0,
        new BigDecimal("15.00")
            .compareTo(
                productBatchRepository
                    .sumAvailableQuantityByVariantId(variant.getId())
                    .orElseThrow()));
    assertEquals(2, productBatchRepository.findAvailableBatchesByVariantId(variant.getId()).size());
  }
}
