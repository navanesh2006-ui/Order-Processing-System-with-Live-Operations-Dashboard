package com.acentra.orderprocessing.dto;

import com.acentra.orderprocessing.model.OrderStatus;
import java.time.Instant;

public class OrderEventDTO {
    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String reason;
    private Instant createdAt;

    public OrderEventDTO() {}

    public OrderEventDTO(Long id, OrderStatus fromStatus, OrderStatus toStatus, String reason, Instant createdAt) {
        this.id = id;
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
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String reason;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder fromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; return this; }
        public Builder toStatus(OrderStatus toStatus) { this.toStatus = toStatus; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public OrderEventDTO build() { return new OrderEventDTO(id, fromStatus, toStatus, reason, createdAt); }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(OrderStatus fromStatus) { this.fromStatus = fromStatus; }

    public OrderStatus getToStatus() { return toStatus; }
    public void setToStatus(OrderStatus toStatus) { this.toStatus = toStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
