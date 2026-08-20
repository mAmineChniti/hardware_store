package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Product;
import tn.inovexahub.hardware_store.entity.ProductConditioning;
import tn.inovexahub.hardware_store.exception.ProductNotFoundException;
import tn.inovexahub.hardware_store.repository.ProductConditioningRepository;
import tn.inovexahub.hardware_store.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductConditioningRepository productConditioningRepository;
  private final ProductBatchService productBatchService;

  public ProductService(
      ProductRepository productRepository,
      ProductConditioningRepository productConditioningRepository,
      ProductBatchService productBatchService) {
    this.productRepository = productRepository;
    this.productConditioningRepository = productConditioningRepository;
    this.productBatchService = productBatchService;
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

  public Product createProductWithInitialBatch(
      Product product,
      BigDecimal initialQuantity,
      BigDecimal initialUnitCost,
      BigDecimal initialUnitPrice,
      Long supplierId,
      String notes) {
    Product created = createProduct(product);
    productBatchService.addBatch(
        created.getId(), initialQuantity, initialUnitCost, initialUnitPrice, supplierId, notes);
    return productRepository.findById(created.getId()).orElseThrow();
  }

  public Product updateProduct(Long id, Product productDetails) {
    Product product =
        productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

    product.setReference(productDetails.getReference());
    product.setName(productDetails.getName());
    product.setDescription(productDetails.getDescription());
    product.setImage(productDetails.getImage());
    product.setCategory(productDetails.getCategory());
    product.setUnitType(productDetails.getUnitType());
    product.setBaseUnit(productDetails.getBaseUnit());
    // stockQuantity managed by updateStockQuantity; averagePurchasePrice recomputed by
    // ProductBatchService; supplier intentionally read-only on update (set only at creation via
    // CreateProductRequest.supplierId)

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
            .orElseThrow(() -> new ProductNotFoundException(productId));
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

  // Stock management
  /**
   * Apply a signed stock adjustment through the batch ledger. Product.stockQuantity is owned by
   * ProductBatchService and is recomputed from the batch quantities.
   */
  public void updateStockQuantity(Long productId, BigDecimal quantityChange) {
    productBatchService.applyStockAdjustment(productId, quantityChange);
  }
}
