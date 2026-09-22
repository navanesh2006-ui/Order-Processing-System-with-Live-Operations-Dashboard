package com.acentra.orderprocessing.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "dead_letter_orders",
    indexes = {
        @Index(name = "idx_dlq_order_id", columnList = "order_id"),
        @Index(name = "idx_dlq_status", columnList = "status"),
        @Index(name = "idx_dlq_last_attempt", columnList = "last_attempt_at")
    }
)
public class DeadLetterOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "failure_reason", nullable = false, length = 1024)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DlqStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DeadLetterOrder() {}

    public DeadLetterOrder(Long id, String orderId, String failureReason, Integer retryCount, Instant lastAttemptAt, String payloadJson, DlqStatus status, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.lastAttemptAt = lastAttemptAt;
        this.payloadJson = payloadJson;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String orderId;
        private String failureReason;
        private Integer retryCount;
        private Instant lastAttemptAt;
        private String payloadJson;
        private DlqStatus status = DlqStatus.ACTIVE;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderId(String orderId) { this.orderId = orderId; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public Builder lastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; return this; }
        public Builder payloadJson(String payloadJson) { this.payloadJson = payloadJson; return this; }
        public Builder status(DlqStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public DeadLetterOrder build() { return new DeadLetterOrder(id, orderId, failureReason, retryCount, lastAttemptAt, payloadJson, status, createdAt); }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public DlqStatus getStatus() { return status; }
    public void setStatus(DlqStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
