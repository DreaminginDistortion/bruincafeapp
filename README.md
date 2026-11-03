# Bruin Café — Android/Kotlin Proof of Concept

A working, click-through proof of concept for the HQ Bruin Café app, built with
**Kotlin + Jetpack Compose**. It implements the customer ordering flow and the
staff inventory flow from Deliverable 2 (Requirements, Workflows, and Data
Design Package), using in-memory mock data shaped like the `bruin_cafe_menu`
Supabase table.

This doc is written assuming most readers are coming from **Swift/SwiftUI** —
the two frameworks map closely enough that the structure should feel familiar
even if the syntax doesn't.

## Quick concept map (Kotlin/Compose → Swift/SwiftUI)

| This project | SwiftUI equivalent |
|---|---|
| `@Composable fun Screen(...)` | `struct Screen: View { var body: some View {...} }` |
| `CafeViewModel : ViewModel()` + `StateFlow` | `class CafeViewModel: ObservableObject` + `@Published` |
| `val uiState by viewModel.uiState.collectAsState()` | `@ObservedObject var viewModel: CafeViewModel` |
| `NavHost` / `NavController` (Navigation-Compose) | `NavigationStack` / `NavigationPath` |
| `data class MenuItem(...)` | `struct MenuItem: Identifiable, Codable` |
| `object MockData { ... }` | a static/mock data source file |
| `object CafeRepository { ... }` | a `CafeService` protocol + implementation |
| Material3 `MaterialTheme` + `Color.kt` | an Asset Catalog + a `Color` extension |

## Project layout (also see supabase/ and tests/ at the project root)

```
app/src/main/java/com/bruincafe/app/
  MainActivity.kt              -- entry point (approx. @main App)
  data/
    Models.kt                  -- MenuItem (with mealPeriod), MealPeriodDef, DayMenu, InventoryItem...
    MealScheduler.kt           -- clock -> which meal is being served (breakfast/lunch)
    MenuStore.kt               -- menu rules (pure Kotlin, unit-tested): items for location+date+meal
    SupabaseConfig.kt          -- Supabase URL + anon key (already filled in)
    Rewards.kt                 -- order/points model + rewards rules + accessibility settings model
    SpecialMenu.kt             -- café + holiday item rules (holiday only inside active dates)
    AppStorage.kt              -- on-device storage: order history + accessibility choices
    SupabaseApi.kt             -- REST calls to Supabase (no SDK needed)
    MockData.kt                -- THE FILE TO EDIT to change menu/inventory content
    CafeRepository.kt          -- holds current data (mock or Supabase) + inventory + sync
  ui/
    CafeViewModel.kt           -- shared app state (location, cart, staff session)
    theme/Color.kt              -- THE FILE TO EDIT to reskin the app's colors (contrast-checked)
    theme/Theme.kt
    navigation/NavGraph.kt     -- wires all 9 screens together
    screens/
      HomeScreen.kt            -- S-01 (Rewards home)
      LocationSelectScreen.kt  -- S-02
      WeeklyMenuScreen.kt      -- S-03
      ItemDetailScreen.kt      -- S-04
      CartScreen.kt            -- S-05
      CheckoutScreen.kt        -- S-06
      StaffLoginScreen.kt      -- S-07
      InventoryDashboardScreen.kt -- S-08
      RestockSimulationScreen.kt  -- S-09
      OrderHistoryScreen.kt    -- S-10 (new)
      CafeHolidayScreen.kt     -- S-11 (new)
      AccessibilitySettingsScreen.kt -- S-12 (new)
      Common.kt                -- shared components (StockBadge, etc.)
```

Screen IDs (S-01..S-09) match the Screen Inventory table in the Deliverable 2
Word doc, and the file names match the boxes in the Navigation Map diagram —
so the diagram, the requirements doc, and the code all point at the same
vocabulary.

## What's implemented

- **Location selection** scopes every later screen (WLM-01).
- **Weekly lunch menu**, grouped by weekday, with a themed multi-item day
  (Thursday "International Day") to demonstrate WLM-05.
- **Holiday/closed-day rule** — Monday shows a closure message, no items (WLM-04).
- **Today's special** — auto-computed from a **simulated "today" date picker**
  at the top of the weekly menu screen (a demo convenience, since the sample
  week is a fixed calendar week and the device's real date won't usually
  match it).
- **HQ-only ready-made breakfast items** — only appear when Headquarters is selected.
- **Sold-out rule** — items at 0 quantity show a "Sold Out" badge and cannot
  be added to cart, enforced in both the UI and the ViewModel.
- **Low-stock rule** — items at/below a threshold show a "Low Stock" badge,
  both to customers (softly) and to staff (as a dashboard alert).
- **Mock order simulation** — placing an order decrements the shared
  in-memory inventory, so the customer and staff views of the same item
  reflect a live order.
- **Staff login** (mock auth), **inventory dashboard** by location, and
  **restock simulation** that updates inventory and re-renders both staff
  and customer screens.

## How to run it

1. Open the `BruinCafeApp/` folder in **Android Studio** (Koala or newer).
   Let it sync Gradle — this project uses standard AndroidX + Jetpack
   Compose dependencies, no custom repositories.
2. Run on any emulator or device with **API 26+**.
3. **Compilation status:** the real Android/Jetpack Compose libraries come
   from Google's Maven repository, which wasn't reachable from the sandbox
   this was built in, so a full Android Studio build could not be run here.
   What *was* verified: the actual Kotlin compiler (`kotlinc` 1.9.24) was run
   against every source file in this project as one module. With the
   Android/Compose/Navigation libraries unavailable, `androidx.*` symbols
   understandably came back "unresolved reference" — but that check also
   confirmed there are **no syntax errors** and **every cross-file reference
   between our own classes resolves correctly** (ViewModel ↔ Repository ↔
   Models ↔ screens ↔ nav graph all line up). It also caught one real bug —
   a `return@Scaffold` inside a nested `Column` block in `CheckoutScreen.kt`,
   which Kotlin doesn't allow across a non-inline lambda boundary — now fixed
   with a plain if/else instead. The breakfast/lunch scheduling and menu rules
   (`MealScheduler`, `MenuStore`) were additionally **compiled and executed**
   on a plain JVM, and all checks in `tests/` pass. The Supabase calls
   (`SupabaseApi`) have **not** been run against a live project. Still budget a first Gradle sync in Android
   Studio to catch anything only the real Compose type-checker would see
   (e.g. exact parameter names/overloads on library functions).

## What's new: rewards, order history, café/holiday menu, accessibility

| Item from the team's list | What was built |
|---|---|
| Home screen for rewards | **S-01 is now the Rewards home**: points total, progress bar to the next reward, rewards unlocked, then buttons for Order Now, Café & Holiday Menu, Order History, Accessibility Settings, Staff Login. Rules: 10 points per item, 100 points = 1 reward (`Rewards.kt`). Points are *added up from order history*, so they can never disagree with it. |
| Order history page | **S-10**: newest-first list of completed orders (date, location, items, points). Saved on the device only (`AppStorage.kt`), newest 50 kept. Nothing about orders is sent to Supabase (data minimization). |
| Café items + holiday menu page | **S-11**: two tabs. *Café Items* = always available, made to order. *Holiday Menu* = shown **only while today is inside a holiday's active dates** (both end dates count); otherwise it says none is active and when the next one starts. Data is in the new `special_items` table (7 café + 9 holiday rows seeded). A demo date picker lets you show each holiday window. |
| Bug: cart not appearing on HQ page | Two causes fixed. (1) The cart bar sat at the bottom of the screen's content column, under the demo controls and the scrolling list, so on small screens / bigger text / the content-heavy HQ menu it could be pushed off-screen. It is now the Scaffold `bottomBar`, pinned on every menu screen (`CartBottomBar` in `Common.kt`). (2) `selectLocation()` cleared the cart *every time* a location was tapped, even re-tapping Headquarters, so a cart built at HQ vanished when you came back through Order or the café page. It now clears only when the location actually changes. *The original team app was not available, so these are the causes found in this project's code; verify against your build.* |
| Accessibility | See below. Screen **S-12** lets the customer choose text size and high contrast. |

### Accessibility features (implemented)

- **Screen readers (TalkBack):** every menu/café/holiday/order card is read as one sentence (e.g. "Tiramisu. Lunch, Dessert. Low stock. International Day Italy."); tappable items announce what they do ("View details for ..."); every screen title is a *heading*; the cart bar reads "Cart, 3 items. Double tap to open."
- **Announcements:** the "Now serving" banner is a live region, so the breakfast-to-lunch switch at 11:00 is announced; adding to cart posts a snackbar ("Added X to your cart."); order confirmation, form errors, and restock changes are live regions.
- **Not color alone:** stock is always written out (In Stock / Low Stock / Sold Out); selected chips and tabs are announced as selected.
- **Touch targets:** buttons, chips and rows are at least 48dp (main buttons 56dp).
- **Color contrast (WCAG AA):** the original brand gold (2.95:1 on cream) failed for text, so a darker `CafeGoldDark` is used for gold text and gold banners. `tests/contrast_check.py` reads the real hex values from `Color.kt` and checks 14 pairs; all pass (5.8:1 to 21:1).
- **Text size:** all text is in `sp`, so the phone's font size is honored; the app adds Default / Large (1.3x) / Extra large (1.6x) on top. Screens scroll so nothing is cut off at large sizes.
- **High contrast mode:** black on white, very dark green, heavy 2dp outlines on cards, chips and banners. Every screen takes colors from the theme (audited: no raw brand colors in screens), so one switch restyles the whole app.
- **Forms:** staff login fields have labels, error state, "Next/Done" keyboard actions, and errors are read aloud. A visible **Back** button on every sub-screen.

**Not covered / still to do:** test with a real TalkBack session and Switch Access on a device, support landscape layouts, and a full accessibility review before release.

## Breakfast / lunch switching + Supabase

**How the app decides what to show.** Every menu item has a `meal_period`
(`breakfast` or `lunch`). Serving hours live in the `meal_periods` table
(default: breakfast 7:00-11:00 AM, lunch 11:00 AM-2:00 PM; the lunch end time
is a placeholder, change it to your real hours). The menu screen compares the
clock to those windows and shows only the active meal's items:

| Time | Screen shows |
|---|---|
| before 7:00 AM | "Kitchen closed - next up: Breakfast at 7:00 AM" |
| 7:00-10:59 AM | breakfast items + "Now serving: Breakfast" banner |
| 11:00 AM-1:59 PM | that day's lunch + today's special + "Now serving: Lunch" banner |
| 2:00 PM and later | "Kitchen closed - next up: ..." |

The clock is re-read every 15 seconds, so the menu flips by itself at 11:00 with
the screen open. Each item card is labelled (e.g. "Breakfast - Entree"). A
holiday row (`is_closed`) closes the whole day for both meals. For demos there
are "Demo: time of day" chips (Live / 8 AM / 12 PM / 4 PM) so you can show the
switch without waiting for 11 AM.

**Status: already connected.** The Supabase project *Bruin Cafe Mobile app*
has the four tables, the breakfast + lunch seed data, Row Level Security and
the API grants. `data/SupabaseConfig.kt` is already filled in with that
project's URL and public anon key, so the app loads live data on launch (the
menu screen shows `Data: Supabase (live)`). Menus are read-only for the app's
key; only `inventory` can be written. The steps below are for setting up a
different project.

**Hooking up Supabase to a new project (about 5 minutes)**

1. Supabase dashboard -> SQL Editor -> paste all of `supabase/schema_and_seed.sql` -> Run.
   This creates `locations`, `meal_periods`, `menu_items`, `inventory`, turns on
   Row Level Security, grants the API roles access, and loads the breakfast + lunch data.
2. Dashboard -> Project Settings -> API: copy the **Project URL** and the
   **anon public** key into `data/SupabaseConfig.kt`. (Never use the `service_role` key.)
3. Run the app. The menu screen shows `Data: Supabase (live)` when it worked,
   or the reason it fell back to mock data (with a Refresh button).

Restocks and mock orders are saved back to the `inventory` table. For the POC
the anon key is allowed to write `inventory`; before launch, remove the two
"poc ... inventory" policies and restrict writes to signed-in staff.

Because it's plain REST (`GET {url}/rest/v1/menu_items?select=*` with `apikey`
and `Authorization: Bearer` headers), the Swift app can use the same tables and
the same requests with `URLSession`; the SQL file is shared by both apps.

**Tests (no Android needed).** `tests/MealLogicTest.kt` (33 checks: boundary times like 10:59 -> 11:00, closed days, per-location menus), `tests/FeatureLogicTest.kt` (24 checks: holiday first/last active day, rewards math, café lists) and `tests/contrast_check.py` (14 color pairs). The Kotlin tests are compiled separately; see the header of each file.

## Making it "easily changeable"

Two files are meant to be the main edit points:

- **`data/MockData.kt`** — every menu item, weekday, closure message, and
  inventory quantity lives here as plain Kotlin data, shaped exactly like the
  Supabase table columns. Add a day, add an item, change a quantity — nothing
  else needs to change.
- **`ui/theme/Color.kt`** — every screen pulls its palette from here via
  `MaterialTheme.colorScheme`. Change five hex values, the whole app reskins.

When the real Supabase backend is ready, `CafeRepository.kt` is the only
other file that should need real changes: replace its function bodies with
network calls that return the same types, and every screen above it keeps
working unmodified — screens never talk to `MockData` directly.

## Suggested next steps

- Wire `CafeRepository` to the actual Supabase table via the Supabase Kotlin
  client (`supabase-kt`), matching the columns already used in `Models.kt`.
- Replace the demo "simulated today" picker with the device's real date once
  a live weekly menu is being published on a rolling basis.
- Add persistence for the cart and staff session (currently in-memory only,
  resets on process death) via DataStore.
