package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.CartDtos.CartLineRequest;
import com.crustcrunch.dto.CartDtos.ComboSelectionRequest;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.DiscountType;
import com.crustcrunch.enums.FulfillmentType;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.repository.CouponRedemptionRepository;
import com.crustcrunch.repository.CouponRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The single source of truth for prices. Every cart quote and every checkout goes through
 * {@link #priceCart}, which re-reads products, options, promotions and coupons from the database.
 * Nothing price-related is ever accepted from the client.
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductRepository products;
    private final PromotionRepository promotions;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final AppProperties.Pricing config;
    private final Clock clock;

    public PricingService(ProductRepository products, PromotionRepository promotions, CouponRepository coupons,
                          CouponRedemptionRepository redemptions, AppProperties props, Clock clock) {
        this.products = products;
        this.promotions = promotions;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.config = props.pricing();
        this.clock = clock;
    }

    // ------------------------------------------------------------------ results

    public record ComboPick(ComboComponent component, ComboComponentChoice choice) {
    }

    public record PricedLine(int index, CartLineRequest request, Product product, ProductVariant variant,
                             List<CustomizationOption> options, List<ComboPick> comboPicks, BigDecimal unitPrice,
                             int quantity, BigDecimal lineSubtotal, Promotion promotion,
                             BigDecimal promotionDiscount, BigDecimal lineTotal, String error) {

        public boolean valid() {
            return error == null;
        }
    }

    public record PricedCart(List<PricedLine> lines, BigDecimal subtotal, BigDecimal promotionDiscount,
                             Coupon coupon, BigDecimal couponDiscount, String couponMessage, BigDecimal deliveryFee,
                             BigDecimal tax, BigDecimal total, List<String> issues) {

        public boolean valid() {
            return issues.isEmpty() && lines.stream().allMatch(PricedLine::valid);
        }

        public BigDecimal discountTotal() {
            return promotionDiscount.add(couponDiscount);
        }
    }

    /** Who is buying — used for per-customer coupon limits. Both fields may be null for anonymous quotes. */
    public record Buyer(Long userId, String email) {
        public static final Buyer ANONYMOUS = new Buyer(null, null);
    }

    // ------------------------------------------------------------------ public API

    @Transactional(readOnly = true)
    public PricedCart priceCart(List<CartLineRequest> lines, String couponCode, FulfillmentType fulfillment,
                                Buyer buyer) {
        return priceCart(lines, couponCode, fulfillment, buyer, false);
    }

    /**
     * @param lockCoupon when true (checkout), the coupon row is locked so concurrent orders cannot exceed limits.
     *                   Must be called inside a read-write transaction in that case.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PricedCart priceCart(List<CartLineRequest> lines, String couponCode, FulfillmentType fulfillment,
                                Buyer buyer, boolean lockCoupon) {
        Instant now = clock.instant();
        Map<Long, Product> productMap = loadProducts(lines);
        List<Promotion> live = promotions.findLive(now);

        List<PricedLine> priced = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            priced.add(priceLine(i, lines.get(i), productMap, live));
        }

        List<PricedLine> validLines = priced.stream().filter(PricedLine::valid).toList();
        BigDecimal subtotal = money(validLines.stream().map(PricedLine::lineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal promoDiscount = money(validLines.stream().map(PricedLine::promotionDiscount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal afterPromos = subtotal.subtract(promoDiscount);

        List<String> issues = new ArrayList<>();
        Coupon coupon = null;
        BigDecimal couponDiscount = BigDecimal.ZERO.setScale(2);
        String couponMessage = null;
        if (couponCode != null && !couponCode.isBlank()) {
            String code = couponCode.trim();
            Optional<Coupon> found = lockCoupon ? coupons.findByCodeForUpdate(code) : coupons.findByCode(code);
            if (found.isEmpty()) {
                couponMessage = "Coupon code not recognised";
            } else {
                CouponOutcome outcome = applyCoupon(found.get(), validLines, afterPromos, buyer, now);
                couponMessage = outcome.message();
                if (outcome.discount() != null) {
                    coupon = found.get();
                    couponDiscount = outcome.discount();
                }
            }
        }

        BigDecimal net = afterPromos.subtract(couponDiscount).max(BigDecimal.ZERO);
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2);
        if (fulfillment == FulfillmentType.DELIVERY && !validLines.isEmpty()) {
            if (net.compareTo(config.freeDeliveryThreshold()) < 0) {
                deliveryFee = money(config.deliveryFee());
            }
        }
        BigDecimal tax = money(net.multiply(config.taxRate()));
        BigDecimal total = money(net.add(deliveryFee).add(tax));

        priced.stream().filter(l -> !l.valid()).forEach(l -> issues.add(l.error()));
        return new PricedCart(priced, subtotal, promoDiscount, coupon, couponDiscount, couponMessage, deliveryFee,
                tax, total, issues);
    }

    /** Display price of a product's cheapest configuration (used on menu cards). */
    public BigDecimal fromPrice(Product p) {
        BigDecimal minDelta = p.getVariants().stream()
                .filter(ProductVariant::isAvailable)
                .map(ProductVariant::getPriceDelta)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        return money(p.getBasePrice().add(minDelta));
    }

    public Optional<Promotion> bestPromotion(Product product, BigDecimal unitPrice, List<Promotion> live) {
        return live.stream()
                .filter(promo -> promo.appliesTo(product))
                .max(Comparator.comparing(promo -> promotionDiscountPerUnit(promo, unitPrice)));
    }

    public BigDecimal promotionDiscountPerUnit(Promotion promo, BigDecimal unitPrice) {
        BigDecimal d = promo.getDiscountType() == DiscountType.PERCENT
                ? unitPrice.multiply(promo.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : promo.getDiscountValue();
        return money(d.min(unitPrice));
    }

    public List<Promotion> livePromotions() {
        return promotions.findLive(clock.instant());
    }

    private String format(BigDecimal amount) {
        java.util.Currency currency = java.util.Currency.getInstance(config.currency());
        java.text.DecimalFormat f = (java.text.DecimalFormat) java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US);
        f.setCurrency(currency);
        // Locale.US renders taka as "BDT"; use the local sign
        if ("BDT".equals(currency.getCurrencyCode())) {
            java.text.DecimalFormatSymbols symbols = f.getDecimalFormatSymbols();
            symbols.setCurrencySymbol("৳");
            f.setDecimalFormatSymbols(symbols);
        }
        f.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return f.format(amount);
    }

    public static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ internals

    private Map<Long, Product> loadProducts(List<CartLineRequest> lines) {
        Set<Long> ids = new HashSet<>();
        for (CartLineRequest l : lines) {
            ids.add(l.productId());
            l.comboSelectionsOrEmpty().forEach(s -> ids.add(s.productId()));
        }
        return products.findAllWithCategoryByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    PricedLine priceLine(int index, CartLineRequest req, Map<Long, Product> productMap, List<Promotion> live) {
        Product product = productMap.get(req.productId());
        if (product == null || !product.isVisible()) {
            return invalid(index, req, product, "An item in your cart is no longer on the menu");
        }
        if (!product.isOrderable()) {
            return invalid(index, req, product, product.getName() + " is sold out right now");
        }

        // Variant (size / portion)
        ProductVariant variant = null;
        List<ProductVariant> variants = product.getVariants();
        if (!variants.isEmpty()) {
            if (req.variantId() == null) {
                variant = variants.stream().filter(v -> v.isDefaultVariant() && v.isAvailable()).findFirst()
                        .orElse(variants.stream().filter(ProductVariant::isAvailable).findFirst().orElse(null));
            } else {
                variant = variants.stream().filter(v -> v.getId().equals(req.variantId())).findFirst().orElse(null);
            }
            if (variant == null) {
                return invalid(index, req, product, "Please choose a valid size for " + product.getName());
            }
            if (!variant.isAvailable()) {
                return invalid(index, req, product, product.getName() + " (" + variant.getName() + ") is sold out");
            }
        } else if (req.variantId() != null) {
            return invalid(index, req, product, product.getName() + " has no size options");
        }

        // Customization options
        Map<Long, CustomizationOption> allowed = new HashMap<>();
        for (ProductCustomizationGroup pcg : product.getCustomizationGroups()) {
            pcg.getGroup().getOptions().forEach(o -> allowed.put(o.getId(), o));
        }
        List<Long> optionIds = req.optionIdsOrEmpty();
        if (new HashSet<>(optionIds).size() != optionIds.size()) {
            return invalid(index, req, product, "Duplicate options selected for " + product.getName());
        }
        List<CustomizationOption> chosen = new ArrayList<>();
        for (Long id : optionIds) {
            CustomizationOption opt = allowed.get(id);
            if (opt == null) {
                return invalid(index, req, product, "An option you picked for " + product.getName() + " is no longer offered");
            }
            if (!opt.isAvailable()) {
                return invalid(index, req, product, opt.getName() + " is currently unavailable");
            }
            chosen.add(opt);
        }
        Map<Long, Long> countsByGroup = chosen.stream()
                .collect(Collectors.groupingBy(o -> o.getGroup().getId(), Collectors.counting()));
        for (ProductCustomizationGroup pcg : product.getCustomizationGroups()) {
            CustomizationGroup g = pcg.getGroup();
            long n = countsByGroup.getOrDefault(g.getId(), 0L);
            if (n < g.getMinSelect()) {
                return invalid(index, req, product, g.getMinSelect() == 1
                        ? "Please choose a " + g.getName().toLowerCase() + " for " + product.getName()
                        : "Please choose at least " + g.getMinSelect() + " " + g.getName().toLowerCase() + " for " + product.getName());
            }
            if (n > g.getMaxSelect()) {
                return invalid(index, req, product, "You can pick at most " + g.getMaxSelect() + " "
                        + g.getName().toLowerCase() + " for " + product.getName());
            }
        }
        // keep display order stable: group order, then option order
        Map<Long, Integer> groupOrder = new HashMap<>();
        product.getCustomizationGroups().forEach(pcg -> groupOrder.put(pcg.getGroup().getId(), pcg.getSortOrder()));
        chosen.sort(Comparator.<CustomizationOption>comparingInt(o -> groupOrder.getOrDefault(o.getGroup().getId(), 0))
                .thenComparingInt(CustomizationOption::getSortOrder));

        // Combo selections
        List<ComboPick> picks = new ArrayList<>();
        List<ComboSelectionRequest> selections = req.comboSelectionsOrEmpty();
        if (product.getProductType() == ProductType.COMBO) {
            Map<Long, List<ComboSelectionRequest>> byComponent = selections.stream()
                    .collect(Collectors.groupingBy(ComboSelectionRequest::componentId));
            Set<Long> componentIds = product.getComboComponents().stream().map(ComboComponent::getId).collect(Collectors.toSet());
            if (!componentIds.containsAll(byComponent.keySet())) {
                return invalid(index, req, product, "Invalid combo selection for " + product.getName());
            }
            for (ComboComponent comp : product.getComboComponents()) {
                List<ComboSelectionRequest> sel = byComponent.getOrDefault(comp.getId(), List.of());
                if (sel.size() != comp.getQuantity()) {
                    return invalid(index, req, product, product.getName() + ": " + comp.getName().toLowerCase()
                            + " (" + comp.getQuantity() + " needed)");
                }
                for (ComboSelectionRequest s : sel) {
                    ComboComponentChoice choice = comp.getChoices().stream()
                            .filter(c -> c.getProduct().getId().equals(s.productId())).findFirst().orElse(null);
                    if (choice == null) {
                        return invalid(index, req, product, "That choice isn't part of " + product.getName());
                    }
                    if (!choice.getProduct().isOrderable()) {
                        return invalid(index, req, product, choice.getProduct().getName() + " is sold out — please pick another option");
                    }
                    picks.add(new ComboPick(comp, choice));
                }
            }
        } else if (!selections.isEmpty()) {
            return invalid(index, req, product, product.getName() + " is not a combo");
        }

        BigDecimal unit = product.getBasePrice()
                .add(variant == null ? BigDecimal.ZERO : variant.getPriceDelta())
                .add(chosen.stream().map(CustomizationOption::getPriceDelta).reduce(BigDecimal.ZERO, BigDecimal::add))
                .add(picks.stream().map(p -> p.choice().getExtraPrice()).reduce(BigDecimal.ZERO, BigDecimal::add));
        unit = money(unit.max(BigDecimal.ZERO));
        int qty = req.quantity();
        BigDecimal lineSubtotal = money(unit.multiply(BigDecimal.valueOf(qty)));

        Promotion promo = bestPromotion(product, unit, live).orElse(null);
        BigDecimal promoDiscount = promo == null ? BigDecimal.ZERO.setScale(2)
                : money(promotionDiscountPerUnit(promo, unit).multiply(BigDecimal.valueOf(qty)));
        if (promo != null && promoDiscount.signum() == 0) {
            promo = null;
        }
        BigDecimal lineTotal = lineSubtotal.subtract(promoDiscount);

        return new PricedLine(index, req, product, variant, chosen, picks, unit, qty, lineSubtotal, promo,
                promoDiscount, lineTotal, null);
    }

    private PricedLine invalid(int index, CartLineRequest req, Product product, String error) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new PricedLine(index, req, product, null, List.of(), List.of(), zero, req.quantity(), zero, null,
                zero, zero, error);
    }

    private record CouponOutcome(BigDecimal discount, String message) {
    }

    private CouponOutcome applyCoupon(Coupon c, List<PricedLine> lines, BigDecimal afterPromos, Buyer buyer, Instant now) {
        if (!c.isActive()) {
            return new CouponOutcome(null, "This coupon is no longer active");
        }
        if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) {
            return new CouponOutcome(null, "This coupon isn't valid yet");
        }
        if (c.getExpiresAt() != null && !now.isBefore(c.getExpiresAt())) {
            return new CouponOutcome(null, "This coupon has expired");
        }
        if (c.getMaxRedemptions() != null && c.getRedemptionCount() >= c.getMaxRedemptions()) {
            return new CouponOutcome(null, "This coupon has reached its redemption limit");
        }
        if (c.getPerUserLimit() != null && (buyer.userId() != null || buyer.email() != null)) {
            long used = redemptions.countForCustomer(c.getId(), buyer.userId(), buyer.email() == null ? "" : buyer.email());
            if (used >= c.getPerUserLimit()) {
                return new CouponOutcome(null, "You've already used this coupon");
            }
        }
        if (afterPromos.compareTo(c.getMinOrderAmount()) < 0) {
            return new CouponOutcome(null, "Spend at least " + format(c.getMinOrderAmount()) + " to use this coupon");
        }
        BigDecimal eligible = afterPromos;
        if (c.getCategory() != null) {
            Long catId = c.getCategory().getId();
            eligible = lines.stream()
                    .filter(l -> l.product().getCategory().getId().equals(catId))
                    .map(PricedLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (eligible.signum() == 0) {
                return new CouponOutcome(null, "This coupon only applies to " + c.getCategory().getName());
            }
        }
        BigDecimal discount = c.getDiscountType() == DiscountType.PERCENT
                ? eligible.multiply(c.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        if (c.getMaxDiscountAmount() != null) {
            discount = discount.min(c.getMaxDiscountAmount());
        }
        discount = money(discount.min(eligible));
        return new CouponOutcome(discount, "Coupon applied: " + (c.getDescription() != null ? c.getDescription() : c.getCode()));
    }
}
