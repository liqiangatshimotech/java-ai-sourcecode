package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.OrderDao;
import com.example.aicustomerservice.dao.OrderQuery;
import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import java.time.Clock;
import java.util.List;

public class OrderService {

    private final OrderDao orderDao;
    private final Clock clock;

    public OrderService(OrderDao orderDao, Clock clock) {
        this.orderDao = orderDao;
        this.clock = clock;
    }

    public List<CustomerOrder> search(OrderStatus status, String customerId, String keyword) {
        return orderDao.search(new OrderQuery(status, customerId, keyword));
    }

    public CustomerOrder get(String orderNo) {
        return orderDao.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在：" + orderNo));
    }

    public CustomerOrder updateStatus(String orderNo, OrderStatus status) {
        CustomerOrder order = get(orderNo);
        return orderDao.save(order.updateStatus(status, clock.instant()));
    }
}
