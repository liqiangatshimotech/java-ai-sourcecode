package com.example.langchain4jtool.dto;

public record InventoryStatus(
    String sku,
    String productName,
    int availableQuantity,
    String warehouse,
    boolean canSell
) {
}
