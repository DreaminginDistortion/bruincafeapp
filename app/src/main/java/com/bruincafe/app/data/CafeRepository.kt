package com.bruincafe.app.data

/**
 * Repository -- the single seam between UI and data.
 *
 * This is the ONLY other file you'd touch to go from mock data to a real
 * Supabase backend: replace the bodies below with network/DB calls that
 * return the same types, and every screen keeps working as-is.
 *
 * For Swift devs: this plays the role a `CafeService`/`Repository` protocol +
 * implementation would play in a SwiftUI app -- screens depend on this
 * abstraction, never on the raw data source.
 *
 * Business rules encoded here (see Deliverable 2, Section 1):
 *  - WLM-01: menu queries are always scoped to a location.
 *  - WLM-04 / holiday rule: a closed day returns no items, only a message.
 *  - WLM-07: "today's special" = entree(s) on the row matching the given date.
 *  - Sold-out rule: items with quantityOnHand <= 0 cannot be added to cart.
 */
object CafeRepository {
    private val inventory: MutableList<InventoryItem> = MockData.inventorySeed()

    fun locations(): List<CafeLocation> = MockData.locations

    fun weeklyMenu(locationId: String): List<DayMenu> =
        when (locationId) {
            "hq" -> MockData.weeklyMenuHq
            "tech_center" -> MockData.weeklyMenuTechCenter
            else -> emptyList()
        }

    /** HQ-only ready-made breakfast items; empty for any other location. */
    fun breakfastItems(locationId: String): List<MenuItem> =
        if (locationId == "hq") MockData.hqBreakfastItems else emptyList()

    /** WLM-07: today's special = entree item(s) on the day matching [simulatedDate]. */
    fun todaysSpecial(locationId: String, simulatedDate: String): List<MenuItem> {
        val day = weeklyMenu(locationId).firstOrNull { it.date == simulatedDate && !it.isClosed }
            ?: return emptyList()
        return day.items.filter { it.course == Course.ENTREE }
    }

    fun inventoryFor(locationId: String): List<InventoryItem> =
        inventory.filter { it.locationId == locationId }

    fun stockStatus(itemId: String): StockStatus =
        inventory.firstOrNull { it.itemId == itemId }?.status ?: StockStatus.IN_STOCK

    fun quantityOnHand(itemId: String): Int =
        inventory.firstOrNull { it.itemId == itemId }?.quantityOnHand ?: 0

    /** Staff action: simulate a restock. Never goes negative; no upper cap for a POC. */
    fun restock(itemId: String, addQuantity: Int) {
        val row = inventory.firstOrNull { it.itemId == itemId } ?: return
        row.quantityOnHand = (row.quantityOnHand + addQuantity).coerceAtLeast(0)
    }

    /**
     * Mock order simulation: decrements inventory for each cart line by the
     * quantity ordered. Returns false (and changes nothing) if any line would
     * be pushed below zero -- mirrors "prevent sold-out items from being
     * added to the cart" holding true all the way through checkout too.
     */
    fun placeMockOrder(lines: List<CartLine>): Boolean {
        val wouldGoNegative = lines.any { line ->
            val row = inventory.firstOrNull { it.itemId == line.item.id }
            row == null || row.quantityOnHand < line.quantity
        }

        if (wouldGoNegative) return false

        lines.forEach { line ->
            inventory.firstOrNull { it.itemId == line.item.id }?.let {
                it.quantityOnHand -= line.quantity
            }
        }

        return true
    }

    fun validateStaffLogin(username: String, password: String): Boolean =
        username.isNotBlank() && password.isNotBlank() &&
            MockData.staffUsernames.any { it.equals(username, ignoreCase = true) }

    /** All dates present across every location's weekly menu, for the demo date-picker. */
    fun allMenuDates(): List<Pair<String, String>> =
        (MockData.weeklyMenuHq + MockData.weeklyMenuTechCenter)
            .distinctBy { it.date }
            .sortedBy { it.date }
            .map { it.date to it.dayOfWeek }
}
