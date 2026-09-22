package com.acentra.orderprocessing.controller;

import com.acentra.orderprocessing.dto.DeadLetterOrderDTO;
import com.acentra.orderprocessing.dto.OrderResponse;
import com.acentra.orderprocessing.service.DeadLetterQueueService;
import com.acentra.orderprocessing.service.OrderProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dlq")
public class DeadLetterQueueController {

    private final DeadLetterQueueService dlqService;
    private final OrderProcessingService orderProcessingService;

    public DeadLetterQueueController(DeadLetterQueueService dlqService, OrderProcessingService orderProcessingService) {
        this.dlqService = dlqService;
        this.orderProcessingService = orderProcessingService;
    }

    @GetMapping
    public ResponseEntity<List<DeadLetterOrderDTO>> getDlqOrders(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(dlqService.getRecentDlqOrders(limit));
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<OrderResponse> replayDlqOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderProcessingService.replayDlqOrder(id));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<DeadLetterOrderDTO> discardDlqOrder(@PathVariable Long id) {
        return ResponseEntity.ok(dlqService.discard(id));
    }
}
