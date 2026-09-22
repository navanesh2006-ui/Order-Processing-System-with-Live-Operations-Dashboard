package com.acentra.orderprocessing.dto;

public class ThreadPoolStatsDTO {
    private int activeThreads;
    private int queueDepth;
    private long completedTasks;
    private int poolSize;
    private int maxPoolSize;
    private double ordersPerSecond;

    public ThreadPoolStatsDTO() {}

    public ThreadPoolStatsDTO(int activeThreads, int queueDepth, long completedTasks, int poolSize, int maxPoolSize, double ordersPerSecond) {
        this.activeThreads = activeThreads;
        this.queueDepth = queueDepth;
        this.completedTasks = completedTasks;
        this.poolSize = poolSize;
        this.maxPoolSize = maxPoolSize;
        this.ordersPerSecond = ordersPerSecond;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int activeThreads;
        private int queueDepth;
        private long completedTasks;
        private int poolSize;
        private int maxPoolSize;
        private double ordersPerSecond;

        public Builder activeThreads(int activeThreads) { this.activeThreads = activeThreads; return this; }
        public Builder queueDepth(int queueDepth) { this.queueDepth = queueDepth; return this; }
        public Builder completedTasks(long completedTasks) { this.completedTasks = completedTasks; return this; }
        public Builder poolSize(int poolSize) { this.poolSize = poolSize; return this; }
        public Builder maxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; return this; }
        public Builder ordersPerSecond(double ordersPerSecond) { this.ordersPerSecond = ordersPerSecond; return this; }
        public ThreadPoolStatsDTO build() {
            return new ThreadPoolStatsDTO(activeThreads, queueDepth, completedTasks, poolSize, maxPoolSize, ordersPerSecond);
        }
    }

    public int getActiveThreads() { return activeThreads; }
    public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }

    public int getQueueDepth() { return queueDepth; }
    public void setQueueDepth(int queueDepth) { this.queueDepth = queueDepth; }

    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }

    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }

    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    public double getOrdersPerSecond() { return ordersPerSecond; }
    public void setOrdersPerSecond(double ordersPerSecond) { this.ordersPerSecond = ordersPerSecond; }
}
