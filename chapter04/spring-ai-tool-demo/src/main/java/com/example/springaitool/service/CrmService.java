package com.example.springaitool.service;

import com.example.springaitool.dto.CustomerProfile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CrmService {

    private final Map<String, CustomerProfile> customers = Map.of(
        "C-2001", new CustomerProfile("C-2001", "上海星河科技", "VIP", "张明", "客户关注发票和售后响应速度"),
        "C-2002", new CustomerProfile("C-2002", "杭州青木电商", "STANDARD", "李娜", "最近一次沟通是升级采购额度"),
        "C-2003", new CustomerProfile("C-2003", "成都云帆教育", "VIP", "王磊", "退款申请需要人工确认合同条款")
    );

    public CustomerProfile findByCustomerId(String customerId) {
        String normalized = requireId(customerId);
        CustomerProfile profile = this.customers.get(normalized);
        if (profile == null) {
            throw new IllegalArgumentException("客户不存在: " + normalized);
        }
        return profile;
    }

    private static String requireId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId 不能为空");
        }
        return customerId.trim().toUpperCase();
    }
}
