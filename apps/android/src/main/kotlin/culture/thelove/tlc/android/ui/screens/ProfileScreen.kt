package culture.thelove.tlc.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.GlassPanel
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.TlcAvatar
import culture.thelove.tlc.android.ui.theme.TlcPalette
import culture.thelove.tlc.experience.Profile

/**
 * The person's own face and name.
 *
 * A photo never leaves app-private storage and never enters the state file or the record: the
 * profile keeps a file name, and the image is downscaled, centre-cropped and re-encoded without
 * metadata. Replacing or removing it is always one tap, and saving the name is a separate act so
 * an accidental edit cannot change the circle.
 */
@Composable
public fun ProfileScreen(
    profile: Profile,
    busy: Boolean,
    processingPhoto: Boolean,
    onSave: (String, String) -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var circle by rememberSaveable(profile.circleName) { mutableStateOf(profile.circleName) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    val cleanName = name.trim()
    val valid = cleanName.isNotBlank() && cleanName.length <= Profile.MAX_NAME && circle.length <= Profile.MAX_CIRCLE
    val dirty = cleanName != profile.displayName || circle.trim() != profile.circleName

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
                eyebrow = stringResource(R.string.profile_eyebrow),
                title = stringResource(R.string.profile_title),
                body = stringResource(R.string.profile_body),
            )
        }

        item(key = "hero") {
            GlassPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TlcAvatar(photoRef = profile.photoRef, size = 96.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(stringResource(R.string.profile_eyebrow), color = TlcPalette.colors.royal)
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = profile.circleName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        profile.handle?.let {
                            Text(
                                text = it.value,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }

        item(key = "photo") {
            SectionCard(title = stringResource(R.string.profile_photo)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TlcAvatar(photoRef = profile.photoRef, size = 72.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (processingPhoto) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = stringResource(R.string.receipt_processing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                enabled = !busy && !processingPhoto,
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
                                OutlinedButton(
                                    onClick = { confirmRemove = true },
                                    enabled = !busy && !processingPhoto,
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Text(stringResource(R.string.profile_remove_photo))
                                }
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.profile_photo_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (confirmRemove) {
                    Text(
                        text = stringResource(R.string.receipt_remove_confirm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                confirmRemove = false
                                onRemovePhoto()
                            },
                            enabled = !busy && !processingPhoto,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.profile_remove_photo))
                        }
                        TextButton(onClick = { confirmRemove = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        }

        item(key = "details") {
            SectionCard(title = stringResource(R.string.profile_name)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= Profile.MAX_NAME) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    isError = cleanName.isBlank(),
                    shape = MaterialTheme.shapes.large,
                )
                OutlinedTextField(
                    value = circle,
                    onValueChange = { if (it.length <= Profile.MAX_CIRCLE) circle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_circle_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = MaterialTheme.shapes.large,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onSave(cleanName, circle.trim()) },
                        enabled = valid && dirty && !busy && !processingPhoto,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.profile_save))
                    }
                    if (dirty) {
                        TextButton(
                            onClick = {
                                name = profile.displayName
                                circle = profile.circleName
                            },
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_profile_body),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "privacy") {
            SectionCard(
                title = stringResource(R.string.settings_privacy),
                container = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.profile_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
