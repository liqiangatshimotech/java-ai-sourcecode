package com.example.environmentcheck;

/** PASS 仅代表当前检查项通过；NOT_TESTED 绝不能汇总成模型已经可用。 */
public record CheckResult(String item, Status status, String detail) {
    public enum Status { PASS, FAIL, NOT_TESTED }
    public static CheckResult pass(String item, String detail) { return new CheckResult(item, Status.PASS, detail); }
    public static CheckResult fail(String item, String detail) { return new CheckResult(item, Status.FAIL, detail); }
}
