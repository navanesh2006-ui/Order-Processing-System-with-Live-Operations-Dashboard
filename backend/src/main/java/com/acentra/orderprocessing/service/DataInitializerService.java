package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.model.Inventory;
import com.acentra.orderprocessing.model.Product;
import com.acentra.orderprocessing.repository.InventoryRepository;
import com.acentra.orderprocessing.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DataInitializerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerService.class);

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public DataInitializerService(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Products already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Seeding initial products and inventories with deliberate scarce & low stock configurations...");

        createProductAndInventory(
                "NV-RTX5090",
                "NVIDIA RTX 5090 Blackwell",
                "Flagship next-gen GPU (Deliberately scarce for concurrency demo)",
                new BigDecimal("1999.00"),
                3 // SCARCE: 3 units
        );

        createProductAndInventory(
                "CD-QUANTUM-H1",
                "CyberDeck Quantum H1",
                "Portable cybernetic computing terminal with sub-atomic co-processor",
                new BigDecimal("3499.00"),
                8 // LOW: 8 units
        );

        createProductAndInventory(
                "TPU-V5E",
                "Neural Accelerator TPU v5e",
                "Dedicated machine learning hardware accelerator module",
                new BigDecimal("899.00"),
                25 // MEDIUM: 25 units
        );

        createProductAndInventory(
                "HL-ENT-V2",
                "HoloLens Enterprise Visor",
                "Spatial computing mixed reality headset for enterprise telemetry",
                new BigDecimal("1250.00"),
                100 // HIGH: 100 units
        );

        createProductAndInventory(
                "UW-OLED-49",
                "UltraWide Curved OLED 49\"",
                "240Hz dual-QHD panoramic curved operations display",
                new BigDecimal("1199.00"),
                150 // HIGH: 150 units
        );

        log.info("Database seeding complete. Products and inventories ready for live operations!");
    }

    private void createProductAndInventory(String sku, String name, String desc, BigDecimal price, int initialStock) {
        Product product = Product.builder()
                .sku(sku)
                .name(name)
                .description(desc)
                .price(price)
                .build();
        Product savedProduct = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(initialStock)
                .version(0L)
                .build();
        inventoryRepository.save(inventory);
    }
}
