package com.acentra.orderprocessing.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "order_events",
    indexes = {
        @Index(name = "idx_event_order_id", columnList = "order_id"),
        @Index(name = "idx_event_created_at", columnList = "created_at")
    }
)
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(length = 512)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OrderEvent() {}

    public OrderEvent(Long id, String orderId, OrderStatus fromStatus, OrderStatus toStatus, String reason, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String orderId;
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String reason;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderId(String orderId) { this.orderId = orderId; return this; }
        public Builder fromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; return this; }
        public Builder toStatus(OrderStatus toStatus) { this.toStatus = toStatus; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public OrderEvent build() { return new OrderEvent(id, orderId, fromStatus, toStatus, reason, createdAt); }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public OrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; }

    public OrderStatus getToStatus() { return toStatus; }
    public void setToStatus(OrderStatus toStatus) { this.toStatus = toStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
