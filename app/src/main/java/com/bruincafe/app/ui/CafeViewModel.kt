package com.bruincafe.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bruincafe.app.data.AccessibilitySettings
import com.bruincafe.app.data.AccessibilityStore
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.CartLine
import com.bruincafe.app.data.MealPeriodDef
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.data.Order
import com.bruincafe.app.data.OrderHistoryStore
import com.bruincafe.app.data.OrderLineRecord
import com.bruincafe.app.data.Rewards
import com.bruincafe.app.data.RewardsStatus
import com.bruincafe.app.data.StockStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/**
 * Holds all shared UI state and exposes it as [StateFlow].
 * Swift analog: an `ObservableObject` with `@Published` properties.
 */
data class CafeUiState(
    val locations: List<CafeLocation> = CafeRepository.locations(),
    val mealPeriods: List<MealPeriodDef> = CafeRepository.mealPeriods(),
    val selectedLocationId: String? = null,
    val simulatedDate: String = CafeRepository.defaultDate(),
    // Real device clock, refreshed by a ticker so the menu flips on its own.
    val now: LocalTime = LocalTime.now(),
    // Demo overrides: when non-null they replace the real time / real date.
    val simulatedTime: LocalTime? = null,
    val holidayDemoDate: LocalDate? = null,
    val cart: List<CartLine> = emptyList(),
    val cartMessage: String? = null,                 // spoken/shown once as a snackbar
    val isStaffLoggedIn: Boolean = false,
    val confirmedOrder: Order? = null,               // set right after a successful checkout
    val lastOrderError: String? = null,
    val orders: List<Order> = OrderHistoryStore.load(),
    val accessibility: AccessibilitySettings = AccessibilityStore.load(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val dataSource: String = CafeRepository.dataSource
) {
    val effectiveTime: LocalTime get() = simulatedTime ?: now
    val effectiveHolidayDate: LocalDate get() = holidayDemoDate ?: LocalDate.now()
    val rewards: RewardsStatus get() = Rewards.status(orders)
}

class CafeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CafeUiState())
    val uiState: StateFlow<CafeUiState> = _uiState

    /** Bumps so inventory screens recompose after inventory changes. */
    private val _inventoryVersion = MutableStateFlow(0)
    val inventoryVersion: StateFlow<Int> = _inventoryVersion

    init {
        refreshData()
        // Tick every 15s so the active meal period updates while the app is open.
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(now = LocalTime.now()) }
                delay(15_000)
            }
        }
    }

    /** Pulls menus, meal periods, café/holiday items and inventory from Supabase (no-op if not configured). */
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val error = withContext(Dispatchers.IO) { CafeRepository.refreshFromSupabase() }
            _uiState.update { s ->
                val stillValid = CafeRepository.allMenuDates().any { it.first == s.simulatedDate }
                s.copy(
                    isLoading = false,
                    loadError = error,
                    dataSource = CafeRepository.dataSource,
                    locations = CafeRepository.locations(),
                    mealPeriods = CafeRepository.mealPeriods(),
                    simulatedDate = if (stillValid) s.simulatedDate else CafeRepository.defaultDate()
                )
            }
            _inventoryVersion.update { it + 1 }
        }
    }

    /**
     * BUG FIX (cart disappearing on HQ): this used to clear the cart EVERY time a
     * location was tapped -- even re-tapping Headquarters -- so a cart built at HQ
     * vanished whenever the customer came back through "Order" or the café page. The cart is
     * now only cleared when the location actually CHANGES (a cart can't mix locations).
     */
    fun selectLocation(locationId: String) {
        _uiState.update {
            if (it.selectedLocationId == locationId) it
            else it.copy(selectedLocationId = locationId, cart = emptyList())
        }
    }

    fun setSimulatedDate(date: String) { _uiState.update { it.copy(simulatedDate = date) } }
    fun setSimulatedTime(time: LocalTime?) { _uiState.update { it.copy(simulatedTime = time) } }
    fun setHolidayDemoDate(date: LocalDate?) { _uiState.update { it.copy(holidayDemoDate = date) } }

    /** Sold-out rule enforced here too, not just visually in the item screen. */
    fun addToCart(item: MenuItem) {
        if (CafeRepository.stockStatus(item.id) == StockStatus.SOLD_OUT) {
            _uiState.update { it.copy(cartMessage = "Sorry, ${item.name} is sold out.") }
            return
        }
        _uiState.update { state ->
            val existing = state.cart.firstOrNull { it.item.id == item.id }
            val updated = if (existing != null) {
                state.cart.map { if (it.item.id == item.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                state.cart + CartLine(item, 1)
            }
            state.copy(cart = updated, cartMessage = "Added ${item.name} to your cart.")
        }
    }

    fun clearCartMessage() { _uiState.update { it.copy(cartMessage = null) } }

    fun removeFromCart(itemId: String) {
        _uiState.update { state -> state.copy(cart = state.cart.filter { it.item.id != itemId }) }
    }

    fun placeOrder() {
        val state = _uiState.value
        val lines = state.cart
        if (!CafeRepository.placeMockOrder(lines)) {
            _uiState.update {
                it.copy(lastOrderError = "One or more items sold out while you were ordering. Please review your cart.")
            }
            return
        }
        val locationName = state.locations.firstOrNull { it.id == state.selectedLocationId }?.displayName ?: "Bruin Café"
        val now = System.currentTimeMillis()
        val order = Order(
            id = now.toString(),
            timeMillis = now,
            locationName = locationName,
            lines = lines.map { OrderLineRecord(it.item.name, it.quantity) }
        )
        val updatedHistory = OrderHistoryStore.add(order)     // saved on this device only
        _uiState.update {
            it.copy(cart = emptyList(), confirmedOrder = order, orders = updatedHistory, lastOrderError = null)
        }
        pushInventory(lines.map { it.item.id })
    }

    fun clearOrderConfirmation() {
        _uiState.update { it.copy(confirmedOrder = null, lastOrderError = null) }
    }

    fun setAccessibility(settings: AccessibilitySettings) {
        AccessibilityStore.save(settings)
        _uiState.update { it.copy(accessibility = settings) }
    }

    fun staffLogin(username: String, password: String): Boolean {
        val ok = CafeRepository.validateStaffLogin(username, password)
        if (ok) _uiState.update { it.copy(isStaffLoggedIn = true) }
        return ok
    }

    fun staffLogout() { _uiState.update { it.copy(isStaffLoggedIn = false) } }

    fun restock(itemId: String, quantity: Int) {
        CafeRepository.restock(itemId, quantity)
        _inventoryVersion.update { it + 1 }
        pushInventory(listOf(itemId))
    }

    private fun pushInventory(ids: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = CafeRepository.pushInventory(ids)
            if (error != null) _uiState.update { it.copy(loadError = error) }
        }
    }
}
