package com.acentra.orderprocessing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RestockRequest {

    @NotNull(message = "Restock amount is required")
    @Min(value = 1, message = "Restock amount must be at least 1")
    private Integer amount;

    public RestockRequest() {}

    public RestockRequest(Integer amount) {
        this.amount = amount;
    }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}
