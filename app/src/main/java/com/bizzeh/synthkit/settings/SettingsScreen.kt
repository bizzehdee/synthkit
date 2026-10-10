package com.bizzeh.synthkit.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.BuildConfig
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.StudioTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    actions: SettingsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = StudioTheme.palette
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, color = p.text)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            SettingRow(stringResource(R.string.settings_theme), stringResource(R.string.settings_theme_detail)) {
                Choices(
                    label = stringResource(R.string.settings_theme),
                    options = ThemeChoice.entries,
                    selected = settings.theme,
                    text = {
                        stringResource(when (it) {
                            ThemeChoice.SYSTEM -> R.string.theme_system
                            ThemeChoice.LIGHT -> R.string.theme_light
                            ThemeChoice.DARK -> R.string.theme_dark
                        })
                    },
                    onSelect = actions.setTheme,
                )
            }
            SettingRow(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_detail)) {
                OnOff(stringResource(R.string.settings_haptics), settings.haptics, actions.setHaptics)
            }
            SettingRow(stringResource(R.string.settings_click), stringResource(R.string.settings_click_detail)) {
                OnOff(stringResource(R.string.settings_click), settings.clickInNewProjects, actions.setClickInNewProjects)
            }
            SettingRow(stringResource(R.string.settings_quantise), stringResource(R.string.settings_quantise_detail)) {
                Choices(
                    label = stringResource(R.string.settings_quantise),
                    options = Quantise.entries,
                    selected = settings.newTrackQuantise,
                    text = {
                        stringResource(when (it) {
                            Quantise.OFF -> R.string.quantise_off
                            Quantise.EIGHTH -> R.string.quantise_eighth
                            Quantise.SIXTEENTH -> R.string.quantise_sixteenth
                        })
                    },
                    onSelect = actions.setNewTrackQuantise,
                )
            }
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = p.muted,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, detail: String, control: @Composable () -> Unit) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .raised(p, 14.dp, fill = p.panel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = p.text)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = p.muted)
        }
        control()
    }
}

@Composable
private fun OnOff(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = StudioTheme.palette
    Switch(
        checked = on,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(checkedTrackColor = p.amber, checkedThumbColor = p.onLit),
        modifier = Modifier.semantics { contentDescription = label },
    )
}

@Composable
private fun <T> Choices(label: String, options: List<T>, selected: T, text: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = label }) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(text(option)) }
        }
    }
}
