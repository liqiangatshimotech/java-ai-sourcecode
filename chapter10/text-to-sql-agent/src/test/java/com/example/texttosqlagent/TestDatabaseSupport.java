package com.example.texttosqlagent;

import javax.sql.DataSource;
import com.example.texttosqlagent.config.ReadOnlyDatabaseFactory;

/** 所有集成测试与应用共用同一条 SELECT 账号初始化路径。 */
public final class TestDatabaseSupport {
    private TestDatabaseSupport() { }
    public static DataSource newInitializedDataSource() {
        return ReadOnlyDatabaseFactory.createDemo();
    }
}
