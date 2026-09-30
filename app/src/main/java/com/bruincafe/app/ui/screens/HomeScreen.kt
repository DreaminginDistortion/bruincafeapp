package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.ui.theme.CafeGold
import com.bruincafe.app.ui.theme.CafeGreen

/** Screen S-01: Home / Splash. Routes into the customer flow or staff mode. */
@Composable
fun HomeScreen(
    onOrderLunch: () -> Unit,
    onStaffLogin: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bruin Café",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = CafeGreen
            )
            Text(
                text = "Weekly lunch, done fresh.",
                style = MaterialTheme.typography.titleMedium,
                color = CafeGold
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onOrderLunch) {
                Text("Order Lunch")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onStaffLogin) {
                Text("Staff Login")
            }
        }
    }
}
