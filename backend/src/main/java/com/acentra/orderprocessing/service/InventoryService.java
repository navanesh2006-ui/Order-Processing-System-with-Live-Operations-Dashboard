package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.InventoryDTO;
import com.acentra.orderprocessing.exception.InsufficientStockException;
import com.acentra.orderprocessing.model.Inventory;
import com.acentra.orderprocessing.model.Product;
import com.acentra.orderprocessing.repository.InventoryRepository;
import com.acentra.orderprocessing.repository.ProductRepository;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WebSocketBroadcaster broadcaster;
    private final int lowStockThreshold;

    private final Counter retryCounter;
    private final Counter oversellBlockedCounter;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WebSocketBroadcaster broadcaster,
            MeterRegistry meterRegistry,
            @Value("${order-processing.low-stock-threshold:5}") int lowStockThreshold
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.broadcaster = broadcaster;
        this.lowStockThreshold = lowStockThreshold;

        this.retryCounter = Counter.builder("order_inventory_optimistic_retries_total")
                .description("Total retry attempts on optimistic lock collision")
                .register(meterRegistry);

        this.oversellBlockedCounter = Counter.builder("order_inventory_oversell_blocked_total")
                .description("Total attempts to oversell stock that were prevented")
                .register(meterRegistry);
    }

    /**
     * Decrements inventory with Optimistic Locking and Resilience4j bounded retry.
     * Propagation.REQUIRES_NEW ensures each retry attempt gets a fresh transaction
     * and reads the latest committed DB state rather than a cached entity snapshot.
     */
    @Retry(name = "inventoryRetry", fallbackMethod = "reserveStockFallback")
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public InventoryDTO reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (inventory.getQuantity() < quantity) {
            oversellBlockedCounter.increment();
            throw new InsufficientStockException(productId, quantity, inventory.getQuantity());
        }

        // Decrement using guarded method
        inventory.decrement(quantity);
        Inventory saved = inventoryRepository.saveAndFlush(inventory);

        log.debug("Successfully reserved {} units for product {}. Remaining: {}, version: {}",
                quantity, productId, saved.getQuantity(), saved.getVersion());

        InventoryDTO dto = toDTO(saved);
        broadcaster.broadcastInventory(dto);

        if (saved.getQuantity() <= lowStockThreshold) {
            broadcaster.broadcastAlert(
                    "LOW_STOCK_ALERT",
                    String.format("Warning: Stock for %s (%s) is low: %d remaining",
                            saved.getProduct().getName(), saved.getProduct().getSku(), saved.getQuantity()),
                    dto
            );
        }

        return dto;
    }

    /**
     * Fallback triggered if all Resilience4j retry attempts are exhausted.
     */
    public InventoryDTO reserveStockFallback(Long productId, int quantity, Exception ex) {
        log.error("Optimistic locking retries exhausted for product {} and quantity {}: {}",
                productId, quantity, ex.getMessage());
        if (ex instanceof InsufficientStockException ise) {
            throw ise;
        }
        throw new IllegalStateException("Inventory reservation failed after maximum retries due to high contention", ex);
    }

    @Transactional
    public InventoryDTO restockProduct(Long productId, int amount) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        inventory.increment(amount);
        Inventory saved = inventoryRepository.saveAndFlush(inventory);

        log.info("Restocked product {} by {} units. New quantity: {}", productId, amount, saved.getQuantity());
        InventoryDTO dto = toDTO(saved);
        broadcaster.broadcastInventory(dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryDTO getInventoryForProduct(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product ID: " + productId));
    }

    public InventoryDTO toDTO(Inventory inventory) {
        Product p = inventory.getProduct();
        return InventoryDTO.builder()
                .id(inventory.getId())
                .productId(p.getId())
                .productSku(p.getSku())
                .productName(p.getName())
                .price(p.getPrice())
                .quantity(inventory.getQuantity())
                .version(inventory.getVersion())
                .lowStock(inventory.getQuantity() <= lowStockThreshold)
                .build();
    }
}
