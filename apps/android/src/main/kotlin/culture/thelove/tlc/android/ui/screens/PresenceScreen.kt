package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.PresenceAudience
import culture.thelove.tlc.android.data.PresenceSettings
import culture.thelove.tlc.android.data.PresenceSignal
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.theme.TlcPalette
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.experience.Person

/**
 * Presence is a choice, never a default.
 *
 * Four independent signals, four audiences, per-person selection and a permanent escape hatch.
 * Every signal starts at **Nobody**. A hidden signal is "not shared" — the app never guesses
 * "offline" from silence. These choices are stored on this device and are the instruction the
 * shared plane must obey.
 */
@Composable
public fun PresenceScreen(
    settings: PresenceSettings,
    people: List<Person>,
    onSave: (PresenceSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    var selectedText by rememberSaveable { mutableStateOf("") }
    var hiddenText by rememberSaveable { mutableStateOf("") }
    var previewHandle by rememberSaveable(people.firstOrNull()?.handle?.value) {
        mutableStateOf(people.firstOrNull()?.handle?.value)
    }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    fun update(next: PresenceSettings) {
        draft = next
        onSave(next)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.presence_reset)) },
            text = { Text(stringResource(R.string.presence_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        update(PresenceSettings.PRIVATE)
                    },
                ) {
                    Text(stringResource(R.string.presence_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 4.dp,
            bottom = 44.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = stringResource(R.string.presence_eyebrow),
                title = stringResource(R.string.presence_title),
                body = stringResource(R.string.presence_body),
            )
        }

        item(key = "invisible") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (draft.invisible) {
                    TlcPalette.colors.royalContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (draft.invisible) {
                    TlcPalette.colors.onRoyalContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (draft.invisible) Icons.Filled.Lock else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.presence_invisible),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                if (draft.invisible) {
                                    R.string.presence_invisible_on
                                } else {
                                    R.string.presence_invisible_off
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = draft.invisible,
                        onCheckedChange = { update(draft.copy(invisible = it)) },
                    )
                }
            }
        }

        item(key = "signals") {
            SectionCard(title = stringResource(R.string.presence_signals_title)) {
                Text(
                    text = stringResource(R.string.presence_signals_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PresenceSignalEditor(
                    signal = PresenceSignal.ONLINE_NOW,
                    settings = draft,
                    onChange = { audience -> update(draft.withAudience(PresenceSignal.ONLINE_NOW, audience)) },
                )
                PresenceSignalEditor(
                    signal = PresenceSignal.LAST_SEEN,
                    settings = draft,
                    onChange = { audience -> update(draft.withAudience(PresenceSignal.LAST_SEEN, audience)) },
                )
                PresenceSignalEditor(
                    signal = PresenceSignal.TYPING,
                    settings = draft,
                    onChange = { audience -> update(draft.withAudience(PresenceSignal.TYPING, audience)) },
                )
                PresenceSignalEditor(
                    signal = PresenceSignal.READ_RECEIPTS,
                    settings = draft,
                    onChange = { audience -> update(draft.withAudience(PresenceSignal.READ_RECEIPTS, audience)) },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.presence_exact_last_seen),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.presence_exact_last_seen_body),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = draft.exactLastSeen,
                        onCheckedChange = { update(draft.copy(exactLastSeen = it)) },
                    )
                }
            }
        }

        item(key = "selected") {
            SectionCard(title = stringResource(R.string.presence_selected_title)) {
                Text(
                    text = stringResource(R.string.presence_selected_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PeopleChips(
                    people = people,
                    selected = draft.selectedPeople,
                    onToggle = { handle ->
                        val next = draft.selectedPeople.toMutableSet().apply {
                            if (!add(handle)) remove(handle)
                        }
                        update(draft.copy(selectedPeople = next))
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedText,
                        onValueChange = { if (it.length <= 24) selectedText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.presence_add_handle)) },
                        prefix = { Text("@") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    TextButton(
                        onClick = {
                            Handle.parse(selectedText)?.let { handle ->
                                update(
                                    draft.copy(
                                        selectedPeople = draft.selectedPeople + handle.value.lowercase(),
                                    ),
                                )
                                selectedText = ""
                            }
                        },
                        enabled = Handle.parse(selectedText) != null,
                    ) {
                        Text(stringResource(R.string.presence_add))
                    }
                }
            }
        }

        item(key = "hidden") {
            SectionCard(title = stringResource(R.string.presence_hidden_title)) {
                Text(
                    text = stringResource(R.string.presence_hidden_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PeopleChips(
                    people = people,
                    selected = draft.hiddenFrom,
                    onToggle = { handle ->
                        val next = draft.hiddenFrom.toMutableSet().apply {
                            if (!add(handle)) remove(handle)
                        }
                        update(draft.copy(hiddenFrom = next))
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hiddenText,
                        onValueChange = { if (it.length <= 24) hiddenText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.presence_add_handle)) },
                        prefix = { Text("@") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    TextButton(
                        onClick = {
                            Handle.parse(hiddenText)?.let { handle ->
                                update(
                                    draft.copy(hiddenFrom = draft.hiddenFrom + handle.value.lowercase()),
                                )
                                hiddenText = ""
                            }
                        },
                        enabled = Handle.parse(hiddenText) != null,
                    ) {
                        Text(stringResource(R.string.presence_add))
                    }
                }
            }
        }

        item(key = "preview") {
            SectionCard(title = stringResource(R.string.presence_preview_title)) {
                Text(
                    text = stringResource(R.string.presence_preview_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (people.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        people.forEach { person ->
                            FilterChip(
                                selected = previewHandle == person.handle.value,
                                onClick = { previewHandle = person.handle.value },
                                label = { Text(person.displayName) },
                            )
                        }
                    }
                }
                val viewer = previewHandle
                val viewerName = people.firstOrNull { it.handle.value == viewer }?.displayName
                    ?: viewer
                    ?: stringResource(R.string.presence_preview_default_viewer)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresenceSignal.entries.forEach { signal ->
                        val shared = draft.shares(signal, viewer)
                        val label = presenceSignalLabel(signal)
                        val detail = when {
                            !shared -> stringResource(R.string.presence_preview_not_shared)
                            signal == PresenceSignal.LAST_SEEN && !draft.exactLastSeen ->
                                stringResource(R.string.presence_preview_coarse)
                            else -> stringResource(R.string.presence_preview_shared)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (shared) {
                                    TlcPalette.colors.forest
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.presence_preview_line, viewerName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "honesty") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Eyebrow(stringResource(R.string.presence_honesty_title), color = TlcPalette.colors.royal)
                    Text(
                        text = stringResource(R.string.presence_honesty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "reset") {
            Button(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.presence_reset))
            }
        }
    }
}

@Composable
private fun PresenceSignalEditor(
    signal: PresenceSignal,
    settings: PresenceSettings,
    onChange: (PresenceAudience) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = presenceSignalLabel(signal),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresenceAudience.entries.forEach { audience ->
                FilterChip(
                    selected = settings.audience(signal) == audience,
                    onClick = { onChange(audience) },
                    label = { Text(presenceAudienceLabel(audience)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (settings.audience(signal) == audience) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PeopleChips(
    people: List<Person>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (people.isEmpty()) {
        Text(
            text = stringResource(R.string.presence_no_circle_people),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        people.forEach { person ->
            val handle = person.handle.value.lowercase()
            FilterChip(
                selected = handle in selected,
                onClick = { onToggle(handle) },
                label = { Text(person.displayName) },
            )
        }
    }
}
