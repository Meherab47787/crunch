package com.crustcrunch.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level check for coupon/promotion requests: start must be before end, and selection bounds must be sane. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface DateRangeValid {

    String message() default "The start date must be before the end date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
