package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.IdentityStore
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.TlcMark
import culture.thelove.tlc.android.ui.theme.TlcPalette

/**
 * The door. A username and a word/phrase passphrase — nothing else.
 *
 * There is no fingerprint prompt, no face prompt, no passkey sheet and no device-credential
 * fallback. The person's own words are the only key. The passphrase is verified against a salted
 * PBKDF2 verifier on the device and is never stored, logged or transmitted.
 *
 * [setup] is used when a record exists but this device has no door yet (for example an app updated
 * from a build without a passphrase). It asks for the same words twice and creates the verifier in
 * place, so the record is never discarded just to add a door.
 */
@Composable
public fun LockScreen(
    username: String,
    displayName: String,
    setup: Boolean,
    busy: Boolean,
    error: String?,
    onUnlock: (String, String) -> Unit,
    onCreate: (String, String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var enteredName by rememberSaveable(username, setup) { mutableStateOf(username) }
    var passphrase by rememberSaveable(setup) { mutableStateOf("") }
    var again by rememberSaveable(setup) { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    val passed = remember(passphrase) { IdentityStore.passphraseProblem(passphrase) == null }
    val matches = passphrase.isNotEmpty() && passphrase == again
    val canSubmit = if (setup) {
        enteredName.isNotBlank() && passed && matches && !busy
    } else {
        passphrase.isNotBlank() && !busy
    }

    if (confirmReset && !setup) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.lock_reset_title)) },
            text = { Text(stringResource(R.string.lock_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.lock_reset_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    fun submit() {
        if (!canSubmit) return
        if (setup) onCreate(enteredName, passphrase) else onUnlock(enteredName, passphrase)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            TlcPalette.colors.royal,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    TlcMark(
                        modifier = Modifier.padding(18.dp).size(54.dp),
                        tint = Color.White,
                        accent = Color.White.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Eyebrow(
                text = stringResource(
                    if (setup) R.string.lock_setup_eyebrow else R.string.lock_eyebrow,
                ),
                color = TlcPalette.colors.royal,
            )
            Text(
                text = if (setup) {
                    stringResource(R.string.lock_setup_title)
                } else {
                    stringResource(R.string.lock_hello, displayName)
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    if (setup) R.string.lock_setup_body else R.string.lock_body,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = enteredName,
            onValueChange = { enteredName = it.lowercase().removePrefix("@").take(24) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.lock_username)) },
            prefix = { Text("@") },
            singleLine = true,
            // Unlocking lets the person correct the name. Setup locks it to the claimed handle,
            // except when an older record never claimed one and there is nothing to lock to.
            enabled = !setup || enteredName.isBlank(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            shape = MaterialTheme.shapes.large,
        )

        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.lock_passphrase)) },
            singleLine = true,
            visualTransformation = if (reveal) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (setup) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                TextButton(onClick = { reveal = !reveal }) {
                    Text(
                        stringResource(
                            if (reveal) R.string.lock_hide else R.string.lock_show,
                        ),
                    )
                }
            },
            supportingText = if (setup && passphrase.isNotEmpty() && !passed) {
                { Text(stringResource(R.string.lock_setup_weak)) }
            } else {
                null
            },
            shape = MaterialTheme.shapes.large,
        )

        if (setup) {
            OutlinedTextField(
                value = again,
                onValueChange = { again = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lock_setup_confirm)) },
                singleLine = true,
                visualTransformation = if (reveal) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = again.isNotEmpty() && !matches,
                supportingText = {
                    Text(
                        stringResource(
                            when {
                                again.isNotEmpty() && !matches -> R.string.lock_setup_mismatch
                                matches && passed -> R.string.lock_setup_ready
                                else -> R.string.onboard_pass_hint
                            },
                        ),
                    )
                },
                shape = MaterialTheme.shapes.large,
            )
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { submit() },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    stringResource(
                        if (setup) R.string.lock_setup_action else R.string.lock_unlock,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(
                    if (setup) R.string.lock_setup_notice else R.string.lock_no_biometrics,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!setup) {
            TextButton(onClick = { confirmReset = true }) {
                Text(stringResource(R.string.lock_forgot))
            }
        }
    }
}
