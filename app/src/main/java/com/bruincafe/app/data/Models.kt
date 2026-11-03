package com.bruincafe.app.data

import java.time.LocalTime

/**
 * Data models.
 *
 * These mirror the Supabase tables in supabase/schema_and_seed.sql
 * (locations, meal_periods, menu_items, inventory).
 *
 * NOTE for Swift devs: these are plain data classes -- the direct equivalent of
 * lightweight Swift `struct`s conforming to `Identifiable` / `Codable`.
 */

enum class Course { APPETIZER, ENTREE, DESSERT }

enum class StockStatus { IN_STOCK, LOW_STOCK, SOLD_OUT }

data class CafeLocation(
    val id: String,       // "hq" | "tech_center"
    val displayName: String,
    val subtitle: String
)

/**
 * A serving window, e.g. breakfast 07:00-11:00. Loaded from the `meal_periods`
 * table, so changing serving hours is a database edit, not an app release.
 * The window is [start, end): the end time itself already belongs to the next period.
 */
data class MealPeriodDef(
    val name: String,      // machine key used on menu items: "breakfast" | "lunch"
    val label: String,     // display text: "Breakfast" | "Lunch"
    val start: LocalTime,
    val end: LocalTime
) {
    fun contains(t: LocalTime): Boolean =
        if (start <= end) t >= start && t < end      // normal same-day window
        else t >= start || t < end                   // window that crosses midnight
}

data class MenuItem(
    val id: String,
    val locationId: String,
    val name: String,
    val description: String,
    val course: Course,
    // "breakfast" / "lunch" (matches MealPeriodDef.name), or "cafe" / "holiday" for special items
    val mealPeriod: String = "lunch"
) {
    /** Text shown on cards and read by screen readers, e.g. "Breakfast", "Café", "Holiday". */
    val categoryLabel: String
        get() = when (mealPeriod) {
            "cafe" -> "Café"
            else -> mealPeriod.replaceFirstChar { it.uppercase() }
        }
}

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
