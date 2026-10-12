package com.crustcrunch.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level check: a delivery order must include a delivery address. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeliveryAddressRequiredValidator.class)
public @interface DeliveryAddressRequired {

    String message() default "A delivery address is required for delivery orders";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
