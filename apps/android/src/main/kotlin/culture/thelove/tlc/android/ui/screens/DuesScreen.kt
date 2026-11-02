package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.EmptyState
import culture.thelove.tlc.android.ui.InfoRow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.formatTimestamp
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.experience.DueRecord

private sealed interface DueSheet {
    data object New : DueSheet

    data class Proof(val id: String) : DueSheet
}

private val PIONEER_KINDS = listOf(
    TokenKind.ACTIVATION,
    TokenKind.REACTIVATION,
    TokenKind.COURTESY,
    TokenKind.JOY,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DuesScreen(
    dues: List<DueRecord>,
    hasDyad: Boolean,
    busy: Boolean,
    blockedReason: String?,
    onRecordDue: (kind: TokenKind, amountKobo: Long, period: FeePeriod) -> Unit,
    onIssue: (id: String) -> Unit,
    onProof: (id: String, amountKobo: Long, reference: String) -> Unit,
    onOpenCircle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<DueSheet?>(null) }
    var kindName by rememberSaveable { mutableStateOf(TokenKind.ACTIVATION.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var periodName by rememberSaveable { mutableStateOf(FeePeriod.ONE_OFF.name) }
    var proofAmount by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var inlineError by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    eyebrow = stringResource(R.string.dues_eyebrow),
                    title = stringResource(R.string.dues_title),
                    body = stringResource(R.string.dues_lead),
                )
            }

            if (blockedReason != null) {
                item(key = "blocked") {
                    SectionCard(
                        title = stringResource(R.string.blocked_title),
                        container = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = blockedReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (!hasDyad) {
                item(key = "no-dyad") {
                    SectionCard(
                        title = stringResource(R.string.circle_co),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.dues_no_co_steward),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        OutlinedButton(
                            onClick = onOpenCircle,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.circle_add_co))
                        }
                    }
                }
            }

            item(key = "new") {
                Button(
                    onClick = {
                        kindName = TokenKind.ACTIVATION.name
                        amount = ""
                        periodName = FeePeriod.ONE_OFF.name
                        inlineError = null
                        sheet = DueSheet.New
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = hasDyad && !busy && blockedReason == null,
                ) {
                    Text(stringResource(R.string.dues_new))
                }
            }

            if (dues.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.dues_empty_title),
                        body = stringResource(R.string.dues_empty_body),
                    )
                }
            } else {
                items(dues, key = { it.id }) { due ->
                    DueCard(
                        due = due,
                        busy = busy,
                        onIssue = { onIssue(due.id) },
                        onProof = {
                            inlineError = null
                            proofAmount = (due.amountKobo / 100).toString()
                            reference = ""
                            sheet = DueSheet.Proof(due.id)
                        },
                        onCopy = {
                            due.bankReference?.let {
                                clipboard.setText(AnnotatedString(it))
                            }
                        },
                    )
                }
            }

            item(key = "honesty") {
                SectionCard(
                    title = stringResource(R.string.today_notice_title),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.dues_no_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.today_notice_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }

    val current = sheet
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (current) {
                    DueSheet.New -> {
                        Text(
                            text = stringResource(R.string.dues_new_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.dues_kind),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PIONEER_KINDS.forEach { kind ->
                                FilterChip(
                                    selected = kindName == kind.name,
                                    onClick = { kindName = kind.name },
                                    label = { Text(kindLabel(kind)) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 12) amount = input
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dues_amount)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = periodName == FeePeriod.ONE_OFF.name,
                                onClick = { periodName = FeePeriod.ONE_OFF.name },
                                label = { Text(stringResource(R.string.dues_period_one_off)) },
                            )
                            FilterChip(
                                selected = periodName == FeePeriod.MONTHLY.name,
                                onClick = { periodName = FeePeriod.MONTHLY.name },
                                label = { Text(stringResource(R.string.dues_period_monthly)) },
                            )
                        }
                        inlineError?.let { error -> DueError(error) }
                        Button(
                            onClick = {
                                val kobo = amount.toLongOrNull()?.let { it * 100 }
                                if (kobo == null || kobo <= 0) {
                                    inlineError = "Enter a whole naira amount."
                                } else {
                                    onRecordDue(
                                        TokenKind.valueOf(kindName),
                                        kobo,
                                        FeePeriod.valueOf(periodName),
                                    )
                                    sheet = null
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.dues_new))
                        }
                    }

                    is DueSheet.Proof -> {
                        Text(
                            text = stringResource(R.string.dues_proof_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.dues_awaiting),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = proofAmount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 12) proofAmount = input
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dues_proof_amount)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { if (it.length <= 120) reference = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dues_proof_reference)) },
                            placeholder = { Text(stringResource(R.string.dues_proof_reference_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        inlineError?.let { error -> DueError(error) }
                        Button(
                            onClick = {
                                val kobo = proofAmount.toLongOrNull()?.let { it * 100 }
                                when {
                                    kobo == null || kobo <= 0 -> inlineError = "Enter the amount you sent."
                                    reference.isBlank() -> inlineError = "Record the bank reference."
                                    else -> {
                                        onProof(current.id, kobo, reference)
                                        sheet = null
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.dues_record_proof))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DueError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun DueCard(
    due: DueRecord,
    busy: Boolean,
    onIssue: () -> Unit,
    onProof: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = kindLabel(due.kind), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (due.period == FeePeriod.MONTHLY) {
                            stringResource(R.string.dues_period_monthly)
                        } else {
                            stringResource(R.string.dues_period_one_off)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatePill(due.state)
            }
            Text(
                text = Money(due.amountKobo).render(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatTimestamp(due.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            due.bankReference?.let {
                InfoRow(label = stringResource(R.string.dues_reference), value = it)
                TextButton(onClick = onCopy) {
                    Text(stringResource(R.string.dues_copy_reference))
                }
            }
            due.statementRef?.let {
                InfoRow(label = stringResource(R.string.dues_state_attested), value = it, mono = false)
            }

            when (due.state) {
                ObligationState.DUE -> {
                    OutlinedButton(
                        onClick = onIssue,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.dues_issue))
                    }
                }
                ObligationState.INSTRUCTION_ISSUED -> {
                    Button(
                        onClick = onProof,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.dues_record_proof))
                    }
                }
                ObligationState.PROOF_SUBMITTED -> {
                    Text(
                        text = stringResource(R.string.dues_awaiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ObligationState.ATTESTED -> {
                    Text(
                        text = stringResource(R.string.dues_attested),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                ObligationState.EXCEPTION -> {
                    Text(
                        text = stringResource(R.string.dues_exception_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                ObligationState.WAIVED -> {
                    Text(
                        text = stringResource(R.string.dues_waived),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ObligationState.EXPIRED -> {
                    Text(
                        text = stringResource(R.string.dues_expired),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatePill(state: ObligationState) {
    val label = when (state) {
        ObligationState.DUE -> stringResource(R.string.dues_state_due)
        ObligationState.INSTRUCTION_ISSUED -> stringResource(R.string.dues_state_instruction)
        ObligationState.PROOF_SUBMITTED -> stringResource(R.string.dues_state_proof)
        ObligationState.ATTESTED -> stringResource(R.string.dues_state_attested)
        ObligationState.EXCEPTION -> stringResource(R.string.dues_state_exception)
        ObligationState.WAIVED -> stringResource(R.string.dues_state_waived)
        ObligationState.EXPIRED -> stringResource(R.string.dues_state_expired)
    }
    val container = when (state) {
        ObligationState.ATTESTED -> MaterialTheme.colorScheme.secondaryContainer
        ObligationState.EXCEPTION -> MaterialTheme.colorScheme.errorContainer
        ObligationState.WAIVED -> MaterialTheme.colorScheme.surfaceVariant
        ObligationState.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when (state) {
        ObligationState.ATTESTED -> MaterialTheme.colorScheme.onSecondaryContainer
        ObligationState.EXCEPTION -> MaterialTheme.colorScheme.onErrorContainer
        ObligationState.WAIVED -> MaterialTheme.colorScheme.onSurfaceVariant
        ObligationState.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Pill(text = label, container = container, content = content)
}

@Composable
private fun kindLabel(kind: TokenKind): String = when (kind) {
    TokenKind.ACTIVATION -> stringResource(R.string.dues_activation)
    TokenKind.REACTIVATION -> stringResource(R.string.dues_reactivation)
    TokenKind.COURTESY -> stringResource(R.string.dues_courtesy)
    TokenKind.JOY -> stringResource(R.string.dues_joy)
    else -> kind.name
}
