package com.acentra.orderprocessing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ChaosTestRequest {

    @Min(value = 5, message = "At least 5 orders required for load test")
    @Max(value = 1000, message = "Maximum 1000 orders per load test")
    private int totalOrders = 100;

    @Min(value = 1, message = "Concurrency level must be at least 1")
    @Max(value = 50, message = "Concurrency level cannot exceed 50")
    private int concurrencyLevel = 10;

    private boolean includeScarceStock = true;

    public ChaosTestRequest() {}

    public ChaosTestRequest(int totalOrders, int concurrencyLevel, boolean includeScarceStock) {
        this.totalOrders = totalOrders;
        this.concurrencyLevel = concurrencyLevel;
        this.includeScarceStock = includeScarceStock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalOrders = 100;
        private int concurrencyLevel = 10;
        private boolean includeScarceStock = true;

        public Builder totalOrders(int totalOrders) { this.totalOrders = totalOrders; return this; }
        public Builder concurrencyLevel(int concurrencyLevel) { this.concurrencyLevel = concurrencyLevel; return this; }
        public Builder includeScarceStock(boolean includeScarceStock) { this.includeScarceStock = includeScarceStock; return this; }
        public ChaosTestRequest build() {
            return new ChaosTestRequest(totalOrders, concurrencyLevel, includeScarceStock);
        }
    }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public int getConcurrencyLevel() { return concurrencyLevel; }
    public void setConcurrencyLevel(int concurrencyLevel) { this.concurrencyLevel = concurrencyLevel; }

    public boolean isIncludeScarceStock() { return includeScarceStock; }
    public void setIncludeScarceStock(boolean includeScarceStock) { this.includeScarceStock = includeScarceStock; }
}
