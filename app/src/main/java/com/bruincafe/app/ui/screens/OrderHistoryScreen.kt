package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.Order
import com.bruincafe.app.data.RewardsStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val whenFormat = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")

/**
 * Screen S-10: Order History. Newest first. Stored on this device only (see AppStorage),
 * and the rewards points shown on the home screen are added up from exactly these orders.
 */
@Composable
fun OrderHistoryScreen(
    orders: List<Order>,
    rewards: RewardsStatus,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle("Order History") }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (orders.isEmpty()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    SectionLabel("No orders yet")
                    Text("Your completed orders will appear here, and each item you order earns rewards points.")
                }
            } else {
                Text(
                    "${orders.size} ${if (orders.size == 1) "order" else "orders"}, ${rewards.totalPoints} points earned in total",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(orders) { order ->
                        val whenText = Instant.ofEpochMilli(order.timeMillis)
                            .atZone(ZoneId.systemDefault()).format(whenFormat)
                        val itemsSpoken = order.lines.joinToString(", ") { "${it.quantity} ${it.name}" }
                        val spoken = "Order on $whenText at ${order.locationName}. $itemsSpoken. " +
                            "${order.itemCount} items, ${order.points} points earned."
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .semantics(mergeDescendants = true) { contentDescription = spoken },
                            colors = CardDefaults.cardColors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer
                            ),
                            border = contrastBorder(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(whenText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(order.locationName, style = MaterialTheme.typography.bodyMedium)
                                order.lines.forEach { line ->
                                    Text("${line.quantity} x ${line.name}", modifier = Modifier.padding(top = 2.dp))
                                }
                                Text(
                                    "+${order.points} points",
                                    color = scheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
