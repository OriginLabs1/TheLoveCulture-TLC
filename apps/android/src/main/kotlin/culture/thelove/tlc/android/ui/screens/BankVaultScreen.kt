package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.domain.Bank
import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.BankAccountRef
import culture.thelove.tlc.domain.BankDirectory
import culture.thelove.tlc.domain.Nuban

/**
 * The encrypted vault. Numbers are masked until someone deliberately asks to see one, the reveal
 * is written to the record, and the screen hides it again on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BankVaultScreen(
    refs: List<BankAccountRef>,
    revealed: BankAccount?,
    busy: Boolean,
    onAdd: (bankCode: String, accountNumber: String, holderName: String) -> Unit,
    onRemove: (String) -> Unit,
    onReveal: (String) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
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
                eyebrow = stringResource(R.string.vault_eyebrow),
                title = stringResource(R.string.vault_title),
                body = stringResource(R.string.vault_body),
            )
        }

        item(key = "lock") {
            SectionCard(
                title = stringResource(R.string.vault_lock_title),
                container = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.vault_lock_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        if (revealed != null) {
            item(key = "revealed") {
                SectionCard(
                    title = stringResource(R.string.vault_revealed_title),
                    container = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = revealed.bank.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = revealed.accountNumber,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = revealed.holderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.vault_reveal_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(revealed.accountNumber))
                            },
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.action_copy))
                        }
                        TextButton(onClick = onHide) {
                            Text(stringResource(R.string.vault_hide))
                        }
                    }
                }
            }
        }

        item(key = "add") {
            Button(
                onClick = { adding = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.vault_add))
            }
        }

        if (refs.isEmpty()) {
            item(key = "empty") {
                SectionCard(title = stringResource(R.string.vault_empty_title)) {
                    Text(
                        text = stringResource(R.string.vault_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(refs, key = { it.id }) { ref ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = ref.bankName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = ref.masked,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = ref.holderName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Pill(text = stringResource(R.string.vault_masked))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onReveal(ref.id) }, shape = MaterialTheme.shapes.large) {
                            Text(stringResource(R.string.vault_reveal))
                        }
                        TextButton(onClick = { onRemove(ref.id) }) {
                            Text(stringResource(R.string.vault_remove))
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddAccountSheet(
            busy = busy,
            onDismiss = { adding = false },
            onAdd = { bankCode, number, holder ->
                onAdd(bankCode, number, holder)
                adding = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
) {
    var bank by remember { mutableStateOf<Bank?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var number by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf("") }
    val valid = bank != null && Nuban.isValid(number, bank!!.code) && holder.isNotBlank()

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
            Eyebrow(stringResource(R.string.vault_add_eyebrow))
            Text(text = stringResource(R.string.vault_add_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.vault_add_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            androidx.compose.foundation.layout.Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(bank?.name ?: stringResource(R.string.vault_choose_bank))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    BankDirectory.NIGERIA.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text("${candidate.name} · ${candidate.code}") },
                            onClick = {
                                bank = candidate
                                expanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = number,
                onValueChange = { value -> number = value.filter { it.isDigit() }.take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vault_account_number)) },
                singleLine = true,
                isError = number.isNotEmpty() && (bank == null || !Nuban.isValid(number, bank!!.code)),
                supportingText = {
                    Text(
                        when {
                            number.isEmpty() -> stringResource(R.string.vault_account_hint)
                            bank == null -> stringResource(R.string.vault_choose_bank)
                            Nuban.isValid(number, bank!!.code) -> stringResource(R.string.vault_account_ok)
                            else -> stringResource(R.string.vault_account_invalid)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.large,
            )

            OutlinedTextField(
                value = holder,
                onValueChange = { if (it.length <= 120) holder = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vault_holder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            Button(
                onClick = { if (bank != null) onAdd(bank!!.code, number, holder.trim()) },
                enabled = valid && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.vault_save))
            }
        }
    }
}
