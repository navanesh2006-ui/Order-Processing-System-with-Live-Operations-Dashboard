package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.OrderEventDTO;
import com.acentra.orderprocessing.dto.OrderRequest;
import com.acentra.orderprocessing.dto.OrderResponse;
import com.acentra.orderprocessing.exception.InsufficientStockException;
import com.acentra.orderprocessing.model.*;
import com.acentra.orderprocessing.repository.OrderEventRepository;
import com.acentra.orderprocessing.repository.OrderRepository;
import com.acentra.orderprocessing.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderQueueService orderQueueService;
    private final DeadLetterQueueService dlqService;
    private final WebSocketBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    private final Counter ordersCreatedCounter;
    private final Counter ordersConfirmedCounter;
    private final Counter ordersFailedCounter;
    private final Counter duplicateOrdersSuppressedCounter;

    public OrderProcessingService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            ProductRepository productRepository,
            InventoryService inventoryService,
            OrderQueueService orderQueueService,
            DeadLetterQueueService dlqService,
            WebSocketBroadcaster broadcaster,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.orderQueueService = orderQueueService;
        this.dlqService = dlqService;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;

        this.ordersCreatedCounter = Counter.builder("order_processing_orders_created_total")
                .description("Total incoming orders received")
                .register(meterRegistry);

        this.ordersConfirmedCounter = Counter.builder("order_processing_orders_confirmed_total")
                .description("Total orders successfully confirmed")
                .register(meterRegistry);

        this.ordersFailedCounter = Counter.builder("order_processing_orders_failed_total")
                .description("Total orders that failed and routed to DLQ")
                .register(meterRegistry);

        this.duplicateOrdersSuppressedCounter = Counter.builder("order_processing_duplicates_suppressed_total")
                .description("Total duplicate order submissions suppressed via idempotency key")
                .register(meterRegistry);
    }

    /**
     * Ingestion point for order submission.
     * Enforces Idempotency -> Persists RECEIVED state -> Submits to ThreadPool queue.
     */
    public OrderResponse submitOrder(OrderRequest request) {
        // 1. Idempotency Check
        Optional<Order> existingOrderOpt = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOrderOpt.isPresent()) {
            Order existing = existingOrderOpt.get();
            duplicateOrdersSuppressedCounter.increment();
            log.info("Duplicate order detected with idempotency key: {}. Suppressing submission.", request.getIdempotencyKey());
            OrderResponse resp = toResponse(existing);
            resp.setDuplicateSuppressed(true);
            return resp;
        }

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + request.getProductId()));

        String orderId = UUID.randomUUID().toString();
        Order order = Order.builder()
                .id(orderId)
                .idempotencyKey(request.getIdempotencyKey())
                .productId(product.getId())
                .quantity(request.getQuantity())
                .priority(request.getPriority() != null ? request.getPriority() : OrderPriority.STANDARD)
                .status(OrderStatus.RECEIVED)
                .build();

        Order saved = saveOrderInNewTx(order);
        recordEvent(orderId, null, OrderStatus.RECEIVED, "Order received and queued");
        ordersCreatedCounter.increment();

        OrderResponse initialResponse = toResponse(saved);
        broadcaster.broadcastOrder(initialResponse);

        // Submit to asynchronous priority worker queue
        orderQueueService.submitOrderTask(orderId, saved.getPriority(), () -> processOrderAsync(saved.getId(), request));

        return initialResponse;
    }

    /**
     * Core worker task executed inside the ThreadPoolExecutor.
     */
    public void processOrderAsync(String orderId, OrderRequest request) {
        try {
            // Transition: RECEIVED -> PROCESSING
            Order order = updateOrderStatus(orderId, OrderStatus.PROCESSING, "Worker thread began reservation attempt");
            broadcaster.broadcastOrder(toResponse(order));

            // Execute stock reservation with Optimistic Locking + Bounded Retries
            inventoryService.reserveStock(order.getProductId(), order.getQuantity());

            // Transition: PROCESSING -> CONFIRMED
            Order confirmed = updateOrderStatus(orderId, OrderStatus.CONFIRMED, "Inventory reserved successfully");
            ordersConfirmedCounter.increment();
            broadcaster.broadcastOrder(toResponse(confirmed));
            log.info("Order {} CONFIRMED for product {} (qty: {})", orderId, order.getProductId(), order.getQuantity());

        } catch (InsufficientStockException ise) {
            handleOrderFailure(orderId, request, ise.getMessage(), 1, "Insufficient stock");
        } catch (Exception ex) {
            handleOrderFailure(orderId, request, ex.getMessage(), 5, "Contention or reservation error");
        }
    }

    private void handleOrderFailure(String orderId, OrderRequest request, String reason, int attempts, String summary) {
        ordersFailedCounter.increment();
        log.warn("Order {} FAILED: {}. Routing to Dead Letter Queue.", orderId, reason);

        // Transition: -> FAILED
        updateOrderStatus(orderId, OrderStatus.FAILED, reason);

        // Route to DLQ
        dlqService.recordFailure(orderId, reason, attempts, request);

        // Transition: -> DEAD_LETTERED
        Order deadLettered = updateOrderStatus(orderId, OrderStatus.DEAD_LETTERED, "Transferred to Dead Letter Queue");
        broadcaster.broadcastOrder(toResponse(deadLettered));
    }

    /**
     * Operator re-submits a dead-lettered order.
     */
    public OrderResponse replayDlqOrder(Long dlqId) {
        DeadLetterOrder dlq = dlqService.getEntity(dlqId);
        Order order = orderRepository.findById(dlq.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dlq.getOrderId()));

        // Mark DLQ item as replayed
        dlqService.markAsReplayed(dlqId);

        // Record replay event
        recordEvent(order.getId(), order.getStatus(), OrderStatus.PROCESSING, "Replayed from DLQ by operator");
        order.setStatus(OrderStatus.PROCESSING);
        Order saved = orderRepository.saveAndFlush(order);
        broadcaster.broadcastOrder(toResponse(saved));

        // Submit task back into queue with VIP priority for customer recovery
        orderQueueService.submitOrderTask(order.getId(), OrderPriority.VIP, () -> {
            try {
                inventoryService.reserveStock(order.getProductId(), order.getQuantity());
                Order confirmed = updateOrderStatus(order.getId(), OrderStatus.CONFIRMED, "Replay confirmed: Inventory reserved");
                ordersConfirmedCounter.increment();
                broadcaster.broadcastOrder(toResponse(confirmed));
            } catch (Exception ex) {
                handleOrderFailure(order.getId(), null, "Replay failed: " + ex.getMessage(), dlq.getRetryCount() + 1, "Replay failed");
            }
        });

        return toResponse(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order saveOrderInNewTx(Order order) {
        return orderRepository.saveAndFlush(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order updateOrderStatus(String orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order saved = orderRepository.saveAndFlush(order);
        recordEvent(orderId, oldStatus, newStatus, reason);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(String orderId, OrderStatus from, OrderStatus to, String reason) {
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .build();
        orderEventRepository.saveAndFlush(event);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    public OrderResponse toResponse(Order order) {
        Product p = productRepository.findById(order.getProductId()).orElse(null);
        List<OrderEventDTO> eventDTOs = orderEventRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(e -> OrderEventDTO.builder()
                        .id(e.getId())
                        .fromStatus(e.getFromStatus())
                        .toStatus(e.getToStatus())
                        .reason(e.getReason())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .idempotencyKey(order.getIdempotencyKey())
                .productId(order.getProductId())
                .productName(p != null ? p.getName() : "Unknown")
                .productSku(p != null ? p.getSku() : "N/A")
                .quantity(order.getQuantity())
                .priority(order.getPriority())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .events(eventDTOs)
                .duplicateSuppressed(false)
                .build();
    }
}
