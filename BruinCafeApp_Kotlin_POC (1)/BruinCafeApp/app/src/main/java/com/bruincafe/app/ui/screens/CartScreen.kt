package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CartLine

/** Screen S-05: Cart. */
@Composable
fun CartScreen(
    cart: List<CartLine>,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    onKeepBrowsing: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Your Cart") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cart.isEmpty()) {
                Text(
                    "Your cart is empty.",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(cart) { line ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(line.item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Qty: ${line.quantity}", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { onRemove(line.item.id) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onCheckout,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Checkout")
                }
                OutlinedButton(onClick = onKeepBrowsing, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Keep Browsing Menu")
                }
            }
        }
    }
}
