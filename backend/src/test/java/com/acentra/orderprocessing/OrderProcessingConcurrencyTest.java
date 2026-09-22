package com.acentra.orderprocessing;

import com.acentra.orderprocessing.dto.InventoryDTO;
import com.acentra.orderprocessing.dto.OrderRequest;
import com.acentra.orderprocessing.dto.OrderResponse;
import com.acentra.orderprocessing.model.*;
import com.acentra.orderprocessing.repository.*;
import com.acentra.orderprocessing.service.InventoryService;
import com.acentra.orderprocessing.service.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
public class OrderProcessingConcurrencyTest {

    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private DeadLetterOrderRepository dlqRepository;

    private Product testProduct;

    @BeforeEach
    public void setUp() {
        dlqRepository.deleteAll();
        orderEventRepository.deleteAll();
        orderRepository.deleteAll();

        // Find or create test product
        testProduct = productRepository.findBySku("CONCURRENT-TEST-SKU").orElseGet(() -> {
            Product p = Product.builder()
                    .sku("CONCURRENT-TEST-SKU")
                    .name("Concurrency Test Widget")
                    .price(new BigDecimal("99.99"))
                    .description("Test product for concurrency proof")
                    .build();
            return productRepository.save(p);
        });

        // Set exact stock to 5
        Inventory inventory = inventoryRepository.findByProductId(testProduct.getId()).orElseGet(() -> {
            return Inventory.builder()
                    .product(testProduct)
                    .quantity(5)
                    .version(0L)
                    .build();
        });
        inventory.setQuantity(5);
        inventoryRepository.saveAndFlush(inventory);
    }

    @Test
    @DisplayName("ZERO OVERSELLING: 50 concurrent buyers competing for 5 units -> exactly 5 confirmed, 0 oversold, rest in DLQ")
    public void testHighConcurrencyZeroOverselling() throws InterruptedException {
        int totalBuyers = 50;
        int availableStock = 5;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalBuyers);

        List<Future<OrderResponse>> futures = new ArrayList<>();

        for (int i = 0; i < totalBuyers; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                startLatch.await(); // Simultaneous blast
                try {
                    OrderRequest req = OrderRequest.builder()
                            .productId(testProduct.getId())
                            .quantity(1)
                            .idempotencyKey("concurrent-buyer-" + UUID.randomUUID())
                            .priority(index % 5 == 0 ? OrderPriority.VIP : OrderPriority.STANDARD)
                            .build();
                    return orderProcessingService.submitOrder(req);
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Release the floodgate
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All submissions should finish within 10s");

        // Give the background worker thread pool time to process all tasks
        Thread.sleep(3000);

        // Verify inventory
        InventoryDTO finalInventory = inventoryService.getInventoryForProduct(testProduct.getId());
        assertEquals(0, finalInventory.getQuantity(), "Inventory must be exactly 0 (never negative!)");

        // Verify orders in DB
        List<Order> orders = orderRepository.findAll();
        assertEquals(totalBuyers, orders.size(), "All orders should be recorded in database");

        long confirmedCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long deadLetteredCount = orders.stream().filter(o -> o.getStatus() == OrderStatus.DEAD_LETTERED).count();

        assertEquals(availableStock, confirmedCount, "Exactly 5 orders must be CONFIRMED");
        assertEquals(totalBuyers - availableStock, deadLetteredCount, "Remaining 45 orders must be DEAD_LETTERED");

        // Verify DLQ entries
        List<DeadLetterOrder> dlqEntries = dlqRepository.findAll();
        assertEquals(totalBuyers - availableStock, dlqEntries.size(), "DLQ table must record all 45 failed orders");

        pool.shutdown();
    }

    @Test
    @DisplayName("IDEMPOTENCY: Duplicate submission with same key is suppressed and does not double-decrement")
    public void testIdempotencyProtection() throws InterruptedException {
        String sharedKey = "idempotent-key-" + UUID.randomUUID();

        OrderRequest req1 = OrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .idempotencyKey(sharedKey)
                .priority(OrderPriority.STANDARD)
                .build();

        OrderResponse resp1 = orderProcessingService.submitOrder(req1);
        assertFalse(resp1.isDuplicateSuppressed(), "First submission is not duplicate");

        // Immediate identical second submission
        OrderRequest req2 = OrderRequest.builder()
                .productId(testProduct.getId())
                .quantity(2)
                .idempotencyKey(sharedKey)
                .priority(OrderPriority.STANDARD)
                .build();

        OrderResponse resp2 = orderProcessingService.submitOrder(req2);
        assertTrue(resp2.isDuplicateSuppressed(), "Second submission must be marked duplicateSuppressed");
        assertEquals(resp1.getId(), resp2.getId(), "Both responses should point to the exact same order ID");

        // Wait for async worker
        Thread.sleep(1500);

        // Initial stock was 5, 2 reserved -> should have exactly 3 left, NOT 1
        InventoryDTO finalInventory = inventoryService.getInventoryForProduct(testProduct.getId());
        assertEquals(3, finalInventory.getQuantity(), "Inventory should only be decremented ONCE (3 remaining)");
    }
}
