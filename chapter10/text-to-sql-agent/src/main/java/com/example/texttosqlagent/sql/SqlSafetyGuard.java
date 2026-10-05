package com.example.texttosqlagent.sql;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

/**
 * 面向本项目 H2 / PUBLIC schema 的 SQL AST 护栏。
 * 解析器负责区分代码、字符串与注释；业务代码只放行明确检查过的语法节点。
 * 解析成功并不等于安全：表、函数、CTE 作用域、每个子查询仍然需要授权检查。
 * 数据库 SELECT 账号是另一条独立边界，不能用本类代替数据库权限。
 */
public class SqlSafetyGuard {
    private static final Set<String> SAFE_FUNCTIONS = Set.of(
        "COUNT", "SUM", "AVG", "MIN", "MAX", "ROUND", "ABS", "COALESCE", "NULLIF",
        "LOWER", "UPPER", "LENGTH", "CONCAT");
    private static final Set<String> SAFE_BINARY = Set.of(
        "Addition", "Subtraction", "Multiplication", "Division", "Modulo", "Concat",
        "AndExpression", "OrExpression", "EqualsTo", "NotEqualsTo", "GreaterThan",
        "GreaterThanEquals", "MinorThan", "MinorThanEquals", "LikeExpression");
    private final int maxRows;
    private final Set<String> allowedTables;

    public SqlSafetyGuard(int maxRows, Set<String> allowedTables) {
        if (maxRows < 1 || maxRows > 10000) {
            throw new IllegalArgumentException("maxRows 必须在 1 到 10000 之间");
        }
        this.maxRows = maxRows;
        // 本项目建表时不使用 quoted identifier，H2 会把真实表名保存为大写。
        this.allowedTables = allowedTables.stream()
            .map(name -> name.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public SqlGuardResult check(String sql) {
        if (sql == null || sql.isBlank()) return rejected("SQL 为空");
        if (sql.length() > 16000) return rejected("SQL 超过 16000 字符预算");
        try {
            // 一次解析整个输入，不能只 parse 第一条；分号出现在字符串里不会误判。
            var statements = CCJSqlParserUtil.parseStatements(sql,
                parser -> parser.withTimeOut(1000));
            if (statements.size() != 1) return rejected("只允许执行一条 SQL 语句");
            var statement = statements.get(0);
            if (!(statement instanceof Select select)) {
                return rejected("只允许 SELECT 查询，拒绝 "
                    + statement.getClass().getSimpleName().toUpperCase(Locale.ROOT));
            }
            inspectSelect(select, Set.of(), 0);
            // 留一行探测位，执行器仍只返回 maxRows 行。内层 LIMIT 不修改。
            // 用户主动 LIMIT 5 是查询语义，应保留；系统增加的上限为 maxRows + 1。
            Limit limit = select.getLimit();
            if (limit == null) {
                limit = new Limit();
                select.setLimit(limit);
                limit.setRowCount(new LongValue(maxRows + 1L));
            } else if (((LongValue) limit.getRowCount()).getBigIntegerValue()
                .compareTo(java.math.BigInteger.valueOf(maxRows + 1L)) > 0) {
                limit.setRowCount(new LongValue(maxRows + 1L));
            }
            return new SqlGuardResult(true, select.toString(), List.of());
        } catch (IllegalArgumentException exception) {
            return rejected(exception.getMessage());
        } catch (Exception exception) {
            // 不回传包含原 SQL / 字面量的解析器异常，避免错误日志泄漏数据。
            return rejected("SQL 无法按受支持语法解析（包括 INTO OUTFILE 等扩展），已拒绝");
        }
    }

    private SqlGuardResult rejected(String reason) {
        return new SqlGuardResult(false, null, List.of(reason));
    }

    /** 每个查询拥有独立的 CTE 作用域；只在定义体通过后公开其名称。 */
    private void inspectSelect(Select select, Set<String> inheritedCtes, int depth) {
        require(depth < 24, "子查询嵌套超过预算");
        require(select.getFetch() == null && select.getLimitBy() == null
            && select.getForClause() == null && select.getIsolation() == null,
            "不支持 FETCH、LIMIT BY 或锁定/隔离扩展");
        inspectLimit(select.getLimit());
        if (select.getOffset() != null) nonnegative(select.getOffset().getOffset());
        Set<String> scope = new HashSet<>(inheritedCtes);
        if (select.getWithItemsList() != null) {
            Set<String> localNames = new HashSet<>();
            for (WithItem item : select.getWithItemsList()) {
                require(!item.isRecursive(), "不支持递归 CTE");
                String name = identifier(item.getAlias().getName());
                require(localNames.add(name), "CTE 名称重复");
                // 自引用、前向引用不会提前进入 scope，因此无法冒充已授权表。
                inspectSelect(item.getSelect(), scope, depth + 1);
                scope.add(name);
            }
        }
        inspectOrder(select.getOrderByElements(), scope, depth);
        if (select instanceof ParenthesedSelect parenthesed) {
            inspectSelect(parenthesed.getSelect(), scope, depth + 1);
        } else if (select instanceof SetOperationList union) {
            for (Select child : union.getSelects()) inspectSelect(child, scope, depth + 1);
        } else if (select instanceof PlainSelect plain) {
            require(plain.getIntoTables() == null && plain.getIntoTempTable() == null,
                "禁止 SELECT INTO / INTO OUTFILE");
            require(plain.getForMode() == null && plain.getForUpdateTable() == null
                && plain.getWait() == null && !plain.isNoWait() && !plain.isSkipLocked(),
                "禁止 SELECT FOR UPDATE / SHARE 锁定语法");
            require(plain.getTop() == null && plain.getFirst() == null && plain.getSkip() == null
                && plain.getQualify() == null && plain.getOracleHierarchical() == null
                && plain.getWindowDefinitions() == null && plain.getLateralViews() == null
                && plain.getKsqlWindow() == null && !plain.isEmitChanges()
                && plain.getOracleHint() == null && plain.getForXmlPath() == null,
                "查询包含未开放的方言扩展");
            inspectFrom(plain.getFromItem(), scope, depth);
            inspectJoins(plain.getJoins(), scope, depth);
            for (SelectItem<?> item : plain.getSelectItems()) inspectExpression(item.getExpression(), scope, depth);
            inspectExpression(plain.getWhere(), scope, depth);
            inspectExpression(plain.getHaving(), scope, depth);
            if (plain.getDistinct() != null && plain.getDistinct().getOnSelectItems() != null) {
                for (SelectItem<?> item : plain.getDistinct().getOnSelectItems())
                    inspectExpression(item.getExpression(), scope, depth);
            }
            if (plain.getGroupBy() != null) {
                inspectExpression(plain.getGroupBy().getGroupByExpressionList(), scope, depth);
                require(plain.getGroupBy().getGroupingSets().isEmpty(), "暂不支持 GROUPING SETS");
            }
        } else throw new IllegalArgumentException("未开放的查询节点：" + select.getClass().getSimpleName());
    }

    private void inspectFrom(FromItem item, Set<String> scope, int depth) {
        if (item == null) return;
        require(item.getPivot() == null && item.getUnPivot() == null, "暂不支持 PIVOT");
        if (item instanceof Table table) {
            String name = identifier(table.getName());
            boolean unqualified = table.getNameParts().size() == 1;
            if (unqualified && scope.contains(name)) return;
            // schema 不能丢掉：private.orders 与 PUBLIC.orders 是两个不同的资源。
            require(table.getNameParts().size() <= 2
                && (unqualified || "PUBLIC".equals(identifier(table.getSchemaName())))
                && allowedTables.contains(name), "表不在白名单内：" + table.getFullyQualifiedName());
            require(table.getIndexHint() == null && table.getSqlServerHints() == null
                && table.getSampleClause() == null, "不支持表提示/采样扩展");
            // 固定物理表归属，避免连接默认 schema 改变导致授权对象漂移。
            table.setSchemaName("PUBLIC");
        } else if (item instanceof ParenthesedSelect subquery) {
            inspectSelect(subquery, scope, depth + 1);
        } else if (item instanceof ParenthesedFromItem parenthesed) {
            inspectFrom(parenthesed.getFromItem(), scope, depth + 1);
            inspectJoins(parenthesed.getJoins(), scope, depth + 1);
        } else throw new IllegalArgumentException("不允许表函数/外部数据源：" + item.getClass().getSimpleName());
    }

    private void inspectJoins(List<Join> joins, Set<String> scope, int depth) {
        if (joins == null) return;
        for (Join join : joins) {
            require(!join.isApply() && join.getJoinWindow() == null && join.getJoinHint() == null,
                "不支持 APPLY / JOIN 扩展");
            // 逗号连接也是 AST Join，和显式 JOIN 一样逐表授权。
            inspectFrom(join.getRightItem(), scope, depth);
            for (Expression on : join.getOnExpressions()) inspectExpression(on, scope, depth);
        }
    }

    /** 显式递归白名单：未知表达式直接拒绝，避免 Visitor 默认空实现悄悄漏查。 */
    private void inspectExpression(Expression expression, Set<String> scope, int depth) {
        if (expression == null) return;
        require(depth < 64, "表达式嵌套超过预算");
        int next = depth + 1;
        if (expression instanceof Column column) {
            require(column.getArrayConstructor() == null, "不支持列数组扩展");
            return;
        }
        if (expression instanceof AllColumns all) {
            require(all.getExceptColumns() == null && all.getReplaceExpressions() == null,
                "不支持星号 EXCEPT / REPLACE 扩展");
            return;
        }
        if (expression instanceof StringValue
            || expression instanceof LongValue || expression instanceof DoubleValue
            || expression instanceof NullValue || expression instanceof DateValue
            || expression instanceof TimeValue || expression instanceof TimestampValue) return;
        if (expression instanceof Select select) inspectSelect(select, scope, next);
        else if (expression instanceof BinaryExpression binary && SAFE_BINARY.contains(expression.getClass().getSimpleName())) {
            inspectExpression(binary.getLeftExpression(), scope, next);
            inspectExpression(binary.getRightExpression(), scope, next);
            if (binary instanceof LikeExpression like) inspectExpression(like.getEscape(), scope, next);
        } else if (expression instanceof ExpressionList<?> list) {
            for (Object child : list) inspectExpression((Expression) child, scope, next);
        } else if (expression instanceof Function function) {
            require(SAFE_FUNCTIONS.contains(function.getName().toUpperCase(Locale.ROOT)), "函数不在白名单内");
            require(function.getNamedParameters() == null && function.getAttribute() == null
                && function.getKeep() == null && !function.isEscaped(), "不支持函数扩展");
            inspectExpression(function.getParameters(), scope, next);
            inspectOrder(function.getOrderByElements(), scope, next);
        } else if (expression instanceof AnalyticExpression analytic) {
            require(SAFE_FUNCTIONS.contains(analytic.getName().toUpperCase(Locale.ROOT)), "窗口函数不在白名单内");
            require(analytic.getKeep() == null && analytic.getWindowElement() == null
                && analytic.getWindowName() == null, "暂不支持命名窗口或自定义窗口边界");
            inspectExpression(analytic.getExpression(), scope, next);
            inspectExpression(analytic.getOffset(), scope, next);
            inspectExpression(analytic.getDefaultValue(), scope, next);
            inspectExpression(analytic.getFilterExpression(), scope, next);
            inspectExpression(analytic.getPartitionExpressionList(), scope, next);
            inspectOrder(analytic.getOrderByElements(), scope, next);
            inspectOrder(analytic.getFuncOrderBy(), scope, next);
        } else if (expression instanceof Parenthesis parenthesis) inspectExpression(parenthesis.getExpression(), scope, next);
        else if (expression instanceof SignedExpression signed) inspectExpression(signed.getExpression(), scope, next);
        else if (expression instanceof NotExpression not) inspectExpression(not.getExpression(), scope, next);
        else if (expression instanceof ExistsExpression exists) inspectExpression(exists.getRightExpression(), scope, next);
        else if (expression instanceof IsNullExpression isNull) inspectExpression(isNull.getLeftExpression(), scope, next);
        else if (expression instanceof InExpression in) {
            inspectExpression(in.getLeftExpression(), scope, next);
            inspectExpression(in.getRightExpression(), scope, next);
        } else if (expression instanceof Between between) {
            inspectExpression(between.getLeftExpression(), scope, next);
            inspectExpression(between.getBetweenExpressionStart(), scope, next);
            inspectExpression(between.getBetweenExpressionEnd(), scope, next);
        } else if (expression instanceof CaseExpression cases) {
            inspectExpression(cases.getSwitchExpression(), scope, next);
            for (WhenClause when : cases.getWhenClauses()) {
                inspectExpression(when.getWhenExpression(), scope, next);
                inspectExpression(when.getThenExpression(), scope, next);
            }
            inspectExpression(cases.getElseExpression(), scope, next);
        } else throw new IllegalArgumentException("未开放的表达式：" + expression.getClass().getSimpleName());
    }

    private void inspectOrder(List<OrderByElement> orders, Set<String> scope, int depth) {
        if (orders != null) for (OrderByElement order : orders) inspectExpression(order.getExpression(), scope, depth);
    }

    private void inspectLimit(Limit limit) {
        if (limit == null) return;
        require(!limit.isLimitAll() && !limit.isLimitNull() && limit.getByExpressions() == null,
            "LIMIT 只能使用非负整数字面量");
        nonnegative(limit.getRowCount());
        if (limit.getOffset() != null) nonnegative(limit.getOffset());
    }

    private void nonnegative(Expression value) {
        require(value instanceof LongValue && ((LongValue) value).getBigIntegerValue().signum() >= 0,
            "LIMIT / OFFSET 只能使用非负整数字面量");
    }

    /** 双引号标识符保留大小写；不把引号内的点误当 schema 分隔符。 */
    private String identifier(String name) {
        if (name == null) return "";
        if (name.startsWith("\"") && name.endsWith("\"")) return name.substring(1, name.length() - 1).replace("\"\"", "\"");
        require(name.matches("[A-Za-z_][A-Za-z0-9_]*"), "标识符不属于本项目 H2 方言");
        return name.toUpperCase(Locale.ROOT);
    }

    private void require(boolean condition, String reason) {
        if (!condition) throw new IllegalArgumentException(reason);
    }
}
