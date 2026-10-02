package com.bruincafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.InventoryItem
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.ui.CafeViewModel
import com.bruincafe.app.ui.screens.CartScreen
import com.bruincafe.app.ui.screens.CheckoutScreen
import com.bruincafe.app.ui.screens.HomeScreen
import com.bruincafe.app.ui.screens.InventoryDashboardScreen
import com.bruincafe.app.ui.screens.ItemDetailScreen
import com.bruincafe.app.ui.screens.LocationSelectScreen
import com.bruincafe.app.ui.screens.RestockSimulationScreen
import com.bruincafe.app.ui.screens.StaffLoginScreen
import com.bruincafe.app.ui.screens.WeeklyMenuScreen

/**
 * Screen route names -- mirror the Screen Inventory IDs (S-01..S-09) in
 * comments so the doc and the code never drift apart.
 */
private object Routes {
    const val HOME = "home"                 // S-01
    const val LOCATION = "location"          // S-02
    const val WEEKLY_MENU = "weekly_menu"    // S-03
    const val ITEM_DETAIL = "item_detail"    // S-04
    const val CART = "cart"                  // S-05
    const val CHECKOUT = "checkout"          // S-06
    const val STAFF_LOGIN = "staff_login"    // S-07
    const val INVENTORY = "inventory"        // S-08
    const val RESTOCK = "restock"            // S-09
}

@Composable
fun BruinCafeNavGraph() {
    val navController: NavHostController = rememberNavController()
    val viewModel: CafeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val inventoryVersion by viewModel.inventoryVersion.collectAsState()

    // Held outside the ViewModel because it's pure UI navigation state
    // (which item the customer/staff member is currently looking at).
    var selectedItem by remember { mutableStateOf<MenuItem?>(null) }
    var selectedInventoryRow by remember { mutableStateOf<InventoryItem?>(null) }
    var staffLocationId by remember { mutableStateOf("hq") }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOrderLunch = { navController.navigate(Routes.LOCATION) },
                onStaffLogin = { navController.navigate(Routes.STAFF_LOGIN) }
            )
        }

        composable(Routes.LOCATION) {
            LocationSelectScreen(
                locations = uiState.locations,
                onLocationChosen = {
                    viewModel.selectLocation(it.id)
                    navController.navigate(Routes.WEEKLY_MENU)
                }
            )
        }

        composable(Routes.WEEKLY_MENU) {
            val locationId = uiState.selectedLocationId ?: "hq"
            val location = uiState.locations.first { it.id == locationId }
            WeeklyMenuScreen(
                location = location,
                weekMenu = CafeRepository.weeklyMenu(locationId),
                breakfastItems = CafeRepository.breakfastItems(locationId),
                simulatedDate = uiState.simulatedDate,
                onSimulatedDateChange = viewModel::setSimulatedDate,
                onItemClick = {
                    selectedItem = it
                    navController.navigate(Routes.ITEM_DETAIL)
                },
                onBack = { navController.popBackStack() },
                onViewCart = { navController.navigate(Routes.CART) },
                cartCount = uiState.cart.sumOf { it.quantity }
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
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.CART) {
            CartScreen(
                cart = uiState.cart,
                onRemove = viewModel::removeFromCart,
                onCheckout = { navController.navigate(Routes.CHECKOUT) },
                onKeepBrowsing = { navController.popBackStack() }
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                cart = uiState.cart,
                orderConfirmed = uiState.lastOrderConfirmed,
                orderError = uiState.lastOrderError,
                onPlaceOrder = viewModel::placeOrder,
                onNewOrder = {
                    viewModel.clearOrderConfirmation()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STAFF_LOGIN) {
            StaffLoginScreen(
                onLogin = viewModel::staffLogin,
                onLoginSuccess = { navController.navigate(Routes.INVENTORY) }
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
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
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
