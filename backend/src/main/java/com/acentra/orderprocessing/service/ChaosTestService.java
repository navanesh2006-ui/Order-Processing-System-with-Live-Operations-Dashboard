package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.ChaosTestRequest;
import com.acentra.orderprocessing.dto.ChaosTestResponse;
import com.acentra.orderprocessing.dto.OrderRequest;
import com.acentra.orderprocessing.dto.OrderResponse;
import com.acentra.orderprocessing.model.OrderPriority;
import com.acentra.orderprocessing.model.Product;
import com.acentra.orderprocessing.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ChaosTestService {

    private static final Logger log = LoggerFactory.getLogger(ChaosTestService.class);

    private final OrderProcessingService orderProcessingService;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final DeadLetterOrderRepository dlqRepository;
    private final WebSocketBroadcaster broadcaster;

    public ChaosTestService(
            OrderProcessingService orderProcessingService,
            InventoryService inventoryService,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            DeadLetterOrderRepository dlqRepository,
            WebSocketBroadcaster broadcaster
    ) {
        this.orderProcessingService = orderProcessingService;
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.dlqRepository = dlqRepository;
        this.broadcaster = broadcaster;
    }

    public ChaosTestResponse runChaosLoadTest(ChaosTestRequest request) {
        String testId = "chaos-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Starting Chaos Load Test [{}]: {} orders across {} concurrent client workers",
                testId, request.getTotalOrders(), request.getConcurrencyLevel());

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new IllegalStateException("No products found in database to execute load test against.");
        }

        long startTime = System.currentTimeMillis();
        ExecutorService clientPool = Executors.newFixedThreadPool(request.getConcurrencyLevel());
        List<Future<OrderResponse>> futures = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < request.getTotalOrders(); i++) {
            Product targetProduct;
            if (request.isIncludeScarceStock() && random.nextDouble() < 0.4) {
                targetProduct = products.get(0); // RTX 5090 Blackwell (scarce)
            } else {
                targetProduct = products.get(random.nextInt(products.size()));
            }

            OrderPriority priority = (random.nextDouble() < 0.15) ? OrderPriority.VIP : OrderPriority.STANDARD;
            int quantity = random.nextInt(3) + 1;
            String idempotencyKey = testId + "-item-" + i;

            OrderRequest orderReq = OrderRequest.builder()
                    .productId(targetProduct.getId())
                    .quantity(quantity)
                    .priority(priority)
                    .idempotencyKey(idempotencyKey)
                    .build();

            futures.add(clientPool.submit(() -> orderProcessingService.submitOrder(orderReq)));
        }

        clientPool.shutdown();
        try {
            clientPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Chaos Load Test [{}] finished in {} ms", testId, durationMs);

        broadcaster.broadcastAlert(
                "CHAOS_TEST_COMPLETE",
                String.format("Chaos load test completed: %d orders fired across %d threads in %d ms",
                        request.getTotalOrders(), request.getConcurrencyLevel(), durationMs),
                Map.of("testId", testId, "orders", request.getTotalOrders(), "durationMs", durationMs)
        );

        return ChaosTestResponse.builder()
                .testId(testId)
                .totalOrdersSubmitted(request.getTotalOrders())
                .status("COMPLETED")
                .durationMs(durationMs)
                .build();
    }

    public Map<String, Object> runIdempotencyDemo(Long productId) {
        String sharedKey = "idempotency-demo-" + UUID.randomUUID();
        Product product = productRepository.findById(productId != null ? productId : 1L)
                .orElse(productRepository.findAll().get(0));

        OrderRequest request1 = OrderRequest.builder()
                .productId(product.getId())
                .quantity(1)
                .idempotencyKey(sharedKey)
                .priority(OrderPriority.VIP)
                .build();

        OrderRequest request2 = OrderRequest.builder()
                .productId(product.getId())
                .quantity(1)
                .idempotencyKey(sharedKey)
                .priority(OrderPriority.VIP)
                .build();

        OrderResponse response1 = orderProcessingService.submitOrder(request1);
        OrderResponse response2 = orderProcessingService.submitOrder(request2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sharedIdempotencyKey", sharedKey);
        result.put("firstSubmission", response1);
        result.put("secondSubmission", response2);
        result.put("duplicateSuppressed", response2.isDuplicateSuppressed());
        result.put("message", response2.isDuplicateSuppressed()
                ? "SUCCESS: Duplicate submission cleanly suppressed! Inventory was only decremented once."
                : "FAIL: Duplicate was not suppressed.");

        return result;
    }

    public void resetAllData() {
        log.warn("Resetting all orders, events, DLQ, and restoring inventory to initial levels...");
        dlqRepository.deleteAll();
        orderEventRepository.deleteAll();
        orderRepository.deleteAll();

        inventoryRepository.findAll().forEach(inv -> {
            switch (inv.getProduct().getSku()) {
                case "NV-RTX5090" -> inv.setQuantity(3);
                case "CD-QUANTUM-H1" -> inv.setQuantity(8);
                case "TPU-V5E" -> inv.setQuantity(25);
                case "HL-ENT-V2" -> inv.setQuantity(100);
                case "UW-OLED-49" -> inv.setQuantity(150);
                default -> inv.setQuantity(50);
            }
            inventoryRepository.save(inv);
        });

        inventoryService.getAllInventories().forEach(broadcaster::broadcastInventory);
    }
}
