package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.PresenceSettings
import culture.thelove.tlc.android.data.SharedContact
import culture.thelove.tlc.android.data.SharedMessage
import culture.thelove.tlc.android.data.SharedMessageState
import culture.thelove.tlc.android.data.SharedPlane
import culture.thelove.tlc.android.data.SharedStatus
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.relativeTime
import culture.thelove.tlc.android.ui.theme.TlcPalette

/**
 * The live shared plane, drawn honestly.
 *
 * "Live" is only said while the socket is open. Presence is drawn from the relay's projection: a
 * hidden signal is "Not shared", never "offline". Message text is the plaintext on this device; the
 * relay only ever holds the sealed envelope.
 */
@Composable
public fun LiveScreen(
    plane: SharedPlane,
    handle: String?,
    displayName: String,
    presence: PresenceSettings,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onExportHandoff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var chatWith by rememberSaveable { mutableStateOf<String?>(null) }
    val chat = chatWith
    if (chat != null) {
        ChatPane(
            plane = plane,
            handle = chat,
            onBack = { chatWith = null },
            modifier = modifier,
        )
        return
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
                eyebrow = stringResource(R.string.live_eyebrow),
                title = stringResource(R.string.live_title),
                body = stringResource(R.string.live_body),
            )
        }

        item(key = "status") {
            StatusCard(
                plane = plane,
                handle = handle,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
            )
        }

        plane.lastNotice?.let { notice ->
            item(key = "notice") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { plane.noticeSeen() }) {
                            Text(stringResource(R.string.live_dismiss))
                        }
                    }
                }
            }
        }

        if (plane.status == SharedStatus.LIVE) {
            item(key = "identity") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Eyebrow(stringResource(R.string.live_connected_eyebrow))
                        Text(
                            text = "@${plane.me.orEmpty()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.live_e2ee_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val pending = plane.contacts.filter { it.pending }
            if (pending.isNotEmpty()) {
                item(key = "requests") {
                    SectionCard(title = stringResource(R.string.live_requests_title)) {
                        pending.forEach { contact ->
                            PendingRow(
                                contact = contact,
                                onAccept = { plane.respondConnection(contact.handle, true) },
                                onDecline = { plane.respondConnection(contact.handle, false) },
                            )
                        }
                    }
                }
            }

            item(key = "add") {
                AddPersonCard(plane = plane)
            }

            item(key = "people-title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.live_people_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { plane.refresh() }) {
                        Text(stringResource(R.string.live_refresh))
                    }
                }
            }

            val people = plane.contacts.filter { it.accepted }
            if (people.isEmpty()) {
                item(key = "people-empty") {
                    Text(
                        text = stringResource(R.string.live_people_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(people, key = { it.handle }) { contact ->
                    PersonRow(
                        contact = contact,
                        unread = plane.messages[contact.handle].orEmpty().count {
                            !it.mine && it.state != SharedMessageState.READ
                        },
                        onClick = {
                            chatWith = contact.handle
                            plane.setActiveChat(contact.handle)
                        },
                    )
                }
            }
        } else {
            item(key = "offline-note") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Eyebrow(stringResource(R.string.live_honesty_title), color = TlcPalette.colors.royal)
                        Text(
                            text = stringResource(R.string.live_offline_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "privacy-note") {
            Text(
                text = stringResource(R.string.live_privacy_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item(key = "peer") {
            SectionCard(title = stringResource(R.string.live_peer_title)) {
                Text(
                    text = stringResource(R.string.live_peer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onExportHandoff,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.live_peer_export))
                }
            }
        }
    }

    LaunchedEffect(plane.status, presence) {
        if (plane.status == SharedStatus.LIVE) plane.syncPresence(presence)
    }
}

@Composable
private fun StatusCard(
    plane: SharedPlane,
    handle: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.live_delete)) },
            text = { Text(stringResource(R.string.live_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        plane.deleteAccount()
                    },
                ) {
                    Text(stringResource(R.string.live_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    SectionCard(title = stringResource(R.string.live_connection_title)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (plane.status == SharedStatus.LIVE) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (plane.status == SharedStatus.LIVE) {
                    TlcPalette.colors.forest
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = statusLabel(plane.status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = plane.relayUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        plane.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        when {
            plane.status == SharedStatus.LIVE -> {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.live_disconnect))
                }
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.live_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            plane.status == SharedStatus.CONNECTING -> {
                Text(
                    text = stringResource(R.string.live_connecting_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    enabled = handle != null,
                ) {
                    Text(stringResource(R.string.live_connect))
                }
                if (handle == null) {
                    Text(
                        text = stringResource(R.string.live_need_username),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPersonCard(plane: SharedPlane) {
    var text by rememberSaveable { mutableStateOf("") }
    SectionCard(title = stringResource(R.string.live_add_title)) {
        Text(
            text = stringResource(R.string.live_add_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 24) text = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.live_add_label)) },
                prefix = { Text("@") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            TextButton(
                onClick = {
                    plane.requestConnection(text)
                    text = ""
                },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.live_add_action))
            }
        }
    }
}

@Composable
private fun PendingRow(
    contact: SharedContact,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "@${contact.handle}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (contact.state == "REQUESTED") {
                stringResource(R.string.live_request_sent)
            } else {
                stringResource(R.string.live_request_received)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (contact.state == "PENDING") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.live_accept))
                }
                OutlinedButton(onClick = onDecline, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.live_decline))
                }
            }
        }
    }
}

@Composable
private fun PersonRow(
    contact: SharedContact,
    unread: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = contact.display.ifBlank { contact.handle },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${contact.handle} · ${presenceLine(contact)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.keyChanged) {
                Text(
                    text = stringResource(R.string.live_key_changed_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (unread > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = unread.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPane(
    plane: SharedPlane,
    handle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contact = plane.contacts.firstOrNull { it.handle == handle }
    val conversation = plane.messages[handle].orEmpty()
    val listState = rememberLazyListState()
    var text by rememberSaveable(handle) { mutableStateOf("") }

    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) listState.animateScrollToItem(conversation.lastIndex)
    }
    LaunchedEffect(handle) { plane.setActiveChat(handle) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(onClick = { plane.setActiveChat(null); onBack() }) {
                Text(stringResource(R.string.live_back_people))
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "@$handle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = contact?.let { presenceLine(it) } ?: stringResource(R.string.live_presence_none),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (contact?.keyChanged == true) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = stringResource(R.string.live_key_changed),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (conversation.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.live_chat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                items(conversation, key = { it.localId }) { message ->
                    MessageBubble(
                        message = message,
                        stateLabel = if (message.mine) stateLabel(message) else null,
                    )
                }
            }
        }

        plane.typingFrom?.let {
            Text(
                text = stringResource(R.string.live_typing, it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    val was = text.isNotBlank()
                    text = it
                    if (!was && it.isNotBlank()) plane.sendTyping(handle, true)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.live_chat_hint)) },
                shape = MaterialTheme.shapes.large,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        plane.send(handle, text)
                        text = ""
                        plane.sendTyping(handle, false)
                    }
                },
                enabled = text.isNotBlank() && plane.status == SharedStatus.LIVE,
            ) {
                Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.live_chat_send))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SharedMessage, stateLabel: String?) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = MaterialTheme.shapes.large,
            color = if (message.mine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (message.mine) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = relativeTime(message.at),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    stateLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (!message.verified) {
                    Text(
                        text = stringResource(R.string.live_unverified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: SharedStatus): String = stringResource(
    when (status) {
        SharedStatus.OFFLINE -> R.string.live_status_offline
        SharedStatus.CONNECTING -> R.string.live_status_connecting
        SharedStatus.LIVE -> R.string.live_status_live
        SharedStatus.ERROR -> R.string.live_status_error
    },
)

@Composable
private fun presenceLine(contact: SharedContact): String {
    val projection = contact.projection ?: return stringResource(R.string.live_presence_none)
    if (projection.onlineNow == true) return stringResource(R.string.live_presence_online)
    val seen = projection.lastSeen
    return when {
        seen == null || seen <= 0L -> if (projection.onlineNow == false) {
            stringResource(R.string.live_presence_offline)
        } else {
            stringResource(R.string.live_presence_none)
        }
        projection.lastSeenCoarse -> stringResource(R.string.live_presence_recently)
        else -> stringResource(R.string.live_presence_last_seen, relativeTime(seen))
    }
}

@Composable
private fun stateLabel(message: SharedMessage): String = stringResource(
    when (message.state) {
        SharedMessageState.QUEUED -> R.string.live_state_queued
        SharedMessageState.SENT -> R.string.live_state_sent
        SharedMessageState.DELIVERED -> R.string.live_state_delivered
        SharedMessageState.READ -> R.string.live_state_read
        SharedMessageState.FAILED -> R.string.live_state_failed
    },
)
