package tn.inovexahub.hardware_store.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductBatchTest {

  private Product product;
  private Product otherProduct;
  private ProductVariant variant;
  private ProductVariant variantWithNullProduct;

  @BeforeEach
  void setUp() {
    product = new Product();
    product.setId(1L);
    product.setName("Test Product");

    otherProduct = new Product();
    otherProduct.setId(2L);
    otherProduct.setName("Other Product");

    variant = new ProductVariant();
    variant.setId(10L);
    variant.setProduct(product);
    variant.setVariantName("Test Variant");

    variantWithNullProduct = new ProductVariant();
    variantWithNullProduct.setId(20L);
    variantWithNullProduct.setProduct(null);
    variantWithNullProduct.setVariantName("Variant with null product");
  }

  @Test
  void validateVariantOwnership_NullVariant_DoesNothing() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(null);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    // This should not throw any exception
    batch.onCreate();
    batch.onUpdate();

    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
  }

  @Test
  void validateVariantOwnership_NullProduct_SetsProductFromVariant() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(null);
    batch.setVariant(variant);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    batch.onCreate();

    assertEquals(product, batch.getProduct());
  }

  @Test
  void validateVariantOwnership_VariantBelongsToProduct_DoesNothing() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    // This should not throw any exception
    batch.onCreate();
    batch.onUpdate();

    assertEquals(product, batch.getProduct());
  }

  @Test
  void validateVariantOwnership_VariantFromDifferentProduct_ThrowsException() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    // Change variant to belong to other product
    variant.setProduct(otherProduct);

    assertThrows(IllegalArgumentException.class, batch::onCreate);
    assertThrows(IllegalArgumentException.class, batch::onUpdate);
  }

  @Test
  void validateVariantOwnership_VariantWithNullProduct_ThrowsException() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variantWithNullProduct);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    assertThrows(IllegalArgumentException.class, batch::onCreate);
    assertThrows(IllegalArgumentException.class, batch::onUpdate);
  }

  @Test
  void validateVariantOwnership_ProductIdNullVariantIdNull_DoesNothing() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    product.setId(null);
    batch.setVariant(variant);
    variant.setProduct(product);
    variant.setId(null);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    // This should not throw any exception when both IDs are null
    batch.onCreate();
    batch.onUpdate();
  }

  @Test
  void onCreate_SetsTimestamps() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    batch.onCreate();

    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
  }

  @Test
  void onUpdate_UpdatesTimestamp() {
    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setQuantity(new BigDecimal("10.000"));
    batch.setUnitCost(new BigDecimal("15.00"));
    batch.setUnitPrice(new BigDecimal("20.00"));

    batch.onCreate();
    LocalDateTime initialUpdatedAt = batch.getUpdatedAt();

    batch.onUpdate();

    assertNotNull(batch.getUpdatedAt());
    assertFalse(batch.getUpdatedAt().isBefore(initialUpdatedAt));
  }
}
