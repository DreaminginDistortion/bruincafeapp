package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.MenuItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

// Demo-only picks so the holiday windows can be shown without waiting for the calendar. null = today.
private val holidayDateChips: List<Pair<String, LocalDate?>> = listOf(
    "Today" to null,
    "Oct 20, 2026" to LocalDate.of(2026, 10, 20),
    "Nov 20, 2026" to LocalDate.of(2026, 11, 20),
    "Dec 15, 2026" to LocalDate.of(2026, 12, 15),
    "Jan 15, 2027" to LocalDate.of(2027, 1, 15)
)

/**
 * Screen S-11: Café & Holiday Menu.
 *  - "Café Items": always available, usually made to order.
 *  - "Holiday Menu": shown ONLY while today's date is inside a holiday's active dates;
 *    otherwise the tab says no holiday menu is active and when the next one starts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeHolidayScreen(
    locations: List<CafeLocation>,
    selectedLocationId: String,
    onLocationChange: (String) -> Unit,
    holidayDemoDate: LocalDate?,
    onHolidayDemoDateChange: (LocalDate?) -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onViewCart: () -> Unit,
    cartCount: Int,
    cartMessage: String?,
    onCartMessageShown: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val snackbarHost = remember { SnackbarHostState() }
    CartMessageEffect(cartMessage, snackbarHost, onCartMessageShown)
    var tab by rememberSaveable { mutableStateOf(0) }

    val date = holidayDemoDate ?: LocalDate.now()
    val cafeItems = CafeRepository.cafeItems(selectedLocationId)
    val holidayItems = CafeRepository.activeHolidayItems(selectedLocationId, date)
    val nextHoliday = CafeRepository.nextHoliday(selectedLocationId, date)
    val locationName = locations.firstOrNull { it.id == selectedLocationId }?.displayName ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { ScreenTitle("Café & Holiday Menu") }, navigationIcon = { BackButton(onBack) })
        },
        bottomBar = { CartBottomBar(cartCount, onViewCart) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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

            TabRow(selectedTabIndex = tab, containerColor = scheme.surface) {
                Tab(
                    selected = tab == 0, onClick = { tab = 0 },
                    selectedContentColor = scheme.primary, unselectedContentColor = scheme.onSurface,
                    text = { Text("Café Items") }
                )
                Tab(
                    selected = tab == 1, onClick = { tab = 1 },
                    selectedContentColor = scheme.primary, unselectedContentColor = scheme.onSurface,
                    text = { Text("Holiday Menu") }
                )
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                if (tab == 0) {
                    item { SectionLabel("Café items at $locationName") }
                    if (cafeItems.isEmpty()) {
                        item { Text("No café items are listed for $locationName.") }
                    } else {
                        items(cafeItems) { s ->
                            MenuItemCard(item = s.item, madeToOrder = s.madeToOrder, onClick = { onItemClick(s.item) })
                        }
                    }
                } else {
                    item {
                        Column {
                            Text(
                                "Showing the holiday menu for ${date.format(dateFormat)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            ChipRow("Demo: pretend today is", holidayDateChips, holidayDemoDate, onHolidayDemoDateChange)
                        }
                    }
                    if (holidayItems.isEmpty()) {
                        item {
                            Surface(
                                color = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = contrastBorder(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("No holiday menu is active right now.", style = MaterialTheme.typography.titleMedium)
                                    nextHoliday?.let {
                                        Text("Next up: ${it.holidayName} starts ${it.activeFrom?.format(dateFormat)}.")
                                    } ?: Text("No upcoming holiday menus are scheduled.")
                                }
                            }
                        }
                    } else {
                        // One heading per holiday, e.g. "Fall Favorites, available through Oct 31, 2026".
                        val groups = holidayItems.groupBy { it.holidayName ?: "Holiday" }
                        groups.forEach { (name, list) ->
                            val until = list.first().activeUntil?.format(dateFormat)
                            item { SectionLabel(if (until != null) "$name (available through $until)" else name) }
                            items(list) { s ->
                                MenuItemCard(item = s.item, madeToOrder = s.madeToOrder, onClick = { onItemClick(s.item) })
                            }
                        }
                    }
                }
            }
        }
    }
}
