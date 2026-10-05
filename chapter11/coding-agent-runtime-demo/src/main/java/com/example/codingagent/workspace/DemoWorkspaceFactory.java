package com.example.codingagent.workspace;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * 创建一个小型业务仓库，供 Coding Agent 演示修改。
 * 为了防止误删真实目录，清理逻辑只允许删除目录名以 coding-agent- 开头的路径。
 */
public final class DemoWorkspaceFactory {

    private DemoWorkspaceFactory() {
    }

    public static Path createFreshWorkspace(Path root) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        if (Files.exists(normalized)) {
            deleteDemoWorkspace(normalized);
        }
        Files.createDirectories(normalized.resolve("src/main/java/com/acme/billing"));
        Files.createDirectories(normalized.resolve("src/test/java/com/acme/billing"));

        Files.writeString(normalized.resolve("src/main/java/com/acme/billing/PriceCalculator.java"),
                priceCalculatorSource());
        Files.writeString(normalized.resolve("src/test/java/com/acme/billing/PriceCalculatorTest.java"),
                priceCalculatorTestSource());
        return normalized;
    }

    private static void deleteDemoWorkspace(Path root) throws IOException {
        if (!root.getFileName().toString().startsWith("coding-agent-")) {
            throw new IllegalArgumentException("Refuse to delete non-demo workspace: " + root);
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static String priceCalculatorSource() {
        return """
                package com.acme.billing;

                import java.math.BigDecimal;
                import java.math.RoundingMode;

                /**
                 * 计费服务里的价格计算器。
                 * 业务约束：会员折扣不能无限放大，否则会把订单金额打穿。
                 */
                public final class PriceCalculator {

                    private static final BigDecimal MAX_DISCOUNT = new BigDecimal("0.20");

                    public BigDecimal calculateFinalPrice(BigDecimal originalPrice, BigDecimal discountRate) {
                        if (originalPrice == null || discountRate == null) {
                            throw new IllegalArgumentException("originalPrice and discountRate must not be null");
                        }
                        if (BigDecimal.ZERO.compareTo(originalPrice) > 0) {
                            throw new IllegalArgumentException("originalPrice must not be negative");
                        }
                        if (BigDecimal.ZERO.compareTo(discountRate) > 0 || MAX_DISCOUNT.compareTo(discountRate) < 0) {
                            throw new IllegalArgumentException("discountRate must be between 0 and " + MAX_DISCOUNT);
                        }
                        BigDecimal discountAmount = originalPrice.multiply(discountRate);
                        return originalPrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                    }
                }
                """;
    }

    private static String priceCalculatorTestSource() {
        return """
                package com.acme.billing;

                import java.math.BigDecimal;

                /**
                 * 独立 Java 测试入口，由受限容器实际编译执行，失败时返回非零退出码。
                 */
                public final class PriceCalculatorTest {

                    public static void main(String[] args) {
                        new PriceCalculatorTest().thirtyPercentDiscount();
                        PriceCalculator calculator = new PriceCalculator();
                        try {
                            calculator.calculateFinalPrice(new BigDecimal("100"), new BigDecimal("0.31"));
                            throw new AssertionError("discount above max must be rejected");
                        } catch (IllegalArgumentException expected) { }
                        try {
                            calculator.calculateFinalPrice(new BigDecimal("-1"), BigDecimal.ZERO);
                            throw new AssertionError("negative price must be rejected");
                        } catch (IllegalArgumentException expected) { }
                        System.out.println("ALL_BILLING_TESTS_PASSED");
                    }

                    public void thirtyPercentDiscount() {
                        PriceCalculator calculator = new PriceCalculator();
                        BigDecimal price = calculator.calculateFinalPrice(new BigDecimal("100.00"), new BigDecimal("0.30"));
                        if (!new BigDecimal("70.00").equals(price)) {
                            throw new AssertionError("30% discount should leave 70.00");
                        }
                    }
                }
                """;
    }
}
