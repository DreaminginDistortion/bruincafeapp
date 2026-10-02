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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.Course
import com.bruincafe.app.data.DayMenu
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.data.StockStatus
import com.bruincafe.app.ui.theme.CafeCream
import com.bruincafe.app.ui.theme.CafeError
import com.bruincafe.app.ui.theme.CafeGold
import com.bruincafe.app.ui.theme.CafeGoldLight
import com.bruincafe.app.ui.theme.CafeGreen

/**
 * Screen S-03: Weekly Lunch Menu.
 *
 * Encodes: WLM-02/03 (five weekday rows), WLM-04/holiday rule (closed day ->
 * message only, no items), WLM-05 (multi-item themed days grouped together),
 * WLM-06 (placeholder for blank description), WLM-07 (today's special),
 * WLM-09 (no row for the date -> "Menu Unavailable"), and the
 * HQ-only-breakfast rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyMenuScreen(
    location: CafeLocation,
    weekMenu: List<DayMenu>,
    breakfastItems: List<MenuItem>,
    simulatedDate: String,
    onSimulatedDateChange: (String) -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onBack: () -> Unit,
    onViewCart: () -> Unit,
    cartCount: Int
) {
    val todaysSpecials = CafeRepository.todaysSpecial(location.id, simulatedDate)
    val allDates = CafeRepository.allMenuDates()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${location.displayName} — Weekly Menu") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Demo-only "simulated today" date picker, since the printed menu's
            // week (Sept 7-11, 2026) may not equal the device's real today.
            Surface(color = CafeCream) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Simulated \"Today\" (demo control)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allDates) { (date, dow) ->
                            val selected = date == simulatedDate
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) CafeGreen else CafeGoldLight,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { onSimulatedDateChange(date) }
                            ) {
                                Text(
                                    text = dow.take(3),
                                    color = if (selected) androidx.compose.ui.graphics.Color.White else CafeGold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {

                if (todaysSpecials.isNotEmpty()) {
                    item {
                        Surface(
                            color = CafeGreen,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Today's Special",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                todaysSpecials.forEach {
                                    Text(
                                        text = it.name,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (breakfastItems.isNotEmpty()) {
                    item {
                        SectionLabel("Ready-Made Breakfast (HQ Only)")
                    }
                    items(breakfastItems) { item ->
                        MenuItemCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }

                val day = weekMenu.firstOrNull { it.date == simulatedDate }

                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        SectionLabel(day?.dayOfWeek ?: "Menu")
                    }
                }

                when {
                    day == null -> item {
                        Text(
                            text = "Menu Unavailable for this date.",
                            color = CafeError,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    day.isClosed -> item {
                        Surface(
                            color = CafeGoldLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = day.closedMessage ?: "Closed.",
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    else -> items(day.items) { item ->
                        MenuItemCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }

            Surface(color = CafeGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).clickable { onViewCart() },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "View Cart ($cartCount)",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(item: MenuItem, onClick: () -> Unit) {
    val status = CafeRepository.stockStatus(item.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CafeGoldLight),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                StockBadge(status)
            }
            Text(
                // WLM-06: fall back to a placeholder rather than showing a blank field.
                text = item.description.ifBlank { "Details coming soon." },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = item.course.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = CafeGold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
