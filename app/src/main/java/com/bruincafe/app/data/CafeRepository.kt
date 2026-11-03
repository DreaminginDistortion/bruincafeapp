package com.bruincafe.app.data

import java.time.LocalDate

/**
 * Repository -- the single seam between UI and data.
 *
 * Starts on MockData so the app always works. Calling [refreshFromSupabase]
 * swaps in live data from Supabase when SupabaseConfig is filled in; if the
 * network call fails, the previous data stays in place and an error message
 * is returned for the UI to show.
 *
 * The menu RULES live in [MenuStore] (pure, testable); this object just holds
 * the current snapshot + inventory and does the network sync.
 */
object CafeRepository {

    @Volatile private var store: MenuStore = MenuStore.fromMock()
    @Volatile private var inventory: MutableList<InventoryItem> = MockData.inventorySeed()
    @Volatile private var live: Boolean = false

    val dataSource: String
        get() = when {
            live -> "Supabase (live)"
            SupabaseConfig.isConfigured -> "Mock data (Supabase unreachable)"
            else -> "Mock data (Supabase not configured)"
        }

    // ---- Reads used by the UI -------------------------------------------------

    fun locations(): List<CafeLocation> = store.locations
    fun mealPeriods(): List<MealPeriodDef> = store.periods
    fun weeklyMenu(locationId: String): List<DayMenu> = store.weekMenu(locationId)
    fun closedMessage(locationId: String, date: String): String? = store.closedMessage(locationId, date)
    fun itemsFor(locationId: String, date: String, period: String): List<MenuItem> =
        store.itemsFor(locationId, date, period)
    fun todaysSpecial(locationId: String, date: String): List<MenuItem> =
        store.todaysSpecial(locationId, date)
    fun allMenuDates(): List<Pair<String, String>> = store.allDates()

    // Café + holiday menu (S-11)
    fun cafeItems(locationId: String): List<SpecialItem> = SpecialMenu.cafeItems(store.special, locationId)
    fun activeHolidayItems(locationId: String, date: LocalDate): List<SpecialItem> =
        SpecialMenu.activeHolidayItems(store.special, locationId, date)
    fun nextHoliday(locationId: String, date: LocalDate): SpecialItem? =
        SpecialMenu.nextHoliday(store.special, locationId, date)

    /** True if stock is counted for this item. Made-to-order café items have no inventory row. */
    fun isTracked(itemId: String): Boolean = inventory.any { it.itemId == itemId }
    fun defaultDate(): String = store.defaultDate(LocalDate.now().toString())

    fun inventoryFor(locationId: String): List<InventoryItem> =
        inventory.filter { it.locationId == locationId }

    fun stockStatus(itemId: String): StockStatus =
        inventory.firstOrNull { it.itemId == itemId }?.status ?: StockStatus.IN_STOCK

    fun quantityOnHand(itemId: String): Int =
        inventory.firstOrNull { it.itemId == itemId }?.quantityOnHand ?: 0

    // ---- Local inventory changes (instant); pushInventory() syncs them ---------

    fun restock(itemId: String, addQuantity: Int) {
        val row = inventory.firstOrNull { it.itemId == itemId } ?: return
        row.quantityOnHand = (row.quantityOnHand + addQuantity).coerceAtLeast(0)
    }

    /** Decrements inventory per cart line; returns false (changing nothing) if any line would go below zero. */
    fun placeMockOrder(lines: List<CartLine>): Boolean {
        // Items with no inventory row (made-to-order café items) are untracked and always orderable.
        val wouldGoNegative = lines.any { line ->
            val row = inventory.firstOrNull { it.itemId == line.item.id }
            row != null && row.quantityOnHand < line.quantity
        }
        if (wouldGoNegative) return false
        lines.forEach { line ->
            inventory.firstOrNull { it.itemId == line.item.id }?.let { it.quantityOnHand -= line.quantity }
        }
        return true
    }

    fun validateStaffLogin(username: String, password: String): Boolean =
        username.isNotBlank() && password.isNotBlank() &&
            MockData.staffUsernames.any { it.equals(username, ignoreCase = true) }

    // ---- Supabase sync (blocking; call off the main thread) --------------------

    /** Returns null on success (or when Supabase isn't configured), else a message for the UI. */
    fun refreshFromSupabase(): String? {
        if (!SupabaseConfig.isConfigured) return null
        return try {
            val locations = SupabaseApi.fetchLocations()
            val periods = SupabaseApi.fetchMealPeriods()
            val rows = SupabaseApi.fetchMenuRows()
            val inv = SupabaseApi.fetchInventory()
            check(locations.isNotEmpty() && periods.isNotEmpty() && rows.isNotEmpty()) {
                "Supabase tables are empty - run supabase/schema_and_seed.sql"
            }
            val itemsById = rows.associate { it.item.id to it.item }
            val newInventory = inv.mapNotNull { r ->
                itemsById[r.itemId]?.let {
                    InventoryItem(r.itemId, it.name, it.locationId, r.quantity, r.threshold)
                }
            }.toMutableList()

            // If the special_items table is missing/unreachable, keep the built-in café + holiday items.
            val special = runCatching { SupabaseApi.fetchSpecialItems() }.getOrElse { MockData.specialItems }

            store = MenuStore.fromRows(locations, periods, rows, special)
            inventory = newInventory
            live = true
            null
        } catch (e: Exception) {
            "Couldn't load from Supabase (${e.message}). Showing ${if (live) "last loaded" else "mock"} data."
        }
    }

    /** Writes the current quantity of each item to Supabase. No-op unless live data is loaded. */
    fun pushInventory(itemIds: List<String>): String? {
        if (!live) return null
        return try {
            // Only items that have an inventory row are written back (café items are untracked).
            itemIds.distinct().filter { id -> inventory.any { it.itemId == id } }.forEach { id ->
                SupabaseApi.upsertInventoryQuantity(id, quantityOnHand(id))
            }
            null
        } catch (e: Exception) {
            "Couldn't save inventory to Supabase (${e.message})."
        }
    }
}
