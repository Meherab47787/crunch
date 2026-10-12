package com.crustcrunch.service;

import com.crustcrunch.dto.AdminDtos.CouponRequest;
import com.crustcrunch.dto.AdminDtos.CouponResponse;
import com.crustcrunch.dto.AdminDtos.PromotionRequest;
import com.crustcrunch.dto.MenuDtos.PromotionResponse;
import com.crustcrunch.entity.Coupon;
import com.crustcrunch.entity.Promotion;
import com.crustcrunch.enums.DiscountType;
import com.crustcrunch.exception.BadRequestException;
import com.crustcrunch.exception.ConflictException;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.AdminMapper;
import com.crustcrunch.mapper.MenuMapper;
import com.crustcrunch.repository.CategoryRepository;
import com.crustcrunch.repository.CouponRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class PromotionAdminService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CouponRepository coupons;
    private final PromotionRepository promotions;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final AdminMapper adminMapper;
    private final MenuMapper menuMapper;
    private final Clock clock;

    public PromotionAdminService(CouponRepository coupons, PromotionRepository promotions,
                                 CategoryRepository categories, ProductRepository products, AdminMapper adminMapper,
                                 MenuMapper menuMapper, Clock clock) {
        this.coupons = coupons;
        this.promotions = promotions;
        this.categories = categories;
        this.products = products;
        this.adminMapper = adminMapper;
        this.menuMapper = menuMapper;
        this.clock = clock;
    }

    // ---------------- coupons

    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons() {
        return coupons.findAllByOrderByCreatedAtDesc().stream().map(adminMapper::toResponse).toList();
    }

    public CouponResponse createCoupon(CouponRequest req) {
        if (coupons.existsByCodeIgnoreCase(req.code().trim())) {
            throw new ConflictException("Coupon code already exists");
        }
        Coupon c = new Coupon();
        applyCoupon(c, req);
        return adminMapper.toResponse(coupons.save(c));
    }

    public CouponResponse updateCoupon(Long id, CouponRequest req) {
        Coupon c = coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found"));
        if (coupons.existsByCodeIgnoreCaseAndIdNot(req.code().trim(), id)) {
            throw new ConflictException("Coupon code already exists");
        }
        applyCoupon(c, req);
        return adminMapper.toResponse(c);
    }

    public void deleteCoupon(Long id) {
        coupons.delete(coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found")));
    }

    private void applyCoupon(Coupon c, CouponRequest req) {
        validatePercent(req.discountType(), req.discountValue());
        c.setCode(req.code().trim().toUpperCase());
        c.setDescription(AuthService.blankToNull(req.description()));
        c.setDiscountType(req.discountType());
        c.setDiscountValue(PricingService.money(req.discountValue()));
        c.setMinOrderAmount(PricingService.money(req.minOrderAmount()));
        c.setMaxDiscountAmount(req.maxDiscountAmount() == null ? null : PricingService.money(req.maxDiscountAmount()));
        c.setCategory(req.categoryId() == null ? null : categories.findById(req.categoryId())
                .orElseThrow(() -> new BadRequestException("Unknown category")));
        c.setStartsAt(req.startsAt());
        c.setExpiresAt(req.expiresAt());
        c.setMaxRedemptions(req.maxRedemptions());
        c.setPerUserLimit(req.perUserLimit());
        c.setActive(req.active());
    }

    // ---------------- promotions

    @Transactional(readOnly = true)
    public List<PromotionResponse> listPromotions() {
        var now = clock.instant();
        return promotions.findAllByOrderByCreatedAtDesc().stream().map(p -> menuMapper.toResponse(p, now)).toList();
    }

    public PromotionResponse createPromotion(PromotionRequest req) {
        Promotion p = new Promotion();
        applyPromotion(p, req);
        return menuMapper.toResponse(promotions.save(p), clock.instant());
    }

    public PromotionResponse updatePromotion(Long id, PromotionRequest req) {
        Promotion p = promotions.findById(id).orElseThrow(() -> new NotFoundException("Promotion not found"));
        applyPromotion(p, req);
        return menuMapper.toResponse(p, clock.instant());
    }

    public void deletePromotion(Long id) {
        promotions.delete(promotions.findById(id).orElseThrow(() -> new NotFoundException("Promotion not found")));
    }

    private void applyPromotion(Promotion p, PromotionRequest req) {
        validatePercent(req.discountType(), req.discountValue());
        if (req.categoryId() != null && req.productId() != null) {
            throw new BadRequestException("Target either a category or a product, not both");
        }
        p.setName(req.name().trim());
        p.setDescription(AuthService.blankToNull(req.description()));
        p.setImageUrl(AuthService.blankToNull(req.imageUrl()));
        p.setDiscountType(req.discountType());
        p.setDiscountValue(PricingService.money(req.discountValue()));
        p.setCategory(req.categoryId() == null ? null : categories.findById(req.categoryId())
                .orElseThrow(() -> new BadRequestException("Unknown category")));
        p.setProduct(req.productId() == null ? null : products.findById(req.productId())
                .orElseThrow(() -> new BadRequestException("Unknown product")));
        p.setStartsAt(req.startsAt());
        p.setEndsAt(req.endsAt());
        p.setActive(req.active());
    }

    private static void validatePercent(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENT && value.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("A percentage discount cannot exceed 100%");
        }
    }
}
