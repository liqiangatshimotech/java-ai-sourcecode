package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.CustomerProfile;
import com.example.aicustomerservice.entity.CustomerTier;
import java.util.Map;
import java.util.Optional;

/**
 * 演示用客户DAO。
 *
 * 真实企业项目中，这一层一般会通过 RPC 或数据库访问会员中心。
 * 课程里先用固定数据，便于专注 AI 客服链路。
 */
public class InMemoryCustomerDao implements CustomerDao {

    private final Map<String, CustomerProfile> customers = Map.of(
        "C10001", new CustomerProfile("C10001", "北京朝阳门店", CustomerTier.ENTERPRISE, "138****0001"),
        "C10002", new CustomerProfile("C10002", "华东 VIP 客户", CustomerTier.VIP, "139****0002"),
        "C10003", new CustomerProfile("C10003", "普通注册用户", CustomerTier.NORMAL, "137****0003")
    );

    @Override
    public Optional<CustomerProfile> findByCustomerId(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
