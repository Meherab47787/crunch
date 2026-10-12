package com.crustcrunch.controller.admin;

import com.crustcrunch.dto.AdminDtos.CouponRequest;
import com.crustcrunch.dto.AdminDtos.CouponResponse;
import com.crustcrunch.dto.AdminDtos.PromotionRequest;
import com.crustcrunch.dto.MenuDtos.PromotionResponse;
import com.crustcrunch.service.PromotionAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin: promotions & coupons")
public class AdminPromotionController {

    private final PromotionAdminService service;

    public AdminPromotionController(PromotionAdminService service) {
        this.service = service;
    }

    @GetMapping("/coupons")
    public List<CouponResponse> coupons() {
        return service.listCoupons();
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(@Valid @RequestBody CouponRequest req) {
        return service.createCoupon(req);
    }

    @PutMapping("/coupons/{id}")
    public CouponResponse updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return service.updateCoupon(id, req);
    }

    @DeleteMapping("/coupons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoupon(@PathVariable Long id) {
        service.deleteCoupon(id);
    }

    @GetMapping("/promotions")
    public List<PromotionResponse> promotions() {
        return service.listPromotions();
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse createPromotion(@Valid @RequestBody PromotionRequest req) {
        return service.createPromotion(req);
    }

    @PutMapping("/promotions/{id}")
    public PromotionResponse updatePromotion(@PathVariable Long id, @Valid @RequestBody PromotionRequest req) {
        return service.updatePromotion(id, req);
    }

    @DeleteMapping("/promotions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromotion(@PathVariable Long id) {
        service.deletePromotion(id);
    }
}
