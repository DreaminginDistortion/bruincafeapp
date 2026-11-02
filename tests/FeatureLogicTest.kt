// Plain-JVM tests for the café/holiday date rules, rewards math, and menu-store wiring.
// Compile each test file SEPARATELY (they share top-level helper names):
//   D=app/src/main/java/com/bruincafe/app/data
//   kotlinc $D/Models.kt $D/MockData.kt $D/MealScheduler.kt $D/MenuStore.kt $D/Rewards.kt $D/SpecialMenu.kt tests/FeatureLogicTest.kt -d out
//   java -cp "out:<kotlinc>/lib/kotlin-stdlib.jar" FeatureLogicTestKt
import com.bruincafe.app.data.*
import java.time.LocalDate
import java.time.LocalTime

var failures = 0
fun check(name: String, cond: Boolean) { println((if (cond) "PASS  " else "FAIL  ") + name); if (!cond) failures++ }
fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

fun main() {
    val sp = MockData.specialItems
    // ---- holiday menu: only inside configured dates ----
    fun names(loc: String, date: LocalDate) = SpecialMenu.activeHolidayItems(sp, loc, date).map { it.item.name }
    check("Sep 30: no holiday items (day before start)", names("hq", d(9, 30)).isEmpty())
    check("Oct 1: Fall Favorites starts (first day counts)", names("hq", d(10, 1)) == listOf("Pumpkin Spice Latte", "Caramel Apple Cider"))
    check("Oct 31: still active (last day counts)", names("hq", d(10, 31)).size == 2)
    check("Nov 1: none between holidays", names("hq", d(11, 1)).isEmpty())
    check("Nov 20: Thanksgiving items", names("hq", d(11, 20)) == listOf("Pumpkin Pie Slice", "Turkey & Cranberry Wrap"))
    check("Dec 24: Winter Holidays last day", names("hq", d(12, 24)).size == 2)
    check("Dec 25: nothing", names("hq", d(12, 25)).isEmpty())
    check("Tech Center Oct 15 has only its own holiday item", names("tech_center", d(10, 15)) == listOf("Pumpkin Spice Latte"))
    check("holiday items never leak into café list", SpecialMenu.cafeItems(sp, "hq").none { it.item.mealPeriod == "holiday" })
    check("HQ café list = 4 made-to-order items", SpecialMenu.cafeItems(sp, "hq").size == 4 && SpecialMenu.cafeItems(sp, "hq").all { it.madeToOrder })
    check("Tech Center café list = 3 items", SpecialMenu.cafeItems(sp, "tech_center").size == 3)
    check("next holiday after Nov 1 is Thanksgiving", SpecialMenu.nextHoliday(sp, "hq", d(11, 1))?.holidayName == "Thanksgiving")
    check("next holiday after Dec 25 = none", SpecialMenu.nextHoliday(sp, "hq", d(12, 25)) == null)
    check("category labels", sp.first { it.item.id == "hq-cafe-latte" }.item.categoryLabel == "Café" &&
        sp.first { it.item.id == "hq-hol-psl" }.item.categoryLabel == "Holiday")

    // ---- rewards ----
    fun order(vararg qty: Int) = Order("x", 0L, "Headquarters", qty.map { OrderLineRecord("i", it) })
    check("no orders -> 0 points, 100 to go", Rewards.status(emptyList()).let { it.totalPoints == 0 && it.pointsToNext == 100 && it.progress == 0f })
    check("3 items = 30 points", order(1, 2).points == 30)
    check("4 orders of 3 items = 120 points, 1 reward, 20 into next",
        Rewards.status(List(4) { order(3) }).let { it.totalPoints == 120 && it.rewardsEarned == 1 && it.pointsIntoCurrent == 20 && it.pointsToNext == 80 })
    check("exactly 100 points -> reward earned, progress resets to 0", Rewards.status(List(2) { order(5) }).let { it.rewardsEarned == 1 && it.progress == 0f && it.pointsToNext == 100 })
    check("progress fraction 0.5 at 50 points", Rewards.status(listOf(order(5))).progress == 0.5f)

    // ---- store still behaves, with special items carried through ----
    val store = MenuStore.fromMock()
    check("MenuStore carries special items", store.special.size == sp.size)
    check("rows-built store accepts special items", MenuStore.fromRows(MockData.locations, MockData.mealPeriods, emptyList(), sp).special.size == sp.size)
    check("a11y defaults", AccessibilitySettings().let { it.textScale == 1.0f && !it.highContrast })
    check("3 text size options", AccessibilitySettings.textScaleOptions.size == 3)

    println(if (failures == 0) "\nALL FEATURE TESTS PASSED" else "\n$failures FAILED")
    if (failures > 0) System.exit(1)
}
