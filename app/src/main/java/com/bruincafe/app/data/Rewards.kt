/*
 * Rewards.kt
 * BruinCafeApp
 *
 * Modified by David Crosen:
 * - Updated rewards rule: Exactly 1 punch earned per completed order.
 * - 10 punches = 1 free lunch.
 */

package com.bruincafe.app.data

/** One line of a past order, stored by name so history still reads correctly if the menu changes. */
data class OrderLineRecord(val name: String, val quantity: Int)

/** A completed (mock) order. Stored only on the customer's own device. */
data class Order(
    val id: String,
    val timeMillis: Long,
    val locationName: String,
    val lines: List<OrderLineRecord>
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }

    // Exactly 1 punch earned per completed order
    val punchesEarned: Int get() = 1
}

data class RewardsStatus(
    val totalPunches: Int,
    val freeLunchesEarned: Int,   // how many full reward thresholds have been reached
    val punchesIntoCurrent: Int,  // progress toward the next reward card
    val punchesToNext: Int,       // punches remaining until next free lunch
    val progress: Float           // 0f..1f for a progress bar
)

object Rewards {
    private const val PUNCHES_PER_FREE_LUNCH = 10

    fun status(orders: List<Order>): RewardsStatus {
        val totalPunches = orders.sumOf { it.punchesEarned }
        val freeLunchesEarned = totalPunches / PUNCHES_PER_FREE_LUNCH
        val punchesIntoCurrent = totalPunches % PUNCHES_PER_FREE_LUNCH
        val punchesToNext = PUNCHES_PER_FREE_LUNCH - punchesIntoCurrent

        return RewardsStatus(
            totalPunches = totalPunches,
            freeLunchesEarned = freeLunchesEarned,
            punchesIntoCurrent = punchesIntoCurrent,
            punchesToNext = punchesToNext,
            progress = punchesIntoCurrent.toFloat() / PUNCHES_PER_FREE_LUNCH.toFloat()
        )
    }
}

/** User-chosen accessibility options (on top of the phone's own system settings). */
data class AccessibilitySettings(
    val textScale: Float = 1.0f,       // multiplies the system font size
    val highContrast: Boolean = false
) {
    companion object {
        val textScaleOptions: List<Pair<String, Float>> = listOf(
            "Default" to 1.0f,
            "Large" to 1.3f,
            "Extra large" to 1.6f
        )
    }
}