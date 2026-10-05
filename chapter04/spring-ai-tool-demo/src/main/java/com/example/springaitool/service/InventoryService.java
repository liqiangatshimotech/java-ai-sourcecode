package com.example.springaitool.service;

import com.example.springaitool.dto.InventoryStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InventoryService {

    private final Map<String, InventoryStatus> inventory = Map.of(
        "SKU-KEYBOARD", new InventoryStatus("SKU-KEYBOARD", "机械键盘", 42, "上海一号仓", true),
        "SKU-MOUSE", new InventoryStatus("SKU-MOUSE", "无线鼠标", 0, "深圳前置仓", false),
        "SKU-MONITOR", new InventoryStatus("SKU-MONITOR", "27 寸显示器", 8, "北京一号仓", true)
    );

    public InventoryStatus findBySku(String sku) {
        String normalized = requireSku(sku);
        InventoryStatus status = this.inventory.get(normalized);
        if (status == null) {
            throw new IllegalArgumentException("SKU 不存在: " + normalized);
        }
        return status;
    }

    private static String requireSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku 不能为空");
        }
        return sku.trim().toUpperCase();
    }
}
