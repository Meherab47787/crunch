# Crust & Crunch — Fast-Food Ordering Platform

A full-stack restaurant platform: a branded storefront for ordering pizza, burgers, fried chicken and more; a customizable menu engine; delivery & pickup checkout; live order tracking; an admin dashboard; and a tablet-friendly kitchen display.

| Layer    | Stack |
|----------|-------|
| Frontend | Angular 21 (standalone components, signals, zoneless), Reactive Forms, Tailwind CSS v4, Lucide icons, Vitest, Playwright |
| Backend  | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator), JWT (jjwt), springdoc OpenAPI, Maven |
| Data     | PostgreSQL 17, Flyway migrations |
| Ops      | Dockerfiles for both apps, Docker Compose with health checks and persistent volumes |

---

## Quick start (Docker)

```bash
cd fastfood-restaurant
cp .env.example .env          # then set POSTGRES_PASSWORD, JWT_SECRET, ADMIN_* and KITCHEN_* values
docker compose up --build
```

| URL | What |
|-----|------|
| http://localhost:8081 | Storefront (nginx serves Angular and proxies `/api` to the backend) |
| http://localhost:8081/admin | Admin dashboard (sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD`) |
| http://localhost:8081/kitchen | Kitchen display (`KITCHEN_EMAIL` / `KITCHEN_PASSWORD`, or an admin) |
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/actuator/health | Health check |

With `DEMO_DATA=true`, the first start on an empty database generates about 60 days of realistic orders and reviews. This gives the dashboard, charts and kitchen board something to show. The demo customers can't sign in.

Coupons in the seed data: `WELCOME10` (10% off over $15, once per customer), `SAVE5` ($5 off $30+), `COMBO15` (15% off combos). An automatic "Pizza Week" 20% promotion is also live.

## Local development

Prerequisites: JDK 21+, Node 22+, Docker (for PostgreSQL and the integration tests).

```bash
# 1. Database
docker run -d --name cc-pg -p 5432:5432 \
  -e POSTGRES_DB=crustcrunch -e POSTGRES_USER=crustcrunch -e POSTGRES_PASSWORD=devpass postgres:17-alpine

# 2. Backend (http://localhost:8080). The Maven wrapper downloads Maven on first use.
cd backend
DB_PASSWORD=devpass JWT_SECRET=$(openssl rand -base64 48) \
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD='ChangeMe123!' \
KITCHEN_EMAIL=kitchen@example.com KITCHEN_PASSWORD='ChangeMe123!' DEMO_DATA=true \
./mvnw spring-boot:run

# 3. Frontend (http://localhost:4200, proxies /api and /uploads to :8080)
cd ../frontend
npm install
npm start
```

---

## Architecture

```
fastfood-restaurant/
├── backend/                     Spring Boot API
│   └── src/main/java/com/crustcrunch/
│       ├── controller/ (+admin/)  REST endpoints (thin: validate → delegate)
│       ├── service/               Business logic, transactions, PricingService
│       ├── repository/            Spring Data JPA (+ Specifications for search)
│       ├── entity/                JPA entities
│       ├── dto/                   Request/response records
│       ├── mapper/                Entity → DTO mapping
│       ├── security/              JWT filter, SecurityConfig, principal
│       ├── payment/               PaymentGateway abstraction (cash, simulated online)
│       ├── validation/            Custom Bean Validation constraints
│       ├── exception/             Typed API exceptions + global handler
│       ├── enums/                 OrderStatus state machine, roles, …
│       └── config/                Properties, OpenAPI, bootstrap, static uploads
├── frontend/                    Angular app (see frontend/README.md for conventions)
│   └── src/
│       ├── styles.css + styles/ global design system: theme tokens, buttons, forms, surfaces, print
│       └── app/         every component = name.component.ts + .html (+ .css when needed)
│           ├── core/        models, services (cart, auth, orders, admin…), guards, interceptor, utils
│           ├── shared/      reusable UI (product card, modal, toasts, skeletons, badges…)
│           ├── layout/      storefront shell + cart drawer
│           └── features/    home, menu, product (customizer), cart, checkout, orders, auth, account, admin, kitchen
├── docker-compose.yml
└── .env.example
```

### Key decisions

**1. One flexible menu model, no table per food type.** Every item is a row in `products`. Its customizations are composed from three generic building blocks:

| Building block | Tables | Used for |
|---|---|---|
| **Variants**: mutually exclusive sizes or portions with a price delta | `product_variants` | Pizza 10"/12"/14"/16", 2/4/8-piece chicken, drink sizes, pasta Regular/Large |
| **Customization groups**: reusable option sets with `SINGLE`/`MULTIPLE` selection, `min`/`max` and per-option price deltas, attached to products in order | `customization_groups`, `customization_options`, `product_customization_groups` | Crust, toppings, extra cheese, patty, sauces, spice level, bread/wrap, dips, ice, "make it a meal" |
| **Combo components**: slots like "Choose a side" (quantity *n*) with eligible products and optional upcharges | `combo_components`, `combo_component_choices` | Burger combo, pizza night, family bucket |

A new kind of food only needs data, not code. On the frontend, a single `ProductCustomizer` plus two small renderers (`option-group`, `combo-slot`) handle every product.

**2. The server is the only source of prices.** Cart lines hold only IDs and quantities (`productId`, `variantId`, `optionIds`, `comboSelections`). `PricingService` re-reads products, options, promotions and coupons for every `POST /api/cart/quote` and again at checkout. It validates availability, group min/max, combo slot counts and option ownership. Any price fields a client sends are ignored. The cart is stored in `localStorage` (it survives refresh and works for guests). Every change triggers a debounced re-quote, so totals, deals and "sold out" errors always come from the backend.

**3. Order snapshots.** `order_items` and `order_item_customizations` copy the product name, category, base price, variant, each option or combo choice with its price delta, quantity, discount and line total at purchase time. Menu edits and deletions never change historical orders. Product and category FKs are `ON DELETE SET NULL`.

**4. Enforced lifecycle.** `OrderStatus.canTransitionTo()` is the single rule set:

```
PENDING → CONFIRMED → PREPARING → READY_FOR_PICKUP → DELIVERED            (pickup)
                                                  → OUT_FOR_DELIVERY → DELIVERED   (delivery)
PENDING / CONFIRMED / PREPARING → CANCELLED
```

Customers may cancel only while an order is `PENDING`. Kitchen staff can advance orders but not cancel them. Admins can do both. Every change is written to `order_status_history`. A cancellation releases the coupon redemption, reverses sales counters and refunds paid online orders through the gateway. Delivering a cash order marks it paid.

**5. Duplicate-submission protection.** Checkout requires an `Idempotency-Key` header. The client creates one key per checkout attempt and keeps it in `sessionStorage` for retries. The column has a unique constraint. Repeated or concurrent submits with the same key return the original order, and a key reused with a different customer returns `409`. Coupon rows are locked (`SELECT … FOR UPDATE`) while redeeming, so redemption limits can't be exceeded under concurrency.

**6. Payments.** `PaymentGateway` is the integration point. It ships with Cash-on-delivery and a **simulated** online gateway (approves everything, issues a reference). The server never sees card data. To add Stripe or a similar provider, implement `PaymentGateway` for `ONLINE` with the provider's hosted checkout and set `PAYMENTS_SIMULATED=false`.

**7. Live status updates.** Order pages poll every 15 s while the order is active (paused while the tab is hidden), and the kitchen board refreshes every 10 s. This is simple and robust behind any proxy. SSE or WebSockets could replace it later without changing the API shape.

**8. Guest tracking.** Each order gets a random `tracking_token`. Guests track at `/orders/{orderNumber}?token=…`, the token is compared in constant time, and recent guest orders are remembered on the device.

### Security

- JWT bearer auth (HS256, secret from `JWT_SECRET`, at least 32 bytes, startup fails otherwise). BCrypt (cost 12) password hashing. The user is reloaded on each request, so disabling an account or changing a role takes effect immediately.
- Roles: `CUSTOMER`, `KITCHEN_STAFF`, `ADMIN`. `/api/admin/**` requires ADMIN and `/api/kitchen/**` requires ADMIN or KITCHEN_STAFF. Public registration always creates customers. Admins cannot demote themselves or remove the last admin.
- Ownership checks: another user's order returns `404` (not `403`) so IDs can't be probed. Reviews are allowed only for products in your own delivered orders, once per order.
- Server-side validation (Bean Validation + custom constraints) and a uniform JSON error body (`timestamp, status, message, path, fieldErrors`). Stack traces are never exposed.
- Explicit CORS allow-list (`CORS_ALLOWED_ORIGINS`). In Docker the browser talks same-origin through nginx.
- No credentials in the repo. The first admin and kitchen accounts are created from env vars on first start.
- Uploads are limited to 5 MB JPEG/PNG/WebP, the magic number is checked and files get random names.

---

## API overview

Full, interactive documentation is at `/swagger-ui.html`.

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Profile | `GET/PUT /api/me`, `POST /api/me/password`, `GET/POST/PUT/DELETE /api/me/addresses` |
| Menu (public) | `GET /api/categories`, `GET /api/products?q&category&minPrice&maxPrice&vegetarian&spicy&available&allergenFree&featured&bestseller&newArrival&type&sort&page&size`, `GET /api/products/{slug}`, `GET /api/products/{slug}/reviews`, `GET /api/reviews/highlights`, `GET /api/promotions`, `GET /api/settings` |
| Cart | `POST /api/cart/quote` (validates and prices the cart, coupon, delivery fee and tax) |
| Orders | `POST /api/orders` (+ `Idempotency-Key`), `GET /api/orders/my`, `GET /api/orders/{id}`, `GET /api/orders/track/{number}?token=`, `POST /api/orders/{id}/cancel`, `POST /api/orders/{id}/reorder`, `POST /api/reviews` |
| Kitchen | `GET /api/kitchen/orders`, `PATCH /api/kitchen/orders/{id}/status` |
| Admin | `/api/admin/dashboard`, `/api/admin/orders`, `/api/admin/categories`, `/api/admin/products` (+ `/availability`), `/api/admin/customization-groups`, `/api/admin/uploads`, `/api/admin/coupons`, `/api/admin/promotions`, `/api/admin/customers`, `/api/admin/staff`, `/api/admin/users/{id}` |

Status codes: `400` validation, `401` unauthenticated, `403` forbidden, `404` not found or not yours, `409` conflict (duplicate email/code/review, reused idempotency key), `422` business rule (sold out, invalid transition, coupon invalid).

## Database

Migrations live in `backend/src/main/resources/db/migration`:

- `V1__init_schema.sql`: tables, FKs, check constraints and indexes for users, addresses, categories, products, images, variants, customization groups/options and their product links, combos, promotions, coupons, orders, order items and customizations, status history, payments, coupon redemptions and reviews. Money is `NUMERIC(10,2)`.
- `V2__seed_menu.sql`: the 10 categories, 46 products, 18 reusable option groups, variants, combos, promotions and coupons. It is ordinary data that can be edited from the admin dashboard.

Hibernate runs with `ddl-auto=validate`, so the entities are checked against the migrated schema on every start.

## Configuration

Every setting is an environment variable (see `.env.example`). The main ones:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` / `POSTGRES_*` | Database connection |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` | Token signing (required) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `KITCHEN_EMAIL`, `KITCHEN_PASSWORD` | Bootstrap accounts (created once) |
| `DEMO_DATA` | Generate sample order history on an empty DB |
| `TAX_RATE`, `DELIVERY_FEE`, `FREE_DELIVERY_THRESHOLD`, `CURRENCY` | Pricing rules (amounts in `CURRENCY`; default BDT) |
| `PAYMENTS_SIMULATED` | Enable the demo online gateway |
| `RESTAURANT_TIMEZONE`, `RESTAURANT_PHONE`, `RESTAURANT_EMAIL`, `RESTAURANT_ADDRESS` | Storefront info and dashboard day boundaries |

## Testing

```bash
# Backend: unit tests + integration tests against a real PostgreSQL via Testcontainers (Docker required)
cd backend && ./mvnw test

# Frontend unit tests (Vitest)
cd frontend && npx ng test --watch=false

# End-to-end (Playwright) against a running stack
cd frontend && npx playwright install chromium
E2E_BASE_URL=http://localhost:8081 \
E2E_ADMIN_EMAIL=… E2E_ADMIN_PASSWORD=… E2E_KITCHEN_EMAIL=… E2E_KITCHEN_PASSWORD=… \
npx playwright test
```

| Suite | Covers |
|---|---|
| `OrderStatusTest` | Every allowed and forbidden transition for delivery and pickup |
| `PricingServiceTest` | Variant, option and combo pricing; min/max selection rules; sold-out handling; promotions (rounding, caps); coupons (expiry, minimums, category scope, caps, limits); delivery fee; tax |
| `StorageServiceTest` | Upload magic-number checks |
| `OrderFlowIntegrationTest` | Public menu, server-side quotes, guest checkout + idempotency, tracking, kitchen/admin authorization, coupon limits and release on cancel, reviews, admin menu CRUD and sold-out toggles |
| Frontend specs | Cart merging, editing, undo, persistence, quote requests that never send prices; customizer pricing and rules; error messages |
| Playwright | Guest and registered ordering flows (desktop + mobile), filters, validation, kitchen and admin order management |
