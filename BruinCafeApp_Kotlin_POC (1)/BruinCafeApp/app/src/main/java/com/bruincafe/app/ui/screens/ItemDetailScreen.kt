package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.data.StockStatus

/** Screen S-04: Item Detail. Sold-out rule: "Add to Cart" is disabled when sold out. */
@Composable
fun ItemDetailScreen(
    item: MenuItem,
    onAddToCart: (MenuItem) -> Unit,
    onBack: () -> Unit
) {
    val status = CafeRepository.stockStatus(item.id)

    Scaffold(topBar = { TopAppBar(title = { Text(item.name) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            StockBadge(status)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.course.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description.ifBlank { "Details coming soon." },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${CafeRepository.quantityOnHand(item.id)} remaining today",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onAddToCart(item) },
                enabled = status != StockStatus.SOLD_OUT,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (status == StockStatus.SOLD_OUT) "Sold Out" else "Add to Cart")
            }
        }
    }
}
