package com.acentra.orderprocessing.dto;

import com.acentra.orderprocessing.model.OrderPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private OrderPriority priority = OrderPriority.STANDARD;

    public OrderRequest() {}

    public OrderRequest(Long productId, Integer quantity, String idempotencyKey, OrderPriority priority) {
        this.productId = productId;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.priority = priority != null ? priority : OrderPriority.STANDARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long productId;
        private Integer quantity;
        private String idempotencyKey;
        private OrderPriority priority = OrderPriority.STANDARD;

        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder priority(OrderPriority priority) { this.priority = priority; return this; }
        public OrderRequest build() { return new OrderRequest(productId, quantity, idempotencyKey, priority); }
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }
}
