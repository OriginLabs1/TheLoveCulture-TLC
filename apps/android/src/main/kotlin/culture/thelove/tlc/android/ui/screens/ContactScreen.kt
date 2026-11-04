package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.relativeTime
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.experience.ContactEvent
import culture.thelove.tlc.domain.ContactIntent
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.Role

/**
 * The Pioneer Desk: the local slice of the Contact & Consent Framework.
 *
 * Purpose and category come before the body. The opening is shown to the sender before it leaves
 * the app, the channel is chosen deliberately, and only the fact of contact is kept — never the
 * message. The public wording still carries the "under legal review" label.
 */
@Composable
public fun ContactScreen(
    role: Role,
    displayName: String,
    handle: Handle?,
    contactLog: List<ContactEvent>,
    busy: Boolean,
    onOpen: (ContactChannel, ContactPurpose, String?, String?) -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var purposeName by rememberSaveable { mutableStateOf(ContactPurpose.MENTORSHIP.name) }
    var category by rememberSaveable { mutableStateOf("") }
    var guidance by rememberSaveable { mutableStateOf("") }
    val purpose = ContactPurpose.valueOf(purposeName)
    val intent = ContactIntent(
        purpose = purpose,
        category = category.trim().ifBlank { null },
        expectedGuidance = guidance.trim().ifBlank { null },
    )
    val signature = buildString {
        append("\n\n— ")
        append(displayName)
        if (handle != null) {
            append(" (")
            append(handle.value)
            append(")")
        }
    }
    val message = intent.opening(role) + signature
    val clipboard = LocalClipboardManager.current

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
                eyebrow = stringResource(R.string.contact_eyebrow),
                title = stringResource(R.string.contact_title),
                body = stringResource(R.string.contact_body),
            )
        }

        item(key = "legal") {
            SectionCard(
                title = stringResource(R.string.contact_legal_title),
                container = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.contact_legal_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        item(key = "purpose") {
            SectionCard(title = stringResource(R.string.contact_purpose)) {
                Text(
                    text = stringResource(R.string.contact_purpose_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ContactPurpose.entries.forEach { candidate ->
                        FilterChip(
                            selected = purpose == candidate,
                            onClick = { purposeName = candidate.name },
                            label = { Text(candidate.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { if (it.length <= 120) category = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.contact_category)) },
                    placeholder = { Text(stringResource(R.string.contact_category_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedTextField(
                    value = guidance,
                    onValueChange = { if (it.length <= 280) guidance = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.contact_guidance)) },
                    placeholder = { Text(stringResource(R.string.contact_guidance_hint)) },
                    minLines = 2,
                    shape = MaterialTheme.shapes.large,
                )
            }
        }

        item(key = "preview") {
            SectionCard(
                title = stringResource(R.string.contact_preview),
                container = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(message)) },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.action_copy))
                    }
                    TextButton(onClick = { category = ""; guidance = "" }) {
                        Text(stringResource(R.string.contact_reset))
                    }
                }
            }
        }

        item(key = "channels") {
            SectionCard(title = stringResource(R.string.contact_channels)) {
                Text(
                    text = stringResource(R.string.contact_channels_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onOpen(ContactChannel.TELEGRAM, purpose, category, guidance) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.contact_telegram))
                }
                OutlinedButton(
                    onClick = { onOpen(ContactChannel.EMAIL, purpose, category, guidance) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.contact_email))
                }
                OutlinedButton(
                    onClick = { onOpen(ContactChannel.WEBSITE, purpose, category, guidance) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.contact_website))
                }
                Text(
                    text = stringResource(R.string.contact_phone_deferred),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "log-label") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(stringResource(R.string.contact_log))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                if (contactLog.isNotEmpty()) {
                    TextButton(onClick = onClearLog, enabled = !busy) {
                        Text(stringResource(R.string.contact_clear))
                    }
                }
            }
        }

        if (contactLog.isEmpty()) {
            item(key = "log-empty") {
                SectionCard(title = stringResource(R.string.contact_log_empty_title)) {
                    Text(
                        text = stringResource(R.string.contact_log_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(contactLog, key = { it.id }) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.purpose.label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Pill(text = event.channel.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                    event.category?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = relativeTime(event.at),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
