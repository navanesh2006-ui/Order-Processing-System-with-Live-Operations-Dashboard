package com.acentra.orderprocessing.dto;

import java.time.Instant;

public class WsMessage {
    private String type;
    private Instant timestamp;
    private Object payload;

    public WsMessage() {
        this.timestamp = Instant.now();
    }

    public WsMessage(String type, Instant timestamp, Object payload) {
        this.type = type;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.payload = payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private Instant timestamp = Instant.now();
        private Object payload;

        public Builder type(String type) { this.type = type; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder payload(Object payload) { this.payload = payload; return this; }
        public WsMessage build() { return new WsMessage(type, timestamp, payload); }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}
