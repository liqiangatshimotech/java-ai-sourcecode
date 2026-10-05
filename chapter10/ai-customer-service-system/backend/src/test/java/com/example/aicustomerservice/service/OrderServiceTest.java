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
}
