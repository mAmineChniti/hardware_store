package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.entity.ProductCost;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductCostRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductConditioningRepository productConditioningRepository;
  private final ProductCostRepository productCostRepository;

  public ProductService(
      ProductRepository productRepository,
      ProductConditioningRepository productConditioningRepository,
      ProductCostRepository productCostRepository) {
    this.productRepository = productRepository;
    this.productConditioningRepository = productConditioningRepository;
    this.productCostRepository = productCostRepository;
  }

  // Product CRUD operations
  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  public Optional<Product> getProductById(Long id) {
    return productRepository.findById(id);
  }

  public Optional<Product> getProductByReference(String reference) {
    return productRepository.findByReference(reference);
  }

  public Product createProduct(Product product) {
    return productRepository.save(product);
  }

  public Product updateProduct(Long id, Product productDetails) {
    Product product =
        productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

    product.setReference(productDetails.getReference());
    product.setName(productDetails.getName());
    product.setDescription(productDetails.getDescription());
    product.setImage(productDetails.getImage());
    product.setCategory(productDetails.getCategory());
    product.setUnitType(productDetails.getUnitType());
    product.setIsHeavyMaterial(productDetails.getIsHeavyMaterial());
    product.setBaseUnit(productDetails.getBaseUnit());
    // stockQuantity and averagePurchasePrice managed by updateStockQuantity and cost-history
    product.setPriceOnSite(productDetails.getPriceOnSite());
    product.setPriceDelivered(productDetails.getPriceDelivered());

    return productRepository.save(product);
  }

  public void deleteProduct(Long id) {
    productRepository.deleteById(id);
  }

  // Search operations
  public List<Product> searchProducts(String keyword) {
    return productRepository.searchByKeyword(keyword);
  }

  public List<Product> getProductsByCategory(String category) {
    return productRepository.findByCategory(category);
  }

  public List<Product> getHeavyMaterials() {
    return productRepository.findByIsHeavyMaterialTrue();
  }

  public List<Product> getLowStockProducts(BigDecimal threshold) {
    return productRepository.findLowStock(threshold);
  }

  // Product Conditioning operations
  public List<ProductConditioning> getProductConditionings(Long productId) {
    return productConditioningRepository.findByProductId(productId);
  }

  public ProductConditioning addProductConditioning(
      Long productId, ProductConditioning conditioning) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    conditioning.setProduct(product);
    return productConditioningRepository.save(conditioning);
  }

  public ProductConditioning updateProductConditioning(
      Long id, ProductConditioning conditioningDetails) {
    ProductConditioning conditioning =
        productConditioningRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Product conditioning not found"));

    conditioning.setDescription(conditioningDetails.getDescription());
    conditioning.setQuantityPerUnit(conditioningDetails.getQuantityPerUnit());
    conditioning.setUnitPrice(conditioningDetails.getUnitPrice());

    return productConditioningRepository.save(conditioning);
  }

  public void deleteProductConditioning(Long id) {
    productConditioningRepository.deleteById(id);
  }

  // Product Cost operations
  public List<ProductCost> getProductCostHistory(Long productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    return productCostRepository.findByProductOrderByEffectiveDateDesc(product);
  }

  public Optional<ProductCost> getCurrentProductCost(Long productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    return productCostRepository.findTopByProductOrderByEffectiveDateDesc(product);
  }

  public ProductCost addProductCost(
      Long productId,
      BigDecimal unitCost,
      LocalDate effectiveDate,
      Supplier supplier,
      String notes) {
    if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Unit cost must be positive");
    }

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    ProductCost productCost = new ProductCost();
    productCost.setProduct(product);
    productCost.setUnitCost(unitCost);
    productCost.setEffectiveDate(effectiveDate);
    productCost.setSupplier(supplier);
    productCost.setNotes(notes);

    ProductCost savedCost = productCostRepository.save(productCost);

    // Update average purchase price (PAMP) for margin calculations
    updateAveragePurchasePrice(product);

    return savedCost;
  }

  public Optional<ProductCost> getProductCostForDate(Long productId, LocalDate date) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    return productCostRepository.findByProductAndEffectiveDate(product, date);
  }

  public List<ProductCost> getProductCostsBetweenDates(
      Long productId, LocalDate startDate, LocalDate endDate) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    return productCostRepository.findByProductAndEffectiveDateBetween(product, startDate, endDate);
  }

  public void deleteProductCost(Long id) {
    ProductCost productCost =
        productCostRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Product cost not found"));
    Product product = productCost.getProduct();
    productCostRepository.deleteById(id);
    updateAveragePurchasePrice(product);
  }

  // Business logic for PAMP calculation
  private void updateAveragePurchasePrice(Product product) {
    List<ProductCost> costs = productCostRepository.findByProductOrderByEffectiveDateDesc(product);
    if (costs.isEmpty()) {
      product.setAveragePurchasePrice(BigDecimal.ZERO);
    } else {
      // Calculate weighted average of recent costs (last 5 costs)
      int limit = Math.min(5, costs.size());
      BigDecimal totalCost = BigDecimal.ZERO;
      BigDecimal totalWeight = BigDecimal.ZERO;

      for (int i = 0; i < limit; i++) {
        ProductCost cost = costs.get(i);
        // More recent costs have higher weight
        BigDecimal weight = BigDecimal.valueOf(limit - i);
        totalCost = totalCost.add(cost.getUnitCost().multiply(weight));
        totalWeight = totalWeight.add(weight);
      }

      if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
        product.setAveragePurchasePrice(totalCost.divide(totalWeight, 3, RoundingMode.HALF_UP));
      }
    }
    productRepository.save(product);
  }

  // Stock management
  public void updateStockQuantity(Long productId, BigDecimal quantityChange) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    // Normalize quantityChange to scale 3 before calculation
    BigDecimal normalizedChange = quantityChange.setScale(3, RoundingMode.HALF_UP);
    BigDecimal newQuantity =
        product.getStockQuantity().add(normalizedChange).setScale(3, RoundingMode.HALF_UP);
    if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Insufficient stock");
    }

    product.setStockQuantity(newQuantity);
    productRepository.save(product);
  }
}
