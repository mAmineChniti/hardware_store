package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

@Service
public class ProductBatchService {

  private static final Logger log = LoggerFactory.getLogger(ProductBatchService.class);

  private final ProductBatchRepository productBatchRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final SupplierRepository supplierRepository;

  public ProductBatchService(
      ProductBatchRepository productBatchRepository,
      ProductRepository productRepository,
      ProductVariantRepository productVariantRepository,
      SupplierRepository supplierRepository) {
    this.productBatchRepository = productBatchRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
    this.supplierRepository = supplierRepository;
  }

  /**
   * Add a new inventory batch for a product variant.
   *
   * @param variantId Variant ID
   * @param quantity Quantity in this batch
   * @param unitCost Purchase cost per unit
   * @param unitPrice User-defined selling price per unit
   * @param supplierId Optional supplier ID
   * @param notes Optional notes
   * @return Created batch
   */
  @Transactional
  public ProductBatch addBatchForVariant(
      Long variantId,
      BigDecimal quantity,
      BigDecimal unitCost,
      BigDecimal unitPrice,
      Long supplierId,
      String notes) {
    if (variantId == null) {
      throw new IllegalArgumentException("Variant ID is required");
    }

    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ProductVariantNotFoundException(variantId));

    validateBatchQuantities(quantity, unitCost, unitPrice);

    ProductBatch batch = new ProductBatch();
    batch.setProduct(variant.getProduct());
    batch.setVariant(variant);
    batch.setQuantity(quantity);
    batch.setUnitCost(unitCost);
    batch.setUnitPrice(unitPrice);
    batch.setNotes(notes);

    if (supplierId != null) {
      batch.setSupplier(resolveSupplier(supplierId));
    }

    ProductBatch savedBatch = productBatchRepository.save(batch);

    refreshProductRollups(variant.getProduct().getId());

    return savedBatch;
  }

  /**
   * Allocate stock from available batches for a specific variant using FIFO.
   *
   * @param variantId Variant ID
   * @param requestedQuantity Quantity to allocate
   * @return List of allocated batches with quantities
   */
  @Transactional
  public List<BatchAllocation> allocateStockFromVariant(
      Long variantId, BigDecimal requestedQuantity) {
    if (variantId == null) {
      throw new IllegalArgumentException("Variant ID is required");
    }
    requestedQuantity = validateRequestedQuantity(requestedQuantity);

    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ProductVariantNotFoundException(variantId));

    List<ProductBatch> availableBatches;
    try {
      availableBatches = productBatchRepository.lockAvailableBatchesByVariantId(variantId);
    } catch (PessimisticLockingFailureException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Stock is currently locked by another operation. Please retry.");
    }

    if (availableBatches.isEmpty()) {
      throw new IllegalArgumentException("No available stock for this variant");
    }

    BigDecimal totalAvailable =
        availableBatches.stream()
            .map(ProductBatch::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalAvailable.compareTo(requestedQuantity) < 0) {
      throw new IllegalArgumentException(
          "Insufficient stock. Available: " + totalAvailable + ", Requested: " + requestedQuantity);
    }

    List<BatchAllocation> allocations = computeAllocations(availableBatches, requestedQuantity);

    refreshProductRollups(variant.getProduct().getId());

    return allocations;
  }

  /** Get all batches for a variant ordered by creation date (oldest first). */
  public List<ProductBatch> getBatchesByVariantId(Long variantId) {
    return productBatchRepository.findByVariantIdOrderByCreatedAtAscIdAsc(variantId);
  }

  /** Get available batches (with remaining quantity) for a variant. */
  public List<ProductBatch> getAvailableBatchesByVariantId(Long variantId) {
    return productBatchRepository.findAvailableBatchesByVariantId(variantId);
  }

  /**
   * Add a new inventory batch for a product.
   *
   * @param productId Product ID
   * @param quantity Quantity in this batch
   * @param unitCost Purchase cost per unit
   * @param unitPrice User-defined selling price per unit
   * @param supplierId Optional supplier ID
   * @param notes Optional notes
   * @return Created batch
   */
  @Transactional
  public ProductBatch addBatch(
      Long productId,
      BigDecimal quantity,
      BigDecimal unitCost,
      BigDecimal unitPrice,
      Long supplierId,
      String notes) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    validateBatchQuantities(quantity, unitCost, unitPrice);

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setQuantity(quantity);
    batch.setUnitCost(unitCost);
    batch.setUnitPrice(unitPrice);
    batch.setNotes(notes);

    if (supplierId != null) {
      batch.setSupplier(resolveSupplier(supplierId));
    }

    ProductBatch savedBatch = productBatchRepository.save(batch);

    refreshProductRollups(productId);

    return savedBatch;
  }

  /**
   * Estimate a FIFO batch allocation without mutating stock. Used to snapshot price and cost when
   * adding a document line (stock is only reserved at validation). Does not throw on insufficient
   * stock since drafts may legitimately exceed current availability. A variantless line is rejected
   * outright when the product has no product-level batches at all: such a line can never be
   * validated, since product-level allocation never draws from variant batches.
   *
   * @param productId Product ID
   * @param requestedQuantity Quantity to estimate
   * @return Estimated batch allocations (possibly partial or empty)
   */
  public List<BatchAllocation> estimateAllocation(Long productId, BigDecimal requestedQuantity) {
    requestedQuantity = validateRequestedQuantity(requestedQuantity);
    List<ProductBatch> availableBatches =
        productBatchRepository.findAvailableBatchesByProductId(productId);
    if (availableBatches.isEmpty()
        && !productBatchRepository.existsByProductIdAndVariantIsNull(productId)) {
      throw new IllegalArgumentException("No product-level stock for this product");
    }
    return estimateAllocations(availableBatches, requestedQuantity);
  }

  /**
   * Estimate a FIFO batch allocation for a variant without mutating stock.
   *
   * @param variantId Variant ID
   * @param requestedQuantity Quantity to estimate
   * @return Estimated batch allocations (possibly partial or empty)
   */
  public List<BatchAllocation> estimateAllocationFromVariant(
      Long variantId, BigDecimal requestedQuantity) {
    requestedQuantity = validateRequestedQuantity(requestedQuantity);
    return estimateAllocations(
        productBatchRepository.findAvailableBatchesByVariantId(variantId), requestedQuantity);
  }

  /**
   * Apply a signed stock adjustment through the batch ledger. Positive adjustments are added to the
   * most recent product-level batch; negative adjustments are deducted FIFO across batches.
   * Product.stockQuantity and the average purchase price are recomputed from the resulting batch
   * quantities.
   *
   * @param productId Product ID
   * @param quantityChange Signed quantity change (+ to add, - to subtract)
   */
  @Transactional
  public void applyStockAdjustment(Long productId, BigDecimal quantityChange) {
    if (quantityChange == null) {
      throw new IllegalArgumentException("Quantity change is required");
    }
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    BigDecimal normalizedChange = quantityChange.setScale(3, RoundingMode.HALF_UP);
    BigDecimal newQuantity =
        product.getStockQuantity().add(normalizedChange).setScale(3, RoundingMode.HALF_UP);
    if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Insufficient stock");
    }

    List<ProductBatch> batches =
        productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(productId);
    if (batches.isEmpty()) {
      throw new IllegalArgumentException("Cannot adjust stock: product has no batches");
    }

    if (normalizedChange.compareTo(BigDecimal.ZERO) > 0) {
      ProductBatch newestProductBatch = null;
      for (ProductBatch batch : batches) {
        if (batch.getVariant() == null) {
          newestProductBatch = batch;
        }
      }
      if (newestProductBatch == null) {
        throw new IllegalArgumentException(
            "Cannot adjust stock: product has no product-level batches");
      }
      newestProductBatch.setQuantity(
          newestProductBatch.getQuantity().add(normalizedChange).setScale(3, RoundingMode.HALF_UP));
      productBatchRepository.save(newestProductBatch);
    } else if (normalizedChange.compareTo(BigDecimal.ZERO) < 0) {
      BigDecimal remaining = normalizedChange.negate();
      for (ProductBatch batch : batches) {
        if (batch.getVariant() != null) {
          continue;
        }
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
          break;
        }
        BigDecimal available = batch.getQuantity();
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
        BigDecimal deduct = available.min(remaining).setScale(3, RoundingMode.DOWN);
        batch.setQuantity(batch.getQuantity().subtract(deduct).setScale(3, RoundingMode.HALF_UP));
        productBatchRepository.save(batch);
        remaining = remaining.subtract(deduct);
      }
      if (remaining.compareTo(BigDecimal.ZERO) > 0) {
        throw new IllegalArgumentException("Insufficient stock");
      }
    }

    refreshProductRollups(productId);
  }

  /**
   * Restore quantities to specific batches (used when cancelling a validated document).
   *
   * @param batchAllocations Map of batchId to quantity to restore
   */
  @Transactional
  public void restoreBatches(Map<Long, BigDecimal> batchAllocations) {
    if (batchAllocations == null || batchAllocations.isEmpty()) {
      return;
    }

    Set<Long> affectedProductIds = new HashSet<>();
    for (Map.Entry<Long, BigDecimal> entry : batchAllocations.entrySet()) {
      productBatchRepository
          .findById(entry.getKey())
          .ifPresentOrElse(
              batch -> {
                batch.setQuantity(batch.getQuantity().add(entry.getValue()));
                productBatchRepository.save(batch);
                affectedProductIds.add(batch.getProduct().getId());
              },
              () -> log.warn("Batch {} not found during restore, skipping", entry.getKey()));
    }

    for (Long productId : affectedProductIds) {
      refreshProductRollups(productId);
    }
  }

  /**
   * Allocate stock from available batches using FIFO.
   *
   * @param productId Product ID
   * @param requestedQuantity Quantity to allocate
   * @return List of allocated batches with quantities
   */
  @Transactional
  public List<BatchAllocation> allocateStock(Long productId, BigDecimal requestedQuantity) {
    requestedQuantity = validateRequestedQuantity(requestedQuantity);

    List<ProductBatch> availableBatches;
    try {
      availableBatches = productBatchRepository.lockAvailableBatchesByProductId(productId);
    } catch (PessimisticLockingFailureException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Stock is currently locked by another operation. Please retry.");
    }

    if (availableBatches.isEmpty()) {
      throw new IllegalArgumentException("No available stock for this product");
    }

    BigDecimal totalAvailable =
        availableBatches.stream()
            .map(ProductBatch::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalAvailable.compareTo(requestedQuantity) < 0) {
      throw new IllegalArgumentException(
          "Insufficient stock. Available: " + totalAvailable + ", Requested: " + requestedQuantity);
    }

    List<BatchAllocation> allocations = computeAllocations(availableBatches, requestedQuantity);

    refreshProductRollups(productId);

    return allocations;
  }

  /** Get all batches for a product ordered by creation date (oldest first). */
  public List<ProductBatch> getBatchesByProductId(Long productId) {
    return productBatchRepository.findByProductIdOrderByCreatedAtAscIdAsc(productId);
  }

  /** Get available batches (with remaining quantity) for a product. */
  public List<ProductBatch> getAvailableBatchesByProductId(Long productId) {
    return productBatchRepository.findAvailableBatchesByProductId(productId);
  }

  /** Delete a batch. */
  @Transactional
  public void deleteBatch(Long batchId) {
    ProductBatch batch =
        productBatchRepository
            .findById(batchId)
            .orElseThrow(() -> new ProductBatchNotFoundException(batchId));

    Long productId = batch.getProduct().getId();
    productBatchRepository.deleteById(batchId);

    refreshProductRollups(productId);
  }

  /** Update batch quantity (e.g., for corrections). */
  @Transactional
  public ProductBatch updateBatchQuantity(Long batchId, BigDecimal newQuantity) {
    ProductBatch batch =
        productBatchRepository
            .findById(batchId)
            .orElseThrow(() -> new ProductBatchNotFoundException(batchId));

    if (newQuantity == null) {
      throw new IllegalArgumentException("Quantity is required");
    }
    if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative");
    }

    batch.setQuantity(newQuantity);
    ProductBatch savedBatch = productBatchRepository.save(batch);

    refreshProductRollups(batch.getProduct().getId());

    return savedBatch;
  }

  /** Update batch pricing. */
  @Transactional
  public ProductBatch updateBatchPricing(
      Long batchId, BigDecimal newUnitCost, BigDecimal newUnitPrice, Boolean adminOverride) {
    ProductBatch batch =
        productBatchRepository
            .findById(batchId)
            .orElseThrow(() -> new ProductBatchNotFoundException(batchId));

    if (newUnitCost == null) {
      throw new IllegalArgumentException("Unit cost is required");
    }
    if (newUnitPrice == null) {
      throw new IllegalArgumentException("Unit price is required");
    }
    if (newUnitCost.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Unit cost cannot be negative");
    }
    if (newUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Unit price cannot be negative");
    }

    // Validate that price is not below cost (unless admin override)
    if (adminOverride == null || !adminOverride) {
      if (newUnitPrice.compareTo(newUnitCost) < 0) {
        throw new IllegalArgumentException(
            "Unit price cannot be below unit cost. Price: "
                + newUnitPrice
                + ", Cost: "
                + newUnitCost
                + ". Use admin override to allow this.");
      }
    }

    batch.setUnitCost(newUnitCost);
    batch.setUnitPrice(newUnitPrice);
    ProductBatch savedBatch = productBatchRepository.save(batch);

    // Refresh the product's stock quantity and weighted average purchase price
    refreshProductRollups(batch.getProduct().getId());

    return savedBatch;
  }

  /** Validate batch quantity, unit cost and unit price values. */
  private void validateBatchQuantities(
      BigDecimal quantity, BigDecimal unitCost, BigDecimal unitPrice) {
    if (quantity == null) {
      throw new IllegalArgumentException("Quantity is required");
    }
    if (unitCost == null) {
      throw new IllegalArgumentException("Unit cost is required");
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("Unit price is required");
    }
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Unit cost cannot be negative");
    }
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Unit price cannot be negative");
    }
  }

  /** Load the supplier for the given ID or fail with a clear error. */
  private Supplier resolveSupplier(Long supplierId) {
    return supplierRepository
        .findById(supplierId)
        .orElseThrow(() -> new SupplierNotFoundException(supplierId));
  }

  /**
   * Validate and normalize a requested quantity (present, positive, scale 3). Normalizing before
   * allocation guarantees FIFO deduction never truncates mid-batch, which would otherwise leave a
   * sub-unit remainder unallocated while batches are already deducted.
   */
  private BigDecimal validateRequestedQuantity(BigDecimal requestedQuantity) {
    if (requestedQuantity == null) {
      throw new IllegalArgumentException("Requested quantity is required");
    }
    BigDecimal normalized = requestedQuantity.setScale(3, RoundingMode.HALF_UP);
    if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Requested quantity must be positive");
    }
    return normalized;
  }

  /** Mutating FIFO allocation across the given batches (deducts batch quantities). */
  private List<BatchAllocation> computeAllocations(
      List<ProductBatch> availableBatches, BigDecimal requestedQuantity) {
    List<BatchAllocation> allocations = new ArrayList<>();
    BigDecimal remainingToAllocate = requestedQuantity;

    for (ProductBatch batch : availableBatches) {
      if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }

      BigDecimal availableInBatch = batch.getQuantity();
      BigDecimal allocateFromBatch =
          availableInBatch.min(remainingToAllocate).setScale(3, RoundingMode.DOWN);
      if (allocateFromBatch.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      // Update batch quantity
      batch.setQuantity(batch.getQuantity().subtract(allocateFromBatch));
      productBatchRepository.save(batch);

      allocations.add(
          new BatchAllocation(
              batch.getId(),
              allocateFromBatch,
              batch.getUnitCost(),
              batch.getUnitPrice(),
              batch.getCreatedAt()));

      remainingToAllocate = remainingToAllocate.subtract(allocateFromBatch);
    }

    return allocations;
  }

  /** Non-mutating FIFO estimate across the given batches. */
  private List<BatchAllocation> estimateAllocations(
      List<ProductBatch> availableBatches, BigDecimal requestedQuantity) {
    List<BatchAllocation> allocations = new ArrayList<>();
    BigDecimal remainingToAllocate = requestedQuantity;

    for (ProductBatch batch : availableBatches) {
      if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }

      BigDecimal allocateFromBatch =
          batch.getQuantity().min(remainingToAllocate).setScale(3, RoundingMode.DOWN);
      if (allocateFromBatch.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      allocations.add(
          new BatchAllocation(
              batch.getId(),
              allocateFromBatch,
              batch.getUnitCost(),
              batch.getUnitPrice(),
              batch.getCreatedAt()));
      remainingToAllocate = remainingToAllocate.subtract(allocateFromBatch);
    }

    return allocations;
  }

  /** Refresh the product's stock quantity and weighted average purchase price in one pass. */
  private void refreshProductRollups(Long productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    BigDecimal totalQuantity =
        productBatchRepository
            .sumAllAvailableQuantityByProductId(productId)
            .orElse(BigDecimal.ZERO);
    BigDecimal totalCost =
        productBatchRepository.sumAllAvailableBatchesCost(productId).orElse(BigDecimal.ZERO);

    product.setStockQuantity(totalQuantity);
    product.setAveragePurchasePrice(
        totalQuantity.compareTo(BigDecimal.ZERO) > 0
            ? totalCost.divide(totalQuantity, 3, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
    productRepository.save(product);
  }

  /** DTO for batch allocation results. */
  public static class BatchAllocation {
    private Long batchId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal unitPrice;
    private java.time.LocalDateTime batchCreatedAt;

    public BatchAllocation(
        Long batchId,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal unitPrice,
        java.time.LocalDateTime batchCreatedAt) {
      this.batchId = batchId;
      this.quantity = quantity;
      this.unitCost = unitCost;
      this.unitPrice = unitPrice;
      this.batchCreatedAt = batchCreatedAt;
    }

    public Long getBatchId() {
      return batchId;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public BigDecimal getUnitCost() {
      return unitCost;
    }

    public BigDecimal getUnitPrice() {
      return unitPrice;
    }

    public java.time.LocalDateTime getBatchCreatedAt() {
      return batchCreatedAt;
    }
  }
}
