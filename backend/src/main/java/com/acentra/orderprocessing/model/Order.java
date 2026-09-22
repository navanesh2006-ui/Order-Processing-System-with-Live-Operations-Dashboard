package com.acentra.orderprocessing.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_created_at", columnList = "created_at")
    }
)
public class Order {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Order() {}

    public Order(String id, String idempotencyKey, Long productId, Integer quantity, OrderPriority priority, OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String idempotencyKey;
        private Long productId;
        private Integer quantity;
        private OrderPriority priority = OrderPriority.STANDARD;
        private OrderStatus status = OrderStatus.RECEIVED;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder priority(OrderPriority priority) { this.priority = priority; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Order build() { return new Order(id, idempotencyKey, productId, quantity, priority, status, createdAt, updatedAt); }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

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
}
