package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.MealPeriodDef
import com.bruincafe.app.data.MealScheduler
import com.bruincafe.app.data.MenuItem
import java.time.LocalTime

// Demo-only quick picks for the "what time is it" override. null = real clock.
private val timeChips: List<Pair<String, LocalTime?>> = listOf(
    "Live clock" to null,
    "8:00 AM" to LocalTime.of(8, 0),
    "12:00 PM" to LocalTime.of(12, 0),
    "4:00 PM" to LocalTime.of(16, 0)
)

/**
 * Screen S-03: Menu (breakfast or lunch, chosen by the clock).
 *
 * The cart bar is the Scaffold's bottomBar, so it is pinned to the bottom of the
 * screen and can never be pushed off by the content above it (the "cart not showing
 * on the HQ page" bug). Adding an item posts a snackbar, which screen readers announce.
 */
@Composable
fun WeeklyMenuScreen(
    location: CafeLocation,
    periods: List<MealPeriodDef>,
    now: LocalTime,
    simulatedTime: LocalTime?,
    onSimulatedTimeChange: (LocalTime?) -> Unit,
    simulatedDate: String,
    onSimulatedDateChange: (String) -> Unit,
    dataSource: String,
    isLoading: Boolean,
    loadError: String?,
    onRefresh: () -> Unit,
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
    var showDemo by rememberSaveable { mutableStateOf(true) }

    val active = MealScheduler.active(now, periods)
    val next = MealScheduler.next(now, periods)
    val periodLabel = active?.label ?: ""
    val closedMessage = CafeRepository.closedMessage(location.id, simulatedDate)
    val menuItems = if (active != null) CafeRepository.itemsFor(location.id, simulatedDate, active.name) else emptyList()
    val specials = if (active?.name == "lunch") CafeRepository.todaysSpecial(location.id, simulatedDate) else emptyList()
    val dateOptions = CafeRepository.allMenuDates().map { (d, dow) -> "${dow.take(3)} ${d.takeLast(5)}" to d }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("${location.displayName} Menu") },
                navigationIcon = { BackButton(onBack) }
            )
        },
        bottomBar = { CartBottomBar(cartCount, onViewCart) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Surface(color = scheme.surfaceVariant, contentColor = scheme.onSurfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Demo controls", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { showDemo = !showDemo }) {
                            Text(if (showDemo) "Hide" else "Show")
                        }
                    }
                    if (showDemo) {
                        ChipRow("Time of day", timeChips, simulatedTime, onSimulatedTimeChange)
                        ChipRow("Date", dateOptions, simulatedDate, onSimulatedDateChange)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Data: $dataSource" + if (isLoading) " (loading)" else "",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefresh) { Text("Refresh") }
                    }
                    loadError?.let {
                        Text(
                            it,
                            color = scheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                item { MealPeriodBanner(active, next, now) }

                if (specials.isNotEmpty() && closedMessage == null) {
                    item {
                        Surface(
                            color = scheme.secondary,
                            contentColor = scheme.onSecondary,
                            shape = RoundedCornerShape(10.dp),
                            border = contrastBorder(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Today's Special", fontWeight = FontWeight.Bold)
                                specials.forEach { Text(it.name) }
                            }
                        }
                    }
                }

                when {
                    closedMessage != null -> item {
                        Surface(
                            color = scheme.secondaryContainer,
                            contentColor = scheme.onSecondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = contrastBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = closedMessage, modifier = Modifier.padding(14.dp))
                        }
                    }
                    active == null -> { /* the banner above already explains the closure */ }
                    menuItems.isEmpty() -> item {
                        Text(
                            "No ${periodLabel.lowercase()} items listed for this date at ${location.displayName}.",
                            color = scheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> items(menuItems) { item ->
                        MenuItemCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPeriodBanner(active: MealPeriodDef?, next: MealPeriodDef?, now: LocalTime) {
    val scheme = MaterialTheme.colorScheme
    val title = if (active != null) "Now serving: ${active.label}" else "Kitchen closed"
    val subtitle = when {
        active != null -> "${MealScheduler.format(active.start)} to ${MealScheduler.format(active.end)}"
        next != null -> "Next up: ${next.label} at ${MealScheduler.format(next.start)}"
        else -> "No serving windows configured"
    }
    val spoken = "$title. $subtitle. Current time ${MealScheduler.format(now)}."
    Surface(
        color = if (active != null) scheme.primary else scheme.secondary,
        contentColor = if (active != null) scheme.onPrimary else scheme.onSecondary,
        shape = RoundedCornerShape(10.dp),
        border = contrastBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            // Live region: when the clock crosses 11:00 and breakfast becomes lunch,
            // a screen reader announces the change without the user having to hunt for it.
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle)
            Text("Current time: ${MealScheduler.format(now)}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Reusable row of selectable pills (used for the demo time, date, and holiday-date pickers). */
@Composable
fun <T> ChipRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { (label, value) ->
                SelectableChip(label = label, selected = value == selected, onClick = { onSelect(value) })
            }
        }
    }
}
