package com.acentra.orderprocessing.controller;

import com.acentra.orderprocessing.dto.ThreadPoolStatsDTO;
import com.acentra.orderprocessing.service.OrderQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final OrderQueueService orderQueueService;

    public MetricsController(OrderQueueService orderQueueService) {
        this.orderQueueService = orderQueueService;
    }

    @GetMapping("/threadpool")
    public ResponseEntity<ThreadPoolStatsDTO> getThreadPoolStats() {
        return ResponseEntity.ok(orderQueueService.getStats());
    }
}
