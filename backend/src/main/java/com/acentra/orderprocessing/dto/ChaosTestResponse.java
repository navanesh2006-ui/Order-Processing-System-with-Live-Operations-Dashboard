package com.acentra.orderprocessing.dto;

public class ChaosTestResponse {
    private String testId;
    private int totalOrdersSubmitted;
    private String status;
    private long durationMs;

    public ChaosTestResponse() {}

    public ChaosTestResponse(String testId, int totalOrdersSubmitted, String status, long durationMs) {
        this.testId = testId;
        this.totalOrdersSubmitted = totalOrdersSubmitted;
        this.status = status;
        this.durationMs = durationMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String testId;
        private int totalOrdersSubmitted;
        private String status;
        private long durationMs;

        public Builder testId(String testId) { this.testId = testId; return this; }
        public Builder totalOrdersSubmitted(int totalOrdersSubmitted) { this.totalOrdersSubmitted = totalOrdersSubmitted; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public ChaosTestResponse build() {
            return new ChaosTestResponse(testId, totalOrdersSubmitted, status, durationMs);
        }
    }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public int getTotalOrdersSubmitted() { return totalOrdersSubmitted; }
    public void setTotalOrdersSubmitted(int totalOrdersSubmitted) { this.totalOrdersSubmitted = totalOrdersSubmitted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
