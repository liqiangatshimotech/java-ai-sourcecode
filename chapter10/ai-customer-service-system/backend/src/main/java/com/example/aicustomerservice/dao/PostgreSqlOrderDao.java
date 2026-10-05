package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL 订单DAO。
 *
 * 订单中心、客服接待里的订单工具都通过 OrderDao 读取数据。
 * 这样运营在订单中心维护状态后，AI Tool 查询到的也是同一份数据库状态。
 */
public class PostgreSqlOrderDao implements OrderDao {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlOrderDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CustomerOrder> search(OrderQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM cs_order WHERE 1 = 1");

        if (query.status() != null) {
            sql.append(" AND status = ?");
            params.add(query.status().name());
        }
        if (query.customerId() != null && !query.customerId().isBlank()) {
            sql.append(" AND customer_id = ?");
            params.add(query.customerId().trim());
        }
        if (query.keyword() != null && !query.keyword().isBlank()) {
            sql.append("""
                 AND (
                    order_no LIKE ?
                    OR customer_name LIKE ?
                    OR product_summary LIKE ?
                 )
                """);
            String keyword = "%" + query.keyword().trim() + "%";
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        sql.append(" ORDER BY updated_at DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapOrder(rs), params.toArray());
    }

    @Override
    public Optional<CustomerOrder> findByOrderNo(String orderNo) {
        List<CustomerOrder> orders = jdbcTemplate.query(
            "SELECT * FROM cs_order WHERE order_no = ?",
            (rs, rowNum) -> mapOrder(rs),
            orderNo
        );
        return orders.stream().findFirst();
    }

    @Override
    public CustomerOrder save(CustomerOrder order) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_order (
                    order_no, customer_id, customer_name, status, amount, product_summary,
                    channel, payment_time, receiver_masked, risk_level, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (order_no) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    customer_name = EXCLUDED.customer_name,
                    status = EXCLUDED.status,
                    amount = EXCLUDED.amount,
                    product_summary = EXCLUDED.product_summary,
                    channel = EXCLUDED.channel,
                    payment_time = EXCLUDED.payment_time,
                    receiver_masked = EXCLUDED.receiver_masked,
                    risk_level = EXCLUDED.risk_level,
                    updated_at = EXCLUDED.updated_at
                """,
            order.orderNo(),
            order.customerId(),
            order.customerName(),
            order.status().name(),
            order.amount(),
            order.productSummary(),
            order.channel(),
            order.paymentTime(),
            order.receiverMasked(),
            order.riskLevel(),
            timestamp(order.createdAt()),
            timestamp(order.updatedAt())
        );
        return order;
    }

    private CustomerOrder mapOrder(ResultSet rs) throws SQLException {
        return new CustomerOrder(
            rs.getString("order_no"),
            rs.getString("customer_id"),
            rs.getString("customer_name"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount"),
            rs.getString("product_summary"),
            rs.getString("channel"),
            rs.getString("payment_time"),
            rs.getString("receiver_masked"),
            rs.getString("risk_level"),
            instant(rs, "created_at"),
            instant(rs, "updated_at")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
