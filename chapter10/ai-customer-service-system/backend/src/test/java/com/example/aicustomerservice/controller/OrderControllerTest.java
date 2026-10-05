package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import com.example.aicustomerservice.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.test.context.TestPropertySource(properties="support.auth.enabled=false")
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void shouldSearchOrdersByCustomer() throws Exception {
        when(orderService.search(isNull(), eq("C10001"), isNull()))
            .thenReturn(List.of(order("SO202606210001", "C10001", OrderStatus.PAID_PICKING)));

        mockMvc.perform(get("/api/support/orders")
                .param("customerId", "C10001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerId").value("C10001"))
            .andExpect(jsonPath("$[0].orderNo").exists());
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        CustomerOrder closed = order("SO202606210003", "C10003", OrderStatus.CLOSED);
        when(orderService.updateStatus("SO202606210003", OrderStatus.CLOSED)).thenReturn(closed);
        when(orderService.get("SO202606210003")).thenReturn(closed);

        mockMvc.perform(post("/api/support/orders/SO202606210003/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "CLOSED",
                      "operator": "online-operator"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNo").value("SO202606210003"))
            .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/support/orders/SO202606210003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    private CustomerOrder order(String orderNo, String customerId, OrderStatus status) {
        Instant now = Instant.parse("2026-06-22T00:00:00Z");
        return new CustomerOrder(
            orderNo,
            customerId,
            "测试客户",
            status,
            new BigDecimal("89.00"),
            "日用品组合 x 1",
            "MINI_PROGRAM",
            "2026-06-18 12:00:00",
            "上海市浦东新区 ***",
            "LOW",
            now,
            now
        );
    }
}
