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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.InventoryItem
import com.bruincafe.app.ui.theme.CafeGold
import com.bruincafe.app.ui.theme.CafeGoldLight
import com.bruincafe.app.ui.theme.CafeGreen

/** Screen S-08: Inventory Dashboard. Staff view inventory per location, with low-stock alerts. */
@Composable
fun InventoryDashboardScreen(
    locations: List<CafeLocation>,
    selectedLocationId: String,
    onLocationChange: (String) -> Unit,
    inventory: List<InventoryItem>,
    onSelectItemToRestock: (InventoryItem) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory — Staff") },
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
                    val selected = loc.id == selectedLocationId
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) CafeGreen else CafeGoldLight,
                        modifier = Modifier.clickable { onLocationChange(loc.id) }
                    ) {
                        Text(
                            loc.displayName,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else CafeGold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            val lowStockCount = inventory.count {
                it.status != com.bruincafe.app.data.StockStatus.IN_STOCK
            }
            if (lowStockCount > 0) {
                Text(
                    "$lowStockCount item(s) need attention (low stock or sold out)",
                    color = CafeGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(inventory) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelectItemToRestock(row) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(row.itemName, fontWeight = FontWeight.Bold)
                                Text(
                                    "${row.quantityOnHand} on hand · threshold ${row.lowStockThreshold}",
                                    style = MaterialTheme.typography.bodySmall
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
