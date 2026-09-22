package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.model.OrderPriority;
import java.time.Instant;

public class PrioritizedOrderTask implements Runnable, Comparable<PrioritizedOrderTask> {

    private final String orderId;
    private final OrderPriority priority;
    private final long sequenceNumber;
    private final Instant queuedAt;
    private final Runnable runnable;

    public PrioritizedOrderTask(String orderId, OrderPriority priority, long sequenceNumber, Runnable runnable) {
        this.orderId = orderId;
        this.priority = priority != null ? priority : OrderPriority.STANDARD;
        this.sequenceNumber = sequenceNumber;
        this.queuedAt = Instant.now();
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }

    @Override
    public int compareTo(PrioritizedOrderTask other) {
        // VIP comes before STANDARD
        if (this.priority != other.priority) {
            return this.priority == OrderPriority.VIP ? -1 : 1;
        }
        // FIFO order within same priority tier
        return Long.compare(this.sequenceNumber, other.sequenceNumber);
    }

    public String getOrderId() { return orderId; }
    public OrderPriority getPriority() { return priority; }
    public long getSequenceNumber() { return sequenceNumber; }
    public Instant getQueuedAt() { return queuedAt; }
    public Runnable getRunnable() { return runnable; }
}
