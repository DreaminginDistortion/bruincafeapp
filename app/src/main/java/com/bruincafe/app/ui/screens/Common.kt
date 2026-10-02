package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.StockStatus
import com.bruincafe.app.ui.theme.CafeError
import com.bruincafe.app.ui.theme.CafeGold
import com.bruincafe.app.ui.theme.CafeGreen

/**
 * One badge, reused everywhere a stock status is shown (weekly menu, item
 * detail, staff inventory dashboard) -- change it once here, it updates
 * everywhere. This is the kind of "easily changeable, layout-wise" component
 * called out in the request.
 */
@Composable
fun StockBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        StockStatus.IN_STOCK -> "In Stock" to CafeGreen
        StockStatus.LOW_STOCK -> "Low Stock" to CafeGold
        StockStatus.SOLD_OUT -> "Sold Out" to CafeError
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}
