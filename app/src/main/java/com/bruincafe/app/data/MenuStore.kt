package com.bruincafe.app.data

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One row of the `menu_items` table, before it is grouped into days. */
data class MenuRow(
    val item: MenuItem,
    val menuDate: String?,        // null = served every open day during its meal period
    val dayOfWeek: String?,
    val isClosed: Boolean,
    val closedMessage: String?
)

/** One row of the `inventory` table. */
data class InventoryRow(val itemId: String, val quantity: Int, val threshold: Int)

/**
 * Immutable snapshot of everything the menu screens need, plus the menu rules.
 * Built either from MockData or from Supabase rows -- the rules are identical.
 * No Android/network code in here, so it is unit-testable on a plain JVM.
 *
 * Menu rules:
 *  - A closed date (is_closed row) closes the whole day, every meal period.
 *  - Items for (location, date, period) = daily items for that period
 *    (menu_date is null) + items dated exactly that day for that period.
 */
data class MenuStore(
    val locations: List<CafeLocation>,
    val periods: List<MealPeriodDef>,
    val weeks: Map<String, List<DayMenu>>,        // locationId -> dated days
    val daily: Map<String, List<MenuItem>>,       // locationId -> undated (every day) items
    val special: List<SpecialItem> = emptyList()  // café + holiday items
) {
    fun weekMenu(locationId: String): List<DayMenu> = weeks[locationId].orEmpty()

    fun closedMessage(locationId: String, date: String): String? =
        weekMenu(locationId).firstOrNull { it.date == date && it.isClosed }
            ?.let { it.closedMessage ?: "Closed." }

    fun itemsFor(locationId: String, date: String, period: String): List<MenuItem> {
        if (closedMessage(locationId, date) != null) return emptyList()
        val dailyItems = daily[locationId].orEmpty().filter { it.mealPeriod == period }
        val datedItems = weekMenu(locationId).firstOrNull { it.date == date }
            ?.items.orEmpty().filter { it.mealPeriod == period }
        return dailyItems + datedItems
    }

    /** Lunch entree(s) dated for [date]; empty on closed days. */
    fun todaysSpecial(locationId: String, date: String): List<MenuItem> {
        if (closedMessage(locationId, date) != null) return emptyList()
        return weekMenu(locationId).firstOrNull { it.date == date }
            ?.items.orEmpty()
            .filter { it.mealPeriod == "lunch" && it.course == Course.ENTREE }
    }

    /** Every (date, dayOfWeek) present in any location's data, oldest first. */
    fun allDates(): List<Pair<String, String>> =
        weeks.values.flatten().distinctBy { it.date }.sortedBy { it.date }
            .map { it.date to it.dayOfWeek }

    /** Real today if the data has it; otherwise the first open day in the data. */
    fun defaultDate(today: String): String {
        val dates = allDates()
        if (dates.any { it.first == today }) return today
        val firstOpen = weeks.values.flatten().filter { !it.isClosed }.minByOrNull { it.date }
        return firstOpen?.date ?: dates.firstOrNull()?.first ?: today
    }

    companion object {
        fun fromMock(): MenuStore = MenuStore(
            locations = MockData.locations,
            periods = MockData.mealPeriods,
            weeks = mapOf(
                "hq" to MockData.weeklyMenuHq,
                "tech_center" to MockData.weeklyMenuTechCenter
            ),
            daily = mapOf("hq" to MockData.hqBreakfastItems),
            special = MockData.specialItems
        )

        fun fromRows(
            locations: List<CafeLocation>,
            periods: List<MealPeriodDef>,
            rows: List<MenuRow>,
            special: List<SpecialItem> = emptyList()
        ): MenuStore {
            val weeks = rows.filter { it.menuDate != null }
                .groupBy { it.item.locationId }
                .mapValues { (_, locRows) ->
                    locRows.groupBy { it.menuDate!! }.map { (date, dayRows) ->
                        val closedRow = dayRows.firstOrNull { it.isClosed }
                        DayMenu(
                            dayOfWeek = dayRows.firstNotNullOfOrNull { it.dayOfWeek } ?: weekdayName(date),
                            date = date,
                            isClosed = closedRow != null,
                            closedMessage = closedRow?.closedMessage,
                            items = dayRows.filter { !it.isClosed }.map { it.item }
                        )
                    }.sortedBy { it.date }
                }
            val daily = rows.filter { it.menuDate == null && !it.isClosed }
                .groupBy { it.item.locationId }
                .mapValues { (_, r) -> r.map { it.item } }
            return MenuStore(locations, periods, weeks, daily, special)
        }

        private fun weekdayName(date: String): String =
            runCatching { LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US) }
                .getOrDefault("")
    }
}
