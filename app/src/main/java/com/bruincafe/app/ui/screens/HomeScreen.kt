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
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * Screen S-01: Home = Rewards home.
 * Shows the customer's points and progress first, then the main places to go.
 * The whole screen scrolls, so it stays usable at "Extra large" text sizes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rewards: RewardsStatus,
    onOrder: () -> Unit,
    onCafeHoliday: () -> Unit,
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

            Text(
                "Earn ${Rewards.POINTS_PER_ITEM} points for every item you order. " +
                    "Every ${Rewards.POINTS_PER_REWARD} points unlocks a reward.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(onClick = onOrder, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Order Now")
            }
            OutlinedButton(onClick = onCafeHoliday, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Café & Holiday Menu")
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
    val unlocked = if (rewards.rewardsEarned == 1) "1 reward unlocked" else "${rewards.rewardsEarned} rewards unlocked"
    // One clear sentence for screen readers instead of five separate fragments.
    val spoken = "Bruin Rewards. ${rewards.totalPoints} points. " +
        "${rewards.pointsToNext} points until your next reward. $unlocked."

    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
        colors = CardDefaults.cardColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
        border = contrastBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Bruin Rewards",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "${rewards.totalPoints} points",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { rewards.progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(12.dp),
                color = scheme.onPrimary,
                trackColor = scheme.onPrimary.copy(alpha = 0.35f)
            )
            Text("${rewards.pointsToNext} points until your next reward")
            Text(unlocked, fontWeight = FontWeight.SemiBold)
        }
    }
}
