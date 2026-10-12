package com.crustcrunch.mapper;

import com.crustcrunch.dto.AdminDtos.CouponResponse;
import com.crustcrunch.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public CouponResponse toResponse(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinOrderAmount(), c.getMaxDiscountAmount(),
                c.getCategory() == null ? null : c.getCategory().getId(),
                c.getCategory() == null ? null : c.getCategory().getName(), c.getStartsAt(), c.getExpiresAt(),
                c.getMaxRedemptions(), c.getPerUserLimit(), c.getRedemptionCount(), c.isActive(), c.getCreatedAt());
    }
}
