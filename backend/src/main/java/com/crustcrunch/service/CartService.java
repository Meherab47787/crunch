package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.CartDtos.*;
import com.crustcrunch.enums.CustomizationKind;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.PricingService.Buyer;
import com.crustcrunch.service.PricingService.PricedCart;
import com.crustcrunch.service.PricingService.PricedLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final PricingService pricing;
    private final AppProperties props;

    public CartService(PricingService pricing, AppProperties props) {
        this.pricing = pricing;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest req) {
        Buyer buyer = CurrentUser.get().map(u -> new Buyer(u.id(), u.email())).orElse(Buyer.ANONYMOUS);
        PricedCart cart = pricing.priceCart(req.items(), req.couponCode(), req.fulfillmentType(), buyer);
        return toResponse(cart, req.couponCode(), props.pricing().freeDeliveryThreshold());
    }

    static QuoteResponse toResponse(PricedCart cart, String requestedCoupon, java.math.BigDecimal freeDeliveryThreshold) {
        List<QuoteLine> lines = cart.lines().stream().map(CartService::toLine).toList();
        return new QuoteResponse(lines, cart.subtotal(), cart.promotionDiscount(), cart.couponDiscount(),
                cart.discountTotal(), cart.deliveryFee(), cart.tax(), cart.total(),
                cart.coupon() != null ? cart.coupon().getCode() : (requestedCoupon == null ? null : requestedCoupon.trim()),
                cart.coupon() != null, cart.couponMessage(), freeDeliveryThreshold, cart.issues(), cart.valid());
    }

    static List<SelectionLine> selections(PricedLine l) {
        List<SelectionLine> out = new ArrayList<>();
        l.options().forEach(o -> out.add(new SelectionLine(CustomizationKind.OPTION, o.getGroup().getName(),
                o.getName(), o.getPriceDelta())));
        l.comboPicks().forEach(p -> out.add(new SelectionLine(CustomizationKind.COMBO_CHOICE, p.component().getName(),
                p.choice().getProduct().getName(), p.choice().getExtraPrice())));
        return out;
    }

    private static QuoteLine toLine(PricedLine l) {
        var p = l.product();
        return new QuoteLine(l.index(), l.request().productId(), p == null ? null : p.getName(),
                p == null ? null : p.getSlug(), p == null ? null : p.getImageUrl(),
                p == null ? null : p.getCategory().getName(), l.variant() == null ? null : l.variant().getName(),
                selections(l), l.unitPrice(), l.quantity(), l.lineSubtotal(), l.promotionDiscount(),
                l.promotion() == null ? null : l.promotion().getName(), l.lineTotal(), l.valid(), l.error());
    }
}
