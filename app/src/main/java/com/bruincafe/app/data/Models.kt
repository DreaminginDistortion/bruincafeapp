package com.bruincafe.app.data

/**
 * Data models.
 *
 * These mirror the Supabase table columns from the Deliverable 2 data design
 * (week_start_date, menu_date, day_of_week, item_name, description, course, is_closed)
 * plus an Inventory model for the staff-facing rules.
 *
 * NOTE for Swift devs: these are plain data classes -- the direct equivalent of
 * lightweight Swift `struct`s conforming to `Identifiable` / `Codable`. There is
 * no framework magic here; a `data class` just auto-generates equals/hashCode/copy.
 */

enum class Course { APPETIZER, ENTREE, DESSERT }

enum class StockStatus { IN_STOCK, LOW_STOCK, SOLD_OUT }

data class CafeLocation(
    val id: String,       // "hq" | "tech_center"
    val displayName: String,
    val subtitle: String
)

data class MenuItem(
    val id: String,
    val locationId: String,
    val name: String,
    val description: String,
    val course: Course
)

data class DayMenu(
    val dayOfWeek: String,     // "Monday" .. "Friday"
    val date: String,          // "2026-09-08" (ISO, string for POC simplicity)
    val isClosed: Boolean,
    val closedMessage: String? = null,
    val items: List<MenuItem> = emptyList()
)

data class InventoryItem(
    val itemId: String,
    val itemName: String,
    val locationId: String,
    var quantityOnHand: Int,
    val lowStockThreshold: Int = 5
) {
    val status: StockStatus
        get() = when {
            quantityOnHand <= 0 -> StockStatus.SOLD_OUT
            quantityOnHand <= lowStockThreshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}

data class CartLine(
    val item: MenuItem,
    val quantity: Int
)
