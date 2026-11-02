package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CartLine

/** Screen S-05: Cart. */
@Composable
fun CartScreen(
    cart: List<CartLine>,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    onKeepBrowsing: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val totalItems = cart.sumOf { it.quantity }
    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle("Your Cart") }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cart.isEmpty()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SectionLabel("Your cart is empty")
                    Text("Add something from the menu and it will show up here.")
                }
                Column(modifier = Modifier.weight(1f)) {}
            } else {
                Text(
                    "$totalItems ${if (totalItems == 1) "item" else "items"} in your cart",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                    items(cart) { line ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer
                            ),
                            border = contrastBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(line.item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Quantity: ${line.quantity}", style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedButton(
                                    onClick = { onRemove(line.item.id) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Remove ${line.item.name} from cart"
                                    }
                                ) { Text("Remove") }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCheckout,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) { Text("Checkout") }
                OutlinedButton(onClick = onKeepBrowsing, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Keep Browsing Menu")
                }
            }
        }
    }
}
