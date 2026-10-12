package com.crustcrunch.validation;

import com.crustcrunch.dto.OrderDtos.CheckoutRequest;
import com.crustcrunch.enums.FulfillmentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DeliveryAddressRequiredValidator implements ConstraintValidator<DeliveryAddressRequired, CheckoutRequest> {

    @Override
    public boolean isValid(CheckoutRequest req, ConstraintValidatorContext ctx) {
        if (req == null || req.fulfillmentType() != FulfillmentType.DELIVERY) {
            return true;
        }
        if (req.address() != null) {
            return true;
        }
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(ctx.getDefaultConstraintMessageTemplate())
                .addPropertyNode("address").addConstraintViolation();
        return false;
    }
}
