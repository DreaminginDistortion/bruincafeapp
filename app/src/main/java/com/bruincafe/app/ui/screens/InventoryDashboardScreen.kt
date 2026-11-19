package com.bruincafe.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** Screen S-08: Inventory Dashboard. Staff view inventory per location and category (Regular / Café / Holiday), with low-stock alerts and quick +1 / −1 stock controls. */
@Composable
fun InventoryDashboardScreen(
    locations: List<CafeLocation>,
    selectedLocationId: String,
    onLocationChange: (String) -> Unit,
    inventory: List<InventoryItem>,
    selectedKind: String,
    onKindChange: (String) -> Unit,
    onSelectItemToRestock: (InventoryItem) -> Unit,
    onQuickAdjust: (InventoryItem, Int) -> Unit,
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

            // Category tabs: Regular menu / Café / Holiday are restocked separately.
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InventoryItem.KINDS) { kind ->
                    SelectableChip(
                        label = InventoryItem.kindLabel(kind),
                        selected = kind == selectedKind,
                        onClick = { onKindChange(kind) }
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
                if (inventory.isEmpty()) {
                    item { Text("No ${InventoryItem.kindLabel(selectedKind)} items at this location.") }
                }
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
                            Column(horizontalAlignment = Alignment.End) {
                                StockBadge(row.status)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { onQuickAdjust(row, -1) },
                                        enabled = row.quantityOnHand > 0,
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .semantics { contentDescription = "Un-stock one ${row.itemName}" }
                                    ) { Text("−1") }
                                    OutlinedButton(
                                        onClick = { onQuickAdjust(row, 1) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .semantics { contentDescription = "Restock one ${row.itemName}" }
                                    ) { Text("+1") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
