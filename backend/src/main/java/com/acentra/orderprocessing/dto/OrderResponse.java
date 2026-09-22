package com.acentra.orderprocessing.dto;

import com.acentra.orderprocessing.model.OrderPriority;
import com.acentra.orderprocessing.model.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderResponse {
    private String id;
    private String idempotencyKey;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private OrderPriority priority;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderEventDTO> events = new ArrayList<>();
    private boolean duplicateSuppressed;

    public OrderResponse() {}

    public OrderResponse(String id, String idempotencyKey, Long productId, String productName, String productSku,
                         Integer quantity, OrderPriority priority, OrderStatus status, Instant createdAt,
                         Instant updatedAt, List<OrderEventDTO> events, boolean duplicateSuppressed) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.events = events != null ? events : new ArrayList<>();
        this.duplicateSuppressed = duplicateSuppressed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String idempotencyKey;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private OrderPriority priority;
        private OrderStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private List<OrderEventDTO> events = new ArrayList<>();
        private boolean duplicateSuppressed;

        public Builder id(String id) { this.id = id; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder productSku(String productSku) { this.productSku = productSku; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder priority(OrderPriority priority) { this.priority = priority; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder events(List<OrderEventDTO> events) { this.events = events; return this; }
        public Builder duplicateSuppressed(boolean duplicateSuppressed) { this.duplicateSuppressed = duplicateSuppressed; return this; }
        public OrderResponse build() {
            return new OrderResponse(id, idempotencyKey, productId, productName, productSku, quantity, priority, status, createdAt, updatedAt, events, duplicateSuppressed);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderEventDTO> getEvents() { return events; }
    public void setEvents(List<OrderEventDTO> events) { this.events = events; }

    public boolean isDuplicateSuppressed() { return duplicateSuppressed; }
    public void setDuplicateSuppressed(boolean duplicateSuppressed) { this.duplicateSuppressed = duplicateSuppressed; }
}
