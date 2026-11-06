package culture.thelove.tlc.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.PresenceAudience
import culture.thelove.tlc.android.data.PresenceSettings
import culture.thelove.tlc.android.data.PresenceSignal
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.StatTile
import culture.thelove.tlc.android.ui.TlcAvatar
import culture.thelove.tlc.android.ui.TlcMark
import culture.thelove.tlc.android.ui.theme.TlcPalette
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.experience.Profile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Facts the profile may show without inventing a balance or a rank. */
public data class ProfileStats(
    public val people: Int,
    public val tokens: Int,
    public val joinedAt: Long,
)

/**
 * The person's own face, name, door and visibility.
 *
 * This is a profile, not a form: a framed face, a monogram when there is no photo, the handle the
 * person chose, and the three things a person actually needs to control — how they appear, who can
 * see them, and the passphrase that opens the app. There are no biometrics anywhere in this screen
 * or behind it.
 */
@Composable
public fun ProfileScreen(
    profile: Profile,
    stats: ProfileStats,
    hasPassphrase: Boolean,
    presence: PresenceSettings,
    busy: Boolean,
    processingPhoto: Boolean,
    onSave: (String, String) -> Unit,
    onSavePhoto: (Uri, Float, Float, Float) -> Unit,
    onRemovePhoto: () -> Unit,
    onOpenPresence: () -> Unit,
    onLock: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var circle by rememberSaveable(profile.circleName) { mutableStateOf(profile.circleName) }
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pendingUri = uri }

    val cleanName = name.trim()
    val valid = cleanName.isNotBlank() &&
        cleanName.length <= Profile.MAX_NAME &&
        circle.length <= Profile.MAX_CIRCLE
    val dirty = cleanName != profile.displayName || circle.trim() != profile.circleName
    val initials = profile.displayName

    pendingUri?.let { uri ->
        AvatarStudio(
            uri = uri,
            onCancel = { pendingUri = null },
            onApply = { zoom, offsetX, offsetY ->
                pendingUri = null
                onSavePhoto(uri, zoom, offsetX, offsetY)
            },
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.profile_remove_photo)) },
            text = { Text(stringResource(R.string.profile_remove_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemovePhoto()
                    },
                    enabled = !busy && !processingPhoto,
                ) {
                    Text(stringResource(R.string.profile_remove_photo))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) {
                    Text(stringResource(R.string.profile_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            ProfileHero(
                profile = profile,
                stats = stats,
                initials = initials,
                processingPhoto = processingPhoto,
                onEditPhoto = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }

        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(R.string.profile_stat_people),
                    value = stats.people.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.profile_stat_tokens),
                    value = stats.tokens.toString(),
                    modifier = Modifier.weight(1f),
                    accent = TlcPalette.colors.forest,
                )
                StatTile(
                    label = stringResource(R.string.profile_stat_since),
                    value = month(stats.joinedAt),
                    modifier = Modifier.weight(1f),
                    accent = TlcPalette.colors.royal,
                )
            }
        }

        item(key = "details") {
            SectionCard(title = stringResource(R.string.profile_details_title)) {
                Text(
                    text = stringResource(R.string.profile_details_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= Profile.MAX_NAME) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    isError = cleanName.isBlank(),
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.profile_counter,
                                cleanName.length,
                                Profile.MAX_NAME,
                            ),
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedTextField(
                    value = circle,
                    onValueChange = { if (it.length <= Profile.MAX_CIRCLE) circle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_circle_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.profile_counter,
                                circle.length,
                                Profile.MAX_CIRCLE,
                            ),
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                )
                AnimatedVisibility(visible = dirty) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onSave(cleanName, circle.trim()) },
                            enabled = valid && !busy && !processingPhoto,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.profile_save))
                        }
                        TextButton(
                            onClick = {
                                name = profile.displayName
                                circle = profile.circleName
                            },
                        ) {
                            Text(stringResource(R.string.profile_discard))
                        }
                    }
                }
                if (!dirty) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = TlcPalette.colors.forest,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.profile_saved_state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "photo") {
            SectionCard(title = stringResource(R.string.profile_photo)) {
                Text(
                    text = stringResource(R.string.profile_photo_studio_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        enabled = !busy && !processingPhoto,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            stringResource(
                                if (profile.photoRef == null) {
                                    R.string.profile_choose_photo
                                } else {
                                    R.string.profile_replace_photo
                                },
                            ),
                        )
                    }
                    if (profile.photoRef != null) {
                        TextButton(
                            onClick = { confirmRemove = true },
                            enabled = !busy && !processingPhoto,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_remove_photo),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.profile_photo_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "presence") {
            PresenceSummaryCard(presence = presence, onOpen = onOpenPresence)
        }

        item(key = "security") {
            SectionCard(title = stringResource(R.string.profile_security_title)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(9.dp).size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = profile.handle?.value ?: stringResource(R.string.profile_no_handle),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = stringResource(
                                if (hasPassphrase) {
                                    R.string.profile_passphrase_on
                                } else {
                                    R.string.profile_passphrase_off
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.profile_no_biometrics),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onLock,
                        enabled = hasPassphrase && !busy,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.profile_lock))
                    }
                    TextButton(
                        onClick = { confirmSignOut = true },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_sign_out),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item(key = "privacy") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.profile_privacy_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(
    profile: Profile,
    stats: ProfileStats,
    initials: String,
    processingPhoto: Boolean,
    onEditPhoto: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(
                            scheme.primary,
                            scheme.secondary,
                            TlcPalette.colors.royal,
                        ),
                    ),
                ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.1f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.95f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.32f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.profile_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.86f),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.profile_member_since, month(stats.joinedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.86f),
                )
            }
            TlcMark(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 34.dp)
                    .size(74.dp),
                tint = Color.White.copy(alpha = 0.22f),
                accent = Color.White.copy(alpha = 0.34f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 102.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                TlcAvatar(
                    photoRef = profile.photoRef,
                    size = 128.dp,
                    initials = initials,
                    ring = true,
                    ringColor = scheme.surface,
                )
                if (processingPhoto) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .clickable(enabled = !processingPhoto, onClick = onEditPhoto),
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = stringResource(R.string.profile_edit_photo),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            if (profile.circleName.isNotBlank()) {
                Text(
                    text = profile.circleName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile.handle?.let { handle ->
                    Surface(
                        shape = CircleShape,
                        color = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = handle.value,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = TlcPalette.colors.royalContainer,
                    contentColor = TlcPalette.colors.onRoyalContainer,
                ) {
                    Text(
                        text = stringResource(
                            when (profile.role) {
                                Role.SINGLE -> R.string.role_single
                                Role.I_PARTNER -> R.string.role_i_partner
                                Role.LOVE_MODEL -> R.string.role_love_model
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenceSummaryCard(presence: PresenceSettings, onOpen: () -> Unit) {
    SectionCard(title = stringResource(R.string.presence_title)) {
        Text(
            text = if (presence.invisible) {
                stringResource(R.string.presence_invisible_summary)
            } else {
                stringResource(R.string.presence_summary)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PresenceRow(PresenceSignal.ONLINE_NOW, presence)
        PresenceRow(PresenceSignal.LAST_SEEN, presence)
        PresenceRow(PresenceSignal.TYPING, presence)
        PresenceRow(PresenceSignal.READ_RECEIPTS, presence)
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.presence_manage))
        }
    }
}

@Composable
private fun PresenceRow(signal: PresenceSignal, presence: PresenceSettings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = presenceSignalLabel(signal),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = if (
                presence.shares(signal, null) ||
                (!presence.invisible && presence.audience(signal) == PresenceAudience.CIRCLE)
            ) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Text(
                text = presenceAudienceLabel(presence.audience(signal)),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun presenceSignalLabel(signal: PresenceSignal): String = stringResource(
    when (signal) {
        PresenceSignal.ONLINE_NOW -> R.string.presence_online_now
        PresenceSignal.LAST_SEEN -> R.string.presence_last_seen
        PresenceSignal.TYPING -> R.string.presence_typing
        PresenceSignal.READ_RECEIPTS -> R.string.presence_read_receipts
    },
)

@Composable
internal fun presenceAudienceLabel(audience: PresenceAudience): String = stringResource(
    when (audience) {
        PresenceAudience.NOBODY -> R.string.presence_nobody
        PresenceAudience.SELECTED -> R.string.presence_selected
        PresenceAudience.CIRCLE -> R.string.presence_circle
        PresenceAudience.EVERYONE -> R.string.presence_everyone
    },
)

private fun month(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
