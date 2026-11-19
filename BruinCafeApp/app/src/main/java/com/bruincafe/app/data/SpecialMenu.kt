package com.bruincafe.app.data

import java.time.LocalDate

/**
 * A café item or a holiday-menu item, from the `special_items` table.
 *   kind "cafe"    -> always available (usually made to order)
 *   kind "holiday" -> only shown from activeFrom through activeUntil (inclusive)
 * [item].mealPeriod holds the kind ("cafe" or "holiday") so cards can label it.
 */
data class SpecialItem(
    val item: MenuItem,
    val holidayName: String?,
    val activeFrom: LocalDate?,
    val activeUntil: LocalDate?,
    val madeToOrder: Boolean
)

/** Rules for the café and holiday menu page. Pure Kotlin, unit-tested on a plain JVM. */
object SpecialMenu {

    fun cafeItems(all: List<SpecialItem>, locationId: String): List<SpecialItem> =
        all.filter { it.item.mealPeriod == "cafe" && it.item.locationId == locationId }

    private fun isActive(s: SpecialItem, date: LocalDate): Boolean {
        val from = s.activeFrom ?: return false
        val until = s.activeUntil ?: return false
        return !date.isBefore(from) && !date.isAfter(until)
    }

    /** Holiday items whose active window contains [date] (both end dates count). */
    fun activeHolidayItems(all: List<SpecialItem>, locationId: String, date: LocalDate): List<SpecialItem> =
        all.filter { it.item.mealPeriod == "holiday" && it.item.locationId == locationId && isActive(it, date) }

    /** The soonest holiday item that starts after [date], or null if none are scheduled. */
    fun nextHoliday(all: List<SpecialItem>, locationId: String, date: LocalDate): SpecialItem? =
        all.filter {
            it.item.mealPeriod == "holiday" && it.item.locationId == locationId &&
                it.activeFrom != null && it.activeFrom.isAfter(date)
        }.minByOrNull { it.activeFrom!! }
}
