package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.CartDtos.CartLineRequest;
import com.crustcrunch.dto.CartDtos.ComboSelectionRequest;
import com.crustcrunch.dto.OrderDtos.*;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.*;
import com.crustcrunch.exception.*;
import com.crustcrunch.mapper.OrderMapper;
import com.crustcrunch.repository.*;
import com.crustcrunch.security.AuthUser;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.PricingService.Buyer;
import com.crustcrunch.service.PricingService.PricedCart;
import com.crustcrunch.service.PricingService.PricedLine;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{8,80}$");
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final OrderRepository orders;
    private final UserRepository users;
    private final ProductRepository products;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final ReviewRepository reviews;
    private final PricingService pricing;
    private final PaymentService payments;
    private final OrderMapper mapper;
    private final AppProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, UserRepository users, ProductRepository products,
                        CouponRepository coupons, CouponRedemptionRepository redemptions, ReviewRepository reviews,
                        PricingService pricing, PaymentService payments, OrderMapper mapper, AppProperties props,
                        Clock clock, PlatformTransactionManager txManager) {
        this.orders = orders;
        this.users = users;
        this.products = products;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.reviews = reviews;
        this.pricing = pricing;
        this.payments = payments;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    // =================================================================== checkout

    /**
     * Places an order. The {@code idempotencyKey} (generated once per checkout attempt by the client)
     * is unique in the database, so double-clicks, retries and concurrent submits all resolve to the
     * same order instead of creating duplicates.
     */
    public OrderResponse checkout(CheckoutRequest req, String idempotencyKey) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new BadRequestException("A valid Idempotency-Key header (8-80 characters) is required");
        }
        Optional<AuthUser> caller = CurrentUser.get();
        try {
            return tx.execute(status -> placeOrder(req, idempotencyKey, caller.orElse(null)));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent submit using the same key: return the winner's order.
            return tx.execute(status -> orders.findByIdempotencyKey(idempotencyKey)
                    .map(o -> replay(o, req, caller.orElse(null)))
                    .orElseThrow(() -> e));
        }
    }

    private OrderResponse placeOrder(CheckoutRequest req, String key, AuthUser caller) {
        Optional<Order> existing = orders.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), req, caller);
        }

        String email = req.contact().email().trim().toLowerCase();
        Buyer buyer = new Buyer(caller == null ? null : caller.id(), email);
        PricedCart cart = pricing.priceCart(req.items(), req.couponCode(), req.fulfillmentType(), buyer, true);
        if (!cart.valid()) {
            throw new BusinessRuleException(String.join(". ", cart.issues()));
        }
        boolean couponRequested = req.couponCode() != null && !req.couponCode().isBlank();
        if (couponRequested && cart.coupon() == null) {
            throw new BusinessRuleException(cart.couponMessage() != null ? cart.couponMessage() : "Coupon is not valid");
        }

        Instant now = clock.instant();
        Order order = new Order();
        order.setOrderNumber(newOrderNumber(now));
        order.setTrackingToken(randomToken());
        order.setIdempotencyKey(key);
        order.setUser(caller == null ? null : users.getReferenceById(caller.id()));
        order.setContactName(req.contact().name().trim());
        order.setContactEmail(email);
        order.setContactPhone(req.contact().phone().trim());
        order.setFulfillmentType(req.fulfillmentType());
        if (req.fulfillmentType() == FulfillmentType.DELIVERY) {
            order.setAddressLine1(req.address().line1().trim());
            order.setAddressLine2(AuthService.blankToNull(req.address().line2()));
            order.setAddressCity(req.address().city().trim());
            order.setAddressPostalCode(AuthService.blankToNull(req.address().postalCode()));
            order.setDeliveryInstructions(AuthService.blankToNull(req.deliveryInstructions()));
        }
        order.setSpecialInstructions(AuthService.blankToNull(req.specialInstructions()));
        order.setAllergyNotes(AuthService.blankToNull(req.allergyNotes()));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(req.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(cart.subtotal());
        order.setDiscountTotal(cart.discountTotal());
        order.setDeliveryFee(cart.deliveryFee());
        order.setTax(cart.tax());
        order.setTotal(cart.total());
        order.setCouponCode(cart.coupon() == null ? null : cart.coupon().getCode());
        order.setEstimatedReadyAt(estimateReady(cart, req.fulfillmentType(), now));

        for (PricedLine line : cart.lines()) {
            order.addItem(snapshot(line));
        }
        order.addHistory(new OrderStatusHistory(null, OrderStatus.PENDING,
                order.getUser(), "Order placed"));
        orders.saveAndFlush(order);

        if (cart.coupon() != null) {
            Coupon c = cart.coupon();
            c.setRedemptionCount(c.getRedemptionCount() + 1);
            CouponRedemption r = new CouponRedemption();
            r.setCoupon(c);
            r.setOrder(order);
            r.setUser(order.getUser());
            r.setContactEmail(email);
            r.setDiscountAmount(cart.couponDiscount());
            redemptions.save(r);
        }
        cart.lines().forEach(l -> products.incrementSold(l.product().getId(), l.quantity()));
        payments.initiate(order);
        return mapper.toResponse(order, true, List.of());
    }

    /** Same key submitted again: only the original submitter may see the result. */
    private OrderResponse replay(Order o, CheckoutRequest req, AuthUser caller) {
        boolean sameUser = o.getUser() == null ? caller == null : caller != null && o.getUser().getId().equals(caller.id());
        boolean sameEmail = o.getContactEmail().equalsIgnoreCase(req.contact().email().trim());
        if (!sameUser || !sameEmail) {
            throw new ConflictException("This checkout session was already used. Please refresh and try again.");
        }
        return mapper.toResponse(o, true, List.of());
    }

    static OrderItem snapshot(PricedLine line) {
        Product p = line.product();
        OrderItem item = new OrderItem();
        item.setProductId(p.getId());
        item.setCategoryId(p.getCategory().getId());
        item.setProductName(p.getName());
        item.setCategoryName(p.getCategory().getName());
        item.setBasePrice(p.getBasePrice());
        if (line.variant() != null) {
            item.setVariantName(line.variant().getName());
            item.setVariantPriceDelta(line.variant().getPriceDelta());
        }
        item.setUnitPrice(line.unitPrice());
        item.setQuantity(line.quantity());
        item.setDiscount(line.promotionDiscount());
        item.setLineTotal(line.lineTotal());
        item.setNotes(AuthService.blankToNull(line.request().notes()));
        for (CustomizationOption o : line.options()) {
            OrderItemCustomization c = new OrderItemCustomization();
            c.setKind(CustomizationKind.OPTION);
            c.setGroupName(o.getGroup().getName());
            c.setOptionName(o.getName());
            c.setPriceDelta(o.getPriceDelta());
            c.setOptionId(o.getId());
            item.addCustomization(c);
        }
        for (PricingService.ComboPick pick : line.comboPicks()) {
            OrderItemCustomization c = new OrderItemCustomization();
            c.setKind(CustomizationKind.COMBO_CHOICE);
            c.setGroupName(pick.component().getName());
            c.setOptionName(pick.choice().getProduct().getName());
            c.setPriceDelta(pick.choice().getExtraPrice());
            // For combo choices option_id holds the combo component id (used by "reorder").
            c.setOptionId(pick.component().getId());
            c.setChoiceProductId(pick.choice().getProduct().getId());
            item.addCustomization(c);
        }
        return item;
    }

    private Instant estimateReady(PricedCart cart, FulfillmentType type, Instant now) {
        int prep = cart.lines().stream().mapToInt(l -> l.product().getPrepMinutes()).max().orElse(10);
        long queue = orders.countByStatusIn(EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING));
        int queueMinutes = (int) Math.min(30, queue * 2);
        int base = Math.max(prep, props.restaurant().pickupMinutes());
        int minutes = base + queueMinutes + (type == FulfillmentType.DELIVERY
                ? Math.max(0, props.restaurant().deliveryMinutes() - props.restaurant().pickupMinutes()) : 0);
        return now.plusSeconds(minutes * 60L);
    }

    private String newOrderNumber(Instant now) {
        String date = NUMBER_DATE.format(now.atZone(ZoneId.of(props.restaurant().timezone())));
        StringBuilder sb = new StringBuilder("CC").append(date).append('-');
        for (int i = 0; i < 5; i++) {
            sb.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // =================================================================== customer views

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(Long userId, int page, int size) {
        return PageResponse.of(orders.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))), mapper::toSummary);
    }

    /** Owner, or any staff member, may view an order. */
    @Transactional(readOnly = true)
    public OrderResponse getForCaller(Long orderId) {
        AuthUser caller = CurrentUser.require();
        Order o = load(orderId);
        assertCanView(o, caller);
        List<Long> reviewed = caller.isStaff() && !isOwner(o, caller) ? List.of()
                : reviews.findByUserIdAndOrderId(caller.id(), o.getId()).stream()
                .map(r -> r.getProduct().getId()).toList();
        return mapper.toResponse(o, true, reviewed);
    }

    /** Guest tracking: order number + secret token, compared in constant time. */
    @Transactional(readOnly = true)
    public OrderResponse track(String orderNumber, String token) {
        Order o = orders.findByOrderNumber(orderNumber)
                .filter(x -> token != null && java.security.MessageDigest.isEqual(
                        x.getTrackingToken().getBytes(), token.getBytes()))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return mapper.toResponse(o, true, List.of());
    }

    @Transactional
    public OrderResponse cancelByCustomer(Long orderId, String reason) {
        AuthUser caller = CurrentUser.require();
        Order o = load(orderId);
        if (!isOwner(o, caller)) {
            throw new NotFoundException("Order not found");
        }
        if (o.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("This order has already been cancelled");
        }
        if (o.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("This order is already being prepared and can no longer be cancelled online. Please call the restaurant.");
        }
        applyTransition(o, OrderStatus.CANCELLED, users.getReferenceById(caller.id()),
                reason == null || reason.isBlank() ? "Cancelled by customer" : reason.trim());
        return mapper.toResponse(o, true, List.of());
    }

    /** Builds cart lines from a past order using current menu data; unavailable items are reported, not added. */
    @Transactional(readOnly = true)
    public ReorderResponse reorder(Long orderId) {
        AuthUser caller = CurrentUser.require();
        Order o = load(orderId);
        if (!isOwner(o, caller)) {
            throw new NotFoundException("Order not found");
        }
        List<CartLineRequest> candidates = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : o.getItems()) {
            Optional<Product> product = item.getProductId() == null ? Optional.empty() : products.findById(item.getProductId());
            if (product.isEmpty()) {
                skipped.add(item.getProductName() + " is no longer on the menu");
                continue;
            }
            Product p = product.get();
            Long variantId = item.getVariantName() == null ? null : p.getVariants().stream()
                    .filter(v -> v.getName().equals(item.getVariantName())).map(ProductVariant::getId)
                    .findFirst().orElse(null);
            List<Long> optionIds = item.getCustomizations().stream()
                    .filter(c -> c.getKind() == CustomizationKind.OPTION && c.getOptionId() != null)
                    .map(OrderItemCustomization::getOptionId).toList();
            List<ComboSelectionRequest> combo = item.getCustomizations().stream()
                    .filter(c -> c.getKind() == CustomizationKind.COMBO_CHOICE && c.getChoiceProductId() != null)
                    .map(c -> new ComboSelectionRequest(c.getOptionId(), c.getChoiceProductId())).toList();
            candidates.add(new CartLineRequest(p.getId(), variantId, optionIds, combo, item.getQuantity(), item.getNotes()));
        }
        List<ReorderLine> ok = new ArrayList<>();
        if (!candidates.isEmpty()) {
            PricedCart cart = pricing.priceCart(candidates, null, null, Buyer.ANONYMOUS);
            for (PricedLine l : cart.lines()) {
                if (l.valid()) {
                    List<String> summary = new ArrayList<>();
                    if (l.variant() != null) {
                        summary.add(l.variant().getName());
                    }
                    CartService.selections(l).forEach(sel -> summary.add(sel.optionName()));
                    BigDecimal unit = l.unitPrice().subtract(l.quantity() == 0 ? BigDecimal.ZERO
                            : l.promotionDiscount().divide(BigDecimal.valueOf(l.quantity()), 2, java.math.RoundingMode.HALF_UP));
                    Product p = l.product();
                    ok.add(new ReorderLine(l.request(), p.getName(), p.getSlug(), p.getImageUrl(),
                            p.getCategory().getName(), summary, unit));
                } else {
                    skipped.add(l.error());
                }
            }
        }
        return new ReorderResponse(ok, skipped);
    }

    // =================================================================== staff

    public record AdminOrderFilter(String q, OrderStatus status, FulfillmentType fulfillmentType,
                                   PaymentMethod paymentMethod, String category, Instant from, Instant to) {
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> search(AdminOrderFilter f, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createdAt")));
        return PageResponse.of(orders.findAll(adminSpec(f), pageable), mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> kitchenQueue() {
        return orders.findByStatusInOrderByCreatedAtAsc(OrderStatus.KITCHEN_VISIBLE).stream()
                .map(o -> mapper.toResponse(o, false, List.of())).toList();
    }

    /**
     * Staff status change. Kitchen staff may move orders forward; only admins may cancel.
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus target, String note) {
        AuthUser caller = CurrentUser.require();
        if (!caller.isStaff()) {
            throw new ForbiddenException("Only staff can update order status");
        }
        if (target == OrderStatus.CANCELLED && !caller.isAdmin()) {
            throw new ForbiddenException("Only administrators can cancel orders");
        }
        Order o = load(orderId);
        applyTransition(o, target, users.getReferenceById(caller.id()), note);
        return mapper.toResponse(o, true, List.of());
    }

    void applyTransition(Order o, OrderStatus target, User actor, String note) {
        OrderStatus from = o.getStatus();
        if (!from.canTransitionTo(target, o.getFulfillmentType())) {
            throw new BusinessRuleException("Cannot change order from " + from + " to " + target
                    + (o.getFulfillmentType() == FulfillmentType.PICKUP && target == OrderStatus.OUT_FOR_DELIVERY
                    ? " (pickup orders are not delivered)" : ""));
        }
        o.setStatus(target);
        o.addHistory(new OrderStatusHistory(from, target, actor, AuthService.blankToNull(note)));
        if (target == OrderStatus.CANCELLED) {
            o.setCancelReason(AuthService.blankToNull(note));
            releaseCoupon(o);
            o.getItems().stream().filter(i -> i.getProductId() != null)
                    .forEach(i -> products.incrementSold(i.getProductId(), -i.getQuantity()));
            payments.refundIfPaid(o);
        } else if (target == OrderStatus.DELIVERED) {
            payments.markCollected(o);
        }
    }

    private void releaseCoupon(Order o) {
        if (o.getCouponCode() == null) {
            return;
        }
        coupons.findByCodeForUpdate(o.getCouponCode()).ifPresent(c -> {
            c.setRedemptionCount(Math.max(0, c.getRedemptionCount() - 1));
            redemptions.deleteByOrderId(o.getId());
        });
    }

    // =================================================================== helpers

    private Order load(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private static boolean isOwner(Order o, AuthUser u) {
        return o.getUser() != null && o.getUser().getId().equals(u.id());
    }

    private static void assertCanView(Order o, AuthUser u) {
        if (!u.isStaff() && !isOwner(o, u)) {
            // 404 rather than 403 so order ids can't be probed
            throw new NotFoundException("Order not found");
        }
    }

    private static Specification<Order> adminSpec(AdminOrderFilter f) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("orderNumber")), like),
                        cb.like(cb.lower(root.get("contactName")), like),
                        cb.like(cb.lower(root.get("contactEmail")), like),
                        cb.like(root.get("contactPhone"), like)));
            }
            if (f.status() != null) {
                ps.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.fulfillmentType() != null) {
                ps.add(cb.equal(root.get("fulfillmentType"), f.fulfillmentType()));
            }
            if (f.paymentMethod() != null) {
                ps.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
            }
            if (f.from() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            }
            if (f.to() != null) {
                ps.add(cb.lessThan(root.get("createdAt"), f.to()));
            }
            if (f.category() != null && !f.category().isBlank()) {
                var sub = query.subquery(Long.class);
                var item = sub.from(OrderItem.class);
                sub.select(item.get("id")).where(cb.equal(item.get("order"), root),
                        cb.equal(cb.lower(item.get("categoryName")), f.category().trim().toLowerCase()));
                ps.add(cb.exists(sub));
            }
            return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
