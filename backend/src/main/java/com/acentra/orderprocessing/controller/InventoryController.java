package com.acentra.orderprocessing.controller;

import com.acentra.orderprocessing.dto.InventoryDTO;
import com.acentra.orderprocessing.dto.RestockRequest;
import com.acentra.orderprocessing.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getAllInventories() {
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryDTO> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryForProduct(productId));
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<InventoryDTO> restockProduct(
            @PathVariable Long productId,
            @Valid @RequestBody RestockRequest request
    ) {
        return ResponseEntity.ok(inventoryService.restockProduct(productId, request.getAmount()));
    }
}
