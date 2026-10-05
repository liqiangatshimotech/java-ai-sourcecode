package com.example.enterpriseopsagent.incident;

/**
 * 排障事件编号生成器。
 *
 * 单独抽出来是为了让测试可以使用固定编号，生产运行时再使用日期和随机后缀。
 */
public interface IncidentIdGenerator {
    String nextIncidentId();
}
