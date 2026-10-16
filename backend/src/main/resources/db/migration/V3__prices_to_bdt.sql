-- Switch the menu from US dollars to Bangladeshi taka (BDT).
-- Catalog prices use local Dhaka pricing: dollar price x 100, rounded to the nearest 10 taka
-- (Margherita 9.99 -> 1000, fries 2.99 -> 300, a 0.75 topping -> 80).

CREATE FUNCTION pg_temp.bdt(usd NUMERIC) RETURNS NUMERIC AS $$
    SELECT round(usd * 100, -1)
$$ LANGUAGE sql IMMUTABLE;

-- ---------- Catalog ----------
UPDATE products                SET base_price  = pg_temp.bdt(base_price);
UPDATE product_variants        SET price_delta = pg_temp.bdt(price_delta);
UPDATE customization_options   SET price_delta = pg_temp.bdt(price_delta);
UPDATE combo_component_choices SET extra_price = pg_temp.bdt(extra_price);

-- ---------- Promotions & coupons (percentages stay as they are) ----------
UPDATE promotions SET discount_value = pg_temp.bdt(discount_value) WHERE discount_type = 'FIXED';
UPDATE coupons
SET discount_value      = CASE WHEN discount_type = 'FIXED' THEN pg_temp.bdt(discount_value) ELSE discount_value END,
    min_order_amount    = pg_temp.bdt(min_order_amount),
    max_discount_amount = pg_temp.bdt(max_discount_amount);

UPDATE promotions SET description = '৳100 off every dessert, every day.'
WHERE description = '$1 off every dessert, every day.';
UPDATE coupons SET description = '10% off your order (max ৳1,000) on orders over ৳1,500'
WHERE description = '10% off your order (max $10) on orders over $15';
UPDATE coupons SET description = '৳500 off orders of ৳3,000 or more'
WHERE description = '$5 off orders of $30 or more';

-- ---------- Orders ----------
-- Generated demo history was priced in dollars; drop it so the app regenerates it in taka
-- (items, history, payments, redemptions and reviews cascade).
DELETE FROM orders WHERE order_number LIKE 'CC-DEMO-%';

-- Real orders keep their exact amounts, restated in taka at the same x100 rate so totals still add up.
UPDATE orders
SET subtotal = subtotal * 100, discount_total = discount_total * 100, delivery_fee = delivery_fee * 100,
    tax = tax * 100, total = total * 100;
UPDATE order_items
SET base_price = base_price * 100, variant_price_delta = variant_price_delta * 100,
    unit_price = unit_price * 100, discount = discount * 100, line_total = line_total * 100;
UPDATE order_item_customizations SET price_delta = price_delta * 100;
UPDATE payments                  SET amount = amount * 100;
UPDATE coupon_redemptions        SET discount_amount = discount_amount * 100;
