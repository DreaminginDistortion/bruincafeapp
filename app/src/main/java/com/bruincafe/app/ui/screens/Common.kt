package com.bruincafe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeRepository
import com.bruincafe.app.data.MenuItem
import com.bruincafe.app.data.StockStatus
import com.bruincafe.app.ui.theme.LocalHighContrast

/*
 * Shared, accessible building blocks. Every screen uses these instead of
 * hand-rolling its own, so a fix here fixes the whole app.
 *
 * Accessibility rules baked in:
 *  - titles and section labels are marked as HEADINGS (screen-reader heading navigation)
 *  - tappable things are at least 48dp tall
 *  - state is spoken in words, never conveyed by color alone
 *  - colors come from the theme (contrast-checked; switchable to high contrast)
 */

/** Heavy outline used in high-contrast mode; null (no border) otherwise. */
@Composable
fun contrastBorder(): BorderStroke? =
    if (LocalHighContrast.current) BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground) else null

/** Screen title for a TopAppBar; announced as a heading by TalkBack. */
@Composable
fun ScreenTitle(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() })
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() }
    )
}

/** Visible "Back" text button (easier to find than a bare arrow, and read aloud as "Go back"). */
@Composable
fun BackButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "Go back" }
    ) { Text("Back") }
}

fun stockSentence(status: StockStatus): String = when (status) {
    StockStatus.IN_STOCK -> "In stock"
    StockStatus.LOW_STOCK -> "Low stock"
    StockStatus.SOLD_OUT -> "Sold out"
}

/** Outlined pill. The WORDS carry the meaning (In Stock / Low Stock / Sold Out); color only reinforces it. */
@Composable
fun StockBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color = when (status) {
        StockStatus.IN_STOCK -> scheme.primary
        StockStatus.LOW_STOCK -> scheme.secondary
        StockStatus.SOLD_OUT -> scheme.error
    }
    Tag(text = stockSentence(status), color = color, modifier = modifier)
}

@Composable
fun Tag(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = color,
        border = BorderStroke(1.5.dp, color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Pill-shaped choice (location, date, tab-like filters). 48dp tall, announced as selected / not selected. */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val border = if (LocalHighContrast.current) {
        BorderStroke(2.dp, scheme.onBackground)
    } else {
        BorderStroke(1.dp, scheme.primary)       // outline so the chip edge is visible, not just a tint
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) scheme.primary else scheme.secondaryContainer,
        contentColor = if (selected) scheme.onPrimary else scheme.onSecondaryContainer,
        border = border,
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(label) }
    }
}

/**
 * One menu/café/holiday item. Read aloud as a single sentence, e.g.
 * "Tiramisu. Lunch, Dessert. Low stock. International Day Italy."
 */
@Composable
fun MenuItemCard(item: MenuItem, onClick: () -> Unit, madeToOrder: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val tracked = CafeRepository.isTracked(item.id)
    val status = CafeRepository.stockStatus(item.id)
    val courseLabel = item.course.name.lowercase().replaceFirstChar { it.uppercase() }
    val description = item.description.ifBlank { "Details coming soon." }
    val availability = when {
        tracked -> stockSentence(status)
        madeToOrder -> "Made to order"
        else -> "Available"
    }
    val spoken = "${item.name}. ${item.categoryLabel}, $courseLabel. $availability. $description"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClickLabel = "View details for ${item.name}", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken },
        colors = CardDefaults.cardColors(
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer
        ),
        border = contrastBorder(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (tracked) StockBadge(status) else Tag(availability, scheme.primary)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "${item.categoryLabel} · $courseLabel",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * CART BAR -- pinned to the bottom of the screen via Scaffold(bottomBar = ...).
 *
 * BUG FIX ("cart not appearing on HQ page"): the cart bar used to be the last child of
 * the screen's content Column, underneath the demo controls and a scrolling list. On
 * smaller screens / larger text (and on HQ, which has the most content: breakfast +
 * lunch + banners) it could be pushed off the bottom and never show. As a Scaffold
 * bottomBar it is laid out first, so it is always visible on every menu screen.
 */
@Composable
fun CartBottomBar(itemCount: Int, onClick: () -> Unit) {
    val noun = if (itemCount == 1) "item" else "items"
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = contrastBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClickLabel = "Open cart", role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Cart, $itemCount $noun. Double tap to open." }
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("View Cart ($itemCount)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Shows a snackbar (announced by screen readers) when the ViewModel posts a cart message, then clears it. */
@Composable
fun CartMessageEffect(message: String?, host: SnackbarHostState, onShown: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            host.showSnackbar(message)
            onShown()
        }
    }
}
