package com.crustcrunch.controller;

import com.crustcrunch.dto.MenuDtos.ReviewResponse;
import com.crustcrunch.dto.OrderDtos.*;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.OrderService;
import com.crustcrunch.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders (customer)")
public class OrderController {

    private final OrderService orders;
    private final ReviewService reviews;

    public OrderController(OrderService orders, ReviewService reviews) {
        this.orders = orders;
        this.reviews = reviews;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order (guest or authenticated). Requires an Idempotency-Key header.")
    public OrderResponse checkout(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                  @Valid @RequestBody CheckoutRequest req) {
        return orders.checkout(req, idempotencyKey);
    }

    @GetMapping("/orders/my")
    public PageResponse<OrderSummaryResponse> myOrders(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return orders.myOrders(CurrentUser.require().id(), page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orders.getForCaller(id);
    }

    @GetMapping("/orders/track/{orderNumber}")
    @Operation(summary = "Guest order tracking using the secret token returned at checkout")
    public OrderResponse track(@PathVariable String orderNumber, @RequestParam String token) {
        return orders.track(orderNumber, token);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req) {
        return orders.cancelByCustomer(id, req == null ? null : req.reason());
    }

    @PostMapping("/orders/{id}/reorder")
    public ReorderResponse reorder(@PathVariable Long id) {
        return orders.reorder(id);
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse review(@Valid @RequestBody CreateReviewRequest req) {
        return reviews.create(CurrentUser.require().id(), req);
    }
}
