package com.bruincafe.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime

/**
 * Minimal Supabase client using only Supabase's REST API (PostgREST) -- no
 * third-party SDK. Every call is a plain HTTPS request with two headers
 * (`apikey` and `Authorization: Bearer <anon key>`), so the iOS/Swift app can
 * make the identical requests with URLSession.
 *
 * All functions BLOCK, so call them off the main thread (CafeViewModel does).
 *
 *   GET  {url}/rest/v1/locations?select=*
 *   GET  {url}/rest/v1/meal_periods?select=*
 *   GET  {url}/rest/v1/menu_items?select=*
 *   GET  {url}/rest/v1/inventory?select=*
 *   POST {url}/rest/v1/inventory?on_conflict=item_id   (upsert, used for restock/orders)
 */
object SupabaseApi {

    private const val TIMEOUT_MS = 8000

    private fun request(
        path: String,
        method: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val conn = URL(SupabaseConfig.url.trimEnd('/') + "/rest/v1/" + path)
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("apikey", SupabaseConfig.anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.anonKey}")
            conn.setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code $text")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)

    fun fetchLocations(): List<CafeLocation> {
        val arr = JSONArray(request("locations?select=*&order=sort_order.asc", "GET"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CafeLocation(o.getString("id"), o.getString("display_name"), o.str("subtitle") ?: "")
        }
    }

    fun fetchMealPeriods(): List<MealPeriodDef> {
        val arr = JSONArray(request("meal_periods?select=*&order=start_time.asc", "GET"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MealPeriodDef(
                name = o.getString("name"),
                label = o.getString("label"),
                start = LocalTime.parse(o.getString("start_time")),   // Postgres "07:00:00"
                end = LocalTime.parse(o.getString("end_time"))
            )
        }
    }

    fun fetchMenuRows(): List<MenuRow> {
        val arr = JSONArray(request("menu_items?select=*&order=sort_order.asc", "GET"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val course = runCatching { Course.valueOf((o.str("course") ?: "entree").uppercase()) }
                .getOrDefault(Course.ENTREE)
            MenuRow(
                item = MenuItem(
                    id = o.getString("id"),
                    locationId = o.getString("location_id"),
                    name = o.getString("name"),
                    description = o.str("description") ?: "",
                    course = course,
                    mealPeriod = o.getString("meal_period")
                ),
                menuDate = o.str("menu_date"),
                dayOfWeek = o.str("day_of_week"),
                isClosed = o.optBoolean("is_closed", false),
                closedMessage = o.str("closed_message")
            )
        }
    }

    /** Café + holiday items from the `special_items` table. */
    fun fetchSpecialItems(): List<SpecialItem> {
        val arr = JSONArray(request("special_items?select=*&order=sort_order.asc", "GET"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val course = runCatching { Course.valueOf((o.str("course") ?: "entree").uppercase()) }
                .getOrDefault(Course.ENTREE)
            SpecialItem(
                item = MenuItem(
                    id = o.getString("id"),
                    locationId = o.getString("location_id"),
                    name = o.getString("name"),
                    description = o.str("description") ?: "",
                    course = course,
                    mealPeriod = o.getString("kind")          // "cafe" or "holiday"
                ),
                holidayName = o.str("holiday_name"),
                activeFrom = o.str("active_from")?.let { LocalDate.parse(it) },
                activeUntil = o.str("active_until")?.let { LocalDate.parse(it) },
                madeToOrder = o.optBoolean("made_to_order", true)
            )
        }
    }

    fun fetchInventory(): List<InventoryRow> {
        val arr = JSONArray(request("inventory?select=*", "GET"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            InventoryRow(o.getString("item_id"), o.getInt("quantity_on_hand"), o.optInt("low_stock_threshold", 5))
        }
    }

    /** Upsert one inventory row's quantity (used after a restock or a mock order). */
    fun upsertInventoryQuantity(itemId: String, quantity: Int) {
        val body = JSONObject().put("item_id", itemId).put("quantity_on_hand", quantity).toString()
        request(
            path = "inventory?on_conflict=item_id",
            method = "POST",
            body = body,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        )
    }
}
