package com.crustcrunch.controller;

import com.crustcrunch.dto.OrderDtos.OrderResponse;
import com.crustcrunch.dto.OrderDtos.StatusUpdateRequest;
import com.crustcrunch.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@Tag(name = "Kitchen display")
public class KitchenController {

    private final OrderService orders;

    public KitchenController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/orders")
    public List<OrderResponse> queue() {
        return orders.kitchenQueue();
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orders.getForCaller(id);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return orders.updateStatus(id, req.status(), req.note());
    }
}
