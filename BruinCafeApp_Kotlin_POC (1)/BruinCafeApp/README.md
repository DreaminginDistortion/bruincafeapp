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

## Project layout

```
app/src/main/java/com/bruincafe/app/
  MainActivity.kt              -- entry point (approx. @main App)
  data/
    Models.kt                  -- MenuItem, DayMenu, InventoryItem, CartLine, etc.
    MockData.kt                -- THE FILE TO EDIT to change menu/inventory content
    CafeRepository.kt          -- business rules; swap for real Supabase calls later
  ui/
    CafeViewModel.kt           -- shared app state (location, cart, staff session)
    theme/Color.kt              -- THE FILE TO EDIT to reskin the app's colors
    theme/Theme.kt
    navigation/NavGraph.kt     -- wires all 9 screens together
    screens/
      HomeScreen.kt            -- S-01
      LocationSelectScreen.kt  -- S-02
      WeeklyMenuScreen.kt      -- S-03
      ItemDetailScreen.kt      -- S-04
      CartScreen.kt            -- S-05
      CheckoutScreen.kt        -- S-06
      StaffLoginScreen.kt      -- S-07
      InventoryDashboardScreen.kt -- S-08
      RestockSimulationScreen.kt  -- S-09
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
   with a plain if/else instead. Still budget a first Gradle sync in Android
   Studio to catch anything only the real Compose type-checker would see
   (e.g. exact parameter names/overloads on library functions).

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
