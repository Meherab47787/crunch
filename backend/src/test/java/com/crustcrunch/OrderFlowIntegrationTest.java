package com.crustcrunch;

import com.crustcrunch.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests against a real PostgreSQL (Testcontainers) with the Flyway seed menu.
 * All tests share one database, so every test uses its own emails / idempotency keys.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    static final String ADMIN_EMAIL = "admin@test.local";
    static final String ADMIN_PASSWORD = "AdminPass123!";
    static final String KITCHEN_EMAIL = "kitchen@test.local";
    static final String KITCHEN_PASSWORD = "KitchenPass123!";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    OrderRepository orderRepository;

    // =================================================================== public menu

    @Nested
    class PublicMenu {

        @Test
        void categoriesAreSeeded() throws Exception {
            JsonNode categories = getJson(get("/api/categories"), 200);

            assertThat(slugs(categories)).contains("pizza", "burgers", "chicken", "sides", "beverages", "combos");
            JsonNode pizza = find(categories, "slug", "pizza");
            assertThat(pizza.get("productCount").asLong()).isEqualTo(7);
        }

        @Test
        void productDetailIncludesVariantsGroupsAndPromotion() throws Exception {
            JsonNode p = product("pepperoni-pizza");

            assertThat(p.at("/summary/name").asText()).isEqualTo("Pepperoni Classic");
            assertThat(p.at("/summary/categorySlug").asText()).isEqualTo("pizza");
            assertThat(p.get("variants")).hasSize(4);
            assertThat(names(p.get("groups"))).containsExactly("Crust", "Pizza Toppings", "Extra Cheese");
            assertThat(p.at("/promotion/name").asText()).isEqualTo("Pizza Week: 20% Off");
        }

        @Test
        void comboDetailListsComponents() throws Exception {
            JsonNode combo = product("burger-combo");

            assertThat(combo.at("/summary/productType").asText()).isEqualTo("COMBO");
            assertThat(names(combo.get("comboComponents")))
                    .containsExactly("Choose your burger", "Choose a side", "Choose a drink");
        }

        @Test
        void unknownProductIs404() throws Exception {
            mvc.perform(get("/api/products/no-such-thing")).andExpect(status().isNotFound());
        }

        @Test
        void searchFiltersByCategory() throws Exception {
            JsonNode page = getJson(get("/api/products").param("category", "pizza").param("size", "50"), 200);

            assertThat(page.get("totalElements").asLong()).isEqualTo(7);
            page.get("content").forEach(p -> assertThat(p.get("categorySlug").asText()).isEqualTo("pizza"));
        }

        @Test
        void searchFiltersByVegetarian() throws Exception {
            JsonNode page = getJson(get("/api/products").param("vegetarian", "true").param("size", "100"), 200);

            assertThat(page.get("content")).isNotEmpty();
            page.get("content").forEach(p -> assertThat(p.get("vegetarian").asBoolean()).isTrue());
            assertThat(slugs(page.get("content"))).contains("margherita-pizza").doesNotContain("pepperoni-pizza");
        }

        @Test
        void searchByTextQuery() throws Exception {
            JsonNode page = getJson(get("/api/products").param("q", "smash").param("size", "50"), 200);

            assertThat(slugs(page.get("content"))).contains("classic-smash-burger").doesNotContain("pepperoni-pizza");
        }

        @Test
        void combinedFilters() throws Exception {
            JsonNode page = getJson(get("/api/products").param("category", "pizza").param("vegetarian", "true")
                    .param("size", "50"), 200);

            assertThat(slugs(page.get("content")))
                    .containsExactlyInAnyOrder("margherita-pizza", "vegetarian-pizza", "build-your-own-pizza");
        }
    }

    // =================================================================== quote

    @Nested
    class Quote {

        @Test
        void quoteRecomputesPricesServerSideAndIgnoresClientPrices() throws Exception {
            Map<String, Object> line = new LinkedHashMap<>(pepperoniLine(1));
            // malicious / stale client-side price fields must be ignored
            line.put("unitPrice", 0.01);
            line.put("price", 0.01);
            line.put("lineTotal", 0.01);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", List.of(line));
            body.put("fulfillmentType", "PICKUP");
            body.put("total", 0.01);
            body.put("subtotal", 0.01);
            body.put("discount", 999);

            JsonNode q = postJson("/api/cart/quote", body, null, 200);

            // Pepperoni 1150 + Medium 300 = 1450; Pizza Week 20% -> 290
            assertThat(q.get("valid").asBoolean()).isTrue();
            assertMoney(q.at("/lines/0/unitPrice"), "1450.00");
            assertMoney(q.at("/lines/0/promotionDiscount"), "290.00");
            assertMoney(q.at("/lines/0/lineTotal"), "1160.00");
            assertThat(q.at("/lines/0/promotionName").asText()).isEqualTo("Pizza Week: 20% Off");
            assertMoney(q.get("subtotal"), "1450.00");
            assertMoney(q.get("deliveryFee"), "0.00");
            // VAT 1160 * 0.05 = 58
            assertMoney(q.get("tax"), "58.00");
            assertMoney(q.get("total"), "1218.00");
        }

        @Test
        void quoteReportsMissingRequiredOption() throws Exception {
            JsonNode p = product("pepperoni-pizza");
            Map<String, Object> line = Map.of("productId", p.at("/summary/id").asLong(), "quantity", 1);

            JsonNode q = postJson("/api/cart/quote", Map.of("items", List.of(line)), null, 200);

            assertThat(q.get("valid").asBoolean()).isFalse();
            assertThat(q.at("/lines/0/error").asText()).isEqualTo("Please choose a crust for Pepperoni Classic");
        }

        @Test
        void comboQuoteIncludesChoiceExtras() throws Exception {
            JsonNode combo = product("burger-combo");
            JsonNode burgerComp = find(combo.get("comboComponents"), "name", "Choose your burger");
            JsonNode sideComp = find(combo.get("comboComponents"), "name", "Choose a side");
            JsonNode drinkComp = find(combo.get("comboComponents"), "name", "Choose a drink");
            long smash = find(burgerComp.get("choices"), "slug", "classic-smash-burger").get("productId").asLong();
            long rings = find(sideComp.get("choices"), "slug", "onion-rings").get("productId").asLong();
            long shake = find(drinkComp.get("choices"), "slug", "milkshake").get("productId").asLong();
            Map<String, Object> line = Map.of(
                    "productId", combo.at("/summary/id").asLong(),
                    "quantity", 1,
                    "comboSelections", List.of(
                            Map.of("componentId", burgerComp.get("id").asLong(), "productId", smash),
                            Map.of("componentId", sideComp.get("id").asLong(), "productId", rings),
                            Map.of("componentId", drinkComp.get("id").asLong(), "productId", shake)));

            JsonNode q = postJson("/api/cart/quote", Map.of("items", List.of(line)), null, 200);

            // 1300 + rings 100 + milkshake 250
            assertThat(q.get("valid").asBoolean()).isTrue();
            assertMoney(q.at("/lines/0/unitPrice"), "1650.00");
        }
    }

    // =================================================================== guest checkout

    @Nested
    class GuestCheckout {

        @Test
        void guestCheckoutIsIdempotentAndTrackable() throws Exception {
            String email = uniqueEmail("guest");
            String key = uniqueKey();
            Map<String, Object> body = pickupCheckout(email, List.of(pepperoniLine(1)), null);

            JsonNode order = checkout(body, key, null, 201);

            long id = order.get("id").asLong();
            String orderNumber = order.get("orderNumber").asText();
            String token = order.get("trackingToken").asText();
            assertThat(token).isNotBlank();
            assertThat(order.get("status").asText()).isEqualTo("PENDING");
            assertMoney(order.get("total"), "1218.00");

            // replay with the same key and body: same order, no new row
            long before = orderRepository.count();
            JsonNode replay = checkout(body, key, null, 201);
            assertThat(replay.get("id").asLong()).isEqualTo(id);
            assertThat(replay.get("orderNumber").asText()).isEqualTo(orderNumber);
            assertThat(orderRepository.count()).isEqualTo(before);

            // same key, different submitter
            checkout(pickupCheckout(uniqueEmail("intruder"), List.of(pepperoniLine(1)), null), key, null, 409);
            assertThat(orderRepository.count()).isEqualTo(before);

            // tracking
            JsonNode tracked = getJson(get("/api/orders/track/{n}", orderNumber).param("token", token), 200);
            assertThat(tracked.get("id").asLong()).isEqualTo(id);
            mvc.perform(get("/api/orders/track/{n}", orderNumber).param("token", "wrong-" + token))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/api/orders/track/{n}", "CC000000-XXXXX").param("token", token))
                    .andExpect(status().isNotFound());
        }

        @Test
        void missingIdempotencyKeyIsRejected() throws Exception {
            mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(pickupCheckout(uniqueEmail("nokey"), List.of(pepperoniLine(1)), null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Missing required header: Idempotency-Key"));
        }

        @Test
        void malformedIdempotencyKeyIsRejected() throws Exception {
            checkout(pickupCheckout(uniqueEmail("badkey"), List.of(pepperoniLine(1)), null), "short", null, 400);
        }

        @Test
        void deliveryWithoutAddressIsRejected() throws Exception {
            Map<String, Object> body = pickupCheckout(uniqueEmail("noaddr"), List.of(pepperoniLine(2)), null);
            body.put("fulfillmentType", "DELIVERY");

            JsonNode err = checkout(body, uniqueKey(), null, 400);

            assertThat(err.at("/fieldErrors/address").asText()).isEqualTo("A delivery address is required for delivery orders");
        }

        @Test
        void deliveryWithAddressSucceeds() throws Exception {
            Map<String, Object> body = pickupCheckout(uniqueEmail("deliver"), List.of(pepperoniLine(1)), null);
            body.put("fulfillmentType", "DELIVERY");
            body.put("address", Map.of("line1", "House 12, Road 5, Banani", "city", "Dhaka"));

            JsonNode order = checkout(body, uniqueKey(), null, 201);

            assertThat(order.get("fulfillmentType").asText()).isEqualTo("DELIVERY");
            // 1160 < 1500 -> delivery fee applies; there is no minimum order for delivery
            assertMoney(order.get("deliveryFee"), "60.00");
        }

        @Test
        void guestCanUseCouponOncePerEmail() throws Exception {
            String email = uniqueEmail("guestcoupon");
            JsonNode order = checkout(pickupCheckout(email, List.of(pepperoniLine(2)), "WELCOME10"), uniqueKey(), null, 201);
            assertThat(order.get("couponCode").asText()).isEqualTo("WELCOME10");

            // second order with the same email is rejected (per-customer limit)
            JsonNode err = checkout(pickupCheckout(email, List.of(pepperoniLine(2)), "WELCOME10"), uniqueKey(), null, 422);
            assertThat(err.get("message").asText()).isEqualTo("You've already used this coupon");
        }

        @Test
        void invalidCartIsRejectedAtCheckout() throws Exception {
            JsonNode p = product("pepperoni-pizza");
            Map<String, Object> noCrust = Map.of("productId", p.at("/summary/id").asLong(), "quantity", 1);

            checkout(pickupCheckout(uniqueEmail("invalid"), List.of(noCrust), null), uniqueKey(), null, 422);
        }
    }

    // =================================================================== kitchen

    @Nested
    class Kitchen {

        @Test
        void kitchenCanAdvanceButNotCancelOrAccessAdmin() throws Exception {
            String kitchen = login(KITCHEN_EMAIL, KITCHEN_PASSWORD);
            long id = checkout(pickupCheckout(uniqueEmail("kq"), List.of(pepperoniLine(1)), null), uniqueKey(), null, 201)
                    .get("id").asLong();

            JsonNode queue = getJson(authed(get("/api/kitchen/orders"), kitchen), 200);
            assertThat(ids(queue)).contains(id);

            assertThat(setStatus("kitchen", id, "CONFIRMED", kitchen, 200).get("status").asText()).isEqualTo("CONFIRMED");
            assertThat(setStatus("kitchen", id, "PREPARING", kitchen, 200).get("status").asText()).isEqualTo("PREPARING");

            setStatus("kitchen", id, "CANCELLED", kitchen, 403);
            mvc.perform(authed(get("/api/admin/dashboard"), kitchen)).andExpect(status().isForbidden());
            mvc.perform(authed(get("/api/admin/orders"), kitchen)).andExpect(status().isForbidden());
            mvc.perform(authed(patch("/api/admin/orders/{id}/status", id), kitchen)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                    .andExpect(status().isForbidden());

            // invalid jump PREPARING -> DELIVERED
            JsonNode err = setStatus("kitchen", id, "DELIVERED", kitchen, 422);
            assertThat(err.get("message").asText()).contains("Cannot change order from PREPARING to DELIVERED");
        }

        @Test
        void kitchenCanMoveATicketBackOneStepAndForwardAgain() throws Exception {
            String kitchen = login(KITCHEN_EMAIL, KITCHEN_PASSWORD);
            long id = checkout(pickupCheckout(uniqueEmail("kb"), List.of(pepperoniLine(1)), null), uniqueKey(), null, 201)
                    .get("id").asLong();
            setStatus("kitchen", id, "CONFIRMED", kitchen, 200);
            setStatus("kitchen", id, "PREPARING", kitchen, 200);
            JsonNode ready = setStatus("kitchen", id, "READY_FOR_PICKUP", kitchen, 200);
            assertThat(ready.get("previousStatus").asText()).isEqualTo("PREPARING");

            JsonNode back = setStatus("kitchen", id, "PREPARING", kitchen, 200);
            assertThat(back.get("status").asText()).isEqualTo("PREPARING");
            assertThat(back.get("previousStatus").asText()).isEqualTo("CONFIRMED");
            assertThat(back.get("nextStatus").asText()).isEqualTo("READY_FOR_PICKUP");
            assertThat(back.get("history")).hasSize(5); // placed, confirmed, preparing, ready, back to preparing

            // two steps back at once is not allowed
            JsonNode err = setStatus("kitchen", id, "PENDING", kitchen, 422);
            assertThat(err.get("message").asText()).contains("Cannot change order from PREPARING to PENDING");

            // and the order can carry on through to completion afterwards
            setStatus("kitchen", id, "READY_FOR_PICKUP", kitchen, 200);
            assertThat(setStatus("kitchen", id, "DELIVERED", kitchen, 200).get("status").asText()).isEqualTo("DELIVERED");
        }

        @Test
        void aDeliveredOrderCannotBeMovedBack() throws Exception {
            String kitchen = login(KITCHEN_EMAIL, KITCHEN_PASSWORD);
            long id = checkout(pickupCheckout(uniqueEmail("kd"), List.of(pepperoniLine(1)), null), uniqueKey(), null, 201)
                    .get("id").asLong();
            for (String st : List.of("CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "DELIVERED")) {
                setStatus("kitchen", id, st, kitchen, 200);
            }
            setStatus("kitchen", id, "READY_FOR_PICKUP", kitchen, 422);
        }

        @Test
        void pickupOrderCannotGoOutForDelivery() throws Exception {
            String kitchen = login(KITCHEN_EMAIL, KITCHEN_PASSWORD);
            long id = checkout(pickupCheckout(uniqueEmail("kp"), List.of(pepperoniLine(1)), null), uniqueKey(), null, 201)
                    .get("id").asLong();
            setStatus("kitchen", id, "CONFIRMED", kitchen, 200);
            setStatus("kitchen", id, "PREPARING", kitchen, 200);
            setStatus("kitchen", id, "READY_FOR_PICKUP", kitchen, 200);

            JsonNode err = setStatus("kitchen", id, "OUT_FOR_DELIVERY", kitchen, 422);
            assertThat(err.get("message").asText()).contains("pickup orders are not delivered");
        }

        @Test
        void customersCannotUseKitchenEndpoints() throws Exception {
            String customer = register(uniqueEmail("notkitchen"));
            mvc.perform(authed(get("/api/kitchen/orders"), customer)).andExpect(status().isForbidden());
            mvc.perform(get("/api/kitchen/orders")).andExpect(status().isUnauthorized());
        }
    }

    // =================================================================== customer

    @Nested
    class Customer {

        @Test
        void couponLimitAndCancellationRestoresEligibility() throws Exception {
            String email = uniqueEmail("cust");
            String token = register(email);

            JsonNode order = checkout(pickupCheckout(email, List.of(pepperoniLine(2)), "WELCOME10"), uniqueKey(), token, 201);
            long id = order.get("id").asLong();
            assertThat(order.get("couponCode").asText()).isEqualTo("WELCOME10");
            assertThat(order.get("customerId").isNull()).isFalse();
            // 2 x 1450 = 2900; promo 2 x 290 = 580; after promo 2320; WELCOME10 = 232
            assertMoney(order.get("subtotal"), "2900.00");
            assertMoney(order.get("discountTotal"), "812.00");

            // the order shows up in "my orders"
            JsonNode mine = getJson(authed(get("/api/orders/my"), token), 200);
            assertThat(ids(mine.get("content"))).containsExactly(id);

            // per-user limit now blocks reuse in a quote
            JsonNode q = quote(List.of(pepperoniLine(2)), "WELCOME10", token);
            assertThat(q.get("couponApplied").asBoolean()).isFalse();
            assertThat(q.get("couponMessage").asText()).isEqualTo("You've already used this coupon");

            // another customer can't see or cancel it
            String other = register(uniqueEmail("other"));
            mvc.perform(authed(get("/api/orders/{id}", id), other)).andExpect(status().isNotFound());
            mvc.perform(authed(post("/api/orders/{id}/cancel", id), other)).andExpect(status().isNotFound());
            // and anonymous callers must authenticate
            mvc.perform(get("/api/orders/{id}", id)).andExpect(status().isUnauthorized());

            // owner can view and cancel while PENDING
            assertThat(getJson(authed(get("/api/orders/{id}", id), token), 200).get("cancellableByCustomer").asBoolean())
                    .isTrue();
            JsonNode cancelled = postJson("/api/orders/" + id + "/cancel", Map.of("reason", "Changed my mind"), token, 200);
            assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
            assertThat(cancelled.get("cancelReason").asText()).isEqualTo("Changed my mind");

            // coupon is usable again
            JsonNode again = quote(List.of(pepperoniLine(2)), "WELCOME10", token);
            assertThat(again.get("couponApplied").asBoolean()).isTrue();
            assertMoney(again.get("couponDiscount"), "232.00");

            // can't cancel twice
            postJson("/api/orders/" + id + "/cancel", Map.of(), token, 422);
        }

        @Test
        void customerCannotCancelOnceConfirmed() throws Exception {
            String email = uniqueEmail("late");
            String token = register(email);
            long id = checkout(pickupCheckout(email, List.of(pepperoniLine(1)), null), uniqueKey(), token, 201)
                    .get("id").asLong();
            setStatus("admin", id, "CONFIRMED", login(ADMIN_EMAIL, ADMIN_PASSWORD), 200);

            postJson("/api/orders/" + id + "/cancel", Map.of(), token, 422);
        }

        @Test
        void reviewOnlyAllowedAfterDelivery() throws Exception {
            String email = uniqueEmail("reviewer");
            String token = register(email);
            JsonNode order = checkout(pickupCheckout(email, List.of(pepperoniLine(1)), null), uniqueKey(), token, 201);
            long orderId = order.get("id").asLong();
            long productId = order.at("/items/0/productId").asLong();
            Map<String, Object> review = Map.of("orderId", orderId, "productId", productId, "rating", 5,
                    "comment", "Great crust");

            postJson("/api/reviews", review, token, 422);

            String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            for (String s : List.of("CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "DELIVERED")) {
                setStatus("admin", orderId, s, admin, 200);
            }

            JsonNode created = postJson("/api/reviews", review, token, 201);
            assertThat(created.get("rating").asInt()).isEqualTo(5);
            assertThat(created.get("productId").asLong()).isEqualTo(productId);

            postJson("/api/reviews", review, token, 409);

            // reviewed product is reported on the order
            JsonNode reloaded = getJson(authed(get("/api/orders/{id}", orderId), token), 200);
            assertThat(reloaded.get("reviewedProductIds").get(0).asLong()).isEqualTo(productId);
        }

        @Test
        void cannotReviewSomeoneElsesOrder() throws Exception {
            String email = uniqueEmail("owner");
            String token = register(email);
            JsonNode order = checkout(pickupCheckout(email, List.of(pepperoniLine(1)), null), uniqueKey(), token, 201);
            long orderId = order.get("id").asLong();
            String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            for (String s : List.of("CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "DELIVERED")) {
                setStatus("admin", orderId, s, admin, 200);
            }

            String other = register(uniqueEmail("stranger"));
            postJson("/api/reviews", Map.of("orderId", orderId, "productId", order.at("/items/0/productId").asLong(),
                    "rating", 1), other, 422);
        }
    }

    // =================================================================== admin

    @Nested
    class Admin {

        @Test
        void dashboardReturnsTotals() throws Exception {
            checkout(pickupCheckout(uniqueEmail("dash"), List.of(pepperoniLine(1)), null), uniqueKey(), null, 201);
            String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

            JsonNode stats = getJson(authed(get("/api/admin/dashboard"), admin), 200);

            assertThat(stats.get("totalOrders").asLong()).isGreaterThanOrEqualTo(1);
            assertThat(stats.get("totalRevenue").decimalValue()).isPositive();
            assertThat(stats.get("statusCounts").isObject()).isTrue();
            assertThat(stats.get("recentOrders").isArray()).isTrue();
        }

        @Test
        void anonymousAndCustomersCannotAccessAdmin() throws Exception {
            mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
            String customer = register(uniqueEmail("nosy"));
            mvc.perform(authed(get("/api/admin/dashboard"), customer)).andExpect(status().isForbidden());
        }

        @Test
        void createdProductAppearsPubliclyAndCanBeSoldOut() throws Exception {
            String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            JsonNode category = postJson("/api/admin/categories", Map.of(
                    "name", "Test Bowls " + suffix, "slug", "test-bowls-" + suffix,
                    "description", "Created by a test", "sortOrder", 50, "active", true), admin, 201);
            long categoryId = category.get("id").asLong();

            JsonNode group = postJson("/api/admin/customization-groups", Map.of(
                    "name", "Base " + suffix, "selectionType", "SINGLE", "minSelect", 1, "maxSelect", 1,
                    "options", List.of(
                            Map.of("name", "Rice", "priceDelta", 0, "available", true, "sortOrder", 1),
                            Map.of("name", "Quinoa", "priceDelta", 0.50, "available", true, "sortOrder", 2))),
                    admin, 201);
            long groupId = group.at("/group/id").asLong();
            long quinoaId = find(group.at("/group/options"), "name", "Quinoa").get("id").asLong();

            String slug = "test-bowl-" + suffix;
            Map<String, Object> productReq = new LinkedHashMap<>();
            productReq.put("categoryId", categoryId);
            productReq.put("name", "Test Bowl " + suffix);
            productReq.put("slug", slug);
            productReq.put("description", "A bowl for testing");
            productReq.put("basePrice", 7.00);
            productReq.put("productType", "STANDARD");
            productReq.put("vegetarian", true);
            productReq.put("prepMinutes", 5);
            productReq.put("available", true);
            productReq.put("visible", true);
            productReq.put("variants", List.of(
                    Map.of("name", "Regular", "priceDelta", 0, "defaultVariant", true, "available", true, "sortOrder", 1),
                    Map.of("name", "Large", "priceDelta", 2.00, "defaultVariant", false, "available", true, "sortOrder", 2)));
            productReq.put("customizationGroupIds", List.of(groupId));
            JsonNode created = postJson("/api/admin/products", productReq, admin, 201);
            long productId = created.at("/summary/id").asLong();

            // visible on the storefront
            JsonNode detail = product(slug);
            assertThat(detail.at("/summary/id").asLong()).isEqualTo(productId);
            assertThat(names(detail.get("variants"))).containsExactly("Regular", "Large");
            assertThat(names(detail.get("groups"))).containsExactly("Base " + suffix);
            JsonNode listed = getJson(get("/api/products").param("category", "test-bowls-" + suffix), 200);
            assertThat(slugs(listed.get("content"))).containsExactly(slug);

            long largeId = find(detail.get("variants"), "name", "Large").get("id").asLong();
            Map<String, Object> line = Map.of("productId", productId, "variantId", largeId,
                    "optionIds", List.of(quinoaId), "quantity", 1);
            JsonNode ok = quote(List.of(line), null, null);
            assertThat(ok.get("valid").asBoolean()).isTrue();
            assertMoney(ok.at("/lines/0/unitPrice"), "9.50");

            // mark sold out
            mvc.perform(authed(patch("/api/admin/products/{id}/availability", productId), admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));

            JsonNode soldOut = quote(List.of(line), null, null);
            assertThat(soldOut.get("valid").asBoolean()).isFalse();
            assertThat(soldOut.at("/lines/0/valid").asBoolean()).isFalse();
            assertThat(soldOut.at("/lines/0/error").asText()).isEqualTo("Test Bowl " + suffix + " is sold out right now");
            // still visible on the menu, flagged unavailable
            assertThat(product(slug).at("/summary/available").asBoolean()).isFalse();
        }

        @Test
        void adminCanCancelAndCouponIsReleased() throws Exception {
            String email = uniqueEmail("admincancel");
            JsonNode order = checkout(pickupCheckout(email, List.of(pepperoniLine(2)), "WELCOME10"), uniqueKey(), null, 201);
            long id = order.get("id").asLong();
            String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

            JsonNode cancelled = setStatus("admin", id, "CANCELLED", admin, 200);
            assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");

            // the guest can use the coupon again
            checkout(pickupCheckout(email, List.of(pepperoniLine(2)), "WELCOME10"), uniqueKey(), null, 201);
        }
    }

    // =================================================================== helpers

    private Map<String, Object> pepperoniLine(int qty) throws Exception {
        JsonNode p = product("pepperoni-pizza");
        long variantId = find(p.get("variants"), "name", "Medium 12\"").get("id").asLong();
        JsonNode crust = find(p.get("groups"), "name", "Crust");
        long classic = find(crust.get("options"), "name", "Classic Hand-Tossed").get("id").asLong();
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("productId", p.at("/summary/id").asLong());
        line.put("variantId", variantId);
        line.put("optionIds", List.of(classic));
        line.put("quantity", qty);
        return line;
    }

    private Map<String, Object> pickupCheckout(String email, List<Map<String, Object>> items, String coupon) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("fulfillmentType", "PICKUP");
        body.put("contact", Map.of("name", "Test Customer", "email", email, "phone", "+1 555 123 4567"));
        body.put("paymentMethod", "CASH");
        if (coupon != null) {
            body.put("couponCode", coupon);
        }
        return body;
    }

    private JsonNode checkout(Map<String, Object> body, String key, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder req = post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key).content(toJson(body));
        return perform(token == null ? req : authed(req, token), expectedStatus);
    }

    private JsonNode quote(List<Map<String, Object>> items, String coupon, String token) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("fulfillmentType", "PICKUP");
        if (coupon != null) {
            body.put("couponCode", coupon);
        }
        return postJson("/api/cart/quote", body, token, 200);
    }

    private JsonNode setStatus(String area, long orderId, String status, String token, int expected) throws Exception {
        return perform(authed(patch("/api/" + area + "/orders/{id}/status", orderId), token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("status", status))), expected);
    }

    private JsonNode product(String slug) throws Exception {
        return getJson(get("/api/products/{slug}", slug), 200);
    }

    private String register(String email) throws Exception {
        return postJson("/api/auth/register", Map.of("email", email, "password", "CustomerPass1!",
                "fullName", "Test Customer"), null, 201).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        return postJson("/api/auth/login", Map.of("email", email, "password", password), null, 200)
                .get("token").asText();
    }

    private JsonNode postJson(String url, Object body, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder req = post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        return perform(token == null ? req : authed(req, token), expectedStatus);
    }

    private JsonNode getJson(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        return perform(req, expectedStatus);
    }

    private JsonNode perform(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(req).andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus())
                .as("HTTP status of %s %s, body: %s", result.getRequest().getMethod(),
                        result.getRequest().getRequestURI(), content)
                .isEqualTo(expectedStatus);
        return content.isEmpty() ? json.nullNode() : json.readTree(content);
    }

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String token) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String toJson(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String uniqueKey() {
        return "test-" + UUID.randomUUID();
    }

    private static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.path(field).asText())) {
                return n;
            }
        }
        throw new AssertionError("No element with " + field + "=" + value + " in " + array);
    }

    private static List<String> slugs(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get("slug").asText()));
        return out;
    }

    private static List<String> names(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get("name").asText()));
        return out;
    }

    private static List<Long> ids(JsonNode array) {
        List<Long> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get("id").asLong()));
        return out;
    }

    private static void assertMoney(JsonNode node, String expected) {
        assertThat(node.isNumber()).as("expected a number but got %s", node).isTrue();
        assertThat(node.decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
    }
}
