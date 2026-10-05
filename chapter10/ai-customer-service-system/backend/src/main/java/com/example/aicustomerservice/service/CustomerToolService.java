package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.LogisticsSnapshot;
import com.example.aicustomerservice.entity.OrderSnapshot;
import com.example.aicustomerservice.entity.RefundAssessment;
import java.util.Optional;

public interface CustomerToolService {

    Optional<OrderSnapshot> findOrder(String customerId, String orderNo);

    Optional<LogisticsSnapshot> findLogistics(String orderNo);

    RefundAssessment assessRefund(String customerId, String orderNo);
}
