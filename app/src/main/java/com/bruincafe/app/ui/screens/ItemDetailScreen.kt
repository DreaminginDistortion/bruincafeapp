package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.data.StockStatus

/** Screen S-04: Item Detail. Sold-out rule: "Add to Cart" is disabled (and says why) when sold out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: MenuItem,
    onAddToCart: (MenuItem) -> Unit,
    onBack: () -> Unit
) {
    val tracked = CafeRepository.isTracked(item.id)
    val status = CafeRepository.stockStatus(item.id)
    val courseLabel = item.course.name.lowercase().replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle(item.name) }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (tracked) StockBadge(status) else Tag("Made to order", MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("${item.categoryLabel} · $courseLabel", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description.ifBlank { "Details coming soon." },
                style = MaterialTheme.typography.bodyLarge
            )
            if (tracked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${CafeRepository.quantityOnHand(item.id)} remaining today", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onAddToCart(item) },
                enabled = status != StockStatus.SOLD_OUT,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Text(if (status == StockStatus.SOLD_OUT) "Sold Out" else "Add to Cart")
            }
        }
    }
}
