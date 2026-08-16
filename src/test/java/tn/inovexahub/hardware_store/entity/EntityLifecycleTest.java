package tn.inovexahub.hardware_store.entity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EntityLifecycleTest {

  private void invokeMethod(Object entity, String methodName) throws Exception {
    Method method = entity.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(entity);
  }

  // ── User ───────────────────────────────────────────────────────────────

  @Test
  void user_onCreate_setsTimestamps() throws Exception {
    User user = new User();

    invokeMethod(user, "onCreate");

    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
    assertTrue(!user.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
    assertTrue(!user.getUpdatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void user_onUpdate_setsUpdatedAt() throws Exception {
    User user = new User();
    invokeMethod(user, "onCreate");
    LocalDateTime originalCreatedAt = user.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(user, "onUpdate");

    assertNotNull(user.getUpdatedAt());
    assertTrue(
        user.getUpdatedAt().isAfter(originalCreatedAt)
            || user.getUpdatedAt().isEqual(originalCreatedAt));
  }

  @Test
  void user_onCreate_normalizesEmail() throws Exception {
    User user = new User();
    user.setEmail("  John.DOE@Example.COM  ");

    invokeMethod(user, "onCreate");

    assertEquals("john.doe@example.com", user.getEmail());
  }

  @Test
  void user_onUpdate_normalizesEmail() throws Exception {
    User user = new User();
    user.setEmail("  Jane.SMITH@test.COM  ");

    invokeMethod(user, "onCreate");
    assertEquals("jane.smith@test.com", user.getEmail());

    user.setEmail("  BOB@DOMAIN.ORG  ");
    invokeMethod(user, "onUpdate");

    assertEquals("bob@domain.org", user.getEmail());
  }

  @Test
  void user_normalizeEmail_withNullEmail_noNPE() throws Exception {
    User user = new User();
    user.setEmail(null);

    invokeMethod(user, "onCreate");

    assertNull(user.getEmail());
    assertNotNull(user.getCreatedAt());
  }

  // ── Supplier ───────────────────────────────────────────────────────────

  @Test
  void supplier_onCreate_setsTimestamps() throws Exception {
    Supplier supplier = new Supplier();

    invokeMethod(supplier, "onCreate");

    assertNotNull(supplier.getCreatedAt());
    assertNotNull(supplier.getUpdatedAt());
  }

  @Test
  void supplier_onUpdate_setsUpdatedAt() throws Exception {
    Supplier supplier = new Supplier();
    invokeMethod(supplier, "onCreate");
    LocalDateTime originalCreatedAt = supplier.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(supplier, "onUpdate");

    assertNotNull(supplier.getUpdatedAt());
    assertTrue(
        supplier.getUpdatedAt().isAfter(originalCreatedAt)
            || supplier.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── Document ───────────────────────────────────────────────────────────

  @Test
  void document_onCreate_setsTimestampsAndDefaultDate() throws Exception {
    Document document = new Document();

    invokeMethod(document, "onCreate");

    assertNotNull(document.getCreatedAt());
    assertNotNull(document.getUpdatedAt());
    assertNotNull(document.getDate());
  }

  @Test
  void document_onCreate_preservesExistingDate() throws Exception {
    Document document = new Document();
    LocalDateTime customDate = LocalDateTime.of(2024, 6, 15, 10, 0);
    document.setDate(customDate);

    invokeMethod(document, "onCreate");

    assertNotNull(document.getCreatedAt());
    assertNotNull(document.getUpdatedAt());
    assertTrue(document.getDate().isEqual(customDate));
  }

  @Test
  void document_onUpdate_setsUpdatedAt() throws Exception {
    Document document = new Document();
    invokeMethod(document, "onCreate");
    LocalDateTime originalCreatedAt = document.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(document, "onUpdate");

    assertNotNull(document.getUpdatedAt());
    assertTrue(
        document.getUpdatedAt().isAfter(originalCreatedAt)
            || document.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── Client ─────────────────────────────────────────────────────────────

  @Test
  void client_onCreate_setsTimestamps() throws Exception {
    Client client = new Client();

    invokeMethod(client, "onCreate");

    assertNotNull(client.getCreatedAt());
    assertNotNull(client.getUpdatedAt());
  }

  @Test
  void client_onUpdate_setsUpdatedAt() throws Exception {
    Client client = new Client();
    invokeMethod(client, "onCreate");
    LocalDateTime originalCreatedAt = client.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(client, "onUpdate");

    assertNotNull(client.getUpdatedAt());
    assertTrue(
        client.getUpdatedAt().isAfter(originalCreatedAt)
            || client.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── DocumentLine ───────────────────────────────────────────────────────

  @Test
  void documentLine_onCreate_setsTimestamps() throws Exception {
    DocumentLine line = new DocumentLine();

    invokeMethod(line, "onCreate");

    assertNotNull(line.getCreatedAt());
    assertNotNull(line.getUpdatedAt());
  }

  @Test
  void documentLine_onUpdate_setsUpdatedAt() throws Exception {
    DocumentLine line = new DocumentLine();
    invokeMethod(line, "onCreate");
    LocalDateTime originalCreatedAt = line.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(line, "onUpdate");

    assertNotNull(line.getUpdatedAt());
    assertTrue(
        line.getUpdatedAt().isAfter(originalCreatedAt)
            || line.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── PaymentReceipt ─────────────────────────────────────────────────────

  @Test
  void paymentReceipt_onCreate_setsTimestampsAndPaymentDate() throws Exception {
    PaymentReceipt receipt = new PaymentReceipt();

    invokeMethod(receipt, "onCreate");

    assertNotNull(receipt.getCreatedAt());
    assertNotNull(receipt.getUpdatedAt());
    assertNotNull(receipt.getPaymentDate());
    assertTrue(!receipt.getPaymentDate().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void paymentReceipt_onCreate_preservesExistingPaymentDate() throws Exception {
    PaymentReceipt receipt = new PaymentReceipt();
    LocalDateTime customDate = LocalDateTime.of(2025, 3, 10, 14, 30);
    receipt.setPaymentDate(customDate);

    invokeMethod(receipt, "onCreate");

    assertNotNull(receipt.getCreatedAt());
    assertNotNull(receipt.getUpdatedAt());
    assertTrue(receipt.getPaymentDate().isEqual(customDate));
  }

  @Test
  void paymentReceipt_onUpdate_setsUpdatedAt() throws Exception {
    PaymentReceipt receipt = new PaymentReceipt();
    invokeMethod(receipt, "onCreate");
    LocalDateTime originalCreatedAt = receipt.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(receipt, "onUpdate");

    assertNotNull(receipt.getUpdatedAt());
    assertTrue(
        receipt.getUpdatedAt().isAfter(originalCreatedAt)
            || receipt.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── RefreshToken ───────────────────────────────────────────────────────

  @Test
  void refreshToken_onCreate_setsCreatedAt() throws Exception {
    RefreshToken token = new RefreshToken();

    invokeMethod(token, "onCreate");

    assertNotNull(token.getCreatedAt());
    assertTrue(!token.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  // ── AuditLog ───────────────────────────────────────────────────────────

  @Test
  void auditLog_onCreate_setsTimestampWhenNull() throws Exception {
    AuditLog log = new AuditLog();

    invokeMethod(log, "onCreate");

    assertNotNull(log.getTimestamp());
    assertTrue(!log.getTimestamp().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void auditLog_onCreate_preservesExistingTimestamp() throws Exception {
    AuditLog log = new AuditLog();
    LocalDateTime customTimestamp = LocalDateTime.of(2025, 8, 20, 9, 15);
    log.setTimestamp(customTimestamp);

    invokeMethod(log, "onCreate");

    assertTrue(log.getTimestamp().isEqual(customTimestamp));
  }

  // ── Product ────────────────────────────────────────────────────────────

  @Test
  void product_onCreate_setsTimestamps() throws Exception {
    Product product = new Product();

    invokeMethod(product, "onCreate");

    assertNotNull(product.getCreatedAt());
    assertNotNull(product.getUpdatedAt());
    assertTrue(!product.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
    assertTrue(!product.getUpdatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void product_onUpdate_setsUpdatedAt() throws Exception {
    Product product = new Product();
    invokeMethod(product, "onCreate");
    LocalDateTime originalCreatedAt = product.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(product, "onUpdate");

    assertNotNull(product.getUpdatedAt());
    assertTrue(
        product.getUpdatedAt().isAfter(originalCreatedAt)
            || product.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── ProductConditioning ────────────────────────────────────────────────

  @Test
  void productConditioning_onCreate_setsTimestamps() throws Exception {
    ProductConditioning conditioning = new ProductConditioning();

    invokeMethod(conditioning, "onCreate");

    assertNotNull(conditioning.getCreatedAt());
    assertNotNull(conditioning.getUpdatedAt());
    assertTrue(!conditioning.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
    assertTrue(!conditioning.getUpdatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void productConditioning_onUpdate_setsUpdatedAt() throws Exception {
    ProductConditioning conditioning = new ProductConditioning();
    invokeMethod(conditioning, "onCreate");
    LocalDateTime originalCreatedAt = conditioning.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(conditioning, "onUpdate");

    assertNotNull(conditioning.getUpdatedAt());
    assertTrue(
        conditioning.getUpdatedAt().isAfter(originalCreatedAt)
            || conditioning.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── ProductVariant ───────────────────────────────────────────────────

  @Test
  void productVariant_onCreate_setsTimestamps() throws Exception {
    ProductVariant variant = new ProductVariant();

    invokeMethod(variant, "onCreate");

    assertNotNull(variant.getCreatedAt());
    assertNotNull(variant.getUpdatedAt());
    assertTrue(!variant.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
    assertTrue(!variant.getUpdatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void productVariant_onUpdate_setsUpdatedAt() throws Exception {
    ProductVariant variant = new ProductVariant();
    invokeMethod(variant, "onCreate");
    LocalDateTime originalCreatedAt = variant.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(variant, "onUpdate");

    assertNotNull(variant.getUpdatedAt());
    assertTrue(
        variant.getUpdatedAt().isAfter(originalCreatedAt)
            || variant.getUpdatedAt().isEqual(originalCreatedAt));
  }

  // ── ProductBatch ──────────────────────────────────────────────────

  @Test
  void productBatch_onCreate_setsTimestamps() throws Exception {
    ProductBatch batch = new ProductBatch();

    invokeMethod(batch, "onCreate");

    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
    assertTrue(!batch.getCreatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
    assertTrue(!batch.getUpdatedAt().isAfter(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void productBatch_onUpdate_setsUpdatedAt() throws Exception {
    ProductBatch batch = new ProductBatch();
    invokeMethod(batch, "onCreate");
    LocalDateTime originalCreatedAt = batch.getCreatedAt();

    Thread.sleep(10);
    invokeMethod(batch, "onUpdate");

    assertNotNull(batch.getUpdatedAt());
    assertTrue(
        batch.getUpdatedAt().isAfter(originalCreatedAt)
            || batch.getUpdatedAt().isEqual(originalCreatedAt));
  }

  @Test
  void productBatch_onCreate_withoutVariant_succeeds() throws Exception {
    ProductBatch batch = new ProductBatch();
    Product product = new Product();
    product.setId(1L);
    batch.setProduct(product);

    invokeMethod(batch, "onCreate");

    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
  }

  @Test
  void productBatch_onCreate_withMismatchedVariant_throwsIllegalArgument() throws Exception {
    Product product = new Product();
    product.setId(1L);
    Product otherProduct = new Product();
    otherProduct.setId(2L);
    ProductVariant variant = new ProductVariant();
    variant.setProduct(otherProduct);

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> invokeMethod(batch, "onCreate"));
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
  }

  @Test
  void productBatch_onUpdate_withMismatchedVariant_throwsIllegalArgument() throws Exception {
    Product product = new Product();
    product.setId(1L);
    Product otherProduct = new Product();
    otherProduct.setId(2L);
    ProductVariant variant = new ProductVariant();
    variant.setProduct(otherProduct);

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> invokeMethod(batch, "onUpdate"));
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
  }

  @Test
  void productBatch_onCreate_derivesProductFromVariant() throws Exception {
    Product product = new Product();
    product.setId(1L);
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);

    ProductBatch batch = new ProductBatch();
    batch.setVariant(variant);

    invokeMethod(batch, "onCreate");

    assertEquals(product, batch.getProduct());
    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
  }

  @Test
  void productBatch_onCreate_variantWithoutProduct_throwsIllegalArgument() throws Exception {
    Product product = new Product();
    product.setId(1L);
    ProductVariant variant = new ProductVariant();

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);

    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> invokeMethod(batch, "onCreate"));
    assertTrue(ex.getCause() instanceof IllegalArgumentException);
  }

  @Test
  void productBatch_onCreate_matchingVariantWithoutIds_succeeds() throws Exception {
    Product product = new Product();
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setVariant(variant);

    invokeMethod(batch, "onCreate");

    assertNotNull(batch.getCreatedAt());
    assertNotNull(batch.getUpdatedAt());
  }

  // helper — duplicated from Assertions to avoid wildcard import
  private static void assertEquals(Object expected, Object actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }
}
