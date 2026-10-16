package com.crustcrunch.service;

import com.crustcrunch.dto.CartDtos.CartLineRequest;
import com.crustcrunch.dto.CartDtos.ComboSelectionRequest;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.*;
import com.crustcrunch.repository.OrderRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.ReviewRepository;
import com.crustcrunch.repository.UserRepository;
import com.crustcrunch.service.PricingService.Buyer;
import com.crustcrunch.service.PricingService.PricedCart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Generates realistic order history (opt-in via BOOTSTRAP_DEMO_DATA=true) so the dashboard,
 * kitchen display and review sections have something to show on a fresh install.
 * Orders are priced through the real {@link PricingService}. Demo customers cannot log in.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private static final String DEMO_PREFIX = "CC-DEMO-";
    private static final String[] AREAS = {"Gulshan 2", "Banani", "Dhanmondi", "Uttara Sector 7", "Mohammadpur", "Bashundhara R/A"};
    private static final String[] POSTCODES = {"1212", "1213", "1209", "1230", "1207", "1229"};
    private static final String[][] CUSTOMERS = {
            {"Maya Thompson", "maya.t@example.com"}, {"Daniel Okafor", "daniel.o@example.com"},
            {"Sofia Martinez", "sofia.m@example.com"}, {"Liam Chen", "liam.c@example.com"},
            {"Aisha Rahman", "aisha.r@example.com"}, {"Noah Williams", "noah.w@example.com"},
            {"Emma Rossi", "emma.r@example.com"}, {"Lucas Novak", "lucas.n@example.com"}};
    private static final String[] COMMENTS = {
            "Crispiest chicken in town and it arrived piping hot. Already planning my next order!",
            "The pepperoni cups up perfectly and the crust has a real chew to it. Proper pizza.",
            "Smash burger was juicy with those lacy crispy edges. Crunch sauce is addictive.",
            "Family bucket fed all five of us with leftovers. Great value for a Friday night.",
            "Loaded fries are dangerous — cheese sauce, bacon, jalapeños, all of it. Loved it.",
            "Ordered for pickup and it was ready exactly when the app said. Super smooth.",
            "Shawarma wrap had loads of garlic sauce and the fries inside are a genius move.",
            "Milkshake was thick enough to need a spoon. The brownie on the side was perfect.",
            "Build-your-own pizza let me go wild with toppings and the price was transparent.",
            "Delivery was quick, the driver was friendly, and everything was still crispy."};

    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final PricingService pricing;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final Random rnd = new Random(42);

    public DemoDataService(UserRepository users, ProductRepository products, OrderRepository orders,
                           ReviewRepository reviews, PricingService pricing, PasswordEncoder encoder,
                           JdbcTemplate jdbc, Clock clock, PlatformTransactionManager txManager) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.reviews = reviews;
        this.pricing = pricing;
        this.encoder = encoder;
        this.jdbc = jdbc;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Commits one day of history at a time rather than everything in one transaction: hosted Postgres
     * proxies drop connections mid-way, and a dropped connection should only cost one retried chunk.
     */
    public void generateIfEmpty() {
        // keyed on demo orders, not all orders, so real orders placed in the meantime don't block regeneration
        if (orders.existsByOrderNumberStartingWith(DEMO_PREFIX)) {
            return;
        }
        List<User> customers = inChunk("customers", () -> {
            List<User> out = new ArrayList<>();
            for (String[] c : CUSTOMERS) {
                out.add(users.findByEmailIgnoreCase(c[1]).orElseGet(() -> {
                    User nu = new User();
                    nu.setEmail(c[1]);
                    nu.setFullName(c[0]);
                    nu.setRole(Role.CUSTOMER);
                    // random password nobody knows; demo customers can't sign in
                    nu.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
                    return users.save(nu);
                }));
            }
            return out;
        });
        Instant now = clock.instant();
        int created = 0;
        List<Order> delivered = new ArrayList<>();
        for (int day = 59; day >= 0; day--) {
            int d = day;
            List<Order> made = inChunk("day -" + day, () -> {
                List<Product> menu = orderableMenu();
                List<Order> out = new ArrayList<>();
                int perDay = 2 + rnd.nextInt(d < 14 ? 6 : 4);
                for (int k = 0; k < perDay; k++) {
                    Instant at = now.minus(Duration.ofDays(d)).minus(Duration.ofMinutes(30 + rnd.nextInt(600)));
                    if (d == 0 && at.isAfter(now.minus(Duration.ofMinutes(90)))) {
                        continue;
                    }
                    Order o = buildOrder(customers.get(rnd.nextInt(customers.size())), menu, at, finalStatus(d));
                    if (o != null) {
                        out.add(o);
                    }
                }
                return out;
            });
            created += made.size();
            made.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).forEach(delivered::add);
        }
        // a few live orders for the kitchen display
        created += inChunk("live orders", () -> {
            List<Product> menu = orderableMenu();
            OrderStatus[] live = {OrderStatus.PENDING, OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                    OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP};
            int n = 0;
            for (int i = 0; i < live.length; i++) {
                if (buildOrder(customers.get(i % customers.size()), menu, now.minus(Duration.ofMinutes(4 + i * 6L)), live[i]) != null) {
                    n++;
                }
            }
            return n;
        });
        Collections.shuffle(delivered, rnd);
        int reviewCount = inChunk("reviews", () -> {
            int n = 0;
            for (Order o : delivered.subList(0, Math.min(14, delivered.size()))) {
                OrderItem item = o.getItems().getFirst();
                if (item.getProductId() == null) {
                    continue;
                }
                Review r = new Review();
                r.setUser(o.getUser());
                r.setOrder(o);
                r.setProduct(products.getReferenceById(item.getProductId()));
                r.setRating(rnd.nextInt(10) < 8 ? 5 : 4);
                r.setComment(COMMENTS[n % COMMENTS.length]);
                reviews.save(r);
                n++;
            }
            return n;
        });
        log.info("Demo data: created {} orders and {} reviews", created, reviewCount);
    }

    private List<Product> orderableMenu() {
        return products.findAll().stream().filter(Product::isOrderable).toList();
    }

    private <T> T inChunk(String what, Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return tx.execute(status -> work.get());
            } catch (DataAccessException e) {
                if (attempt >= 3) {
                    throw e;
                }
                log.warn("Demo data: {} failed (attempt {}), retrying: {}", what, attempt, e.getMessage());
            }
        }
    }

    private OrderStatus finalStatus(int daysAgo) {
        if (daysAgo == 0) {
            return rnd.nextInt(5) == 0 ? OrderStatus.OUT_FOR_DELIVERY : OrderStatus.DELIVERED;
        }
        return rnd.nextInt(20) == 0 ? OrderStatus.CANCELLED : OrderStatus.DELIVERED;
    }

    private Order buildOrder(User customer, List<Product> menu, Instant createdAt, OrderStatus target) {
        int lines = 1 + rnd.nextInt(3);
        List<CartLineRequest> cart = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            cart.add(defaultLine(menu.get(rnd.nextInt(menu.size())), 1 + (rnd.nextInt(4) == 0 ? 1 : 0)));
        }
        FulfillmentType ft = rnd.nextBoolean() ? FulfillmentType.DELIVERY : FulfillmentType.PICKUP;
        PricedCart priced = pricing.priceCart(cart, null, ft, Buyer.ANONYMOUS, false);
        if (!priced.valid()) {
            ft = FulfillmentType.PICKUP;
            priced = pricing.priceCart(cart, null, ft, Buyer.ANONYMOUS, false);
            if (!priced.valid()) {
                return null;
            }
        }
        if (target == OrderStatus.OUT_FOR_DELIVERY) {
            ft = FulfillmentType.DELIVERY;
        }
        Order o = new Order();
        o.setOrderNumber(DEMO_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        o.setTrackingToken(UUID.randomUUID().toString().replace("-", ""));
        o.setIdempotencyKey("demo-" + UUID.randomUUID());
        o.setUser(customer);
        o.setContactName(customer.getFullName());
        o.setContactEmail(customer.getEmail());
        o.setContactPhone("+880 17" + (10_000_000 + rnd.nextInt(90_000_000)));
        o.setFulfillmentType(ft);
        if (ft == FulfillmentType.DELIVERY) {
            int area = rnd.nextInt(AREAS.length);
            o.setAddressLine1("House " + (1 + rnd.nextInt(120)) + ", Road " + (1 + rnd.nextInt(40)) + ", " + AREAS[area]);
            o.setAddressCity("Dhaka");
            o.setAddressPostalCode(POSTCODES[area]);
        }
        if (rnd.nextInt(6) == 0) {
            o.setAllergyNotes("Nut allergy — please keep separate");
        }
        if (rnd.nextInt(5) == 0) {
            o.setSpecialInstructions("Extra napkins please");
        }
        o.setPaymentMethod(rnd.nextBoolean() ? PaymentMethod.CASH : PaymentMethod.ONLINE);
        o.setPaymentStatus(o.getPaymentMethod() == PaymentMethod.ONLINE || target == OrderStatus.DELIVERED
                ? PaymentStatus.PAID : PaymentStatus.PENDING);
        if (target == OrderStatus.CANCELLED) {
            o.setPaymentStatus(o.getPaymentMethod() == PaymentMethod.ONLINE ? PaymentStatus.REFUNDED : PaymentStatus.PENDING);
            o.setCancelReason("Customer requested cancellation");
        }
        o.setSubtotal(priced.subtotal());
        o.setDiscountTotal(priced.discountTotal());
        o.setDeliveryFee(priced.deliveryFee());
        o.setTax(priced.tax());
        o.setTotal(priced.total());
        o.setStatus(target);
        o.setEstimatedReadyAt(createdAt.plus(Duration.ofMinutes(ft == FulfillmentType.DELIVERY ? 35 : 15)));
        priced.lines().forEach(l -> o.addItem(OrderService.snapshot(l)));

        OrderStatus prev = null;
        for (OrderStatus s : path(target, ft)) {
            o.addHistory(new OrderStatusHistory(prev, s, null, prev == null ? "Order placed" : null));
            prev = s;
        }
        orders.saveAndFlush(o);
        Timestamp ts = Timestamp.from(createdAt);
        jdbc.update("UPDATE orders SET created_at = ?, updated_at = ? WHERE id = ?", ts, ts, o.getId());
        jdbc.update("UPDATE order_status_history SET created_at = ? WHERE order_id = ?", ts, o.getId());
        o.setCreatedAt(createdAt);
        return o;
    }

    private static List<OrderStatus> path(OrderStatus target, FulfillmentType ft) {
        if (target == OrderStatus.CANCELLED) {
            return List.of(OrderStatus.PENDING, OrderStatus.CANCELLED);
        }
        List<OrderStatus> out = new ArrayList<>();
        OrderStatus s = OrderStatus.PENDING;
        out.add(s);
        while (s != target) {
            s = s.next(ft);
            out.add(s);
        }
        return out;
    }

    private CartLineRequest defaultLine(Product p, int qty) {
        Long variantId = p.getVariants().stream().filter(ProductVariant::isDefaultVariant).map(ProductVariant::getId)
                .findFirst().orElse(null);
        List<Long> options = new ArrayList<>();
        for (ProductCustomizationGroup pcg : p.getCustomizationGroups()) {
            CustomizationGroup g = pcg.getGroup();
            List<CustomizationOption> avail = g.getOptions().stream().filter(CustomizationOption::isAvailable).toList();
            if (g.getMinSelect() > 0) {
                avail.stream().filter(CustomizationOption::isDefaultOption).findFirst()
                        .or(() -> avail.stream().findFirst())
                        .ifPresent(o -> options.add(o.getId()));
            } else if (g.getSelectionType() == SelectionType.MULTIPLE && !avail.isEmpty() && rnd.nextInt(3) == 0) {
                options.add(avail.get(rnd.nextInt(avail.size())).getId());
            }
        }
        List<ComboSelectionRequest> combo = new ArrayList<>();
        if (p.getProductType() == ProductType.COMBO) {
            for (ComboComponent c : p.getComboComponents()) {
                for (int i = 0; i < c.getQuantity(); i++) {
                    ComboComponentChoice ch = c.getChoices().get(rnd.nextInt(c.getChoices().size()));
                    combo.add(new ComboSelectionRequest(c.getId(), ch.getProduct().getId()));
                }
            }
        }
        return new CartLineRequest(p.getId(), variantId, options, combo, qty, null);
    }
}
