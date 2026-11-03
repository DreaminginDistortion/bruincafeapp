package com.bruincafe.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.CafeLocation

/** Screen S-02: Location Select. WLM-01 -- a location must be chosen before any menu query. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectScreen(
    locations: List<CafeLocation>,
    onLocationChosen: (CafeLocation) -> Unit,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(title = { ScreenTitle("Choose a Location") }, navigationIcon = { BackButton(onBack) })
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding)) {
            items(locations) { location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable(onClickLabel = "Choose ${location.displayName}", role = Role.Button) {
                            onLocationChosen(location)
                        }
                        .semantics { contentDescription = "${location.displayName}. ${location.subtitle}." },
                    colors = CardDefaults.cardColors(
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer
                    ),
                    border = contrastBorder(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(location.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(location.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
