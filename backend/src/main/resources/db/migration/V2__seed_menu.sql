-- =====================================================================
-- Seed menu. Everything here is ordinary data and is editable from the
-- admin dashboard — nothing in the application code depends on it.
-- =====================================================================

CREATE FUNCTION pg_temp.img(photo TEXT) RETURNS TEXT AS $$
    SELECT 'https://images.unsplash.com/photo-' || photo || '?w=900&q=75&auto=format&fit=crop'
$$ LANGUAGE sql IMMUTABLE;

CREATE FUNCTION pg_temp.cat(s TEXT) RETURNS BIGINT AS $$
    SELECT id FROM categories WHERE slug = s
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.prod(s TEXT) RETURNS BIGINT AS $$
    SELECT id FROM products WHERE slug = s
$$ LANGUAGE sql STABLE;

CREATE FUNCTION pg_temp.grp(n TEXT) RETURNS BIGINT AS $$
    SELECT id FROM customization_groups WHERE name = n
$$ LANGUAGE sql STABLE;

-- ---------- Categories ----------
INSERT INTO categories (name, slug, description, image_url, icon, sort_order) VALUES
('Pizza',               'pizza',      'Stone-baked pies with hand-stretched dough and real mozzarella.', pg_temp.img('1513104890138-7c749659a591'), 'pizza',        1),
('Burgers',             'burgers',    'Smashed, stacked and stuffed — griddled to order.',               pg_temp.img('1568901346375-23c9450c58cd'), 'beef',         2),
('Fried Chicken',       'chicken',    'Buttermilk-brined, double-dredged and fried golden.',             pg_temp.img('1626645738196-c2a7c87a8f58'), 'drumstick',    3),
('Sandwiches & Wraps',  'sandwiches', 'Toasted, pressed and wrapped for eating on the move.',            pg_temp.img('1528735602780-2552fd46c7af'), 'sandwich',     4),
('Pasta',               'pasta',      'Creamy, saucy and baked — comfort in a bowl.',                    pg_temp.img('1621996346565-e3dbc646d9a9'), 'utensils',     5),
('Sides & Snacks',      'sides',      'Fries, rings, sticks and everything you share (or don''t).',      pg_temp.img('1573080496219-bb080dd4f877'), 'popcorn',      6),
('Rice Meals',          'rice',       'Wok-tossed rice and loaded bowls.',                               pg_temp.img('1603133872878-684f208fb84b'), 'soup',         7),
('Desserts',            'desserts',   'Something sweet to finish strong.',                               pg_temp.img('1606313564200-e75d5e30476c'), 'cake-slice',   8),
('Beverages',           'beverages',  'Ice-cold sodas, fresh lemonade, coffee and thick shakes.',        pg_temp.img('1572490122747-3968b75cc699'), 'cup-soda',     9),
('Combos & Meal Deals', 'combos',     'Build-your-own bundles that save you money.',                     pg_temp.img('1610614819513-58e34989848b'), 'package',      10);

-- ---------- Reusable customization groups ----------
INSERT INTO customization_groups (name, description, selection_type, min_select, max_select) VALUES
('Crust',            'Choose your crust',                     'SINGLE',   1, 1),
('Pizza Toppings',   'Add up to 6 extra toppings',            'MULTIPLE', 0, 6),
('Extra Cheese',     'Because more cheese is always better',  'MULTIPLE', 0, 1),
('Patty',            'How hungry are you?',                   'SINGLE',   1, 1),
('Cheese',           'Pick a cheese (optional)',              'SINGLE',   0, 1),
('Sauces',           'Up to 3 sauces',                        'MULTIPLE', 0, 3),
('Burger Extras',    'Load it up',                            'MULTIPLE', 0, 5),
('Spice Level',      'From gentle to inferno',                'SINGLE',   1, 1),
('Make It a Meal',   'Add fries and a drink',                 'SINGLE',   0, 1),
('Bread or Wrap',    'Choose your carrier',                   'SINGLE',   1, 1),
('Sandwich Add-ons', 'Extra fillings',                        'MULTIPLE', 0, 4),
('Pasta Sauce',      'Choose your sauce',                     'SINGLE',   1, 1),
('Add Protein',      'Make it heartier',                      'SINGLE',   0, 1),
('Dips',             'Up to 3 dips',                          'MULTIPLE', 0, 3),
('Ice',              'Ice preference',                        'SINGLE',   1, 1),
('Drink Extras',     'Treat yourself',                        'MULTIPLE', 0, 2),
('Rice Extras',      'Top up your bowl',                      'MULTIPLE', 0, 3),
('Sundae Toppings',  'Up to 3 toppings',                      'MULTIPLE', 0, 3);

INSERT INTO customization_options (group_id, name, price_delta, is_default, sort_order) VALUES
(pg_temp.grp('Crust'), 'Classic Hand-Tossed', 0, TRUE, 1),
(pg_temp.grp('Crust'), 'Thin & Crispy', 0, FALSE, 2),
(pg_temp.grp('Crust'), 'Cheese-Stuffed Crust', 2.50, FALSE, 3),
(pg_temp.grp('Crust'), 'Gluten-Free Base', 3.00, FALSE, 4),

(pg_temp.grp('Pizza Toppings'), 'Pepperoni', 1.50, FALSE, 1),
(pg_temp.grp('Pizza Toppings'), 'Mushrooms', 1.00, FALSE, 2),
(pg_temp.grp('Pizza Toppings'), 'Red Onions', 0.75, FALSE, 3),
(pg_temp.grp('Pizza Toppings'), 'Green Peppers', 0.75, FALSE, 4),
(pg_temp.grp('Pizza Toppings'), 'Black Olives', 1.00, FALSE, 5),
(pg_temp.grp('Pizza Toppings'), 'Jalapeños', 0.75, FALSE, 6),
(pg_temp.grp('Pizza Toppings'), 'Grilled Chicken', 2.00, FALSE, 7),
(pg_temp.grp('Pizza Toppings'), 'Italian Sausage', 1.75, FALSE, 8),
(pg_temp.grp('Pizza Toppings'), 'Pineapple', 1.00, FALSE, 9),
(pg_temp.grp('Pizza Toppings'), 'Smoked Bacon', 1.75, FALSE, 10),

(pg_temp.grp('Extra Cheese'), 'Extra Mozzarella', 1.50, FALSE, 1),

(pg_temp.grp('Patty'), 'Single Patty', 0, TRUE, 1),
(pg_temp.grp('Patty'), 'Double Patty', 3.00, FALSE, 2),

(pg_temp.grp('Cheese'), 'American', 0.75, FALSE, 1),
(pg_temp.grp('Cheese'), 'Aged Cheddar', 1.00, FALSE, 2),
(pg_temp.grp('Cheese'), 'Swiss', 1.00, FALSE, 3),
(pg_temp.grp('Cheese'), 'Pepper Jack', 1.00, FALSE, 4),

(pg_temp.grp('Sauces'), 'Ketchup', 0, FALSE, 1),
(pg_temp.grp('Sauces'), 'Mayo', 0, FALSE, 2),
(pg_temp.grp('Sauces'), 'Mustard', 0, FALSE, 3),
(pg_temp.grp('Sauces'), 'Smoky BBQ', 0, FALSE, 4),
(pg_temp.grp('Sauces'), 'Signature Crunch Sauce', 0.50, FALSE, 5),
(pg_temp.grp('Sauces'), 'Chipotle Mayo', 0.50, FALSE, 6),
(pg_temp.grp('Sauces'), 'Garlic Aioli', 0.50, FALSE, 7),

(pg_temp.grp('Burger Extras'), 'Smoked Bacon', 1.75, FALSE, 1),
(pg_temp.grp('Burger Extras'), 'Fried Egg', 1.25, FALSE, 2),
(pg_temp.grp('Burger Extras'), 'Avocado', 1.50, FALSE, 3),
(pg_temp.grp('Burger Extras'), 'Caramelized Onions', 0.75, FALSE, 4),
(pg_temp.grp('Burger Extras'), 'Jalapeños', 0.75, FALSE, 5),
(pg_temp.grp('Burger Extras'), 'Extra Pickles', 0, FALSE, 6),

(pg_temp.grp('Spice Level'), 'Mild', 0, TRUE, 1),
(pg_temp.grp('Spice Level'), 'Medium', 0, FALSE, 2),
(pg_temp.grp('Spice Level'), 'Hot', 0, FALSE, 3),
(pg_temp.grp('Spice Level'), 'Inferno', 0, FALSE, 4),

(pg_temp.grp('Make It a Meal'), 'Regular Fries + Drink', 3.99, FALSE, 1),
(pg_temp.grp('Make It a Meal'), 'Large Fries + Drink', 4.99, FALSE, 2),

(pg_temp.grp('Bread or Wrap'), 'Toasted White', 0, TRUE, 1),
(pg_temp.grp('Bread or Wrap'), 'Whole Wheat', 0, FALSE, 2),
(pg_temp.grp('Bread or Wrap'), 'Brioche', 0.50, FALSE, 3),
(pg_temp.grp('Bread or Wrap'), 'Flour Tortilla Wrap', 0, FALSE, 4),
(pg_temp.grp('Bread or Wrap'), 'Pita', 0.50, FALSE, 5),

(pg_temp.grp('Sandwich Add-ons'), 'Extra Chicken', 2.50, FALSE, 1),
(pg_temp.grp('Sandwich Add-ons'), 'Cheese Slice', 0.75, FALSE, 2),
(pg_temp.grp('Sandwich Add-ons'), 'Smoked Bacon', 1.75, FALSE, 3),
(pg_temp.grp('Sandwich Add-ons'), 'Pickled Veggies', 0.50, FALSE, 4),
(pg_temp.grp('Sandwich Add-ons'), 'Fries Inside', 1.00, FALSE, 5),

(pg_temp.grp('Pasta Sauce'), 'Alfredo', 0, TRUE, 1),
(pg_temp.grp('Pasta Sauce'), 'Marinara', 0, FALSE, 2),
(pg_temp.grp('Pasta Sauce'), 'Rosé', 0, FALSE, 3),
(pg_temp.grp('Pasta Sauce'), 'Basil Pesto', 1.00, FALSE, 4),

(pg_temp.grp('Add Protein'), 'Grilled Chicken', 2.50, FALSE, 1),
(pg_temp.grp('Add Protein'), 'Beef Meatballs', 3.00, FALSE, 2),
(pg_temp.grp('Add Protein'), 'Garlic Shrimp', 4.00, FALSE, 3),

(pg_temp.grp('Dips'), 'Ketchup', 0, FALSE, 1),
(pg_temp.grp('Dips'), 'Ranch', 0.50, FALSE, 2),
(pg_temp.grp('Dips'), 'Cheese Sauce', 0.75, FALSE, 3),
(pg_temp.grp('Dips'), 'Garlic Dip', 0.50, FALSE, 4),
(pg_temp.grp('Dips'), 'Sweet Chili', 0.50, FALSE, 5),

(pg_temp.grp('Ice'), 'Regular Ice', 0, TRUE, 1),
(pg_temp.grp('Ice'), 'Light Ice', 0, FALSE, 2),
(pg_temp.grp('Ice'), 'No Ice', 0, FALSE, 3),

(pg_temp.grp('Drink Extras'), 'Extra Espresso Shot', 0.75, FALSE, 1),
(pg_temp.grp('Drink Extras'), 'Whipped Cream', 0.50, FALSE, 2),
(pg_temp.grp('Drink Extras'), 'Vanilla Syrup', 0.60, FALSE, 3),
(pg_temp.grp('Drink Extras'), 'Caramel Syrup', 0.60, FALSE, 4),

(pg_temp.grp('Rice Extras'), 'Fried Egg', 1.00, FALSE, 1),
(pg_temp.grp('Rice Extras'), 'Extra Chicken', 2.50, FALSE, 2),
(pg_temp.grp('Rice Extras'), 'Extra Veggies', 1.00, FALSE, 3),
(pg_temp.grp('Rice Extras'), 'Crispy Shallots', 0.50, FALSE, 4),

(pg_temp.grp('Sundae Toppings'), 'Hot Fudge', 0.50, FALSE, 1),
(pg_temp.grp('Sundae Toppings'), 'Salted Caramel', 0.50, FALSE, 2),
(pg_temp.grp('Sundae Toppings'), 'Rainbow Sprinkles', 0.30, FALSE, 3),
(pg_temp.grp('Sundae Toppings'), 'Crushed Peanuts', 0.60, FALSE, 4);

-- ---------- Products ----------
INSERT INTO products (category_id, name, slug, description, ingredients, allergens, base_price, image_url,
                      vegetarian, spicy, calories, prep_minutes, featured, bestseller, is_new, sort_order,
                      rating_avg, rating_count, sold_count) VALUES
-- Pizza (base price = 10" small)
(pg_temp.cat('pizza'), 'Margherita', 'margherita-pizza', 'San Marzano tomato, fior di latte and fresh basil on our 48-hour dough.', 'Tomato sauce, mozzarella, basil, olive oil', 'Gluten, Dairy', 9.99, pg_temp.img('1574071318508-1cdbab80d002'), TRUE, FALSE, 820, 14, TRUE, TRUE, FALSE, 1, 4.70, 212, 940),
(pg_temp.cat('pizza'), 'Pepperoni Classic', 'pepperoni-pizza', 'Loaded edge-to-edge with crispy cupping pepperoni.', 'Tomato sauce, mozzarella, pepperoni', 'Gluten, Dairy', 11.49, pg_temp.img('1628840042765-356cda07504e'), FALSE, FALSE, 980, 14, TRUE, TRUE, FALSE, 2, 4.80, 351, 1320),
(pg_temp.cat('pizza'), 'BBQ Chicken', 'bbq-chicken-pizza', 'Smoky BBQ base, roasted chicken, red onion and cilantro.', 'BBQ sauce, chicken, mozzarella, red onion, cilantro', 'Gluten, Dairy', 12.49, pg_temp.img('1565299624946-b28f40a0ae38'), FALSE, FALSE, 1010, 15, FALSE, TRUE, FALSE, 3, 4.60, 188, 760),
(pg_temp.cat('pizza'), 'Meat Lovers', 'meat-lovers-pizza', 'Pepperoni, Italian sausage, bacon and ham. Not for the faint-hearted.', 'Tomato sauce, mozzarella, pepperoni, sausage, bacon, ham', 'Gluten, Dairy', 13.99, pg_temp.img('1594007654729-407eedc4be65'), FALSE, FALSE, 1180, 15, FALSE, FALSE, FALSE, 4, 4.50, 143, 610),
(pg_temp.cat('pizza'), 'Garden Vegetarian', 'vegetarian-pizza', 'Peppers, mushrooms, olives, red onion and cherry tomatoes.', 'Tomato sauce, mozzarella, peppers, mushrooms, olives, onion, tomatoes', 'Gluten, Dairy', 11.49, pg_temp.img('1571407970349-bc81e7e96d47'), TRUE, FALSE, 860, 14, FALSE, FALSE, FALSE, 5, 4.40, 97, 420),
(pg_temp.cat('pizza'), 'Build Your Own', 'build-your-own-pizza', 'Start with cheese and tomato, then make it yours.', 'Tomato sauce, mozzarella + your toppings', 'Gluten, Dairy', 8.99, pg_temp.img('1513104890138-7c749659a591'), TRUE, FALSE, 760, 15, TRUE, FALSE, TRUE, 6, 4.60, 64, 310),
(pg_temp.cat('pizza'), 'Diavola Inferno', 'diavola-pizza', 'Spicy salami, Calabrian chili and hot honey drizzle.', 'Tomato sauce, mozzarella, spicy salami, chili, hot honey', 'Gluten, Dairy', 12.99, pg_temp.img('1604382354936-07c5d9983bd3'), FALSE, TRUE, 990, 14, FALSE, FALSE, TRUE, 7, 4.70, 41, 150),

-- Burgers
(pg_temp.cat('burgers'), 'Classic Smash Burger', 'classic-smash-burger', 'Two crispy-edged smashed patties, pickles, onion and Crunch sauce on a potato bun.', 'Beef, potato bun, pickles, onion, crunch sauce', 'Gluten, Egg, Mustard', 8.49, pg_temp.img('1568901346375-23c9450c58cd'), FALSE, FALSE, 720, 9, TRUE, TRUE, FALSE, 1, 4.80, 402, 1580),
(pg_temp.cat('burgers'), 'Double Cheeseburger', 'double-cheeseburger', 'Double beef, double American cheese, ketchup and mustard.', 'Beef, American cheese, bun, ketchup, mustard, pickles', 'Gluten, Dairy, Mustard', 9.99, pg_temp.img('1553979459-d2229ba7433b'), FALSE, FALSE, 890, 9, FALSE, TRUE, FALSE, 2, 4.70, 288, 1100),
(pg_temp.cat('burgers'), 'Crispy Chicken Burger', 'crispy-chicken-burger', 'Buttermilk fried chicken thigh, slaw and pickles on brioche.', 'Chicken, brioche, slaw, pickles, mayo', 'Gluten, Egg, Dairy', 8.99, pg_temp.img('1606755962773-d324e0a13086'), FALSE, FALSE, 760, 10, FALSE, TRUE, FALSE, 3, 4.60, 231, 980),
(pg_temp.cat('burgers'), 'Smokehouse BBQ Burger', 'smokehouse-bbq-burger', 'Beef, bacon, onion rings, cheddar and smoky BBQ sauce.', 'Beef, bacon, onion rings, cheddar, BBQ sauce, bun', 'Gluten, Dairy', 10.99, pg_temp.img('1550547660-d9450f859349'), FALSE, FALSE, 1040, 11, FALSE, FALSE, TRUE, 4, 4.50, 77, 340),
(pg_temp.cat('burgers'), 'Garden Veggie Burger', 'veggie-burger', 'Crispy chickpea & black bean patty, avocado, tomato and greens.', 'Chickpea patty, avocado, tomato, lettuce, vegan mayo, bun', 'Gluten, Soy', 8.99, pg_temp.img('1520072959219-c595dc870360'), TRUE, FALSE, 610, 10, FALSE, FALSE, FALSE, 5, 4.30, 66, 260),

-- Fried chicken
(pg_temp.cat('chicken'), 'Original Crispy Chicken', 'crispy-chicken', 'Bone-in pieces brined for 12 hours and fried crunchy.', 'Chicken, buttermilk, seasoned flour', 'Gluten, Dairy', 6.49, pg_temp.img('1626645738196-c2a7c87a8f58'), FALSE, FALSE, 540, 12, TRUE, TRUE, FALSE, 1, 4.70, 315, 1250),
(pg_temp.cat('chicken'), 'Nashville Hot Chicken', 'spicy-chicken', 'Dunked in cayenne chili oil, served with pickles.', 'Chicken, buttermilk, flour, cayenne oil, pickles', 'Gluten, Dairy', 6.99, pg_temp.img('1562967914-608f82629710'), FALSE, TRUE, 590, 12, FALSE, TRUE, FALSE, 2, 4.60, 199, 870),
(pg_temp.cat('chicken'), 'Chicken Wings', 'chicken-wings', 'Crispy wings tossed in your choice of heat.', 'Chicken wings, seasoning, sauce', 'Celery', 7.99, pg_temp.img('1527477396000-e27163b481c2'), FALSE, TRUE, 620, 12, FALSE, TRUE, FALSE, 3, 4.60, 254, 1040),
(pg_temp.cat('chicken'), 'Golden Nuggets', 'chicken-nuggets', 'All-white-meat nuggets with a light crunchy coat.', 'Chicken breast, breadcrumb', 'Gluten', 5.49, pg_temp.img('1562967916-eb82221dfb92'), FALSE, FALSE, 430, 8, FALSE, FALSE, FALSE, 4, 4.40, 140, 690),
(pg_temp.cat('chicken'), 'Chicken Tenders', 'chicken-tenders', 'Hand-breaded strips, juicy inside and crackly outside.', 'Chicken breast, buttermilk, flour', 'Gluten, Dairy', 6.99, pg_temp.img('1608039755401-742074f0548d'), FALSE, FALSE, 510, 10, FALSE, FALSE, TRUE, 5, 4.50, 58, 230),

-- Sandwiches & wraps
(pg_temp.cat('sandwiches'), 'Triple-Decker Club', 'club-sandwich', 'Roast chicken, bacon, egg, lettuce and tomato on three layers of toast.', 'Chicken, bacon, egg, lettuce, tomato, mayo, bread', 'Gluten, Egg', 8.99, pg_temp.img('1528735602780-2552fd46c7af'), FALSE, FALSE, 680, 9, FALSE, FALSE, FALSE, 1, 4.40, 88, 380),
(pg_temp.cat('sandwiches'), 'Grilled Chicken Sandwich', 'grilled-chicken-sandwich', 'Chargrilled chicken breast, pesto mayo, roasted peppers.', 'Chicken, pesto mayo, roasted peppers, rocket, bread', 'Gluten, Egg, Dairy, Nuts', 8.49, pg_temp.img('1619740455993-9e612b1af08a'), FALSE, FALSE, 560, 9, FALSE, FALSE, FALSE, 2, 4.50, 73, 320),
(pg_temp.cat('sandwiches'), 'Chicken Shawarma Wrap', 'shawarma-wrap', 'Spiced chicken, garlic toum, pickles and fries rolled tight.', 'Chicken, garlic sauce, pickles, fries, flatbread', 'Gluten, Egg', 7.99, pg_temp.img('1626700051175-6818013e1d4f'), FALSE, TRUE, 640, 8, TRUE, TRUE, FALSE, 3, 4.70, 267, 1010),
(pg_temp.cat('sandwiches'), 'Crispy Chicken Wrap', 'crispy-chicken-wrap', 'Crispy tenders, lettuce, cheddar and ranch.', 'Chicken tenders, lettuce, cheddar, ranch, tortilla', 'Gluten, Dairy, Egg', 7.49, pg_temp.img('1600891964092-4316c288032e'), FALSE, FALSE, 610, 8, FALSE, FALSE, FALSE, 4, 4.30, 61, 290),

-- Pasta
(pg_temp.cat('pasta'), 'Fettuccine Alfredo', 'fettuccine-alfredo', 'Silky parmesan cream sauce with cracked black pepper.', 'Fettuccine, cream, parmesan, butter', 'Gluten, Dairy, Egg', 10.49, pg_temp.img('1645112411341-6c4fd023714a'), TRUE, FALSE, 890, 12, FALSE, FALSE, FALSE, 1, 4.50, 92, 410),
(pg_temp.cat('pasta'), 'Creamy Chicken Penne', 'creamy-chicken-pasta', 'Penne in garlic cream with grilled chicken and spinach.', 'Penne, chicken, cream, garlic, spinach, parmesan', 'Gluten, Dairy', 11.49, pg_temp.img('1563379926898-05f4575a45d8'), FALSE, FALSE, 940, 12, TRUE, TRUE, FALSE, 2, 4.60, 156, 640),
(pg_temp.cat('pasta'), 'Spaghetti Pomodoro', 'spaghetti', 'Slow-cooked tomato sauce, basil and parmesan.', 'Spaghetti, tomato, garlic, basil, parmesan', 'Gluten, Dairy', 9.49, pg_temp.img('1551183053-bf91a1d81141'), TRUE, FALSE, 720, 11, FALSE, FALSE, FALSE, 3, 4.30, 70, 300),
(pg_temp.cat('pasta'), 'Baked Four-Cheese Pasta', 'baked-pasta', 'Rigatoni baked under mozzarella, cheddar, parmesan and ricotta.', 'Rigatoni, tomato, mozzarella, cheddar, parmesan, ricotta', 'Gluten, Dairy', 11.99, pg_temp.img('1621996346565-e3dbc646d9a9'), TRUE, FALSE, 1020, 16, FALSE, FALSE, TRUE, 4, 4.60, 39, 170),

-- Sides
(pg_temp.cat('sides'), 'Sea-Salt Fries', 'french-fries', 'Skin-on fries, twice cooked for maximum crunch.', 'Potatoes, sea salt, sunflower oil', NULL, 2.99, pg_temp.img('1573080496219-bb080dd4f877'), TRUE, FALSE, 340, 5, FALSE, TRUE, FALSE, 1, 4.60, 420, 2100),
(pg_temp.cat('sides'), 'Loaded Fries', 'loaded-fries', 'Fries smothered in cheese sauce, bacon bits, jalapeños and scallions.', 'Fries, cheese sauce, bacon, jalapeños, scallions', 'Dairy', 5.99, pg_temp.img('1630384060421-cb20d0e0649d'), FALSE, TRUE, 690, 7, TRUE, TRUE, FALSE, 2, 4.70, 233, 1020),
(pg_temp.cat('sides'), 'Beer-Battered Onion Rings', 'onion-rings', 'Thick-cut sweet onions in a golden crunchy batter.', 'Onion, flour, batter', 'Gluten', 3.99, pg_temp.img('1639024471283-03518883512d'), TRUE, FALSE, 410, 6, FALSE, FALSE, FALSE, 3, 4.40, 118, 560),
(pg_temp.cat('sides'), 'Mozzarella Sticks', 'mozzarella-sticks', 'Stretchy mozzarella in Italian breadcrumbs with marinara.', 'Mozzarella, breadcrumbs, marinara', 'Gluten, Dairy, Egg', 5.49, pg_temp.img('1531749668029-2db88e4276c7'), TRUE, FALSE, 480, 7, FALSE, FALSE, FALSE, 4, 4.50, 101, 480),
(pg_temp.cat('sides'), 'Cheesy Garlic Bread', 'garlic-bread', 'Garlic butter, herbs and melted mozzarella.', 'Bread, garlic butter, parsley, mozzarella', 'Gluten, Dairy', 3.99, pg_temp.img('1573140401552-3fab0b24306f'), TRUE, FALSE, 390, 6, FALSE, FALSE, FALSE, 5, 4.50, 134, 610),

-- Rice
(pg_temp.cat('rice'), 'Egg Fried Rice', 'egg-fried-rice', 'Wok-tossed jasmine rice with egg, peas and scallions.', 'Jasmine rice, egg, peas, carrot, scallion, soy', 'Egg, Soy', 7.49, pg_temp.img('1603133872878-684f208fb84b'), TRUE, FALSE, 560, 9, FALSE, FALSE, FALSE, 1, 4.30, 67, 300),
(pg_temp.cat('rice'), 'Teriyaki Chicken Rice Bowl', 'chicken-rice-bowl', 'Glazed chicken thigh, sesame greens and pickled slaw over rice.', 'Chicken, teriyaki, rice, greens, sesame, slaw', 'Soy, Sesame, Gluten', 9.99, pg_temp.img('1512058564366-18510be2db19'), FALSE, FALSE, 720, 11, TRUE, FALSE, FALSE, 2, 4.60, 112, 480),
(pg_temp.cat('rice'), 'Spicy Korean Chicken Bowl', 'korean-chicken-bowl', 'Crispy gochujang chicken, kimchi and a fried egg.', 'Chicken, gochujang, rice, kimchi, egg, scallion', 'Soy, Sesame, Egg, Gluten', 10.49, pg_temp.img('1617196034796-73dfa7b1fd56'), FALSE, TRUE, 780, 11, FALSE, FALSE, TRUE, 3, 4.70, 45, 190),

-- Desserts
(pg_temp.cat('desserts'), 'Fudge Brownie', 'fudge-brownie', 'Dense, gooey dark-chocolate brownie.', 'Chocolate, butter, sugar, eggs, flour', 'Gluten, Dairy, Egg', 3.49, pg_temp.img('1606313564200-e75d5e30476c'), TRUE, FALSE, 420, 2, FALSE, TRUE, FALSE, 1, 4.80, 190, 820),
(pg_temp.cat('desserts'), 'Triple Chocolate Cake', 'chocolate-cake', 'Three layers of chocolate sponge and ganache.', 'Chocolate, flour, sugar, eggs, cream', 'Gluten, Dairy, Egg', 4.99, pg_temp.img('1578985545062-69928b1d9587'), TRUE, FALSE, 540, 2, FALSE, FALSE, FALSE, 2, 4.70, 88, 360),
(pg_temp.cat('desserts'), 'Chocolate Chip Cookies', 'chocolate-chip-cookies', 'Baked fresh every hour — crisp edges, soft centres.', 'Flour, butter, brown sugar, chocolate chips', 'Gluten, Dairy, Egg', 2.99, pg_temp.img('1499636136210-6f4ee915583e'), TRUE, FALSE, 380, 2, FALSE, FALSE, FALSE, 3, 4.60, 75, 400),
(pg_temp.cat('desserts'), 'Vanilla Bean Sundae', 'ice-cream-sundae', 'Soft-serve vanilla with your choice of toppings.', 'Milk, cream, sugar, vanilla', 'Dairy', 3.29, pg_temp.img('1563805042-7684c019e1cb'), TRUE, FALSE, 330, 2, FALSE, FALSE, TRUE, 4, 4.50, 52, 260),

-- Beverages (base price = small)
(pg_temp.cat('beverages'), 'Fountain Soda', 'fountain-soda', 'Cola, diet cola, lemon-lime or orange.', 'Carbonated soft drink', NULL, 1.99, pg_temp.img('1622483767028-3f66f32aef97'), TRUE, FALSE, 180, 1, FALSE, TRUE, FALSE, 1, 4.40, 150, 2400),
(pg_temp.cat('beverages'), 'Fresh Lemonade', 'fresh-lemonade', 'Squeezed in-house with a hint of mint.', 'Lemon, sugar, water, mint', NULL, 2.79, pg_temp.img('1621263764928-df1444c5e859'), TRUE, FALSE, 150, 2, FALSE, FALSE, FALSE, 2, 4.60, 98, 720),
(pg_temp.cat('beverages'), 'Peach Iced Tea', 'iced-tea', 'Brewed black tea with ripe peach.', 'Black tea, peach, sugar', NULL, 2.49, pg_temp.img('1556679343-c7306c1976bc'), TRUE, FALSE, 120, 2, FALSE, FALSE, FALSE, 3, 4.40, 60, 450),
(pg_temp.cat('beverages'), 'Fresh Brew Coffee', 'coffee', 'Single-origin drip coffee, hot or iced.', 'Coffee', NULL, 2.29, pg_temp.img('1509042239860-f550ce710b93'), TRUE, FALSE, 5, 2, FALSE, FALSE, FALSE, 4, 4.30, 44, 380),
(pg_temp.cat('beverages'), 'Thick Milkshake', 'milkshake', 'Chocolate, vanilla or strawberry — spoon optional.', 'Milk, ice cream, flavouring', 'Dairy', 4.49, pg_temp.img('1572490122747-3968b75cc699'), TRUE, FALSE, 560, 3, TRUE, TRUE, FALSE, 5, 4.80, 176, 900);

-- Combos (product_type = COMBO)
INSERT INTO products (category_id, name, slug, description, ingredients, allergens, base_price, image_url, product_type,
                      vegetarian, spicy, calories, prep_minutes, featured, bestseller, is_new, sort_order,
                      rating_avg, rating_count, sold_count) VALUES
(pg_temp.cat('combos'), 'Burger & Fries Combo', 'burger-combo', 'Any burger, a side and a drink. The classic.', 'See chosen items', 'Varies by selection', 12.99, pg_temp.img('1594212699903-ec8a3eca50f5'), 'COMBO', FALSE, FALSE, NULL, 10, TRUE, TRUE, FALSE, 1, 4.80, 330, 1450),
(pg_temp.cat('combos'), 'Pizza Night for Two', 'pizza-combo', 'One pizza, a shareable side and two drinks.', 'See chosen items', 'Varies by selection', 19.99, pg_temp.img('1607013251379-e6eecfffe234'), 'COMBO', FALSE, FALSE, NULL, 15, TRUE, TRUE, FALSE, 2, 4.70, 190, 820),
(pg_temp.cat('combos'), 'Family Chicken Bucket', 'family-bucket', 'Four chicken servings, two sides and four drinks. Feeds the crew.', 'See chosen items', 'Varies by selection', 32.99, pg_temp.img('1585238342024-78d387f4a707'), 'COMBO', FALSE, FALSE, NULL, 18, TRUE, TRUE, FALSE, 3, 4.70, 140, 520),
(pg_temp.cat('combos'), 'Wings & Wrap Deal', 'wings-wrap-deal', 'A wrap, a portion of wings and a drink.', 'See chosen items', 'Varies by selection', 15.49, pg_temp.img('1527477396000-e27163b481c2'), 'COMBO', FALSE, TRUE, NULL, 12, FALSE, FALSE, TRUE, 4, 4.50, 36, 140);

-- ---------- Variants ----------
INSERT INTO product_variants (product_id, name, price_delta, is_default, sort_order)
SELECT p.id, v.name, v.delta, v.def, v.ord
FROM products p
CROSS JOIN (VALUES ('Small 10"', 0.00, FALSE, 1), ('Medium 12"', 3.00, TRUE, 2), ('Large 14"', 6.00, FALSE, 3), ('Family 16"', 9.00, FALSE, 4))
    AS v(name, delta, def, ord)
WHERE p.category_id = pg_temp.cat('pizza');

INSERT INTO product_variants (product_id, name, price_delta, is_default, sort_order) VALUES
(pg_temp.prod('crispy-chicken'),  '2 pieces', 0.00, TRUE, 1),
(pg_temp.prod('crispy-chicken'),  '4 pieces', 4.50, FALSE, 2),
(pg_temp.prod('crispy-chicken'),  '8 pieces', 11.00, FALSE, 3),
(pg_temp.prod('spicy-chicken'),   '2 pieces', 0.00, TRUE, 1),
(pg_temp.prod('spicy-chicken'),   '4 pieces', 4.50, FALSE, 2),
(pg_temp.prod('spicy-chicken'),   '8 pieces', 11.00, FALSE, 3),
(pg_temp.prod('chicken-wings'),   '6 wings', 0.00, TRUE, 1),
(pg_temp.prod('chicken-wings'),   '12 wings', 7.00, FALSE, 2),
(pg_temp.prod('chicken-wings'),   '24 wings', 16.00, FALSE, 3),
(pg_temp.prod('chicken-nuggets'), '6 pieces', 0.00, TRUE, 1),
(pg_temp.prod('chicken-nuggets'), '10 pieces', 2.50, FALSE, 2),
(pg_temp.prod('chicken-nuggets'), '20 pieces', 6.50, FALSE, 3),
(pg_temp.prod('chicken-tenders'), '3 tenders', 0.00, TRUE, 1),
(pg_temp.prod('chicken-tenders'), '5 tenders', 3.50, FALSE, 2),
(pg_temp.prod('french-fries'),    'Regular', 0.00, TRUE, 1),
(pg_temp.prod('french-fries'),    'Large', 1.50, FALSE, 2),
(pg_temp.prod('onion-rings'),     'Regular', 0.00, TRUE, 1),
(pg_temp.prod('onion-rings'),     'Large', 1.50, FALSE, 2),
(pg_temp.prod('mozzarella-sticks'), '5 sticks', 0.00, TRUE, 1),
(pg_temp.prod('mozzarella-sticks'), '8 sticks', 2.50, FALSE, 2),
(pg_temp.prod('garlic-bread'),    '4 slices', 0.00, TRUE, 1),
(pg_temp.prod('garlic-bread'),    '8 slices', 2.50, FALSE, 2),
(pg_temp.prod('chocolate-chip-cookies'), '2 cookies', 0.00, TRUE, 1),
(pg_temp.prod('chocolate-chip-cookies'), '6 cookies', 4.00, FALSE, 2),
(pg_temp.prod('ice-cream-sundae'), 'Single scoop', 0.00, TRUE, 1),
(pg_temp.prod('ice-cream-sundae'), 'Double scoop', 1.50, FALSE, 2),
(pg_temp.prod('coffee'),          'Regular', 0.00, TRUE, 1),
(pg_temp.prod('coffee'),          'Large', 0.80, FALSE, 2),
(pg_temp.prod('milkshake'),       'Regular', 0.00, TRUE, 1),
(pg_temp.prod('milkshake'),       'Large', 1.50, FALSE, 2);

INSERT INTO product_variants (product_id, name, price_delta, is_default, sort_order)
SELECT p.id, v.name, v.delta, v.def, v.ord
FROM products p
CROSS JOIN (VALUES ('Regular', 0.00, TRUE, 1), ('Large', 3.00, FALSE, 2)) AS v(name, delta, def, ord)
WHERE p.category_id IN (pg_temp.cat('pasta'), pg_temp.cat('rice'));

INSERT INTO product_variants (product_id, name, price_delta, is_default, sort_order)
SELECT p.id, v.name, v.delta, v.def, v.ord
FROM products p
CROSS JOIN (VALUES ('Small', 0.00, FALSE, 1), ('Medium', 0.50, TRUE, 2), ('Large', 1.00, FALSE, 3)) AS v(name, delta, def, ord)
WHERE p.slug IN ('fountain-soda', 'fresh-lemonade', 'iced-tea');

-- ---------- Product <-> customization group links ----------
INSERT INTO product_customization_groups (product_id, group_id, sort_order)
SELECT p.id, g.id, x.ord
FROM (VALUES
    -- pizza: crust, toppings, extra cheese
    ('pizza', 'Crust', 1), ('pizza', 'Pizza Toppings', 2), ('pizza', 'Extra Cheese', 3),
    -- burgers
    ('burgers', 'Patty', 1), ('burgers', 'Cheese', 2), ('burgers', 'Sauces', 3), ('burgers', 'Burger Extras', 4), ('burgers', 'Make It a Meal', 5),
    -- sandwiches & wraps
    ('sandwiches', 'Bread or Wrap', 1), ('sandwiches', 'Sauces', 2), ('sandwiches', 'Sandwich Add-ons', 3),
    -- pasta
    ('pasta', 'Pasta Sauce', 1), ('pasta', 'Add Protein', 2), ('pasta', 'Extra Cheese', 3),
    -- rice
    ('rice', 'Rice Extras', 1)
) AS x(cat_slug, group_name, ord)
JOIN products p ON p.category_id = pg_temp.cat(x.cat_slug) AND p.product_type = 'STANDARD'
JOIN customization_groups g ON g.name = x.group_name;

-- Veggie burger has no beef patty choice
DELETE FROM product_customization_groups
WHERE product_id = pg_temp.prod('veggie-burger') AND group_id = pg_temp.grp('Patty');
-- Chicken burger: single/double fillet still makes sense; keep Patty.

INSERT INTO product_customization_groups (product_id, group_id, sort_order)
SELECT pg_temp.prod(x.slug), pg_temp.grp(x.group_name), x.ord
FROM (VALUES
    ('crispy-chicken', 'Spice Level', 1), ('crispy-chicken', 'Dips', 2), ('crispy-chicken', 'Make It a Meal', 3),
    ('spicy-chicken', 'Spice Level', 1), ('spicy-chicken', 'Dips', 2), ('spicy-chicken', 'Make It a Meal', 3),
    ('chicken-wings', 'Spice Level', 1), ('chicken-wings', 'Dips', 2), ('chicken-wings', 'Make It a Meal', 3),
    ('chicken-nuggets', 'Dips', 1), ('chicken-nuggets', 'Make It a Meal', 2),
    ('chicken-tenders', 'Spice Level', 1), ('chicken-tenders', 'Dips', 2), ('chicken-tenders', 'Make It a Meal', 3),
    ('french-fries', 'Dips', 1),
    ('loaded-fries', 'Dips', 1),
    ('onion-rings', 'Dips', 1),
    ('mozzarella-sticks', 'Dips', 1),
    ('shawarma-wrap', 'Spice Level', 4),
    ('ice-cream-sundae', 'Sundae Toppings', 1),
    ('fountain-soda', 'Ice', 1),
    ('fresh-lemonade', 'Ice', 1),
    ('iced-tea', 'Ice', 1),
    ('coffee', 'Drink Extras', 1),
    ('milkshake', 'Drink Extras', 1)
) AS x(slug, group_name, ord);

-- ---------- Combo components ----------
INSERT INTO combo_components (combo_product_id, name, quantity, sort_order) VALUES
(pg_temp.prod('burger-combo'), 'Choose your burger', 1, 1),
(pg_temp.prod('burger-combo'), 'Choose a side', 1, 2),
(pg_temp.prod('burger-combo'), 'Choose a drink', 1, 3),
(pg_temp.prod('pizza-combo'), 'Choose your pizza', 1, 1),
(pg_temp.prod('pizza-combo'), 'Choose a side to share', 1, 2),
(pg_temp.prod('pizza-combo'), 'Choose two drinks', 2, 3),
(pg_temp.prod('family-bucket'), 'Choose 4 chicken servings', 4, 1),
(pg_temp.prod('family-bucket'), 'Choose 2 sides', 2, 2),
(pg_temp.prod('family-bucket'), 'Choose 4 drinks', 4, 3),
(pg_temp.prod('wings-wrap-deal'), 'Choose your wrap', 1, 1),
(pg_temp.prod('wings-wrap-deal'), 'Choose your wings', 1, 2),
(pg_temp.prod('wings-wrap-deal'), 'Choose a drink', 1, 3);

CREATE FUNCTION pg_temp.comp(combo TEXT, comp_name TEXT) RETURNS BIGINT AS $$
    SELECT c.id FROM combo_components c JOIN products p ON p.id = c.combo_product_id
    WHERE p.slug = combo AND c.name = comp_name
$$ LANGUAGE sql STABLE;

INSERT INTO combo_component_choices (component_id, product_id, extra_price, is_default)
SELECT pg_temp.comp(x.combo, x.comp), pg_temp.prod(x.slug), x.extra, x.def
FROM (VALUES
    ('burger-combo', 'Choose your burger', 'classic-smash-burger', 0.00, TRUE),
    ('burger-combo', 'Choose your burger', 'double-cheeseburger', 1.50, FALSE),
    ('burger-combo', 'Choose your burger', 'crispy-chicken-burger', 0.00, FALSE),
    ('burger-combo', 'Choose your burger', 'smokehouse-bbq-burger', 2.00, FALSE),
    ('burger-combo', 'Choose your burger', 'veggie-burger', 0.00, FALSE),
    ('burger-combo', 'Choose a side', 'french-fries', 0.00, TRUE),
    ('burger-combo', 'Choose a side', 'onion-rings', 1.00, FALSE),
    ('burger-combo', 'Choose a side', 'loaded-fries', 2.50, FALSE),
    ('burger-combo', 'Choose a drink', 'fountain-soda', 0.00, TRUE),
    ('burger-combo', 'Choose a drink', 'fresh-lemonade', 0.50, FALSE),
    ('burger-combo', 'Choose a drink', 'iced-tea', 0.50, FALSE),
    ('burger-combo', 'Choose a drink', 'milkshake', 2.50, FALSE),

    ('pizza-combo', 'Choose your pizza', 'margherita-pizza', 0.00, TRUE),
    ('pizza-combo', 'Choose your pizza', 'pepperoni-pizza', 1.50, FALSE),
    ('pizza-combo', 'Choose your pizza', 'bbq-chicken-pizza', 2.50, FALSE),
    ('pizza-combo', 'Choose your pizza', 'meat-lovers-pizza', 3.50, FALSE),
    ('pizza-combo', 'Choose your pizza', 'vegetarian-pizza', 1.50, FALSE),
    ('pizza-combo', 'Choose a side to share', 'garlic-bread', 0.00, TRUE),
    ('pizza-combo', 'Choose a side to share', 'mozzarella-sticks', 1.00, FALSE),
    ('pizza-combo', 'Choose a side to share', 'chicken-wings', 2.50, FALSE),
    ('pizza-combo', 'Choose two drinks', 'fountain-soda', 0.00, TRUE),
    ('pizza-combo', 'Choose two drinks', 'fresh-lemonade', 0.50, FALSE),
    ('pizza-combo', 'Choose two drinks', 'iced-tea', 0.50, FALSE),

    ('family-bucket', 'Choose 4 chicken servings', 'crispy-chicken', 0.00, TRUE),
    ('family-bucket', 'Choose 4 chicken servings', 'spicy-chicken', 0.00, FALSE),
    ('family-bucket', 'Choose 4 chicken servings', 'chicken-tenders', 0.50, FALSE),
    ('family-bucket', 'Choose 2 sides', 'french-fries', 0.00, TRUE),
    ('family-bucket', 'Choose 2 sides', 'onion-rings', 0.50, FALSE),
    ('family-bucket', 'Choose 2 sides', 'loaded-fries', 2.00, FALSE),
    ('family-bucket', 'Choose 4 drinks', 'fountain-soda', 0.00, TRUE),
    ('family-bucket', 'Choose 4 drinks', 'iced-tea', 0.25, FALSE),

    ('wings-wrap-deal', 'Choose your wrap', 'shawarma-wrap', 0.00, TRUE),
    ('wings-wrap-deal', 'Choose your wrap', 'crispy-chicken-wrap', 0.00, FALSE),
    ('wings-wrap-deal', 'Choose your wings', 'chicken-wings', 0.00, TRUE),
    ('wings-wrap-deal', 'Choose your wings', 'chicken-nuggets', 0.00, FALSE),
    ('wings-wrap-deal', 'Choose a drink', 'fountain-soda', 0.00, TRUE),
    ('wings-wrap-deal', 'Choose a drink', 'fresh-lemonade', 0.50, FALSE)
) AS x(combo, comp, slug, extra, def);

-- Gallery images (primary image is products.image_url; these are extras)
INSERT INTO product_images (product_id, url, alt_text, sort_order)
SELECT id, image_url, name, 0 FROM products;

-- ---------- Promotions (auto-applied, no code needed) ----------
INSERT INTO promotions (name, description, image_url, discount_type, discount_value, category_id, product_id, starts_at, ends_at, active) VALUES
('Pizza Week: 20% Off', 'Every pizza on the menu is 20% off. No code needed.', pg_temp.img('1513104890138-7c749659a591'), 'PERCENT', 20, pg_temp.cat('pizza'), NULL, now() - interval '1 day', now() + interval '60 days', TRUE),
('Sweet Deal', '$1 off every dessert, every day.', pg_temp.img('1606313564200-e75d5e30476c'), 'FIXED', 1.00, pg_temp.cat('desserts'), NULL, NULL, NULL, TRUE),
('Shake It Up', '15% off thick milkshakes this month.', pg_temp.img('1572490122747-3968b75cc699'), 'PERCENT', 15, NULL, pg_temp.prod('milkshake'), now() - interval '1 day', now() + interval '30 days', TRUE);

-- ---------- Coupons ----------
INSERT INTO coupons (code, description, discount_type, discount_value, min_order_amount, max_discount_amount, category_id, starts_at, expires_at, max_redemptions, per_user_limit) VALUES
('WELCOME10', '10% off your order (max $10) on orders over $15', 'PERCENT', 10, 15.00, 10.00, NULL, NULL, NULL, NULL, 1),
('SAVE5', '$5 off orders of $30 or more', 'FIXED', 5, 30.00, NULL, NULL, NULL, now() + interval '180 days', 1000, NULL),
('COMBO15', '15% off Combos & Meal Deals', 'PERCENT', 15, 0, 15.00, pg_temp.cat('combos'), NULL, NULL, NULL, NULL);
