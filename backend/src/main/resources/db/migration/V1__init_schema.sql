-- =====================================================================
-- Crust & Crunch — core schema
-- Money is stored as NUMERIC(10,2). Order tables snapshot prices/names so
-- historical orders never change when the menu does.
-- =====================================================================

-- ---------- Users & addresses ----------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(30),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                    CHECK (role IN ('CUSTOMER', 'ADMIN', 'KITCHEN_STAFF')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
CREATE INDEX ix_users_role ON users (role);

CREATE TABLE addresses (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label           VARCHAR(50)  NOT NULL,
    line1           VARCHAR(200) NOT NULL,
    line2           VARCHAR(200),
    city            VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20),
    instructions    VARCHAR(300),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_addresses_user ON addresses (user_id);

-- ---------- Menu ----------
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(80)  NOT NULL,
    slug            VARCHAR(80)  NOT NULL UNIQUE,
    description     VARCHAR(500),
    image_url       VARCHAR(500),
    icon            VARCHAR(40),
    sort_order      INT          NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT        NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    name            VARCHAR(120)  NOT NULL,
    slug            VARCHAR(140)  NOT NULL UNIQUE,
    description     VARCHAR(1000),
    ingredients     VARCHAR(1000),
    allergens       VARCHAR(300),
    base_price      NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
    image_url       VARCHAR(500),
    product_type    VARCHAR(20)   NOT NULL DEFAULT 'STANDARD' CHECK (product_type IN ('STANDARD', 'COMBO')),
    vegetarian      BOOLEAN       NOT NULL DEFAULT FALSE,
    spicy           BOOLEAN       NOT NULL DEFAULT FALSE,
    calories        INT,
    prep_minutes    INT           NOT NULL DEFAULT 10,
    available       BOOLEAN       NOT NULL DEFAULT TRUE,  -- false = sold out
    visible         BOOLEAN       NOT NULL DEFAULT TRUE,  -- false = hidden from storefront
    featured        BOOLEAN       NOT NULL DEFAULT FALSE,
    bestseller      BOOLEAN       NOT NULL DEFAULT FALSE,
    is_new          BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order      INT           NOT NULL DEFAULT 0,
    rating_avg      NUMERIC(3,2)  NOT NULL DEFAULT 0,
    rating_count    INT           NOT NULL DEFAULT 0,
    sold_count      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_visible_available ON products (visible, available);
CREATE INDEX ix_products_sold ON products (sold_count DESC);

CREATE TABLE product_images (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    alt_text        VARCHAR(200),
    sort_order      INT          NOT NULL DEFAULT 0
);
CREATE INDEX ix_product_images_product ON product_images (product_id);

-- Mutually exclusive sizes/portions (pizza sizes, piece counts, drink sizes...)
CREATE TABLE product_variants (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    name            VARCHAR(80)   NOT NULL,
    price_delta     NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    available       BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_product_variants_product ON product_variants (product_id);

-- Reusable option groups (Crust, Toppings, Sauces, Spice level, Ice...)
CREATE TABLE customization_groups (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(80)  NOT NULL,
    description     VARCHAR(300),
    selection_type  VARCHAR(10)  NOT NULL CHECK (selection_type IN ('SINGLE', 'MULTIPLE')),
    min_select      INT          NOT NULL DEFAULT 0 CHECK (min_select >= 0),
    max_select      INT          NOT NULL DEFAULT 1 CHECK (max_select >= 1),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (max_select >= min_select)
);

CREATE TABLE customization_options (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT        NOT NULL REFERENCES customization_groups (id) ON DELETE CASCADE,
    name            VARCHAR(80)   NOT NULL,
    price_delta     NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (price_delta >= 0),
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    available       BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_customization_options_group ON customization_options (group_id);

-- Product <-> option-group association
CREATE TABLE product_customization_groups (
    product_id      BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    group_id        BIGINT NOT NULL REFERENCES customization_groups (id) ON DELETE CASCADE,
    sort_order      INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, group_id)
);
CREATE INDEX ix_pcg_group ON product_customization_groups (group_id);

-- Combo slots ("Choose your main", "Choose a side", "Choose a drink")
CREATE TABLE combo_components (
    id              BIGSERIAL PRIMARY KEY,
    combo_product_id BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    name            VARCHAR(80)  NOT NULL,
    quantity        INT          NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    sort_order      INT          NOT NULL DEFAULT 0
);
CREATE INDEX ix_combo_components_combo ON combo_components (combo_product_id);

CREATE TABLE combo_component_choices (
    id              BIGSERIAL PRIMARY KEY,
    component_id    BIGINT        NOT NULL REFERENCES combo_components (id) ON DELETE CASCADE,
    product_id      BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    extra_price     NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (extra_price >= 0),
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    UNIQUE (component_id, product_id)
);

-- ---------- Promotions & coupons ----------
CREATE TABLE promotions (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    description     VARCHAR(500),
    image_url       VARCHAR(500),
    discount_type   VARCHAR(10)   NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value  NUMERIC(10,2) NOT NULL CHECK (discount_value > 0),
    category_id     BIGINT        REFERENCES categories (id) ON DELETE CASCADE,
    product_id      BIGINT        REFERENCES products (id) ON DELETE CASCADE,
    starts_at       TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_promotions_active ON promotions (active, starts_at, ends_at);

CREATE TABLE coupons (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(40)   NOT NULL,
    description         VARCHAR(300),
    discount_type       VARCHAR(10)   NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value      NUMERIC(10,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    max_discount_amount NUMERIC(10,2),
    category_id         BIGINT        REFERENCES categories (id) ON DELETE SET NULL,
    starts_at           TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    max_redemptions     INT,
    per_user_limit      INT,
    redemption_count    INT           NOT NULL DEFAULT 0,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_coupons_code_upper ON coupons (upper(code));

-- ---------- Orders ----------
CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    order_number        VARCHAR(20)   NOT NULL UNIQUE,
    tracking_token      VARCHAR(64)   NOT NULL,
    idempotency_key     VARCHAR(80)   NOT NULL UNIQUE,
    user_id             BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    contact_name        VARCHAR(120)  NOT NULL,
    contact_phone       VARCHAR(30)   NOT NULL,
    contact_email       VARCHAR(255)  NOT NULL,
    fulfillment_type    VARCHAR(10)   NOT NULL CHECK (fulfillment_type IN ('DELIVERY', 'PICKUP')),
    address_line1       VARCHAR(200),
    address_line2       VARCHAR(200),
    address_city        VARCHAR(100),
    address_postal_code VARCHAR(20),
    delivery_instructions VARCHAR(500),
    special_instructions  VARCHAR(500),
    allergy_notes       VARCHAR(500),
    status              VARCHAR(20)   NOT NULL,
    payment_method      VARCHAR(20)   NOT NULL,
    payment_status      VARCHAR(20)   NOT NULL,
    subtotal            NUMERIC(10,2) NOT NULL,
    discount_total      NUMERIC(10,2) NOT NULL DEFAULT 0,
    delivery_fee        NUMERIC(10,2) NOT NULL DEFAULT 0,
    tax                 NUMERIC(10,2) NOT NULL DEFAULT 0,
    total               NUMERIC(10,2) NOT NULL,
    coupon_code         VARCHAR(40),
    estimated_ready_at  TIMESTAMPTZ,
    cancel_reason       VARCHAR(300),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (fulfillment_type = 'PICKUP' OR (address_line1 IS NOT NULL AND address_city IS NOT NULL))
);
CREATE INDEX ix_orders_user ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status);
CREATE INDEX ix_orders_created ON orders (created_at DESC);

CREATE TABLE order_items (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id          BIGINT        REFERENCES products (id) ON DELETE SET NULL,
    category_id         BIGINT        REFERENCES categories (id) ON DELETE SET NULL,
    product_name        VARCHAR(120)  NOT NULL,
    category_name       VARCHAR(80)   NOT NULL,
    variant_name        VARCHAR(80),
    base_price          NUMERIC(10,2) NOT NULL,
    variant_price_delta NUMERIC(10,2) NOT NULL DEFAULT 0,
    unit_price          NUMERIC(10,2) NOT NULL,
    quantity            INT           NOT NULL CHECK (quantity > 0),
    discount            NUMERIC(10,2) NOT NULL DEFAULT 0,
    line_total          NUMERIC(10,2) NOT NULL,
    notes               VARCHAR(300)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);
CREATE INDEX ix_order_items_product ON order_items (product_id);

CREATE TABLE order_item_customizations (
    id                  BIGSERIAL PRIMARY KEY,
    order_item_id       BIGINT        NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    kind                VARCHAR(15)   NOT NULL CHECK (kind IN ('OPTION', 'COMBO_CHOICE')),
    group_name          VARCHAR(80)   NOT NULL,
    option_name         VARCHAR(120)  NOT NULL,
    price_delta         NUMERIC(10,2) NOT NULL DEFAULT 0,
    option_id           BIGINT,
    choice_product_id   BIGINT
);
CREATE INDEX ix_oic_item ON order_item_customizations (order_item_id);

CREATE TABLE order_status_history (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status         VARCHAR(20),
    to_status           VARCHAR(20)  NOT NULL,
    changed_by          BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    note                VARCHAR(300),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_osh_order ON order_status_history (order_id, created_at);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method              VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    amount              NUMERIC(10,2) NOT NULL,
    provider            VARCHAR(40)   NOT NULL,
    provider_reference  VARCHAR(120),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_payments_order ON payments (order_id);

CREATE TABLE coupon_redemptions (
    id                  BIGSERIAL PRIMARY KEY,
    coupon_id           BIGINT        NOT NULL REFERENCES coupons (id) ON DELETE CASCADE,
    order_id            BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    user_id             BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    contact_email       VARCHAR(255)  NOT NULL,
    discount_amount     NUMERIC(10,2) NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (order_id)
);
CREATE INDEX ix_coupon_redemptions_coupon ON coupon_redemptions (coupon_id);

CREATE TABLE reviews (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id          BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    order_id            BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    rating              INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment             VARCHAR(1000),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, product_id, order_id)
);
CREATE INDEX ix_reviews_product ON reviews (product_id, created_at DESC);
