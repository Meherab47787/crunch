package com.crustcrunch.controller;

import com.crustcrunch.dto.CartDtos.QuoteRequest;
import com.crustcrunch.dto.CartDtos.QuoteResponse;
import com.crustcrunch.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @PostMapping("/quote")
    @Operation(summary = "Validate a cart and compute authoritative prices, discounts, coupon, delivery fee and tax")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest req) {
        return cart.quote(req);
    }
}
