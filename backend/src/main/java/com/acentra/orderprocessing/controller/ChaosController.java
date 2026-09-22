package com.acentra.orderprocessing.controller;

import com.acentra.orderprocessing.dto.ChaosTestRequest;
import com.acentra.orderprocessing.dto.ChaosTestResponse;
import com.acentra.orderprocessing.service.ChaosTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosTestService chaosTestService;

    public ChaosController(ChaosTestService chaosTestService) {
        this.chaosTestService = chaosTestService;
    }

    @PostMapping("/load-test")
    public ResponseEntity<ChaosTestResponse> runLoadTest(@Valid @RequestBody(required = false) ChaosTestRequest request) {
        if (request == null) {
            request = ChaosTestRequest.builder().build();
        }
        return ResponseEntity.ok(chaosTestService.runChaosLoadTest(request));
    }

    @PostMapping("/idempotency-demo")
    public ResponseEntity<Map<String, Object>> runIdempotencyDemo(
            @RequestParam(required = false, defaultValue = "1") Long productId
    ) {
        return ResponseEntity.ok(chaosTestService.runIdempotencyDemo(productId));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetAllData() {
        chaosTestService.resetAllData();
        return ResponseEntity.ok(Map.of("message", "System orders and DLQ reset successfully. Inventory restored."));
    }
}
