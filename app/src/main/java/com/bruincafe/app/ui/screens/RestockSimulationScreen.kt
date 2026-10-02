package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.InventoryItem
import com.bruincafe.app.ui.theme.CafeGreen

/** Screen S-09: Restock Simulation. Staff enter a quantity and confirm; inventory updates live. */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Restock: ${item.itemName}") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp)
        ) {
            Text(
                text = "Current quantity on hand: $currentQuantity",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Simulated restock amount: $addQty")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { if (addQty > 1) addQty -= 5 }
                ) { Text("-5") }
                OutlinedButton(
                    onClick = { addQty += 5 }
                ) { Text("+5") }
                OutlinedButton(
                    onClick = { addQty += 20 }
                ) { Text("+20") }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onConfirmRestock(addQty)
                    justConfirmed = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirm Restock") }
            if (justConfirmed) {
                Text(
                    text = "Inventory updated: now ${currentQuantity} on hand.",
                    color = CafeGreen,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back to Inventory") }
        }
    }
}
