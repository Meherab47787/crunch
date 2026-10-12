package com.crustcrunch.mapper;

import com.crustcrunch.dto.MenuDtos.*;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.service.PricingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@Component
public class MenuMapper {

    private final PricingService pricing;

    public MenuMapper(PricingService pricing) {
        this.pricing = pricing;
    }

    public CategoryResponse toResponse(Category c, long productCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                c.getIcon(), c.getSortOrder(), c.isActive(), productCount);
    }

    public ProductSummary toSummary(Product p, List<Promotion> livePromotions) {
        BigDecimal from = pricing.fromPrice(p);
        Promotion promo = pricing.bestPromotion(p, from, livePromotions).orElse(null);
        BigDecimal sale = promo == null ? null : from.subtract(pricing.promotionDiscountPerUnit(promo, from));
        if (sale != null && sale.compareTo(from) >= 0) {
            sale = null;
            promo = null;
        }
        Category c = p.getCategory();
        boolean customizable = !p.getVariants().isEmpty() || !p.getCustomizationGroups().isEmpty()
                || p.getProductType() == ProductType.COMBO;
        return new ProductSummary(p.getId(), p.getSlug(), p.getName(), p.getDescription(), c.getId(), c.getName(),
                c.getSlug(), p.getBasePrice(), from, sale, promo == null ? null : promo.getName(), p.getImageUrl(),
                p.getProductType(), p.isVegetarian(), p.isSpicy(), p.getAllergens(), p.getCalories(),
                p.isAvailable(), p.isVisible(), p.isFeatured(), p.isBestseller(), p.isNewArrival(), p.getRatingAvg(),
                p.getRatingCount(), p.getSoldCount(), customizable);
    }

    public ProductDetail toDetail(Product p, List<Promotion> livePromotions) {
        ProductSummary summary = toSummary(p, livePromotions);
        List<String> images = Stream.concat(
                        Stream.ofNullable(p.getImageUrl()),
                        p.getImages().stream().map(ProductImage::getUrl))
                .distinct().toList();
        List<VariantResponse> variants = p.getVariants().stream().map(this::toResponse).toList();
        List<GroupResponse> groups = p.getCustomizationGroups().stream()
                .map(pcg -> toResponse(pcg.getGroup())).toList();
        List<ComboComponentResponse> combo = p.getComboComponents().stream().map(this::toResponse).toList();
        PromotionBadge badge = pricing.bestPromotion(p, summary.fromPrice(), livePromotions)
                .map(pr -> new PromotionBadge(pr.getId(), pr.getName(), pr.getDiscountType(), pr.getDiscountValue()))
                .orElse(null);
        return new ProductDetail(summary, p.getIngredients(), p.getPrepMinutes(), p.getSortOrder(), images, variants,
                groups, combo, badge);
    }

    public VariantResponse toResponse(ProductVariant v) {
        return new VariantResponse(v.getId(), v.getName(), v.getPriceDelta(), v.isDefaultVariant(), v.isAvailable(),
                v.getSortOrder());
    }

    public GroupResponse toResponse(CustomizationGroup g) {
        return new GroupResponse(g.getId(), g.getName(), g.getDescription(), g.getSelectionType(), g.getMinSelect(),
                g.getMaxSelect(), g.isRequired(), g.getOptions().stream().map(this::toResponse).toList());
    }

    public OptionResponse toResponse(CustomizationOption o) {
        return new OptionResponse(o.getId(), o.getName(), o.getPriceDelta(), o.isDefaultOption(), o.isAvailable(),
                o.getSortOrder());
    }

    public ComboComponentResponse toResponse(ComboComponent c) {
        return new ComboComponentResponse(c.getId(), c.getName(), c.getQuantity(), c.getSortOrder(),
                c.getChoices().stream().map(ch -> new ComboChoiceResponse(ch.getProduct().getId(),
                        ch.getProduct().getName(), ch.getProduct().getSlug(), ch.getProduct().getImageUrl(),
                        ch.getExtraPrice(), ch.isDefaultChoice(), ch.getProduct().isOrderable())).toList());
    }

    public ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), displayName(r.getUser().getFullName()), r.getRating(), r.getComment(),
                r.getProduct().getId(), r.getProduct().getName(), r.getProduct().getSlug(), r.getCreatedAt());
    }

    public PromotionResponse toResponse(Promotion p, java.time.Instant now) {
        Category c = p.getCategory();
        Product prod = p.getProduct();
        return new PromotionResponse(p.getId(), p.getName(), p.getDescription(), p.getImageUrl(), p.getDiscountType(),
                p.getDiscountValue(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                c == null ? null : c.getSlug(), prod == null ? null : prod.getId(),
                prod == null ? null : prod.getName(), prod == null ? null : prod.getSlug(), p.getStartsAt(),
                p.getEndsAt(), p.isActive(), p.isLiveAt(now));
    }

    /** "Jane Doe" -> "Jane D." so public reviews don't expose full names. */
    static String displayName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Guest";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
