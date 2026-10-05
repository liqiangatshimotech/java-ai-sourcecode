package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;

public interface OrderDao {

    List<CustomerOrder> search(OrderQuery query);

    Optional<CustomerOrder> findByOrderNo(String orderNo);

    CustomerOrder save(CustomerOrder order);
}
