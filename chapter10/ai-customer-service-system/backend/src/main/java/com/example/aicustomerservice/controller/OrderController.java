package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.dto.OrderResponse;
import com.example.aicustomerservice.dto.OrderStatusUpdateRequest;
import com.example.aicustomerservice.entity.OrderStatus;
import com.example.aicustomerservice.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> search(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String keyword
    ) {
        return orderService.search(status, customerId, keyword)
            .stream()
            .map(OrderResponse::from)
            .toList();
    }

    @GetMapping("/{orderNo}")
    public OrderResponse get(@PathVariable String orderNo) {
        return OrderResponse.from(orderService.get(orderNo));
    }

    @PostMapping("/{orderNo}/status")
    public OrderResponse updateStatus(
        @PathVariable String orderNo,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return OrderResponse.from(orderService.updateStatus(orderNo, request.status()));
    }
}
