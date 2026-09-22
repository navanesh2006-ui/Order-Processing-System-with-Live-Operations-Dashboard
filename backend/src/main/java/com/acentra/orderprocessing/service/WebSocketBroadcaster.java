package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class WebSocketBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcaster.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrder(Object orderData) {
        WsMessage msg = WsMessage.builder()
                .type("ORDER_UPDATE")
                .timestamp(Instant.now())
                .payload(orderData)
                .build();
        messagingTemplate.convertAndSend("/topic/orders", msg);
    }

    public void broadcastInventory(Object inventoryData) {
        WsMessage msg = WsMessage.builder()
                .type("INVENTORY_UPDATE")
                .timestamp(Instant.now())
                .payload(inventoryData)
                .build();
        messagingTemplate.convertAndSend("/topic/inventory", msg);
    }

    public void broadcastMetrics(Object metricsData) {
        WsMessage msg = WsMessage.builder()
                .type("METRICS_UPDATE")
                .timestamp(Instant.now())
                .payload(metricsData)
                .build();
        messagingTemplate.convertAndSend("/topic/metrics", msg);
    }

    public void broadcastDlq(Object dlqData) {
        WsMessage msg = WsMessage.builder()
                .type("DLQ_UPDATE")
                .timestamp(Instant.now())
                .payload(dlqData)
                .build();
        messagingTemplate.convertAndSend("/topic/dlq", msg);
    }

    public void broadcastAlert(String alertType, String message, Object details) {
        WsMessage msg = WsMessage.builder()
                .type(alertType)
                .timestamp(Instant.now())
                .payload(Map.of("message", message, "details", details))
                .build();
        messagingTemplate.convertAndSend("/topic/alerts", msg);
    }
}
