package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.InventoryItem

/** Screen S-09: Restock Simulation. Staff enter a quantity and confirm; inventory updates live. */
@Composable
fun RestockSimulationScreen(
    item: InventoryItem,
    currentQuantity: Int,
    onConfirmRestock: (Int) -> Unit,
    onBack: () -> Unit
) {
    var addQty by remember { mutableStateOf(10) }
    var justConfirmed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle("Restock: ${item.itemName}") }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Current quantity on hand: $currentQuantity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Simulated restock amount: $addQty",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }   // announces each change
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { addQty = (addQty - 5).coerceAtLeast(1) },    // never goes below 1
                    modifier = Modifier.semantics { contentDescription = "Decrease restock amount by 5" }
                ) { Text("-5") }
                OutlinedButton(
                    onClick = { addQty += 5 },
                    modifier = Modifier.semantics { contentDescription = "Increase restock amount by 5" }
                ) { Text("+5") }
                OutlinedButton(
                    onClick = { addQty += 20 },
                    modifier = Modifier.semantics { contentDescription = "Increase restock amount by 20" }
                ) { Text("+20") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onConfirmRestock(addQty)
                    justConfirmed = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) { Text("Confirm Restock") }
            if (justConfirmed) {
                Text(
                    "Inventory updated: now $currentQuantity on hand.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Back to Inventory")
            }
        }
    }
}
