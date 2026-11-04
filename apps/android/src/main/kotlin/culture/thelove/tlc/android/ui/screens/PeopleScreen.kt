package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.TlcMark
import culture.thelove.tlc.domain.BankAccountRef
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.experience.Person
import culture.thelove.tlc.domain.Persona
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.experience.AppSnapshot

/**
 * People: the addresses this circle can reach. `@pioneer` is a role and `@sircollins` is the
 * person behind it; both are reserved, both are shown honestly, and neither can be claimed by
 * someone who is not them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun PeopleScreen(
    snapshot: AppSnapshot,
    busy: Boolean,
    onAddPerson: (Handle, String, Role, String?) -> Unit,
    onAttachBank: (String, String?) -> Unit,
    onRemovePerson: (String) -> Unit,
    onSupport: (Handle) -> Unit,
    onContact: () -> Unit,
    onBankVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var attachFor by remember { mutableStateOf<Person?>(null) }

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
                eyebrow = stringResource(R.string.people_eyebrow),
                title = stringResource(R.string.people_title),
                body = stringResource(R.string.people_body),
            )
        }

        item(key = "pioneer") {
            SectionCard(
                title = stringResource(R.string.people_pioneer_title),
                container = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TlcMark(modifier = Modifier.size(44.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = Persona.PIONEER.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "@pioneer · @sircollins",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.people_pioneer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onContact, shape = MaterialTheme.shapes.large) {
                        Text(stringResource(R.string.people_open_desk))
                    }
                    OutlinedButton(onClick = { onSupport(Handle.of("pioneer")) }, shape = MaterialTheme.shapes.large) {
                        Text(stringResource(R.string.people_support))
                    }
                }
            }
        }

        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { adding = true },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(stringResource(R.string.people_add))
                }
                OutlinedButton(onClick = onBankVault, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.people_vault))
                }
            }
        }

        if (snapshot.people.isEmpty()) {
            item(key = "empty") {
                SectionCard(title = stringResource(R.string.people_empty_title)) {
                    Text(
                        text = stringResource(R.string.people_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(snapshot.people, key = { it.id }) { person ->
            PersonCard(
                person = person,
                bank = snapshot.bankRef(person.bankRefId),
                onSupport = { onSupport(person.handle) },
                onAttach = { attachFor = person },
                onRemove = { onRemovePerson(person.id) },
            )
        }

        item(key = "privacy") {
            SectionCard(
                title = stringResource(R.string.people_privacy_title),
                container = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.people_privacy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (adding) {
        AddPersonSheet(
            bankAccounts = snapshot.bankAccounts,
            busy = busy,
            onDismiss = { adding = false },
            onAdd = { handle, name, role, bankRefId ->
                onAddPerson(handle, name, role, bankRefId)
                adding = false
            },
        )
    }

    val person = attachFor
    if (person != null) {
        ModalBottomSheet(
            onDismissRequest = { attachFor = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Eyebrow(stringResource(R.string.people_attach_eyebrow))
                Text(
                    text = stringResource(R.string.people_attach_title, person.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.people_attach_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (snapshot.bankAccounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vault_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = { attachFor = null; onBankVault() }) {
                        Text(stringResource(R.string.people_vault))
                    }
                }
                snapshot.bankAccounts.forEach { ref ->
                    BankChoice(
                        ref = ref,
                        selected = person.bankRefId == ref.id,
                        onClick = {
                            onAttachBank(person.id, ref.id)
                            attachFor = null
                        },
                    )
                }
                if (person.bankRefId != null) {
                    TextButton(onClick = { onAttachBank(person.id, null); attachFor = null }) {
                        Text(stringResource(R.string.people_attach_none))
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    person: Person,
    bank: BankAccountRef?,
    onSupport: () -> Unit,
    onAttach: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = person.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = person.handle.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(text = person.badge)
            }
            Text(
                text = bank?.label ?: stringResource(R.string.people_no_bank),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSupport, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.people_support))
                }
                TextButton(onClick = onAttach) {
                    Text(stringResource(R.string.people_attach))
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.people_remove))
                }
            }
        }
    }
}

@Composable
private fun BankChoice(ref: BankAccountRef, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = ref.bankName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${ref.masked} · ${ref.holderName}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonSheet(
    bankAccounts: List<BankAccountRef>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Handle, String, Role, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var handleText by rememberSaveable { mutableStateOf("") }
    var edited by rememberSaveable { mutableStateOf(false) }
    var roleName by rememberSaveable { mutableStateOf(Role.SINGLE.name) }
    var bankRefId by rememberSaveable { mutableStateOf<String?>(null) }
    val role = Role.valueOf(roleName)
    val handle = Handle.parse(handleText)
    val reserved = handle != null && Handle.isReserved(handle.value)
    val valid = name.isNotBlank() && handle != null && !reserved

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Eyebrow(stringResource(R.string.people_add_eyebrow))
            Text(text = stringResource(R.string.people_add_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    if (value.length <= 80) {
                        name = value
                        if (!edited) handleText = Handle.suggest(value.ifBlank { "member" }, "000000").value
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.people_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = handleText,
                onValueChange = { value ->
                    if (value.length <= 24) {
                        handleText = value.lowercase().removePrefix("@")
                        edited = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.people_handle)) },
                prefix = { Text("@") },
                singleLine = true,
                isError = handle == null || reserved,
                supportingText = {
                    Text(
                        when {
                            handleText.isBlank() -> stringResource(R.string.people_handle_hint)
                            reserved -> stringResource(R.string.people_handle_reserved)
                            handle == null -> stringResource(R.string.people_handle_invalid)
                            else -> stringResource(R.string.people_handle_ok, handle.value)
                        },
                    )
                },
                shape = MaterialTheme.shapes.large,
            )
            Text(text = stringResource(R.string.people_role), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Role.entries.forEach { candidate ->
                    FilterChip(
                        selected = role == candidate,
                        onClick = { roleName = candidate.name },
                        label = { Text(roleLabel(candidate)) },
                    )
                }
            }
            if (bankAccounts.isNotEmpty()) {
                Text(text = stringResource(R.string.people_attach_hint), style = MaterialTheme.typography.titleSmall)
                bankAccounts.forEach { ref ->
                    BankChoice(ref = ref, selected = bankRefId == ref.id, onClick = { bankRefId = ref.id })
                }
            }
            Button(
                onClick = { if (handle != null) onAdd(handle, name.trim(), role, bankRefId) },
                enabled = valid && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.people_add))
            }
        }
    }
}

private fun roleLabel(role: Role): String = when (role) {
    Role.SINGLE -> "Single"
    Role.I_PARTNER -> "I-Partner"
    Role.LOVE_MODEL -> "Love Model"
}
