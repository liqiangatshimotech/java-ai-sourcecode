package com.example.springaitool.dto;

public record InventoryStatus(
    String sku,
    String productName,
    int availableQuantity,
    String warehouse,
    boolean canSell
) {
}
