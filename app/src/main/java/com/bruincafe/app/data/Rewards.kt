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
    val points: Int get() = Rewards.pointsForItems(itemCount)
}

data class RewardsStatus(
    val totalPoints: Int,
    val rewardsEarned: Int,       // how many full reward thresholds have been reached
    val pointsIntoCurrent: Int,   // progress toward the next reward
    val pointsToNext: Int,
    val progress: Float           // 0f..1f for a progress bar
)

/**
 * Rewards rules. Points are DERIVED from order history (nothing extra is stored),
 * so the rewards balance and the order history can never disagree.
 *
 *   10 points per item ordered, 100 points = 1 reward (e.g. a free café item).
 */
object Rewards {
    const val POINTS_PER_ITEM = 10
    const val POINTS_PER_REWARD = 100

    fun pointsForItems(itemCount: Int): Int = itemCount.coerceAtLeast(0) * POINTS_PER_ITEM

    fun status(orders: List<Order>): RewardsStatus {
        val total = orders.sumOf { it.points }
        val into = total % POINTS_PER_REWARD
        return RewardsStatus(
            totalPoints = total,
            rewardsEarned = total / POINTS_PER_REWARD,
            pointsIntoCurrent = into,
            pointsToNext = POINTS_PER_REWARD - into,
            progress = into.toFloat() / POINTS_PER_REWARD
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
