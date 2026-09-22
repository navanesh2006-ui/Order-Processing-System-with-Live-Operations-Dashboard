package com.acentra.orderprocessing.model;

import com.acentra.orderprocessing.exception.InsufficientStockException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
@Table(
    name = "inventories",
    indexes = {
        @Index(name = "idx_inventory_product_id", columnList = "product_id", unique = true)
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Min(value = 0, message = "Inventory quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    @Version
    @Column(nullable = false)
    private Long version;

    public Inventory() {}

    public Inventory(Long id, Product product, Integer quantity, Long version) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Product product;
        private Integer quantity;
        private Long version;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder product(Product product) { this.product = product; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder version(Long version) { this.version = version; return this; }
        public Inventory build() { return new Inventory(id, product, quantity, version); }
    }

    public synchronized void decrement(int amount) {
        if (this.quantity < amount) {
            throw new InsufficientStockException(
                this.product != null ? this.product.getId() : null,
                amount,
                this.quantity
            );
        }
        this.quantity -= amount;
    }

    public synchronized void increment(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Increment amount must be non-negative");
        }
        this.quantity += amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
