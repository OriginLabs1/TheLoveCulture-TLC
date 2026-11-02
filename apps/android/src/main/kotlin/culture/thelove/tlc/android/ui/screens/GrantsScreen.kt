package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.EmptyState
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.InfoRow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.formatTimestamp
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.experience.GrantEvent
import culture.thelove.tlc.experience.GrantStage
import culture.thelove.tlc.experience.GrowthGrant

private sealed interface GrantSheet {
    data object New : GrantSheet

    data class Attach(val id: String) : GrantSheet

    data class Verify(val id: String) : GrantSheet

    data class Pay(val id: String) : GrantSheet

    data class Withdraw(val id: String) : GrantSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun GrantsScreen(
    grants: List<GrowthGrant>,
    busy: Boolean,
    blockedReason: String?,
    onCreate: (beneficiary: String, purpose: String, amountKobo: Long?) -> Unit,
    onAdvance: (id: String, event: GrantEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<GrantSheet?>(null) }
    var beneficiary by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var inlineError by rememberSaveable { mutableStateOf<String?>(null) }

    fun open(next: GrantSheet) {
        inlineError = null
        note = ""
        sheet = next
    }

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
                    eyebrow = stringResource(R.string.grants_eyebrow),
                    title = stringResource(R.string.grants_title),
                    body = stringResource(R.string.grants_lead),
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

            item(key = "new") {
                Button(
                    onClick = {
                        beneficiary = ""
                        purpose = ""
                        amount = ""
                        open(GrantSheet.New)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = !busy && blockedReason == null,
                ) {
                    Text(stringResource(R.string.grants_new))
                }
            }

            if (grants.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.grants_empty_title),
                        body = stringResource(R.string.grants_empty_body),
                    )
                }
            } else {
                items(grants, key = { it.id }) { grant ->
                    GrantCard(
                        grant = grant,
                        onAttach = { open(GrantSheet.Attach(grant.id)) },
                        onVerify = { open(GrantSheet.Verify(grant.id)) },
                        onPay = { open(GrantSheet.Pay(grant.id)) },
                        onWithdraw = { open(GrantSheet.Withdraw(grant.id)) },
                    )
                }
            }

            item(key = "note") {
                SectionCard(
                    title = stringResource(R.string.module_grants),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.grants_direct_note),
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
                    GrantSheet.New -> {
                        Text(
                            text = stringResource(R.string.grants_new_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        OutlinedTextField(
                            value = beneficiary,
                            onValueChange = { if (it.length <= GrowthGrant.MAX_BENEFICIARY) beneficiary = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.grants_beneficiary)) },
                            placeholder = { Text(stringResource(R.string.grants_beneficiary_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        OutlinedTextField(
                            value = purpose,
                            onValueChange = { if (it.length <= GrowthGrant.MAX_PURPOSE) purpose = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.grants_purpose)) },
                            placeholder = { Text(stringResource(R.string.grants_purpose_hint)) },
                            minLines = 2,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 12) amount = input
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.grants_amount)) },
                            placeholder = { Text(stringResource(R.string.grants_amount_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        inlineError?.let { SheetError(it) }
                        Button(
                            onClick = {
                                val kobo = amount.toLongOrNull()?.let { it * 100 }
                                when {
                                    beneficiary.isBlank() -> inlineError = "Name the beneficiary."
                                    purpose.isBlank() -> inlineError = "Describe the milestone."
                                    amount.isNotBlank() && (kobo == null || kobo <= 0) ->
                                        inlineError = "Enter a whole naira amount, or leave it blank."
                                    else -> {
                                        onCreate(beneficiary, purpose, kobo)
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
                            Text(stringResource(R.string.grants_new))
                        }
                    }

                    is GrantSheet.Attach -> SheetForm(
                        title = stringResource(R.string.grants_attach_title),
                        value = note,
                        onValue = { if (it.length <= GrowthGrant.MAX_EVIDENCE) note = it },
                        label = stringResource(R.string.grants_attach),
                        placeholder = stringResource(R.string.grants_attach_hint),
                        error = inlineError,
                        confirm = stringResource(R.string.grants_attach),
                        enabled = !busy,
                        onConfirm = {
                            if (note.isBlank()) {
                                inlineError = "Describe the evidence you have."
                            } else {
                                onAdvance(current.id, GrantEvent.AttachEvidence(note, System.currentTimeMillis()))
                                sheet = null
                            }
                        },
                    )

                    is GrantSheet.Verify -> SheetForm(
                        title = stringResource(R.string.grants_verify_title),
                        value = note,
                        onValue = { if (it.length <= GrowthGrant.MAX_BENEFICIARY) note = it },
                        label = stringResource(R.string.grants_verify),
                        placeholder = stringResource(R.string.grants_verify_hint),
                        error = inlineError,
                        confirm = stringResource(R.string.grants_verify),
                        enabled = !busy,
                        onConfirm = {
                            if (note.isBlank()) {
                                inlineError = "Name who verified the milestone."
                            } else {
                                onAdvance(current.id, GrantEvent.Verify(note, System.currentTimeMillis()))
                                sheet = null
                            }
                        },
                    )

                    is GrantSheet.Pay -> SheetForm(
                        title = stringResource(R.string.grants_pay_title),
                        value = note,
                        onValue = { if (it.length <= GrowthGrant.MAX_REFERENCE) note = it },
                        label = stringResource(R.string.grants_reference),
                        placeholder = stringResource(R.string.grants_pay_hint),
                        error = inlineError,
                        confirm = stringResource(R.string.grants_pay),
                        enabled = !busy,
                        onConfirm = {
                            if (note.isBlank()) {
                                inlineError = "Record the bank reference you have."
                            } else {
                                onAdvance(current.id, GrantEvent.RecordPayment(note, System.currentTimeMillis()))
                                sheet = null
                            }
                        },
                    )

                    is GrantSheet.Withdraw -> SheetForm(
                        title = stringResource(R.string.grants_withdraw_title),
                        value = note,
                        onValue = { if (it.length <= GrowthGrant.MAX_PURPOSE) note = it },
                        label = stringResource(R.string.grants_withdraw_title),
                        placeholder = stringResource(R.string.grants_withdraw_hint),
                        error = inlineError,
                        confirm = stringResource(R.string.grants_withdraw),
                        enabled = !busy,
                        onConfirm = {
                            if (note.isBlank()) {
                                inlineError = "Say why the pledge is withdrawn."
                            } else {
                                onAdvance(current.id, GrantEvent.Withdraw(note, System.currentTimeMillis()))
                                sheet = null
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetForm(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    confirm: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = 2,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    error?.let { SheetError(it) }
    Button(
        onClick = onConfirm,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(confirm)
    }
}

@Composable
private fun SheetError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun GrantCard(
    grant: GrowthGrant,
    onAttach: () -> Unit,
    onVerify: () -> Unit,
    onPay: () -> Unit,
    onWithdraw: () -> Unit,
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
                    Eyebrow(stringResource(R.string.grants_title))
                    Text(text = grant.beneficiary, style = MaterialTheme.typography.titleMedium)
                }
                StagePill(grant.stage)
            }
            Text(
                text = grant.purpose,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = grant.amountKobo?.let { Money(it).render() }
                        ?: stringResource(R.string.grants_no_amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = relative(grant.pledgedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StageTrack(grant.stage)

            grant.evidence?.let { InfoRow(label = stringResource(R.string.grants_attach), value = it, mono = false) }
            grant.witness?.let { InfoRow(label = stringResource(R.string.grants_verify), value = it, mono = false) }
            grant.bankReference?.let {
                InfoRow(label = stringResource(R.string.grants_reference), value = it)
            }
            grant.withdrawnReason?.let {
                InfoRow(label = stringResource(R.string.grants_withdraw), value = it, mono = false)
            }
            grant.paidAt?.let {
                InfoRow(label = stringResource(R.string.grants_stage_paid), value = formatTimestamp(it), mono = false)
            }

            when (grant.stage) {
                GrantStage.PLEDGED -> {
                    Button(
                        onClick = onAttach,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.grants_attach))
                    }
                }
                GrantStage.EVIDENCE_RECORDED -> {
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.grants_verify))
                    }
                }
                GrantStage.VERIFIED -> {
                    Button(
                        onClick = onPay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.grants_pay))
                    }
                }
                GrantStage.PAID, GrantStage.WITHDRAWN -> Unit
            }

            if (grant.stage != GrantStage.PAID && grant.stage != GrantStage.WITHDRAWN) {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.grants_withdraw))
                }
            }
        }
    }
}

@Composable
private fun StagePill(stage: GrantStage) {
    val label = when (stage) {
        GrantStage.PLEDGED -> stringResource(R.string.grants_stage_pledged)
        GrantStage.EVIDENCE_RECORDED -> stringResource(R.string.grants_stage_evidence)
        GrantStage.VERIFIED -> stringResource(R.string.grants_stage_verified)
        GrantStage.PAID -> stringResource(R.string.grants_stage_paid)
        GrantStage.WITHDRAWN -> stringResource(R.string.grants_stage_withdrawn)
    }
    val container = when (stage) {
        GrantStage.PAID -> MaterialTheme.colorScheme.secondaryContainer
        GrantStage.WITHDRAWN -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when (stage) {
        GrantStage.PAID -> MaterialTheme.colorScheme.onSecondaryContainer
        GrantStage.WITHDRAWN -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Pill(text = label, container = container, content = content)
}

@Composable
private fun StageTrack(stage: GrantStage) {
    if (stage == GrantStage.WITHDRAWN) return
    val order = listOf(
        GrantStage.PLEDGED,
        GrantStage.EVIDENCE_RECORDED,
        GrantStage.VERIFIED,
        GrantStage.PAID,
    )
    val reached = order.indexOf(stage)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        order.forEachIndexed { index, item ->
            val active = index <= reached
            Box(
                modifier = Modifier
                    .size(if (index == reached) 12.dp else 8.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
            if (index != order.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (index < reached) {
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

private fun relative(epochMillis: Long): String = formatTimestamp(epochMillis)
