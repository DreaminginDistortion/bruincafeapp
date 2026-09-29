package com.bruincafe.app.data

/**
 * ============================================================================
 *  MOCK DATA -- THE ONE FILE YOU SHOULD NEED TO EDIT
 * ============================================================================
 * Everything the app displays lives in this file, shaped exactly like the
 * `bruin_cafe_menu` Supabase table from Deliverable 2. When the real Supabase
 * integration is wired in, `CafeRepository` is the only other file that
 * changes -- swap its in-memory lists for real network calls and every screen
 * keeps working unmodified, because screens only ever talk to the repository.
 *
 * To add/edit a menu item: add/edit a line in `weeklyMenuHq` (or `weeklyMenuTechCenter`).
 * To add/edit inventory: add/edit a line in `inventorySeed`.
 * To change locations: edit `locations`.
 * ============================================================================
 */
object MockData {

    val locations = listOf(
        CafeLocation(id = "hq", displayName = "Headquarters", subtitle = "HQ Bruin Café"),
        CafeLocation(id = "tech_center", displayName = "Tech Center", subtitle = "Tech Center Café")
    )

    // Week of September 7-11, 2026 (from the HQ Bruin Café printed menu).
    val weeklyMenuHq: List<DayMenu> = listOf(
        DayMenu(
            dayOfWeek = "Monday",
            date = "2026-09-07",
            isClosed = true,
            closedMessage = "Closed - Labor Day. We'll be back Tuesday with a fresh week of chef-prepared specials."
        ),
        DayMenu(
            dayOfWeek = "Tuesday",
            date = "2026-09-08",
            isClosed = false,
            items = listOf(
                MenuItem(
                    id = "hq-tue-soup", locationId = "hq",
                    name = "Roasted Tomato Soup & Grilled Cheese",
                    description = "Rich, slow-roasted tomato soup blended with aromatic herbs, served with a golden buttery grilled cheese.",
                    course = Course.ENTREE
                )
            )
        ),
        DayMenu(
            dayOfWeek = "Wednesday",
            date = "2026-09-09",
            isClosed = false,
            items = listOf(
                MenuItem(
                    id = "hq-wed-chicken", locationId = "hq",
                    name = "Homestyle Chicken & Rice",
                    description = "Tender, seasoned chicken served over savory rice.",
                    course = Course.ENTREE
                )
            )
        ),
        DayMenu(
            dayOfWeek = "Thursday",
            date = "2026-09-10",
            isClosed = false,
            items = listOf(
                MenuItem(
                    id = "hq-thu-antipasto", locationId = "hq",
                    name = "Antipasto Cups",
                    description = "International Day Italy.",
                    course = Course.APPETIZER
                ),
                MenuItem(
                    id = "hq-thu-ravioli", locationId = "hq",
                    name = "Sausage & Broccolini Ravioli with Vodka Sauce",
                    description = "International Day Italy.",
                    course = Course.ENTREE
                ),
                MenuItem(
                    id = "hq-thu-tiramisu", locationId = "hq",
                    name = "Tiramisu",
                    description = "International Day Italy.",
                    course = Course.DESSERT
                )
            )
        ),
        DayMenu(
            dayOfWeek = "Friday",
            date = "2026-09-11",
            isClosed = false,
            items = listOf(
                MenuItem(
                    id = "hq-fri-nachos", locationId = "hq",
                    name = "Beer Cheese Philly Nachos",
                    description = "Crispy tortilla chips piled high with seasoned Philly-style beef, sauteed peppers and onions, and warm beer cheese sauce.",
                    course = Course.ENTREE
                )
            )
        )
    )

    // Tech Center placeholder week -- same shape, different items, so the
    // location-specific-menu requirement is demonstrable. Edit freely.
    val weeklyMenuTechCenter: List<DayMenu> = listOf(
        DayMenu(dayOfWeek = "Monday", date = "2026-09-07", isClosed = true,
            closedMessage = "Closed - Labor Day."),
        DayMenu(dayOfWeek = "Tuesday", date = "2026-09-08", isClosed = false, items = listOf(
            MenuItem("tc-tue-wrap", "tech_center", "Chicken Caesar Wrap",
                "Grilled chicken, romaine, parmesan, house caesar dressing.", Course.ENTREE)
        )),
        DayMenu(dayOfWeek = "Wednesday", date = "2026-09-09", isClosed = false, items = listOf(
            MenuItem("tc-wed-bowl", "tech_center", "Southwest Grain Bowl",
                "Quinoa, black beans, corn, peppers, chipotle crema.", Course.ENTREE)
        )),
        DayMenu(dayOfWeek = "Thursday", date = "2026-09-10", isClosed = false, items = listOf(
            MenuItem("tc-thu-burger", "tech_center", "Smash Burger & Fries",
                "Double smash patty, cheddar, house sauce.", Course.ENTREE)
        )),
        DayMenu(dayOfWeek = "Friday", date = "2026-09-11", isClosed = false, items = listOf(
            MenuItem("tc-fri-pizza", "tech_center", "Margherita Flatbread",
                "San marzano tomato, fresh mozzarella, basil.", Course.ENTREE)
        ))
    )

    // Headquarters-only ready-made breakfast items (shown only when HQ is selected).
    val hqBreakfastItems = listOf(
        MenuItem("hq-bfast-burrito", "hq", "Ready-Made Breakfast Burrito",
            "Scrambled egg, cheddar, potato, salsa. Grab-and-go.", Course.ENTREE),
        MenuItem("hq-bfast-parfait", "hq", "Yogurt Parfait Cup",
            "Vanilla yogurt, granola, seasonal berries.", Course.DESSERT)
    )

    // Inventory rows -- one per menu item per location, plus the breakfast items.
    // quantityOnHand / lowStockThreshold drive the sold-out and low-stock rules.
    fun inventorySeed(): MutableList<InventoryItem> {
        val all = weeklyMenuHq.flatMap { it.items } +
            weeklyMenuTechCenter.flatMap { it.items } +
            hqBreakfastItems

        // Hand-picked starting quantities so the POC demonstrates all three
        // stock states (in-stock, low-stock, sold-out) out of the box.
        val startingQty: Map<String, Int> = mapOf(
            "hq-tue-soup" to 18,
            "hq-wed-chicken" to 3,       // low stock
            "hq-thu-antipasto" to 12,
            "hq-thu-ravioli" to 0,       // sold out
            "hq-thu-tiramisu" to 4,      // low stock
            "hq-fri-nachos" to 25,
            "hq-bfast-burrito" to 6,
            "hq-bfast-parfait" to 2,     // low stock
            "tc-tue-wrap" to 15,
            "tc-wed-bowl" to 9,
            "tc-thu-burger" to 0,        // sold out
            "tc-fri-pizza" to 14
        )

        return all.map { item ->
            InventoryItem(
                itemId = item.id,
                itemName = item.name,
                locationId = item.locationId,
                quantityOnHand = startingQty[item.id] ?: 10,
                lowStockThreshold = 5
            )
        }.toMutableList()
    }

    // Very small mock staff directory -- any of these log in with any password
    // for demo purposes. Swap for real auth later.
    val staffUsernames = listOf("staff", "manager", "hq_admin")
}
