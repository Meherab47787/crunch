package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.CartDtos.CartLineRequest;
import com.crustcrunch.dto.CartDtos.ComboSelectionRequest;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.DiscountType;
import com.crustcrunch.enums.FulfillmentType;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.enums.SelectionType;
import com.crustcrunch.repository.CouponRedemptionRepository;
import com.crustcrunch.repository.CouponRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.PromotionRepository;
import com.crustcrunch.service.PricingService.Buyer;
import com.crustcrunch.service.PricingService.PricedCart;
import com.crustcrunch.service.PricingService.PricedLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Mock
    ProductRepository productRepo;
    @Mock
    PromotionRepository promotionRepo;
    @Mock
    CouponRepository couponRepo;
    @Mock
    CouponRedemptionRepository redemptionRepo;

    PricingService pricing;

    Category pizzaCat;
    Category burgerCat;
    Category sidesCat;
    Category combosCat;

    Product pizza;
    ProductVariant medium;
    ProductVariant small;
    CustomizationGroup crust;
    CustomizationOption classicCrust;
    CustomizationOption stuffedCrust;
    CustomizationGroup toppings;
    CustomizationOption pepperoni;
    CustomizationOption mushroom;
    CustomizationOption olives;
    CustomizationGroup unrelatedGroup;
    CustomizationOption unrelatedOption;

    Product burger;
    Product fries;
    Product soda;
    Product combo;
    ComboComponent sideComponent;
    ComboComponent drinkComponent;

    @BeforeEach
    void setUp() {
        var pricingConfig = new AppProperties.Pricing(new BigDecimal("0.08"), new BigDecimal("2.99"),
                new BigDecimal("35.00"), "USD");
        var props = new AppProperties(null, null, pricingConfig, null, null, null);
        pricing = new PricingService(productRepo, promotionRepo, couponRepo, redemptionRepo, props,
                Clock.fixed(NOW, ZoneOffset.UTC));

        pizzaCat = category(1L, "Pizza");
        burgerCat = category(2L, "Burgers");
        sidesCat = category(3L, "Sides");
        combosCat = category(4L, "Combos");

        // Pizza: base 10.00, sizes Small (+0) / Medium (+3, default); Crust required (1..1); Toppings 0..2
        pizza = product(10L, pizzaCat, "Pizza", "10.00");
        small = variant(100L, pizza, "Small", "0.00", false);
        medium = variant(101L, pizza, "Medium", "3.00", true);
        crust = group(50L, "Crust", 1, 1);
        classicCrust = option(500L, crust, "Classic", "0.00", 1);
        stuffedCrust = option(501L, crust, "Stuffed", "2.50", 2);
        toppings = group(51L, "Toppings", 0, 2);
        pepperoni = option(510L, toppings, "Pepperoni", "1.50", 1);
        mushroom = option(511L, toppings, "Mushroom", "1.00", 2);
        olives = option(512L, toppings, "Olives", "0.75", 3);
        pizza.getCustomizationGroups().add(new ProductCustomizationGroup(pizza, crust, 1));
        pizza.getCustomizationGroups().add(new ProductCustomizationGroup(pizza, toppings, 2));
        // a group that exists but is NOT attached to the pizza
        unrelatedGroup = group(52L, "Patty", 0, 1);
        unrelatedOption = option(520L, unrelatedGroup, "Double Patty", "3.00", 1);

        burger = product(20L, burgerCat, "Burger", "8.30");
        fries = product(30L, sidesCat, "Fries", "3.00");
        soda = product(31L, sidesCat, "Soda", "2.00");

        // Combo: base 12.00 + side (fries, +1.00) + drink (soda, +0.50)
        combo = product(40L, combosCat, "Burger Combo", "12.00");
        combo.setProductType(ProductType.COMBO);
        sideComponent = component(60L, combo, "Choose a side", 1);
        choice(sideComponent, fries, "1.00");
        drinkComponent = component(61L, combo, "Choose a drink", 1);
        choice(drinkComponent, soda, "0.50");

        lenient().when(productRepo.findAllWithCategoryByIdIn(anyCollection()))
                .thenReturn(List.of(pizza, burger, fries, soda, combo));
        lenient().when(promotionRepo.findLive(NOW)).thenReturn(List.of());
    }

    // ------------------------------------------------------------------ unit price & validation

    @Test
    void unitPriceIsBasePlusVariantPlusOptions() {
        PricedCart cart = quote(line(pizza, medium, List.of(stuffedCrust, pepperoni, mushroom), 2));

        PricedLine l = cart.lines().getFirst();
        assertThat(l.valid()).isTrue();
        // 10.00 + 3.00 + 2.50 + 1.50 + 1.00
        assertThat(l.unitPrice()).isEqualByComparingTo("18.00");
        assertThat(l.lineSubtotal()).isEqualByComparingTo("36.00");
        assertThat(l.variant()).isSameAs(medium);
        assertThat(cart.subtotal()).isEqualByComparingTo("36.00");
    }

    @Test
    void defaultVariantIsUsedWhenNoneSelected() {
        PricedCart cart = quote(new CartLineRequest(pizza.getId(), null, List.of(classicCrust.getId()), null, 1, null));

        assertThat(cart.lines().getFirst().variant()).isSameAs(medium);
        assertThat(cart.lines().getFirst().unitPrice()).isEqualByComparingTo("13.00");
    }

    @Test
    void comboUnitPriceIncludesChoiceExtras() {
        PricedCart cart = quote(new CartLineRequest(combo.getId(), null, null, List.of(
                new ComboSelectionRequest(sideComponent.getId(), fries.getId()),
                new ComboSelectionRequest(drinkComponent.getId(), soda.getId())), 1, null));

        PricedLine l = cart.lines().getFirst();
        assertThat(l.valid()).isTrue();
        assertThat(l.unitPrice()).isEqualByComparingTo("13.50");
        assertThat(l.comboPicks()).hasSize(2);
    }

    @Test
    void comboMissingAComponentIsInvalid() {
        PricedCart cart = quote(new CartLineRequest(combo.getId(), null, null, List.of(
                new ComboSelectionRequest(sideComponent.getId(), fries.getId())), 1, null));

        assertThat(cart.lines().getFirst().valid()).isFalse();
        assertThat(cart.lines().getFirst().error()).contains("choose a drink");
    }

    @Test
    void missingRequiredGroupMakesLineInvalid() {
        PricedCart cart = quote(line(pizza, medium, List.of(pepperoni), 1));

        PricedLine l = cart.lines().getFirst();
        assertThat(l.valid()).isFalse();
        assertThat(l.error()).isEqualTo("Please choose a crust for Pizza");
        assertThat(cart.valid()).isFalse();
        assertThat(cart.issues()).containsExactly("Please choose a crust for Pizza");
        assertThat(cart.subtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void exceedingGroupMaximumMakesLineInvalid() {
        PricedCart cart = quote(line(pizza, medium, List.of(classicCrust, pepperoni, mushroom, olives), 1));

        assertThat(cart.lines().getFirst().valid()).isFalse();
        assertThat(cart.lines().getFirst().error()).isEqualTo("You can pick at most 2 toppings for Pizza");
    }

    @Test
    void optionFromAGroupNotAttachedToTheProductIsRejected() {
        PricedCart cart = quote(line(pizza, medium, List.of(classicCrust, unrelatedOption), 1));

        assertThat(cart.lines().getFirst().valid()).isFalse();
        assertThat(cart.lines().getFirst().error()).contains("no longer offered");
    }

    @Test
    void soldOutProductIsInvalid() {
        burger.setAvailable(false);

        PricedCart cart = quote(line(burger, null, List.of(), 1));

        assertThat(cart.lines().getFirst().valid()).isFalse();
        assertThat(cart.lines().getFirst().error()).isEqualTo("Burger is sold out right now");
        assertThat(cart.valid()).isFalse();
    }

    @Test
    void invalidLinesDoNotContributeToTotals() {
        burger.setAvailable(false);

        PricedCart cart = quote(line(burger, null, List.of(), 1), line(fries, null, List.of(), 2));

        assertThat(cart.subtotal()).isEqualByComparingTo("6.00");
        assertThat(cart.valid()).isFalse();
    }

    // ------------------------------------------------------------------ promotions

    @Test
    void percentPromotionRoundsHalfUpPerUnit() {
        // 8.30 * 15% = 1.245 -> 1.25 (HALF_UP; HALF_EVEN/DOWN would give 1.24)
        Promotion promo = promotion(1L, DiscountType.PERCENT, "15", burgerCat, null);
        when(promotionRepo.findLive(NOW)).thenReturn(List.of(promo));

        PricedCart cart = quote(line(burger, null, List.of(), 3));

        PricedLine l = cart.lines().getFirst();
        assertThat(l.promotion()).isSameAs(promo);
        assertThat(l.promotionDiscount()).isEqualByComparingTo("3.75");
        assertThat(l.lineSubtotal()).isEqualByComparingTo("24.90");
        assertThat(l.lineTotal()).isEqualByComparingTo("21.15");
        assertThat(cart.promotionDiscount()).isEqualByComparingTo("3.75");
    }

    @Test
    void fixedPromotionIsCappedAtUnitPrice() {
        Promotion promo = promotion(2L, DiscountType.FIXED, "5.00", null, fries);
        when(promotionRepo.findLive(NOW)).thenReturn(List.of(promo));

        PricedCart cart = quote(line(fries, null, List.of(), 2));

        PricedLine l = cart.lines().getFirst();
        assertThat(l.promotionDiscount()).isEqualByComparingTo("6.00");
        assertThat(l.lineTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void bestOfSeveralPromotionsIsApplied() {
        Promotion small10 = promotion(3L, DiscountType.PERCENT, "10", pizzaCat, null);
        Promotion fixed2 = promotion(4L, DiscountType.FIXED, "2.00", null, pizza);
        when(promotionRepo.findLive(NOW)).thenReturn(List.of(small10, fixed2));

        // unit 13.00: 10% = 1.30, fixed = 2.00 -> fixed wins
        PricedCart cart = quote(line(pizza, medium, List.of(classicCrust), 1));

        assertThat(cart.lines().getFirst().promotion()).isSameAs(fixed2);
        assertThat(cart.lines().getFirst().promotionDiscount()).isEqualByComparingTo("2.00");
    }

    @Test
    void promotionForAnotherCategoryDoesNotApply() {
        when(promotionRepo.findLive(NOW)).thenReturn(List.of(promotion(5L, DiscountType.PERCENT, "20", pizzaCat, null)));

        PricedCart cart = quote(line(burger, null, List.of(), 1));

        assertThat(cart.lines().getFirst().promotion()).isNull();
        assertThat(cart.promotionDiscount()).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------ coupons

    @Nested
    class Coupons {

        @Test
        void minimumOrderNotMetRejectsCoupon() {
            Coupon c = coupon(1L, "BIG", DiscountType.PERCENT, "10");
            c.setMinOrderAmount(new BigDecimal("50.00"));
            when(couponRepo.findByCode("BIG")).thenReturn(Optional.of(c));

            PricedCart cart = quoteWithCoupon("BIG", line(burger, null, List.of(), 1));

            assertThat(cart.coupon()).isNull();
            assertThat(cart.couponDiscount()).isEqualByComparingTo("0.00");
            assertThat(cart.couponMessage()).isEqualTo("Spend at least $50 to use this coupon");
            // a rejected coupon is reported but doesn't make the cart itself invalid
            assertThat(cart.valid()).isTrue();
        }

        @Test
        void expiredCouponIsRejected() {
            Coupon c = coupon(2L, "OLD", DiscountType.FIXED, "5.00");
            c.setExpiresAt(NOW.minusSeconds(1));
            when(couponRepo.findByCode("OLD")).thenReturn(Optional.of(c));

            PricedCart cart = quoteWithCoupon("OLD", line(burger, null, List.of(), 2));

            assertThat(cart.coupon()).isNull();
            assertThat(cart.couponMessage()).isEqualTo("This coupon has expired");
        }

        @Test
        void couponExpiringExactlyNowIsExpired() {
            Coupon c = coupon(3L, "EDGE", DiscountType.FIXED, "5.00");
            c.setExpiresAt(NOW);
            when(couponRepo.findByCode("EDGE")).thenReturn(Optional.of(c));

            assertThat(quoteWithCoupon("EDGE", line(burger, null, List.of(), 2)).coupon()).isNull();
        }

        @Test
        void unknownCouponReportsMessage() {
            when(couponRepo.findByCode("NOPE")).thenReturn(Optional.empty());

            PricedCart cart = quoteWithCoupon("NOPE", line(burger, null, List.of(), 1));

            assertThat(cart.couponMessage()).isEqualTo("Coupon code not recognised");
            assertThat(cart.coupon()).isNull();
        }

        @Test
        void categoryRestrictedCouponOnlyDiscountsEligibleLines() {
            Coupon c = coupon(4L, "SIDES50", DiscountType.PERCENT, "50");
            c.setCategory(sidesCat);
            when(couponRepo.findByCode("SIDES50")).thenReturn(Optional.of(c));

            // burger 8.30 (not eligible) + fries 3.00 (eligible)
            PricedCart cart = quoteWithCoupon("SIDES50", line(burger, null, List.of(), 1), line(fries, null, List.of(), 1));

            assertThat(cart.coupon()).isSameAs(c);
            assertThat(cart.couponDiscount()).isEqualByComparingTo("1.50");
        }

        @Test
        void categoryRestrictedCouponWithNoEligibleLinesIsRejected() {
            Coupon c = coupon(5L, "SIDES50", DiscountType.PERCENT, "50");
            c.setCategory(sidesCat);
            when(couponRepo.findByCode("SIDES50")).thenReturn(Optional.of(c));

            PricedCart cart = quoteWithCoupon("SIDES50", line(burger, null, List.of(), 1));

            assertThat(cart.coupon()).isNull();
            assertThat(cart.couponMessage()).isEqualTo("This coupon only applies to Sides");
        }

        @Test
        void percentCouponIsCappedAtMaxDiscount() {
            Coupon c = coupon(6L, "HALF", DiscountType.PERCENT, "50");
            c.setMaxDiscountAmount(new BigDecimal("2.00"));
            when(couponRepo.findByCode("HALF")).thenReturn(Optional.of(c));

            PricedCart cart = quoteWithCoupon("HALF", line(burger, null, List.of(), 2));

            assertThat(cart.couponDiscount()).isEqualByComparingTo("2.00");
        }

        @Test
        void fixedCouponNeverExceedsOrderValue() {
            Coupon c = coupon(7L, "TENOFF", DiscountType.FIXED, "10.00");
            when(couponRepo.findByCode("TENOFF")).thenReturn(Optional.of(c));

            PricedCart cart = quoteWithCoupon("TENOFF", line(fries, null, List.of(), 1));

            assertThat(cart.couponDiscount()).isEqualByComparingTo("3.00");
            assertThat(cart.total()).isEqualByComparingTo("0.00");
        }

        @Test
        void perCustomerLimitBlocksReuse() {
            Coupon c = coupon(8L, "ONCE", DiscountType.PERCENT, "10");
            c.setPerUserLimit(1);
            when(couponRepo.findByCode("ONCE")).thenReturn(Optional.of(c));
            when(redemptionRepo.countForCustomer(8L, 42L, "me@example.com")).thenReturn(1L);

            PricedCart cart = pricing.priceCart(List.of(line(burger, null, List.of(), 2)), "ONCE",
                    FulfillmentType.PICKUP, new Buyer(42L, "me@example.com"));

            assertThat(cart.coupon()).isNull();
            assertThat(cart.couponMessage()).isEqualTo("You've already used this coupon");
        }

        @Test
        void exhaustedCouponIsRejected() {
            Coupon c = coupon(9L, "LIMITED", DiscountType.FIXED, "1.00");
            c.setMaxRedemptions(5);
            c.setRedemptionCount(5);
            when(couponRepo.findByCode("LIMITED")).thenReturn(Optional.of(c));

            assertThat(quoteWithCoupon("LIMITED", line(burger, null, List.of(), 1)).couponMessage())
                    .isEqualTo("This coupon has reached its redemption limit");
        }

        @Test
        void couponAppliesAfterPromotionDiscounts() {
            when(promotionRepo.findLive(NOW)).thenReturn(List.of(promotion(9L, DiscountType.FIXED, "1.00", sidesCat, null)));
            Coupon c = coupon(10L, "TEN", DiscountType.PERCENT, "10");
            when(couponRepo.findByCode("TEN")).thenReturn(Optional.of(c));

            // fries 3.00 x 5 = 15.00, promo -5.00 -> 10.00, coupon 10% -> 1.00
            PricedCart cart = quoteWithCoupon("TEN", line(fries, null, List.of(), 5));

            assertThat(cart.promotionDiscount()).isEqualByComparingTo("5.00");
            assertThat(cart.couponDiscount()).isEqualByComparingTo("1.00");
            assertThat(cart.discountTotal()).isEqualByComparingTo("6.00");
        }
    }

    // ------------------------------------------------------------------ delivery fee & tax

    @Test
    void deliveryFeeChargedBelowFreeDeliveryThreshold() {
        // burger 8.30 x 2 = 16.60
        PricedCart cart = pricing.priceCart(List.of(line(burger, null, List.of(), 2)), null,
                FulfillmentType.DELIVERY, Buyer.ANONYMOUS);

        assertThat(cart.deliveryFee()).isEqualByComparingTo("2.99");
        assertThat(cart.valid()).isTrue();
    }

    @Test
    void deliveryFeeWaivedAtOrAboveFreeDeliveryThreshold() {
        // 18.00 x 2 = 36.00 >= 35.00
        PricedCart cart = pricing.priceCart(List.of(line(pizza, medium, List.of(stuffedCrust, pepperoni, mushroom), 2)),
                null, FulfillmentType.DELIVERY, Buyer.ANONYMOUS);

        assertThat(cart.deliveryFee()).isEqualByComparingTo("0.00");
        // tax 36.00 * 0.08 = 2.88
        assertThat(cart.tax()).isEqualByComparingTo("2.88");
        assertThat(cart.total()).isEqualByComparingTo("38.88");
    }

    @Test
    void pickupNeverHasDeliveryFee() {
        PricedCart cart = quote(line(fries, null, List.of(), 1));

        assertThat(cart.deliveryFee()).isEqualByComparingTo("0.00");
    }

    @Test
    void smallDeliveryOrderIsAllowedAndPaysDeliveryFee() {
        PricedCart cart = pricing.priceCart(List.of(line(fries, null, List.of(), 1)), null,
                FulfillmentType.DELIVERY, Buyer.ANONYMOUS);

        assertThat(cart.valid()).isTrue();
        assertThat(cart.deliveryFee()).isEqualByComparingTo("2.99");
    }

    @Test
    void taxIsComputedOnNetAfterDiscountsAndExcludesDeliveryFee() {
        Coupon c = coupon(11L, "TEN", DiscountType.PERCENT, "10");
        when(couponRepo.findByCode("TEN")).thenReturn(Optional.of(c));

        // 16.60 - 1.66 = 14.94 net; tax = 1.1952 -> 1.20; + delivery 2.99
        PricedCart cart = pricing.priceCart(List.of(line(burger, null, List.of(), 2)), "TEN",
                FulfillmentType.DELIVERY, Buyer.ANONYMOUS);

        assertThat(cart.subtotal()).isEqualByComparingTo("16.60");
        assertThat(cart.couponDiscount()).isEqualByComparingTo("1.66");
        assertThat(cart.tax()).isEqualByComparingTo("1.20");
        assertThat(cart.deliveryFee()).isEqualByComparingTo("2.99");
        assertThat(cart.total()).isEqualByComparingTo("19.13");
    }

    @Test
    void freeDeliveryThresholdUsesNetAfterDiscounts() {
        Coupon c = coupon(12L, "FIVE", DiscountType.FIXED, "5.00");
        when(couponRepo.findByCode("FIVE")).thenReturn(Optional.of(c));

        // 36.00 gross, 31.00 after coupon -> below 35.00 threshold, fee applies
        PricedCart cart = pricing.priceCart(List.of(line(pizza, medium, List.of(stuffedCrust, pepperoni, mushroom), 2)),
                "FIVE", FulfillmentType.DELIVERY, Buyer.ANONYMOUS);

        assertThat(cart.deliveryFee()).isEqualByComparingTo("2.99");
        assertThat(cart.tax()).isEqualByComparingTo("2.48");
        assertThat(cart.total()).isEqualByComparingTo("36.47");
    }

    // ------------------------------------------------------------------ helpers

    private PricedCart quote(CartLineRequest... lines) {
        return pricing.priceCart(List.of(lines), null, FulfillmentType.PICKUP, Buyer.ANONYMOUS);
    }

    private PricedCart quoteWithCoupon(String code, CartLineRequest... lines) {
        return pricing.priceCart(List.of(lines), code, FulfillmentType.PICKUP, Buyer.ANONYMOUS);
    }

    private static CartLineRequest line(Product p, ProductVariant v, List<CustomizationOption> options, int qty) {
        return new CartLineRequest(p.getId(), v == null ? null : v.getId(),
                options.stream().map(CustomizationOption::getId).toList(), null, qty, null);
    }

    private static Category category(Long id, String name) {
        Category c = new Category();
        ReflectionTestUtils.setField(c, "id", id);
        c.setName(name);
        c.setSlug(name.toLowerCase());
        c.setActive(true);
        return c;
    }

    private static Product product(Long id, Category category, String name, String basePrice) {
        Product p = new Product();
        ReflectionTestUtils.setField(p, "id", id);
        p.setCategory(category);
        p.setName(name);
        p.setSlug(name.toLowerCase().replace(' ', '-'));
        p.setBasePrice(new BigDecimal(basePrice));
        return p;
    }

    private static ProductVariant variant(Long id, Product p, String name, String delta, boolean isDefault) {
        ProductVariant v = new ProductVariant();
        ReflectionTestUtils.setField(v, "id", id);
        v.setProduct(p);
        v.setName(name);
        v.setPriceDelta(new BigDecimal(delta));
        v.setDefaultVariant(isDefault);
        p.getVariants().add(v);
        return v;
    }

    private static CustomizationGroup group(Long id, String name, int min, int max) {
        CustomizationGroup g = new CustomizationGroup();
        ReflectionTestUtils.setField(g, "id", id);
        g.setName(name);
        g.setSelectionType(max == 1 ? SelectionType.SINGLE : SelectionType.MULTIPLE);
        g.setMinSelect(min);
        g.setMaxSelect(max);
        return g;
    }

    private static CustomizationOption option(Long id, CustomizationGroup g, String name, String delta, int sort) {
        CustomizationOption o = new CustomizationOption();
        ReflectionTestUtils.setField(o, "id", id);
        o.setGroup(g);
        o.setName(name);
        o.setPriceDelta(new BigDecimal(delta));
        o.setSortOrder(sort);
        g.getOptions().add(o);
        return o;
    }

    private static ComboComponent component(Long id, Product comboProduct, String name, int qty) {
        ComboComponent c = new ComboComponent();
        ReflectionTestUtils.setField(c, "id", id);
        c.setComboProduct(comboProduct);
        c.setName(name);
        c.setQuantity(qty);
        comboProduct.getComboComponents().add(c);
        return c;
    }

    private static void choice(ComboComponent component, Product product, String extra) {
        ComboComponentChoice ch = new ComboComponentChoice();
        ch.setComponent(component);
        ch.setProduct(product);
        ch.setExtraPrice(new BigDecimal(extra));
        component.getChoices().add(ch);
    }

    private static Promotion promotion(Long id, DiscountType type, String value, Category category, Product product) {
        Promotion p = new Promotion();
        ReflectionTestUtils.setField(p, "id", id);
        p.setName("Promo " + id);
        p.setDiscountType(type);
        p.setDiscountValue(new BigDecimal(value));
        p.setCategory(category);
        p.setProduct(product);
        return p;
    }

    private static Coupon coupon(Long id, String code, DiscountType type, String value) {
        Coupon c = new Coupon();
        ReflectionTestUtils.setField(c, "id", id);
        c.setCode(code);
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        return c;
    }
}
