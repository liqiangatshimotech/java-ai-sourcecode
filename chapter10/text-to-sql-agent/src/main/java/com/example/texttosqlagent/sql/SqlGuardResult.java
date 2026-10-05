package com.example.texttosqlagent.sql;

import java.util.List;

/**
 * SQL 护栏校验结果。
 *
 * @param passed       是否通过全部护栏规则
 * @param sanitizedSql 通过校验后最终允许执行的 SQL（可能被自动追加/收紧 LIMIT）；
 *                     未通过时为 null，上层不允许再执行
 * @param violations   违规明细，逐条说明踩了哪条护栏，用于审计和回复用户
 */
public record SqlGuardResult(boolean passed, String sanitizedSql, List<String> violations) {
}
