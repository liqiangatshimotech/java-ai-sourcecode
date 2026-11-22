package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    @Test
    void shouldSearchAndUpdateOrderStatus() {
        Clock clock = Clock.systemUTC();
        OrderService orderService = new OrderService(new InMemoryOrderDao(clock), clock);

        var enterpriseOrders = orderService.search(null, "C10001", null);
        var pickingOrders = orderService.search(OrderStatus.PAID_PICKING, null, null);
        var updated = orderService.updateStatus("SO202606210001", OrderStatus.REFUNDING);

        assertThat(enterpriseOrders).hasSize(2);
        assertThat(pickingOrders).extracting(CustomerOrder::orderNo).contains("SO202606210001");
        assertThat(updated.status()).isEqualTo(OrderStatus.REFUNDING);
        assertThat(orderService.get("SO202606210001").status()).isEqualTo(OrderStatus.REFUNDING);
    }

    @Test
    void shouldRejectMissingOrder() {
        Clock clock = Clock.systemUTC();
        OrderService orderService = new OrderService(new InMemoryOrderDao(clock), clock);

        assertThatThrownBy(() -> orderService.get("SO-NOT-FOUND"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("订单不存在");
    }

    @Test
    void shouldAllowLegalStatusTransitionsStepByStep() {
        Clock clock = Clock.systemUTC();
        OrderService orderService = new OrderService(new InMemoryOrderDao(clock), clock);

        // SO202606210001 的种子状态是 PAID_PICKING，按迁移表一步步走到 CLOSED。
        assertThat(orderService.updateStatus("SO202606210001", OrderStatus.SHIPPED).status())
            .isEqualTo(OrderStatus.SHIPPED);
        assertThat(orderService.updateStatus("SO202606210001", OrderStatus.DELIVERED).status())
            .isEqualTo(OrderStatus.DELIVERED);
        assertThat(orderService.updateStatus("SO202606210001", OrderStatus.CLOSED).status())
            .isEqualTo(OrderStatus.CLOSED);
    }

    @Test
    void shouldRejectIllegalStatusTransitionAndKeepCurrentStatus() {
        Clock clock = Clock.systemUTC();
        OrderService orderService = new OrderService(new InMemoryOrderDao(clock), clock);

        // SO202606210004 的种子状态是 REFUNDING：退款中的订单不能回到已发货。
        assertThatThrownBy(() -> orderService.updateStatus("SO202606210004", OrderStatus.SHIPPED))
            .isInstanceOfSatisfying(BusinessException.class, exception -> {
                assertThat(exception.code()).isEqualTo("ORDER_STATUS_TRANSITION_INVALID");
                assertThat(exception.getMessage()).contains("REFUNDING").contains("SHIPPED");
            });
        // 目标状态等于当前状态同样拒绝。
        assertThatThrownBy(() -> orderService.updateStatus("SO202606210004", OrderStatus.REFUNDING))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("ORDER_STATUS_TRANSITION_INVALID"));

        // 关闭后的订单是终态，不能再改回拣货中；被拒绝的请求不会改动已保存的状态。
        orderService.updateStatus("SO202606210004", OrderStatus.CLOSED);
        assertThatThrownBy(() -> orderService.updateStatus("SO202606210004", OrderStatus.PAID_PICKING))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("ORDER_STATUS_TRANSITION_INVALID"));
        assertThat(orderService.get("SO202606210004").status()).isEqualTo(OrderStatus.CLOSED);
    }
}
