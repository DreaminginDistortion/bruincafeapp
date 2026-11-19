package com.bruincafe.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny on-device storage (Android SharedPreferences). Call [init] once from
 * MainActivity. Until then (or in plain-JVM tests) values are kept in memory only.
 *
 * Privacy: order history and accessibility choices stay on the customer's own
 * device. Nothing here is sent to Supabase.
 */
object AppStorage {
    private var prefs: SharedPreferences? = null
    private val memory = mutableMapOf<String, String>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("bruin_cafe_prefs", Context.MODE_PRIVATE)
        }
    }

    fun getString(key: String): String? = prefs?.getString(key, null) ?: memory[key]

    fun putString(key: String, value: String) {
        memory[key] = value
        prefs?.edit()?.putString(key, value)?.apply()
    }
}

/** Saves the newest 50 orders as JSON. Points are derived from these, never stored. */
object OrderHistoryStore {
    private const val KEY = "order_history_v1"
    private const val MAX_ORDERS = 50

    fun load(): List<Order> {
        val raw = AppStorage.getString(KEY) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val linesJson = o.getJSONArray("lines")
                Order(
                    id = o.getString("id"),
                    timeMillis = o.getLong("time"),
                    locationName = o.getString("location"),
                    lines = List(linesJson.length()) { j ->
                        val l = linesJson.getJSONObject(j)
                        OrderLineRecord(l.getString("name"), l.getInt("qty"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()    // corrupted data: start fresh rather than crash
        }
    }

    /** Adds [order] at the top and returns the updated list. */
    fun add(order: Order): List<Order> {
        val updated = (listOf(order) + load()).take(MAX_ORDERS)
        val arr = JSONArray()
        updated.forEach { ord ->
            val lines = JSONArray()
            ord.lines.forEach { lines.put(JSONObject().put("name", it.name).put("qty", it.quantity)) }
            arr.put(JSONObject().put("id", ord.id).put("time", ord.timeMillis)
                .put("location", ord.locationName).put("lines", lines))
        }
        AppStorage.putString(KEY, arr.toString())
        return updated
    }
}

object AccessibilityStore {
    private const val KEY_SCALE = "a11y_text_scale"
    private const val KEY_CONTRAST = "a11y_high_contrast"

    fun load(): AccessibilitySettings = AccessibilitySettings(
        textScale = AppStorage.getString(KEY_SCALE)?.toFloatOrNull() ?: 1.0f,
        highContrast = AppStorage.getString(KEY_CONTRAST) == "true"
    )

    fun save(settings: AccessibilitySettings) {
        AppStorage.putString(KEY_SCALE, settings.textScale.toString())
        AppStorage.putString(KEY_CONTRAST, settings.highContrast.toString())
    }
}
