package com.bruincafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.InventoryItem
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.ui.CafeViewModel
import com.bruincafe.app.ui.screens.AccessibilitySettingsScreen
import com.bruincafe.app.ui.screens.CafeHolidayScreen
import com.bruincafe.app.ui.screens.CartScreen
import com.bruincafe.app.ui.screens.CheckoutScreen
import com.bruincafe.app.ui.screens.HomeScreen
import com.bruincafe.app.ui.screens.InventoryDashboardScreen
import com.bruincafe.app.ui.screens.ItemDetailScreen
import com.bruincafe.app.ui.screens.LocationSelectScreen
import com.bruincafe.app.ui.screens.OrderHistoryScreen
import com.bruincafe.app.ui.screens.RestockSimulationScreen
import com.bruincafe.app.ui.screens.StaffLoginScreen
import com.bruincafe.app.ui.screens.WeeklyMenuScreen

/**
 * Screen route names -- mirror the Screen Inventory IDs in comments so the
 * document and the code never drift apart.
 */
private object Routes {
    const val HOME = "home"                       // S-01  Rewards home
    const val LOCATION = "location"               // S-02
    const val WEEKLY_MENU = "weekly_menu"         // S-03
    const val ITEM_DETAIL = "item_detail"         // S-04
    const val CART = "cart"                       // S-05
    const val CHECKOUT = "checkout"               // S-06
    const val STAFF_LOGIN = "staff_login"         // S-07
    const val INVENTORY = "inventory"             // S-08
    const val RESTOCK = "restock"                 // S-09
    const val ORDER_HISTORY = "order_history"     // S-10 (new)
    const val CAFE_HOLIDAY = "cafe_holiday"       // S-11 (new)
    const val ACCESSIBILITY = "accessibility"     // S-12 (new)
}

@Composable
fun BruinCafeNavGraph(viewModel: CafeViewModel) {
    val navController: NavHostController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val inventoryVersion by viewModel.inventoryVersion.collectAsState()

    // Pure UI navigation state: which item the customer / staff member is looking at.
    var selectedItem by remember { mutableStateOf<MenuItem?>(null) }
    var selectedInventoryRow by remember { mutableStateOf<InventoryItem?>(null) }
    var staffLocationId by remember { mutableStateOf("hq") }

    val currentLocationId = uiState.selectedLocationId ?: uiState.locations.first().id
    val goBack: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                rewards = uiState.rewards,
                onOrder = { navController.navigate(Routes.LOCATION) },
                onCafeHoliday = {
                    viewModel.selectLocation(currentLocationId)
                    navController.navigate(Routes.CAFE_HOLIDAY)
                },
                onOrderHistory = { navController.navigate(Routes.ORDER_HISTORY) },
                onAccessibility = { navController.navigate(Routes.ACCESSIBILITY) },
                onStaffLogin = { navController.navigate(Routes.STAFF_LOGIN) }
            )
        }

        composable(Routes.LOCATION) {
            LocationSelectScreen(
                locations = uiState.locations,
                onLocationChosen = {
                    viewModel.selectLocation(it.id)
                    navController.navigate(Routes.WEEKLY_MENU)
                },
                onBack = goBack
            )
        }

        composable(Routes.WEEKLY_MENU) {
            val location = uiState.locations.first { it.id == currentLocationId }
            WeeklyMenuScreen(
                location = location,
                periods = uiState.mealPeriods,
                now = uiState.effectiveTime,
                simulatedTime = uiState.simulatedTime,
                onSimulatedTimeChange = viewModel::setSimulatedTime,
                simulatedDate = uiState.simulatedDate,
                onSimulatedDateChange = viewModel::setSimulatedDate,
                dataSource = uiState.dataSource,
                isLoading = uiState.isLoading,
                loadError = uiState.loadError,
                onRefresh = viewModel::refreshData,
                onItemClick = {
                    selectedItem = it
                    navController.navigate(Routes.ITEM_DETAIL)
                },
                onViewCart = { navController.navigate(Routes.CART) },
                cartCount = uiState.cart.sumOf { it.quantity },
                cartMessage = uiState.cartMessage,
                onCartMessageShown = viewModel::clearCartMessage,
                onBack = goBack
            )
        }

        composable(Routes.CAFE_HOLIDAY) {
            CafeHolidayScreen(
                locations = uiState.locations,
                selectedLocationId = currentLocationId,
                onLocationChange = viewModel::selectLocation,
                holidayDemoDate = uiState.holidayDemoDate,
                onHolidayDemoDateChange = viewModel::setHolidayDemoDate,
                onItemClick = {
                    selectedItem = it
                    navController.navigate(Routes.ITEM_DETAIL)
                },
                onViewCart = { navController.navigate(Routes.CART) },
                cartCount = uiState.cart.sumOf { it.quantity },
                cartMessage = uiState.cartMessage,
                onCartMessageShown = viewModel::clearCartMessage,
                onBack = goBack
            )
        }

        composable(Routes.ITEM_DETAIL) {
            selectedItem?.let { item ->
                ItemDetailScreen(
                    item = item,
                    onAddToCart = {
                        viewModel.addToCart(it)
                        navController.popBackStack()
                    },
                    onBack = goBack
                )
            }
        }

        composable(Routes.CART) {
            CartScreen(
                cart = uiState.cart,
                onRemove = viewModel::removeFromCart,
                onCheckout = { navController.navigate(Routes.CHECKOUT) },
                onKeepBrowsing = goBack,
                onBack = goBack
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                cart = uiState.cart,
                confirmedOrder = uiState.confirmedOrder,
                orderError = uiState.lastOrderError,
                onPlaceOrder = viewModel::placeOrder,
                onNewOrder = {
                    viewModel.clearOrderConfirmation()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onViewHistory = {
                    navController.navigate(Routes.ORDER_HISTORY) { popUpTo(Routes.HOME) }
                    viewModel.clearOrderConfirmation()
                },
                onBack = goBack
            )
        }

        composable(Routes.ORDER_HISTORY) {
            OrderHistoryScreen(orders = uiState.orders, rewards = uiState.rewards, onBack = goBack)
        }

        composable(Routes.ACCESSIBILITY) {
            AccessibilitySettingsScreen(
                settings = uiState.accessibility,
                onChange = viewModel::setAccessibility,
                onBack = goBack
            )
        }

        composable(Routes.STAFF_LOGIN) {
            StaffLoginScreen(
                onLogin = viewModel::staffLogin,
                onLoginSuccess = { navController.navigate(Routes.INVENTORY) },
                onBack = goBack
            )
        }

        composable(Routes.INVENTORY) {
            // Reading inventoryVersion here forces recomposition after a restock.
            val refreshKey = inventoryVersion
            InventoryDashboardScreen(
                locations = uiState.locations,
                selectedLocationId = staffLocationId,
                onLocationChange = { staffLocationId = it },
                inventory = CafeRepository.inventoryFor(staffLocationId),
                onSelectItemToRestock = {
                    selectedInventoryRow = it
                    navController.navigate(Routes.RESTOCK)
                },
                onLogout = {
                    viewModel.staffLogout()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.RESTOCK) {
            selectedInventoryRow?.let { row ->
                val refreshKey = inventoryVersion
                RestockSimulationScreen(
                    item = row,
                    currentQuantity = CafeRepository.quantityOnHand(row.itemId),
                    onConfirmRestock = { qty -> viewModel.restock(row.itemId, qty) },
                    onBack = goBack
                )
            }
        }
    }
}
