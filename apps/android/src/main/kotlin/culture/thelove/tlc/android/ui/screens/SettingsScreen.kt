package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.experience.Preferences
import culture.thelove.tlc.experience.ThemeMode

@Composable
public fun SettingsScreen(
    preferences: Preferences,
    versionName: String,
    versionCode: Int,
    busy: Boolean,
    onPreferences: (Preferences) -> Unit,
    onExportRecord: () -> Unit,
    onExportBundle: () -> Unit,
    onImportBundle: () -> Unit,
    onShareSeal: () -> Unit,
    onVerify: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 4.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = stringResource(R.string.settings_eyebrow),
                title = stringResource(R.string.settings_title),
                body = stringResource(R.string.settings_privacy_body),
            )
        }

        item(key = "appearance") {
            SectionCard(title = stringResource(R.string.settings_appearance)) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.theme == mode,
                            onClick = { onPreferences(preferences.copy(theme = mode)) },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }
                SettingSwitch(
                    title = stringResource(R.string.settings_motion),
                    body = stringResource(R.string.settings_motion_body),
                    checked = preferences.motion,
                    enabled = !busy,
                    onChecked = { onPreferences(preferences.copy(motion = it)) },
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_haptics),
                    body = stringResource(R.string.settings_haptics_body),
                    checked = preferences.haptics,
                    enabled = !busy,
                    onChecked = { onPreferences(preferences.copy(haptics = it)) },
                )
            }
        }

        item(key = "receptive") {
            SectionCard(
                title = stringResource(R.string.settings_receptive),
                container = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.settings_receptive_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_receptive),
                    body = stringResource(R.string.role_single_body),
                    checked = preferences.receptiveMode,
                    enabled = !busy,
                    onChecked = { onPreferences(preferences.copy(receptiveMode = it)) },
                )
            }
        }

        item(key = "data") {
            SectionCard(title = stringResource(R.string.settings_data)) {
                SettingAction(
                    title = stringResource(R.string.settings_export_record),
                    body = stringResource(R.string.settings_export_record_body),
                    enabled = !busy,
                    onClick = onExportRecord,
                )
                SettingAction(
                    title = stringResource(R.string.settings_export_bundle),
                    body = stringResource(R.string.settings_export_bundle_body),
                    enabled = !busy,
                    onClick = onExportBundle,
                )
                SettingAction(
                    title = stringResource(R.string.settings_import_bundle),
                    body = stringResource(R.string.settings_import_bundle_body),
                    enabled = !busy,
                    onClick = onImportBundle,
                )
                SettingAction(
                    title = stringResource(R.string.settings_share_seal),
                    body = stringResource(R.string.settings_share_seal_body),
                    enabled = !busy,
                    onClick = onShareSeal,
                )
                SettingAction(
                    title = stringResource(R.string.settings_verify),
                    body = stringResource(R.string.settings_verify_body),
                    enabled = !busy,
                    onClick = onVerify,
                )
            }
        }

        item(key = "privacy") {
            SectionCard(
                title = stringResource(R.string.settings_privacy),
                container = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.settings_data_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        item(key = "about") {
            SettingAction(
                title = stringResource(R.string.settings_about),
                body = stringResource(R.string.settings_about_body),
                enabled = true,
                onClick = onAbout,
            )
        }

        item(key = "version") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Eyebrow(stringResource(R.string.app_doctrine))
                Text(
                    text = stringResource(R.string.settings_version, versionName, versionCode),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_stores),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChecked(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun SettingAction(
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}
