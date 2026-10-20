package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CartLine
import com.bruincafe.app.ui.theme.CafeError
import com.bruincafe.app.ui.theme.CafeGreen

/** Screen S-06: Checkout / Order Confirmation -- runs the mock order simulation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: List<CartLine>,
    orderConfirmed: Boolean,
    orderError: String?,
    onPlaceOrder: () -> Unit,
    onNewOrder: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Checkout") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            if (orderConfirmed) {
                Text(
                    text = "Order Confirmed!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CafeGreen,
                    fontWeight = FontWeight.Bold
                )

                Text("This is a mock order simulation -- sample inventory has been updated.")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNewOrder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start a New Order")
                }
            } else {
                Text(
                    text = "Order Summary",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { line: CartLine ->
                        Text("${line.quantity} x ${line.item.name}")
                    }
                }

                orderError?.let {
                    Text(
                        text = it,
                        color = CafeError,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = onPlaceOrder,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Place Order")
                }
            }
        }
    }
}
