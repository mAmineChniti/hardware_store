package tn.inovexahub.hardware_store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductVariant;
import tn.inovexahub.hardware_store.exception.ProductNotFoundException;
import tn.inovexahub.hardware_store.exception.ProductVariantNotFoundException;
import tn.inovexahub.hardware_store.exception.SkuAlreadyExistsException;
import tn.inovexahub.hardware_store.repository.ProductBatchRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;
import tn.inovexahub.hardware_store.repository.ProductVariantRepository;

@Service
public class ProductVariantService {

  private static final int SKU_MAX_LENGTH = 50;
  private static final int VARIANT_NAME_MAX_LENGTH = 100;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ProductVariantRepository productVariantRepository;
  private final ProductRepository productRepository;
  private final ProductBatchRepository productBatchRepository;

  public ProductVariantService(
      ProductVariantRepository productVariantRepository,
      ProductRepository productRepository,
      ProductBatchRepository productBatchRepository) {
    this.productVariantRepository = productVariantRepository;
    this.productRepository = productRepository;
    this.productBatchRepository = productBatchRepository;
  }

  /**
   * Create a new product variant.
   *
   * @param productId Product ID
   * @param sku Unique SKU for the variant (trimmed and upper-cased before storage)
   * @param variantName Optional variant name (can be left null if fully described by attributes)
   * @param attributes JSON attributes string
   * @return Created variant
   */
  @Transactional
  public ProductVariant createVariant(
      Long productId, String sku, String variantName, String attributes) {
    if (productId == null) {
      throw new IllegalArgumentException("Product ID is required");
    }

    String normalizedSku = normalizeSku(sku);
    String normalizedName = normalizeVariantName(variantName);
    String normalizedAttributes = normalizeAttributes(attributes);

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    if (productVariantRepository.existsBySku(normalizedSku)) {
      throw new SkuAlreadyExistsException(normalizedSku);
    }

    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setSku(normalizedSku);
    variant.setVariantName(normalizedName);
    variant.setAttributes(normalizedAttributes);

    try {
      return productVariantRepository.saveAndFlush(variant);
    } catch (DataIntegrityViolationException e) {
      // Unique constraint is the final guard against concurrent duplicate SKUs
      throw new SkuAlreadyExistsException(normalizedSku);
    }
  }

  /** Get all variants for a product. */
  public List<ProductVariant> getVariantsByProductId(Long productId) {
    if (productId == null) {
      throw new IllegalArgumentException("Product ID is required");
    }
    return productVariantRepository.findByProductIdOrderByVariantNameAscIdAsc(productId);
  }

  /** Get variant by ID. */
  public Optional<ProductVariant> getVariantById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Variant ID is required");
    }
    return productVariantRepository.findById(id);
  }

  /** Get variant by SKU. */
  public Optional<ProductVariant> getVariantBySku(String sku) {
    if (sku == null) {
      throw new IllegalArgumentException("SKU is required");
    }
    return productVariantRepository.findBySku(normalizeSku(sku));
  }

  /**
   * Update variant.
   *
   * @param id Variant ID
   * @param sku New SKU (optional)
   * @param variantName New variant name (optional)
   * @param attributes New attributes (optional)
   * @return Updated variant
   */
  @Transactional
  public ProductVariant updateVariant(Long id, String sku, String variantName, String attributes) {
    if (id == null) {
      throw new IllegalArgumentException("Variant ID is required");
    }

    ProductVariant variant =
        productVariantRepository
            .findById(id)
            .orElseThrow(() -> new ProductVariantNotFoundException(id));

    if (sku != null) {
      String normalizedSku = normalizeSku(sku);
      if (!normalizedSku.equals(variant.getSku())) {
        if (productVariantRepository.existsBySku(normalizedSku)) {
          throw new SkuAlreadyExistsException(normalizedSku);
        }
        variant.setSku(normalizedSku);
      }
    }

    if (variantName != null) {
      variant.setVariantName(normalizeVariantName(variantName));
    }

    if (attributes != null) {
      variant.setAttributes(normalizeAttributes(attributes));
    }

    try {
      return productVariantRepository.saveAndFlush(variant);
    } catch (DataIntegrityViolationException e) {
      // Unique constraint is the final guard against concurrent duplicate SKUs
      throw new SkuAlreadyExistsException(variant.getSku());
    }
  }

  /** Delete variant. */
  @Transactional
  public void deleteVariant(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Variant ID is required");
    }

    if (!productVariantRepository.existsById(id)) {
      throw new ProductVariantNotFoundException(id);
    }

    if (productBatchRepository.existsByVariantId(id)) {
      throw new IllegalArgumentException("Cannot delete variant with existing batches");
    }

    try {
      productVariantRepository.deleteById(id);
      // Flush so deferred FK violations surface inside this try block instead of at commit
      productVariantRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new IllegalArgumentException(
          "Cannot delete variant referenced by existing documents or batches");
    }
  }

  /** Trim and upper-case SKU, enforcing non-blank and length limits. */
  private String normalizeSku(String sku) {
    if (sku == null || sku.trim().isEmpty()) {
      throw new IllegalArgumentException("SKU is required");
    }
    String normalized = sku.trim().toUpperCase(java.util.Locale.ROOT);
    if (normalized.length() > SKU_MAX_LENGTH) {
      throw new IllegalArgumentException("SKU must not exceed 50 characters");
    }
    return normalized;
  }

  /** Trim variant name; optional, returns null when absent or blank. */
  private String normalizeVariantName(String variantName) {
    if (variantName == null || variantName.trim().isEmpty()) {
      return null;
    }
    String normalized = variantName.trim();
    if (normalized.length() > VARIANT_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException("Variant name must not exceed 100 characters");
    }
    return normalized;
  }

  /** Validate that attributes (when present) contain valid JSON. */
  private String normalizeAttributes(String attributes) {
    if (attributes == null || attributes.trim().isEmpty()) {
      return null;
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(attributes);
      if (!node.isObject()) {
        throw new IllegalArgumentException("Attributes must be a JSON object");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Attributes must be valid JSON");
    }
    return attributes;
  }
}
