package com.example.texttosqlagent.schema;

import java.util.Set;

/**
 * 业务授权范围与数据库发现分开维护：发现一张表不等于授权查询它。
 * 本项目只对五张电商表开放；生产系统应由可信身份映射到授权视图/表。
 */
public final class BusinessTablePolicy {
    public static final Set<String> NAMES = Set.of("CUSTOMERS", "PRODUCTS", "ORDERS", "ORDER_ITEMS", "PAYMENTS");
    private BusinessTablePolicy() { }
}
