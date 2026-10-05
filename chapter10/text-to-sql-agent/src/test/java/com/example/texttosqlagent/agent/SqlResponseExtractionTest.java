package com.example.texttosqlagent.agent;

import static org.junit.jupiter.api.Assertions.*;
import com.example.texttosqlagent.sql.SqlSafetyGuard;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 模型响应提取必须保留完整输入，让护栏看得到第二条语句。 */
class SqlResponseExtractionTest {
    @Test
    void keepsMultipleStatementsForGuardToReject() {
        String raw = "```sql\nSELECT * FROM orders; DELETE FROM orders\n```";
        String sql = LangChain4jSqlGenerationModel.extractSql(raw);
        assertTrue(sql.contains("DELETE"));
        assertFalse(new SqlSafetyGuard(200, Set.of("orders")).check(sql).passed());
    }
    @Test
    void preservesSemicolonsAndCommentMarkersInStrings() {
        String sql = "SELECT ';--/*text*/' FROM orders";
        assertEquals(sql, LangChain4jSqlGenerationModel.extractSql(sql));
    }
}
