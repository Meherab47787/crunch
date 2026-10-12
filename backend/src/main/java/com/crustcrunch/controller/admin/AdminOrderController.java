package com.crustcrunch.controller.admin;

import com.crustcrunch.dto.DashboardDtos.DashboardStats;
import com.crustcrunch.dto.OrderDtos.OrderResponse;
import com.crustcrunch.dto.OrderDtos.OrderSummaryResponse;
import com.crustcrunch.dto.OrderDtos.StatusUpdateRequest;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.enums.FulfillmentType;
import com.crustcrunch.enums.OrderStatus;
import com.crustcrunch.enums.PaymentMethod;
import com.crustcrunch.service.DashboardService;
import com.crustcrunch.service.OrderService;
import com.crustcrunch.service.OrderService.AdminOrderFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin: orders & dashboard")
public class AdminOrderController {

    private final OrderService orders;
    private final DashboardService dashboard;

    public AdminOrderController(OrderService orders, DashboardService dashboard) {
        this.orders = orders;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return dashboard.stats();
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) FulfillmentType fulfillmentType,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orders.search(new AdminOrderFilter(q, status, fulfillmentType, paymentMethod, category, from, to), page, size);
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
