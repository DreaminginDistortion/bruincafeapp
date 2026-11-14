/*
 * HomeScreen.kt
 * BruinCafeApp
 *
 * Modified by David Crosen:
 * - Updated rewards card text and descriptions to reflect the new punch card rules ($7 spent = 1 punch, 10 punches = free lunch).
 */

package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.Rewards
import com.bruincafe.app.data.RewardsStatus

/**
 * Screen S-01: Home
 * Shows the customer's punches and progress first, then the main places to go.
 * The whole screen scrolls, so it stays usable at "Extra large" text sizes.
 */
@Composable
fun HomeScreen(
    rewards: RewardsStatus,
    onOrder: () -> Unit,
    onCafeHoliday: () -> Unit,
    onViewCart: () -> Unit,
    onOrderHistory: () -> Unit,
    onAccessibility: () -> Unit,
    onStaffLogin: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle("Bruin Café") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RewardsCard(rewards)
           // New Rewards system (Added by David Crosen)
            Text(
                "Earn 1 punch for every $7 spent on your order. " +
                        "10 punches unlocks a free lunch!",
                style = MaterialTheme.typography.bodyMedium
            )
            // David Changed "Order Lunch" to "Order Lunch/Breakfast"
            Button(onClick = onOrder, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Order Lunch/Breakfast")
            }
            OutlinedButton(onClick = onCafeHoliday, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Café & Holiday Menu")
            }
            OutlinedButton(onClick = onViewCart, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("View Cart")
            }
            OutlinedButton(onClick = onOrderHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Order History")
            }
            OutlinedButton(onClick = onAccessibility, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Accessibility Settings")
            }
            TextButton(onClick = onStaffLogin, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Staff Login")
            }
        }
    }
}

@Composable
private fun RewardsCard(rewards: RewardsStatus) {
    val scheme = MaterialTheme.colorScheme
    val unlocked = if (rewards.freeLunchesEarned == 1) "1 free lunch unlocked" else "${rewards.freeLunchesEarned} free lunches unlocked"
    // One clear sentence for screen readers instead of five separate fragments.
    val spoken = "Bruin Rewards Punch Card. ${rewards.totalPunches} punches. " +
            "${rewards.punchesToNext} punches until your next free lunch. $unlocked."

    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
        colors = CardDefaults.cardColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
        border = contrastBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Bruin Rewards Punch Card",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "${rewards.totalPunches} Punches",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { rewards.progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(12.dp),
                color = scheme.onPrimary,
                trackColor = scheme.onPrimary.copy(alpha = 0.35f)
            )
            Text("${rewards.punchesToNext} punches until your next free lunch")
            Text(unlocked, fontWeight = FontWeight.SemiBold)
        }
    }
}