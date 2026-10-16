package com.crustcrunch.dto;

import com.crustcrunch.enums.DiscountType;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.enums.SelectionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read models for the storefront menu. */
public final class MenuDtos {

    private MenuDtos() {
    }

    public record CategoryResponse(Long id, String name, String slug, String description, String imageUrl,
                                   String icon, int sortOrder, boolean active, long productCount) {
    }

    /**
     * @param fromPrice  cheapest configuration (base + cheapest variant), before promotions
     * @param salePrice  fromPrice after the best live promotion, or null when no promotion applies
     */
    public record ProductSummary(Long id, String slug, String name, String description, Long categoryId,
                                 String categoryName, String categorySlug, BigDecimal basePrice, BigDecimal fromPrice,
                                 BigDecimal salePrice, String promotionName, String imageUrl, ProductType productType,
                                 boolean vegetarian, boolean spicy, String allergens, Integer calories,
                                 boolean available, boolean visible, boolean featured, boolean bestseller,
                                 boolean newArrival, BigDecimal ratingAvg, int ratingCount, int soldCount,
                                 boolean customizable) {
    }

    public record ProductDetail(ProductSummary summary, String ingredients, int prepMinutes, int sortOrder,
                                List<String> images, List<VariantResponse> variants, List<GroupResponse> groups,
                                List<ComboComponentResponse> comboComponents, PromotionBadge promotion) {
    }

    public record PromotionBadge(Long id, String name, DiscountType discountType, BigDecimal discountValue) {
    }

    public record VariantResponse(Long id, String name, BigDecimal priceDelta, boolean defaultVariant,
                                  boolean available, int sortOrder) {
    }

    public record GroupResponse(Long id, String name, String description, SelectionType selectionType,
                                int minSelect, int maxSelect, boolean required, List<OptionResponse> options) {
    }

    public record OptionResponse(Long id, String name, BigDecimal priceDelta, boolean defaultOption,
                                 boolean available, int sortOrder) {
    }

    public record ComboComponentResponse(Long id, String name, int quantity, int sortOrder,
                                         List<ComboChoiceResponse> choices) {
    }

    public record ComboChoiceResponse(Long productId, String name, String slug, String imageUrl,
                                      BigDecimal extraPrice, boolean defaultChoice, boolean available) {
    }

    public record ReviewResponse(Long id, String authorName, int rating, String comment, Long productId,
                                 String productName, String productSlug, Instant createdAt) {
    }

    public record PromotionResponse(Long id, String name, String description, String imageUrl,
                                    DiscountType discountType, BigDecimal discountValue, Long categoryId,
                                    String categoryName, String categorySlug, Long productId, String productName,
                                    String productSlug, Instant startsAt, Instant endsAt, boolean active,
                                    boolean live) {
    }

    public record HoursEntry(String days, String open, String close) {
    }

    public record PublicSettings(String restaurantName, String tagline, String phone, String email, String address,
                                 String currency, List<HoursEntry> hours, BigDecimal taxRate, BigDecimal deliveryFee,
                                 BigDecimal freeDeliveryThreshold,
                                 int deliveryMinutes, int pickupMinutes) {
    }
}
