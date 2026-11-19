// Plain-JVM test of the breakfast/lunch switching + menu rules (no Android needed).
// Run from the project root with a Kotlin compiler (kotlinc) and Java installed:
//   D=app/src/main/java/com/bruincafe/app/data
//   kotlinc $D/Models.kt $D/MockData.kt $D/MealScheduler.kt $D/MenuStore.kt $D/Rewards.kt $D/SpecialMenu.kt tests/MealLogicTest.kt -d out
//   java -cp "out:<kotlinc>/lib/kotlin-stdlib.jar" MealLogicTestKt
import com.bruincafe.app.data.*
import java.time.LocalTime

var failures = 0
fun check(name: String, cond: Boolean) {
    println((if (cond) "PASS  " else "FAIL  ") + name)
    if (!cond) failures++
}

fun main() {
    val periods = MockData.mealPeriods   // breakfast 07-11, lunch 11-14
    fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)
    fun act(t: LocalTime) = MealScheduler.active(t, periods)?.name

    // ---- time-based switching ----
    check("06:59 -> nothing served",        act(at(6, 59)) == null)
    check("07:00 -> breakfast (start incl.)", act(at(7)) == "breakfast")
    check("10:59 -> breakfast",             act(at(10, 59)) == "breakfast")
    check("11:00 -> lunch (switch moment)", act(at(11)) == "lunch")
    check("13:59 -> lunch",                 act(at(13, 59)) == "lunch")
    check("14:00 -> nothing (end excl.)",   act(at(14)) == null)
    check("23:30 -> nothing",               act(at(23, 30)) == null)
    check("next at 06:00 is breakfast",     MealScheduler.next(at(6), periods)?.name == "breakfast")
    check("next at 09:00 is lunch",         MealScheduler.next(at(9), periods)?.name == "lunch")
    check("next at 15:00 wraps to breakfast", MealScheduler.next(at(15), periods)?.name == "breakfast")
    check("format 07:00 -> 7:00 AM",        MealScheduler.format(at(7)) == "7:00 AM")
    check("format 13:05 -> 1:05 PM",        MealScheduler.format(at(13, 5)) == "1:05 PM")
    check("format 12:00 -> 12:00 PM",       MealScheduler.format(at(12)) == "12:00 PM")
    check("format 00:30 -> 12:30 AM",       MealScheduler.format(at(0, 30)) == "12:30 AM")
    val overnight = MealPeriodDef("late", "Late", at(22), at(2))
    check("overnight window contains 23:00 and 01:00 but not 12:00",
        overnight.contains(at(23)) && overnight.contains(at(1)) && !overnight.contains(at(12)))

    // ---- menu rules (mock data) ----
    val store = MenuStore.fromMock()
    val bf = store.itemsFor("hq", "2026-09-08", "breakfast").map { it.name }
    val lu = store.itemsFor("hq", "2026-09-08", "lunch").map { it.name }
    check("HQ Tue breakfast = 4 breakfast items", bf.size == 4 && store.itemsFor("hq", "2026-09-08", "breakfast").all { it.mealPeriod == "breakfast" })
    check("HQ Tue lunch = tomato soup only",   lu == listOf("Roasted Tomato Soup & Grilled Cheese"))
    check("breakfast and lunch lists do not overlap", bf.none { it in lu })
    check("HQ Thu lunch = 3 Italy Day items",  store.itemsFor("hq", "2026-09-10", "lunch").size == 3)
    check("Tech Center has no breakfast",      store.itemsFor("tech_center", "2026-09-08", "breakfast").isEmpty())
    check("Mon Labor Day closed -> no breakfast", store.itemsFor("hq", "2026-09-07", "breakfast").isEmpty())
    check("Mon Labor Day closed -> no lunch",     store.itemsFor("hq", "2026-09-07", "lunch").isEmpty())
    check("closed message present",            store.closedMessage("hq", "2026-09-07")?.contains("Labor Day") == true)
    check("Thu special = ravioli only",        store.todaysSpecial("hq", "2026-09-10").map { it.id } == listOf("hq-thu-ravioli"))
    check("defaultDate keeps today when in data", store.defaultDate("2026-09-09") == "2026-09-09")
    check("defaultDate falls back to first open day", store.defaultDate("2026-10-09") == "2026-09-08")

    // ---- rows -> store (what Supabase data goes through) ----
    fun row(id: String, loc: String, period: String, date: String?, closed: Boolean = false, msg: String? = null) =
        MenuRow(MenuItem(id, loc, id, "", Course.ENTREE, period), date, null, closed, msg)
    val rows = listOf(
        row("b1", "hq", "breakfast", null),
        row("l1", "hq", "lunch", "2026-10-09"),
        row("l2", "hq", "lunch", "2026-10-09"),
        row("x",  "hq", "lunch", "2026-10-12", closed = true, msg = "Closed - Columbus Day")
    )
    val live = MenuStore.fromRows(MockData.locations, periods, rows)
    check("rows: Fri 10/9 breakfast = daily item", live.itemsFor("hq", "2026-10-09", "breakfast").map { it.id } == listOf("b1"))
    check("rows: Fri 10/9 lunch = 2 dated items",  live.itemsFor("hq", "2026-10-09", "lunch").map { it.id } == listOf("l1", "l2"))
    check("rows: weekday derived from date (Friday)", live.weekMenu("hq").first().dayOfWeek == "Friday")
    check("rows: closed day suppresses daily breakfast", live.itemsFor("hq", "2026-10-12", "breakfast").isEmpty())
    check("rows: closed message carried through",  live.closedMessage("hq", "2026-10-12") == "Closed - Columbus Day")
    check("rows: defaultDate picks real today",    live.defaultDate("2026-10-09") == "2026-10-09")

    println(if (failures == 0) "\nALL TESTS PASSED" else "\n$failures TEST(S) FAILED")
    if (failures > 0) System.exit(1)
}
