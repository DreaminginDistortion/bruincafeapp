/*
 * RestockSimulationScreen.kt
 * BruinCafeApp
 *
 * Modified by David Crosen on 11/13/2026:
 * - Replaced old static buttons with direct number input and instant update actions.
 * - Maintained exact InventoryItem parameter passing and live region accessibility semantics.
 */
package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.InventoryItem

/**
 * Screen S-09: Restock / Unstock Simulation. Staff enter a quantity adjustment to add or remove stock.
 */
@Composable
fun RestockSimulationScreen(
    item: InventoryItem,
    currentQuantity: Int,
    onConfirmRestock: (Int) -> Boolean,
    onBack: () -> Unit
) {
    var qtyText by remember { mutableStateOf("10") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle("Adjust Stock: ${item.itemName}") }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Current quantity on hand: $currentQuantity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Helper instruction message showing staff they can remove stock
            Text(
                "Tip: Type a positive number (e.g. 10) to add stock, or a negative number (e.g. -5) to remove stock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text("Adjustment Amount (e.g. 10 or -5)") },
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Stock adjustment input for ${item.itemName}"
                        liveRegion = LiveRegionMode.Polite
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val delta = qtyText.toIntOrNull() ?: 0
                    if (delta != 0) {
                        val success = onConfirmRestock(delta)
                        if (success) {
                            isError = false
                            statusMessage = "Stock updated successfully."
                        } else {
                            isError = true
                            statusMessage = "Cannot remove more than the available quantity ($currentQuantity on hand)."
                        }
                    } else {
                        isError = true
                        statusMessage = "Please enter a valid non-zero number."
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) { Text("Apply Stock Change") }

            statusMessage?.let { msg ->
                Text(
                    msg,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Back to Inventory")
            }
        }
    }
}