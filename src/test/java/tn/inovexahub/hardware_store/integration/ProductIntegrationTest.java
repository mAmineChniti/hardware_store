package tn.inovexahub.hardware_store.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.enums.UnitType;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductIntegrationTest {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private ProductRepository productRepository;

  @Autowired private ProductConditioningRepository productConditioningRepository;

  @Test
  void whenCreateProduct_thenProductCanBeRetrieved() {
    Product product = new Product();
    product.setReference("PROD001");
    product.setName("Test Product");
    product.setDescription("Test description");
    product.setUnitType(UnitType.UNITARY);
    product.setIsHeavyMaterial(false);
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
  void whenFindHeavyMaterials_thenReturnHeavyMaterials() {
    Product heavyProduct = new Product();
    heavyProduct.setReference("PROD001");
    heavyProduct.setName("Cement");
    heavyProduct.setIsHeavyMaterial(true);
    heavyProduct.setUnitType(UnitType.WEIGHT);
    heavyProduct.setStockQuantity(new BigDecimal("1000.00"));
    heavyProduct.setAveragePurchasePrice(new BigDecimal("50.00"));
    entityManager.persist(heavyProduct);

    Product lightProduct = new Product();
    lightProduct.setReference("PROD002");
    lightProduct.setName("Hammer");
    lightProduct.setIsHeavyMaterial(false);
    lightProduct.setUnitType(UnitType.UNITARY);
    lightProduct.setStockQuantity(new BigDecimal("50.00"));
    lightProduct.setAveragePurchasePrice(new BigDecimal("20.00"));
    entityManager.persist(lightProduct);

    entityManager.flush();

    List<Product> heavyMaterials = productRepository.findByIsHeavyMaterialTrue();

    assertNotNull(heavyMaterials);
    assertEquals(1, heavyMaterials.size());
    assertEquals("Cement", heavyMaterials.get(0).getName());
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
}
