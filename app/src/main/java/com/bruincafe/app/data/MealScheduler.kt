package com.bruincafe.app.data

import java.time.LocalTime

/**
 * Decides which meal period is being served at a given time.
 * Pure functions, no Android or network dependencies.
 */
object MealScheduler {

    /** The period whose window contains [now], or null if the kitchen is between periods/closed. */
    fun active(now: LocalTime, periods: List<MealPeriodDef>): MealPeriodDef? =
        periods.firstOrNull { it.contains(now) }

    /** The next period to start after [now]; wraps to tomorrow's first period if none remain today. */
    fun next(now: LocalTime, periods: List<MealPeriodDef>): MealPeriodDef? =
        periods.filter { it.start > now }.minByOrNull { it.start }
            ?: periods.minByOrNull { it.start }

    /** 7:00 AM style formatting. */
    fun format(t: LocalTime): String {
        val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
        val suffix = if (t.hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, t.minute, suffix)
    }
}
