package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CartLine
import com.bruincafe.app.data.Order

/** Screen S-06: Checkout / Order Confirmation -- runs the mock order simulation and awards points. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: List<CartLine>,
    confirmedOrder: Order?,
    orderError: String?,
    onPlaceOrder: () -> Unit,
    onNewOrder: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Checkout") },
                // After the order is placed, "Back" would return to an empty cart; use the buttons instead.
                navigationIcon = { if (confirmedOrder == null) BackButton(onBack) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {

            if (confirmedOrder != null) {
                // Live region: a screen reader announces the confirmation as soon as it appears.
                Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text(
                        "Order Confirmed!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text("You earned ${confirmedOrder.points} rewards points.", fontWeight = FontWeight.SemiBold)
                    Text("This is a mock order simulation -- sample inventory has been updated.")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Back to Rewards Home")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onViewHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("View Order History")
                }
            } else {
                SectionLabel("Order Summary")
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cart) { line: CartLine -> Text("${line.quantity} x ${line.item.name}") }
                }

                orderError?.let {
                    Text(
                        it,
                        color = scheme.error,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }

                Button(
                    onClick = onPlaceOrder,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) { Text("Place Order") }
            }
        }
    }
}
