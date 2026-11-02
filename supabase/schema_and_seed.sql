-- ============================================================================
--  Bruin Café -- Supabase schema + seed data  (breakfast AND lunch)
--  Run this whole file once in:  Supabase dashboard -> SQL Editor -> New query
--  It is safe to re-run: tables use IF NOT EXISTS, seeds use ON CONFLICT.
--  It does NOT touch your older `bruin_cafe_menu` table.
-- ============================================================================

-- 1) TABLES ------------------------------------------------------------------

create table if not exists locations (
  id            text primary key,              -- 'hq' | 'tech_center'
  display_name  text not null,
  subtitle      text,
  sort_order    int  not null default 0
);

-- Serving windows. THE APP READS THESE TO DECIDE BREAKFAST vs LUNCH.
-- Change the hours here (e.g. breakfast 07:00-10:30) and every app updates.
-- start_time is inclusive, end_time is exclusive.
create table if not exists meal_periods (
  name        text primary key,                -- machine key: 'breakfast' | 'lunch'
  label       text not null,                   -- display text
  start_time  time not null,
  end_time    time not null,
  check (start_time <> end_time)
);

-- One row per menu item, tagged with the meal it belongs to.
--   menu_date NULL     -> served every open day during that meal period (e.g. breakfast staples)
--   menu_date = a date -> only served that day (e.g. the weekly lunch special)
--   is_closed = true   -> the whole location is closed that date (holiday); name/description ignored
create table if not exists menu_items (
  id             text primary key,
  location_id    text not null references locations(id),
  meal_period    text not null references meal_periods(name),
  name           text not null,
  description    text,
  course         text not null default 'entree'
                   check (course in ('appetizer','entree','dessert')),
  menu_date      date,
  day_of_week    text,
  is_closed      boolean not null default false,
  closed_message text,
  sort_order     int not null default 0
);
create index if not exists menu_items_lookup
  on menu_items (location_id, meal_period, menu_date);

create table if not exists inventory (
  item_id              text primary key references menu_items(id) on delete cascade,
  quantity_on_hand     int  not null default 0 check (quantity_on_hand >= 0),
  low_stock_threshold  int  not null default 5
);

-- 2) SECURITY (Row Level Security) ---------------------------------------------
-- The app uses the public "anon" key, so RLS is what protects your data.
-- Menus are public-read. Inventory is also writable by anon so the POC's restock
-- and mock-order buttons can save -- TIGHTEN THIS before a real launch (see notes).

alter table locations    enable row level security;
alter table meal_periods enable row level security;
alter table menu_items   enable row level security;
alter table inventory    enable row level security;

drop policy if exists "public read locations"    on locations;
drop policy if exists "public read meal_periods" on meal_periods;
drop policy if exists "public read menu_items"   on menu_items;
drop policy if exists "public read inventory"    on inventory;
drop policy if exists "poc insert inventory"     on inventory;
drop policy if exists "poc update inventory"     on inventory;

create policy "public read locations"    on locations    for select to anon, authenticated using (true);
create policy "public read meal_periods" on meal_periods for select to anon, authenticated using (true);
create policy "public read menu_items"   on menu_items   for select to anon, authenticated using (true);
create policy "public read inventory"    on inventory    for select to anon, authenticated using (true);

-- POC ONLY: lets the app save restocks/orders with the anon key.
create policy "poc insert inventory" on inventory for insert to anon, authenticated with check (true);
create policy "poc update inventory" on inventory for update to anon, authenticated using (true) with check (true);

-- 3) OPTIONAL: ask the DATABASE which meal is being served right now ----------
-- The app decides this itself from the device clock, but this is handy for a
-- web dashboard or the iOS app:   POST {url}/rest/v1/rpc/current_meal_period
create or replace function public.current_meal_period(tz text default 'America/New_York')
returns text
language sql stable
set search_path = ''
as $$
  select name
  from public.meal_periods
  where (now() at time zone tz)::time >= start_time
    and (now() at time zone tz)::time <  end_time
  order by start_time
  limit 1;
$$;

-- 2b) GRANTS ------------------------------------------------------------------
-- Newer Supabase projects do not auto-grant new tables to the API roles, and
-- without these the app's anon key gets "permission denied". RLS policies above
-- still apply on top of these grants.
grant usage on schema public to anon, authenticated;
grant select on public.locations, public.meal_periods, public.menu_items, public.inventory to anon, authenticated;
grant insert, update on public.inventory to anon, authenticated;   -- POC: restock + mock orders
grant execute on function public.current_meal_period(text) to anon, authenticated;

-- 4) SEED DATA ---------------------------------------------------------------

insert into locations (id, display_name, subtitle, sort_order) values
  ('hq',          'Headquarters', 'HQ Bruin Café',    1),
  ('tech_center', 'Tech Center',  'Tech Center Café', 2)
on conflict (id) do nothing;

insert into meal_periods (name, label, start_time, end_time) values
  ('breakfast', 'Breakfast', '07:00', '11:00'),
  ('lunch',     'Lunch',     '11:00', '14:00')      -- lunch end time is an assumption: edit it
on conflict (name) do nothing;

-- Breakfast (undated = every open day). HQ only for now.
insert into menu_items (id, location_id, meal_period, name, description, course, sort_order) values
  ('hq-bfast-burrito',   'hq', 'breakfast', 'Ready-Made Breakfast Burrito',  'Scrambled egg, cheddar, potato, salsa. Grab-and-go.',          'entree',    1),
  ('hq-bfast-croissant', 'hq', 'breakfast', 'Bacon, Egg & Cheese Croissant', 'Flaky butter croissant with bacon, egg, and melted cheddar.', 'entree',    2),
  ('hq-bfast-parfait',   'hq', 'breakfast', 'Yogurt Parfait Cup',            'Vanilla yogurt, granola, seasonal berries.',                    'dessert',   3),
  ('hq-bfast-fruit',     'hq', 'breakfast', 'Fresh Fruit Cup',               'Seasonal cut fruit.',                                           'appetizer', 4)
on conflict (id) do nothing;

-- Lunch, week of Sept 7-11, 2026 (dated). Add a new batch of rows each week.
insert into menu_items (id, location_id, meal_period, name, description, course, menu_date, day_of_week, is_closed, closed_message, sort_order) values
  ('hq-2026-09-07-closed', 'hq', 'lunch', 'Closed - Labor Day', null, 'entree', '2026-09-07', 'Monday', true,
     'Closed - Labor Day. We''ll be back Tuesday with a fresh week of chef-prepared specials.', 0),
  ('hq-tue-soup',     'hq', 'lunch', 'Roasted Tomato Soup & Grilled Cheese',
     'Rich, slow-roasted tomato soup blended with aromatic herbs, served with a golden buttery grilled cheese.', 'entree', '2026-09-08', 'Tuesday',   false, null, 1),
  ('hq-wed-chicken',  'hq', 'lunch', 'Homestyle Chicken & Rice',
     'Tender, seasoned chicken served over savory rice.', 'entree', '2026-09-09', 'Wednesday', false, null, 1),
  ('hq-thu-antipasto','hq', 'lunch', 'Antipasto Cups',
     'International Day Italy.', 'appetizer', '2026-09-10', 'Thursday', false, null, 1),
  ('hq-thu-ravioli',  'hq', 'lunch', 'Sausage & Broccolini Ravioli with Vodka Sauce',
     'International Day Italy.', 'entree',    '2026-09-10', 'Thursday', false, null, 2),
  ('hq-thu-tiramisu', 'hq', 'lunch', 'Tiramisu',
     'International Day Italy.', 'dessert',   '2026-09-10', 'Thursday', false, null, 3),
  ('hq-fri-nachos',   'hq', 'lunch', 'Beer Cheese Philly Nachos',
     'Crispy tortilla chips piled high with seasoned Philly-style beef, sauteed peppers and onions, and warm beer cheese sauce.', 'entree', '2026-09-11', 'Friday', false, null, 1),

  ('tc-2026-09-07-closed', 'tech_center', 'lunch', 'Closed - Labor Day', null, 'entree', '2026-09-07', 'Monday', true, 'Closed - Labor Day.', 0),
  ('tc-tue-wrap',   'tech_center', 'lunch', 'Chicken Caesar Wrap',   'Grilled chicken, romaine, parmesan, house caesar dressing.', 'entree', '2026-09-08', 'Tuesday',   false, null, 1),
  ('tc-wed-bowl',   'tech_center', 'lunch', 'Southwest Grain Bowl',  'Quinoa, black beans, corn, peppers, chipotle crema.',        'entree', '2026-09-09', 'Wednesday', false, null, 1),
  ('tc-thu-burger', 'tech_center', 'lunch', 'Smash Burger & Fries',  'Double smash patty, cheddar, house sauce.',                  'entree', '2026-09-10', 'Thursday',  false, null, 1),
  ('tc-fri-pizza',  'tech_center', 'lunch', 'Margherita Flatbread',  'San marzano tomato, fresh mozzarella, basil.',               'entree', '2026-09-11', 'Friday',    false, null, 1)
on conflict (id) do nothing;

-- Starting stock (shows all three states: in stock, low stock, sold out).
insert into inventory (item_id, quantity_on_hand, low_stock_threshold) values
  ('hq-bfast-burrito', 6, 5), ('hq-bfast-croissant', 11, 5), ('hq-bfast-parfait', 2, 5), ('hq-bfast-fruit', 20, 5),
  ('hq-tue-soup', 18, 5), ('hq-wed-chicken', 3, 5), ('hq-thu-antipasto', 12, 5),
  ('hq-thu-ravioli', 0, 5), ('hq-thu-tiramisu', 4, 5), ('hq-fri-nachos', 25, 5),
  ('tc-tue-wrap', 15, 5), ('tc-wed-bowl', 9, 5), ('tc-thu-burger', 0, 5), ('tc-fri-pizza', 14, 5)
on conflict (item_id) do nothing;

-- ============================================================================
-- NOTES
--  * Add a menu item:  insert into menu_items (...) values (...);  then add its inventory row.
--  * Change serving hours:  update meal_periods set start_time='07:00', end_time='10:30' where name='breakfast';
--  * Holiday closure:  insert a row with is_closed=true, the menu_date, and a closed_message.
--  * Before launch: remove the two "poc ... inventory" policies and let only signed-in
--    staff update inventory (Supabase Auth + a staff role), instead of the open anon policy.
-- ============================================================================

-- ============================================================================
-- 5) CAFÉ ITEMS + HOLIDAY MENU  (added with the rewards / holiday-menu update)
--    kind 'cafe'    -> always available (usually made to order)
--    kind 'holiday' -> shown only from active_from through active_until (inclusive)
-- ============================================================================
create table if not exists public.special_items (
  id            text primary key,
  location_id   text not null references public.locations(id),
  kind          text not null check (kind in ('cafe','holiday')),
  holiday_name  text,
  name          text not null,
  description   text,
  course        text not null default 'entree'
                  check (course in ('appetizer','entree','dessert')),
  made_to_order boolean not null default true,
  active_from   date,
  active_until  date,
  sort_order    int not null default 0,
  check (kind <> 'holiday'
         or (active_from is not null and active_until is not null and active_until >= active_from))
);
create index if not exists special_items_lookup
  on public.special_items (location_id, kind, active_from);

alter table public.special_items enable row level security;
drop policy if exists "public read special_items" on public.special_items;
create policy "public read special_items" on public.special_items
  for select to anon, authenticated using (true);
grant select on public.special_items to anon, authenticated;     -- read-only for the app

insert into public.special_items
  (id, location_id, kind, holiday_name, name, description, course, made_to_order, active_from, active_until, sort_order) values
  ('hq-cafe-espresso',   'hq', 'cafe', null, 'Espresso',           'Double shot, pulled fresh to order.',                        'entree',  true, null, null, 1),
  ('hq-cafe-latte',      'hq', 'cafe', null, 'Café Latte',         'Espresso and steamed milk. Hot or iced.',                    'entree',  true, null, null, 2),
  ('hq-cafe-coldbrew',   'hq', 'cafe', null, 'Cold Brew',          'Slow-steeped for 16 hours, served over ice.',                'entree',  true, null, null, 3),
  ('hq-cafe-hotchoc',    'hq', 'cafe', null, 'Hot Chocolate',      'Rich chocolate with steamed milk.',                          'dessert', true, null, null, 4),
  ('tc-cafe-drip',       'tech_center', 'cafe', null, 'Drip Coffee',  'Fresh-brewed house blend.',                              'entree',  true, null, null, 1),
  ('tc-cafe-icedlatte',  'tech_center', 'cafe', null, 'Iced Latte',   'Espresso over milk and ice.',                            'entree',  true, null, null, 2),
  ('tc-cafe-chai',       'tech_center', 'cafe', null, 'Chai Latte',   'Spiced black tea with steamed milk.',                    'entree',  true, null, null, 3),
  ('hq-hol-psl',         'hq', 'holiday', 'Fall Favorites', 'Pumpkin Spice Latte',   'Espresso, pumpkin, cinnamon, and steamed milk.',        'entree',  true, '2026-10-01', '2026-10-31', 1),
  ('hq-hol-cider',       'hq', 'holiday', 'Fall Favorites', 'Caramel Apple Cider',   'Warm apple cider with caramel.',                        'dessert', true, '2026-10-01', '2026-10-31', 2),
  ('tc-hol-psl',         'tech_center', 'holiday', 'Fall Favorites', 'Pumpkin Spice Latte', 'Espresso, pumpkin, cinnamon, and steamed milk.',  'entree',  true, '2026-10-01', '2026-10-31', 1),
  ('hq-hol-pie',         'hq', 'holiday', 'Thanksgiving', 'Pumpkin Pie Slice',       'Classic pumpkin pie with whipped cream.',               'dessert', false, '2026-11-16', '2026-11-26', 1),
  ('hq-hol-turkeywrap',  'hq', 'holiday', 'Thanksgiving', 'Turkey & Cranberry Wrap', 'Roast turkey, cranberry sauce, stuffing, and greens.',  'entree',  true, '2026-11-16', '2026-11-26', 2),
  ('tc-hol-turkeywrap',  'tech_center', 'holiday', 'Thanksgiving', 'Turkey & Cranberry Wrap', 'Roast turkey, cranberry sauce, stuffing, and greens.', 'entree', true, '2026-11-16', '2026-11-26', 1),
  ('hq-hol-mocha',       'hq', 'holiday', 'Winter Holidays', 'Peppermint Mocha',     'Espresso, chocolate, peppermint, and whipped cream.',   'entree',  true, '2026-12-01', '2026-12-24', 1),
  ('hq-hol-gingerbread', 'hq', 'holiday', 'Winter Holidays', 'Gingerbread Cookie',   'Soft-baked gingerbread with icing.',                    'dessert', false, '2026-12-01', '2026-12-24', 2),
  ('tc-hol-mocha',       'tech_center', 'holiday', 'Winter Holidays', 'Peppermint Mocha', 'Espresso, chocolate, peppermint, and whipped cream.', 'entree', true, '2026-12-01', '2026-12-24', 1)
on conflict (id) do nothing;
-- To schedule a new holiday menu: insert rows with kind='holiday' and the active_from / active_until dates.
