package com.bruincafe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bruincafe.app.data.AccessibilitySettings

/**
 * Screen S-12: Accessibility Settings.
 * Choices are saved on the device and applied to the whole app immediately
 * (BruinCafeTheme reads them in MainActivity).
 */
@Composable
fun AccessibilitySettingsScreen(
    settings: AccessibilitySettings,
    onChange: (AccessibilitySettings) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { ScreenTitle("Accessibility Settings") }, navigationIcon = { BackButton(onBack) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel("Text size")
            Text(
                "This is added on top of your phone's own font size setting.",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(modifier = Modifier.selectableGroup()) {
                AccessibilitySettings.textScaleOptions.forEach { (label, scale) ->
                    val selected = settings.textScale == scale
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onChange(settings.copy(textScale = scale)) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)   // the whole row is the target
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            Text(
                "Sample text: Roasted Tomato Soup & Grilled Cheese",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SectionLabel("Display")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = settings.highContrast,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(highContrast = it)) }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("High contrast colors", modifier = Modifier.weight(1f).padding(end = 12.dp))
                Switch(checked = settings.highContrast, onCheckedChange = null)
            }
            Text(
                "Black text on white with heavy outlines around cards and buttons.",
                style = MaterialTheme.typography.bodyMedium
            )

            SectionLabel("Also built in")
            Text(
                "Screen reader support (TalkBack): every item is read as one clear sentence, " +
                    "titles are headings, and menu changes and cart updates are announced. " +
                    "Buttons are at least 48dp tall. Stock and meal status are always written out " +
                    "in words, never color alone. The app also follows your phone's font size and display size.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
