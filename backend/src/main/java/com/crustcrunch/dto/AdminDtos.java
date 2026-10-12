package com.crustcrunch.dto;

import com.crustcrunch.enums.DiscountType;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.enums.SelectionType;
import com.crustcrunch.validation.DateRangeValid;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Write models for the admin dashboard. */
public final class AdminDtos {

    private AdminDtos() {
    }

    private static final String SLUG = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 80) @Pattern(regexp = SLUG, message = "must be lowercase words separated by dashes") String slug,
            @Size(max = 500) String description,
            @Size(max = 500) String imageUrl,
            @Size(max = 40) String icon,
            int sortOrder,
            boolean active) {
    }

    public record VariantRequest(
            Long id,
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("-1000.00") @DecimalMax("1000.00") @Digits(integer = 8, fraction = 2) BigDecimal priceDelta,
            boolean defaultVariant,
            boolean available,
            int sortOrder) {
    }

    public record ComboChoiceRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal extraPrice,
            boolean defaultChoice) {
    }

    public record ComboComponentRequest(
            Long id,
            @NotBlank @Size(max = 80) String name,
            @Min(1) @Max(20) int quantity,
            int sortOrder,
            @NotEmpty List<@Valid @NotNull ComboChoiceRequest> choices) {
    }

    public record ProductRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 140) @Pattern(regexp = SLUG, message = "must be lowercase words separated by dashes") String slug,
            @Size(max = 1000) String description,
            @Size(max = 1000) String ingredients,
            @Size(max = 300) String allergens,
            @NotNull @DecimalMin("0.00") @DecimalMax("10000.00") @Digits(integer = 8, fraction = 2) BigDecimal basePrice,
            @Size(max = 500) String imageUrl,
            @NotNull ProductType productType,
            boolean vegetarian,
            boolean spicy,
            @Min(0) Integer calories,
            @Min(0) @Max(240) int prepMinutes,
            boolean available,
            boolean visible,
            boolean featured,
            boolean bestseller,
            boolean newArrival,
            int sortOrder,
            List<@Valid @NotNull VariantRequest> variants,
            List<@NotNull Long> customizationGroupIds,
            List<@Valid @NotNull ComboComponentRequest> comboComponents,
            List<@NotBlank @Size(max = 500) String> galleryImages) {
    }

    public record AvailabilityRequest(Boolean available, Boolean visible) {
    }

    public record OptionRequest(
            Long id,
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") @Digits(integer = 8, fraction = 2) BigDecimal priceDelta,
            boolean defaultOption,
            boolean available,
            int sortOrder) {
    }

    public record GroupRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 300) String description,
            @NotNull SelectionType selectionType,
            @Min(0) @Max(20) int minSelect,
            @Min(1) @Max(20) int maxSelect,
            @NotEmpty List<@Valid @NotNull OptionRequest> options) {
    }

    public record GroupAdminResponse(MenuDtos.GroupResponse group, long usedByProducts) {
    }

    @DateRangeValid
    public record CouponRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "may only contain letters, digits, - and _") String code,
            @Size(max = 300) String description,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal discountValue,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal minOrderAmount,
            @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal maxDiscountAmount,
            Long categoryId,
            Instant startsAt,
            Instant expiresAt,
            @Min(1) Integer maxRedemptions,
            @Min(1) Integer perUserLimit,
            boolean active) {
    }

    public record CouponResponse(Long id, String code, String description, DiscountType discountType,
                                 BigDecimal discountValue, BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                 Long categoryId, String categoryName, Instant startsAt, Instant expiresAt,
                                 Integer maxRedemptions, Integer perUserLimit, int redemptionCount, boolean active,
                                 Instant createdAt) {
    }

    @DateRangeValid
    public record PromotionRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @Size(max = 500) String imageUrl,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal discountValue,
            Long categoryId,
            Long productId,
            Instant startsAt,
            Instant endsAt,
            boolean active) {
    }

    public record UploadResponse(String url) {
    }
}
