package com.bruincafe.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.InventoryItem
import com.bruincafe.app.data.StockStatus

/** Screen S-08: Inventory Dashboard. Staff view inventory per location, with low-stock alerts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardScreen(
    locations: List<CafeLocation>,
    selectedLocationId: String,
    onLocationChange: (String) -> Unit,
    inventory: List<InventoryItem>,
    onSelectItemToRestock: (InventoryItem) -> Unit,
    onLogout: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val needAttention = inventory.count { it.status != StockStatus.IN_STOCK }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Inventory (Staff)") },
                actions = { TextButton(onClick = onLogout) { Text("Log Out") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations) { loc ->
                    SelectableChip(
                        label = loc.displayName,
                        selected = loc.id == selectedLocationId,
                        onClick = { onLocationChange(loc.id) }
                    )
                }
            }

            if (needAttention > 0) {
                Text(
                    "$needAttention item(s) need attention (low stock or sold out)",
                    color = scheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(inventory) { row ->
                    val spoken = "${row.itemName}. ${stockSentence(row.status)}. " +
                        "${row.quantityOnHand} on hand, low stock threshold ${row.lowStockThreshold}."
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(onClickLabel = "Restock ${row.itemName}", role = Role.Button) {
                                onSelectItemToRestock(row)
                            }
                            .semantics { contentDescription = spoken },
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
                                Text(row.itemName, fontWeight = FontWeight.Bold)
                                Text(
                                    "${row.quantityOnHand} on hand · threshold ${row.lowStockThreshold}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            StockBadge(row.status)
                        }
                    }
                }
            }
        }
    }
}
