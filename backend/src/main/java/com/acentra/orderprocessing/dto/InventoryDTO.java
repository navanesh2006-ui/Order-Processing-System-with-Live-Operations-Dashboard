package com.acentra.orderprocessing.dto;

import java.math.BigDecimal;

public class InventoryDTO {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private Long version;
    private boolean lowStock;

    public InventoryDTO() {}

    public InventoryDTO(Long id, Long productId, String productSku, String productName, BigDecimal price, Integer quantity, Long version, boolean lowStock) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.version = version;
        this.lowStock = lowStock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long productId;
        private String productSku;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private Long version;
        private boolean lowStock;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder productSku(String productSku) { this.productSku = productSku; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder version(Long version) { this.version = version; return this; }
        public Builder lowStock(boolean lowStock) { this.lowStock = lowStock; return this; }
        public InventoryDTO build() {
            return new InventoryDTO(id, productId, productSku, productName, price, quantity, version, lowStock);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }
}
