package com.crustcrunch.validation;

import com.crustcrunch.dto.AdminDtos.CouponRequest;
import com.crustcrunch.dto.AdminDtos.PromotionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class DateRangeValidator implements ConstraintValidator<DateRangeValid, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext ctx) {
        Instant start;
        Instant end;
        if (value instanceof CouponRequest c) {
            start = c.startsAt();
            end = c.expiresAt();
        } else if (value instanceof PromotionRequest p) {
            start = p.startsAt();
            end = p.endsAt();
        } else {
            return true;
        }
        return start == null || end == null || start.isBefore(end);
    }
}
