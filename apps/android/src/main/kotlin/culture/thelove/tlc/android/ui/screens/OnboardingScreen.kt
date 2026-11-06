package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.IdentityError
import culture.thelove.tlc.android.data.IdentityStore
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.GlassPanel
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.Role

/**
 * First launch. Four honest screens: what this is, what a role means, the name the device will
 * greet you with, and the words that open the door. Nothing is created until the final tap, and
 * nothing leaves the device. There is no biometric shortcut: the passphrase is the key.
 */
@Composable
public fun OnboardingScreen(
    submitting: Boolean,
    error: String?,
    onFinish: (
        displayName: String,
        circleName: String,
        role: Role,
        handle: Handle?,
        passphrase: String,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var circle by rememberSaveable { mutableStateOf("") }
    var handleText by rememberSaveable { mutableStateOf("") }
    var handleEdited by rememberSaveable { mutableStateOf(false) }
    var roleName by rememberSaveable { mutableStateOf(Role.SINGLE.name) }
    var consented by rememberSaveable { mutableStateOf(false) }
    var passphrase by rememberSaveable { mutableStateOf("") }
    var passphraseAgain by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    val role = Role.valueOf(roleName)
    val last = step == 3
    val handle = Handle.parse(handleText)
    val handleReserved = handle != null && Handle.isReserved(handle.value)
    val passphraseProblem = IdentityStore.passphraseProblem(passphrase)
    val matches = passphrase.isNotEmpty() && passphrase == passphraseAgain
    val canFinish = name.isNotBlank() && handle != null && !handleReserved && consented &&
        passphraseProblem == null && matches && !submitting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_wordmark),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_doctrine),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.onboard_step, step + 1, 4),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (step == 0) {
                TextButton(onClick = { step = 2 }) {
                    Text(stringResource(R.string.onboard_skip))
                }
            }
        }

        when (step) {
            0 -> WelcomeStep()
            1 -> RoleStep(selected = role, onSelect = { roleName = it.name })
            2 -> ProfileStep(
                name = name,
                circle = circle,
                handleText = handleText,
                handleReserved = handleReserved,
                handleValid = handle != null,
                consented = consented,
                onName = { value ->
                    if (value.length <= 80) {
                        name = value
                        if (!handleEdited) {
                            handleText = Handle.suggest(value.ifBlank { "member" }, "000000").value
                        }
                    }
                },
                onCircle = { if (it.length <= 80) circle = it },
                onHandle = { value ->
                    if (value.length <= 24) {
                        handleText = value.lowercase().removePrefix("@")
                        handleEdited = true
                    }
                },
                onConsent = { consented = it },
            )
            else -> PassphraseStep(
                passphrase = passphrase,
                again = passphraseAgain,
                reveal = reveal,
                problem = passphraseProblem,
                matches = matches,
                onPassphrase = { passphrase = it },
                onAgain = { passphraseAgain = it },
                onReveal = { reveal = it },
            )
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                TextButton(
                    onClick = { step -= 1 },
                    enabled = !submitting,
                ) {
                    Text(stringResource(R.string.action_back))
                }
            }
            Button(
                onClick = {
                    if (last) {
                        onFinish(name.trim(), circle.trim(), role, Handle.parse(handleText), passphrase)
                    } else {
                        step += 1
                    }
                },
                enabled = if (last) canFinish else !submitting,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(
                            when {
                                last -> R.string.onboard_create
                                step == 0 -> R.string.onboard_begin
                                else -> R.string.onboard_next
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Eyebrow(stringResource(R.string.onboard_welcome_eyebrow))
        Text(
            text = stringResource(R.string.onboard_welcome_title),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.onboard_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassPanel {
            PromiseRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.onboard_promise_local),
                body = stringResource(R.string.onboard_promise_local_body),
            )
            PromiseRow(
                icon = Icons.Filled.CheckCircle,
                title = stringResource(R.string.onboard_promise_chain),
                body = stringResource(R.string.onboard_promise_chain_body),
            )
            PromiseRow(
                icon = Icons.Filled.Star,
                title = stringResource(R.string.onboard_promise_non_custody),
                body = stringResource(R.string.onboard_promise_non_custody_body),
            )
        }
    }
}

@Composable
private fun PromiseRow(icon: ImageVector, title: String, body: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(19.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoleStep(selected: Role, onSelect: (Role) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow(stringResource(R.string.onboard_role_eyebrow))
        Text(
            text = stringResource(R.string.onboard_role_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.onboard_role_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoleCard(
            role = Role.SINGLE,
            title = stringResource(R.string.role_single),
            body = stringResource(R.string.role_single_body),
            icon = Icons.Filled.Person,
            selected = selected == Role.SINGLE,
            onSelect = onSelect,
        )
        RoleCard(
            role = Role.I_PARTNER,
            title = stringResource(R.string.role_i_partner),
            body = stringResource(R.string.role_i_partner_body),
            icon = Icons.Filled.Star,
            selected = selected == Role.I_PARTNER,
            onSelect = onSelect,
        )
        RoleCard(
            role = Role.LOVE_MODEL,
            title = stringResource(R.string.role_love_model),
            body = stringResource(R.string.role_love_model_body),
            icon = Icons.Filled.CheckCircle,
            selected = selected == Role.LOVE_MODEL,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun RoleCard(
    role: Role,
    title: String,
    body: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: (Role) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(role) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = { onSelect(role) })
        }
    }
}

@Composable
private fun ProfileStep(
    name: String,
    circle: String,
    handleText: String,
    handleReserved: Boolean,
    handleValid: Boolean,
    consented: Boolean,
    onName: (String) -> Unit,
    onCircle: (String) -> Unit,
    onHandle: (String) -> Unit,
    onConsent: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow(stringResource(R.string.onboard_profile_eyebrow))
        Text(
            text = stringResource(R.string.onboard_profile_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.onboard_profile_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboard_name)) },
            placeholder = { Text(stringResource(R.string.onboard_name_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        OutlinedTextField(
            value = handleText,
            onValueChange = onHandle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboard_handle)) },
            prefix = { Text("@") },
            singleLine = true,
            isError = handleText.isNotBlank() && (!handleValid || handleReserved),
            supportingText = {
                Text(
                    when {
                        handleReserved -> stringResource(R.string.people_handle_reserved)
                        handleText.isBlank() || !handleValid -> stringResource(R.string.people_handle_hint)
                        else -> stringResource(R.string.onboard_handle_ok, handleText)
                    },
                )
            },
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = circle,
            onValueChange = onCircle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboard_circle)) },
            placeholder = { Text(stringResource(R.string.onboard_circle_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onConsent(!consented) }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = consented, onCheckedChange = onConsent)
            Text(
                text = stringResource(R.string.onboard_consent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        SectionNote(
            title = stringResource(R.string.onboard_ready),
            body = stringResource(R.string.settings_privacy_body),
        )
    }
}

@Composable
private fun PassphraseStep(
    passphrase: String,
    again: String,
    reveal: Boolean,
    problem: IdentityError?,
    matches: Boolean,
    onPassphrase: (String) -> Unit,
    onAgain: (String) -> Unit,
    onReveal: (Boolean) -> Unit,
) {
    val strength = IdentityStore.strength(passphrase)
    val words = IdentityStore.wordCount(passphrase)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow(stringResource(R.string.onboard_pass_eyebrow))
        Text(
            text = stringResource(R.string.onboard_pass_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.onboard_pass_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = passphrase,
            onValueChange = onPassphrase,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboard_pass_phrase)) },
            placeholder = { Text(stringResource(R.string.onboard_pass_hint)) },
            singleLine = true,
            visualTransformation = if (reveal) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            trailingIcon = {
                TextButton(onClick = { onReveal(!reveal) }) {
                    Text(stringResource(if (reveal) R.string.lock_hide else R.string.lock_show))
                }
            },
            supportingText = {
                Text(
                    stringResource(
                        if (words >= 4) R.string.onboard_pass_words_ok else R.string.onboard_pass_words,
                        words,
                    ),
                )
            },
            shape = MaterialTheme.shapes.large,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (index < strength) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
            Text(
                text = stringResource(
                    when (strength) {
                        4 -> R.string.onboard_pass_strength_4
                        3 -> R.string.onboard_pass_strength_3
                        2 -> R.string.onboard_pass_strength_2
                        else -> R.string.onboard_pass_strength_1
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = again,
            onValueChange = onAgain,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboard_pass_again)) },
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
            isError = again.isNotEmpty() && !matches,
            supportingText = {
                if (again.isNotEmpty() && !matches) {
                    Text(stringResource(R.string.onboard_pass_mismatch))
                } else if (problem == null && matches) {
                    Text(stringResource(R.string.onboard_pass_ready))
                }
            },
            shape = MaterialTheme.shapes.large,
        )

        SectionNote(
            title = stringResource(R.string.onboard_pass_notice_title),
            body = stringResource(R.string.onboard_pass_notice_body),
        )
    }
}

@Composable
private fun SectionNote(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
