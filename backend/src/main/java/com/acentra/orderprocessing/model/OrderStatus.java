package com.acentra.orderprocessing.model;

public enum OrderStatus {
    RECEIVED,
    PROCESSING,
    CONFIRMED,
    FAILED,
    DEAD_LETTERED
}
