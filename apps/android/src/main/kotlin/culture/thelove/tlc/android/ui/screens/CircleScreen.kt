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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.formatTimestamp
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.Proposal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CircleScreen(
    snapshot: AppSnapshot,
    busy: Boolean,
    blockedReason: String?,
    onAddCoSteward: (name: String) -> Unit,
    onPropose: (action: SensitiveAction, summary: String?) -> Unit,
    onApprove: (proposalId: String, actorId: String) -> Unit,
    onVeto: (proposalId: String, actorId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }
    var coName by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var actionName by rememberSaveable { mutableStateOf(SensitiveAction.CHANGE_BANK_DETAILS.name) }
    var inlineError by rememberSaveable { mutableStateOf<String?>(null) }
    val primary = snapshot.primary
    val co = snapshot.coSteward

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
                    eyebrow = stringResource(R.string.circle_eyebrow),
                    title = stringResource(R.string.circle_title),
                    body = stringResource(R.string.circle_lead),
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

            item(key = "primary") {
                SeatCard(
                    role = stringResource(R.string.circle_primary),
                    name = primary.name,
                    body = stringResource(R.string.circle_primary_body),
                    seated = true,
                )
            }

            item(key = "co") {
                SeatCard(
                    role = stringResource(R.string.circle_co),
                    name = co?.name ?: stringResource(R.string.circle_vacant),
                    body = if (co != null) {
                        stringResource(R.string.circle_co_body)
                    } else {
                        stringResource(R.string.circle_identity_note)
                    },
                    seated = co != null,
                )
                if (co == null) {
                    OutlinedButton(
                        onClick = {
                            coName = ""
                            inlineError = null
                            adding = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.circle_add_co))
                    }
                }
            }

            item(key = "two-key") {
                SectionCard(
                    title = stringResource(R.string.circle_two_key),
                    container = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.circle_two_key_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(
                        onClick = {
                            actionName = SensitiveAction.CHANGE_BANK_DETAILS.name
                            summary = ""
                            inlineError = null
                            opening = true
                        },
                        enabled = snapshot.hasDyad && !busy && blockedReason == null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.circle_open_decision))
                    }
                }
            }

            item(key = "decisions-label") {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow(stringResource(R.string.circle_decisions))
                    Text(
                        text = if (snapshot.proposals.isEmpty()) {
                            stringResource(R.string.circle_no_decisions)
                        } else {
                            stringResource(R.string.circle_waiting)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(snapshot.proposals, key = { it.id }) { proposal ->
                ProposalCard(
                    proposal = proposal,
                    primary = primary.id,
                    primaryName = primary.name,
                    co = co?.id,
                    coName = co?.name,
                    busy = busy,
                    onApprove = onApprove,
                    onVeto = onVeto,
                )
            }

            item(key = "identity") {
                SectionCard(
                    title = stringResource(R.string.module_circle),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.circle_identity_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.circle_add_co_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                OutlinedTextField(
                    value = coName,
                    onValueChange = { if (it.length <= 80) coName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.circle_add_co)) },
                    placeholder = { Text(stringResource(R.string.circle_add_co_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )
                Text(
                    text = stringResource(R.string.circle_add_co_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                inlineError?.let { CircleError(it) }
                Button(
                    onClick = {
                        if (coName.isBlank()) {
                            inlineError = "Name the Co-Steward."
                        } else {
                            onAddCoSteward(coName)
                            adding = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.circle_add_co))
                }
                TextButton(onClick = { adding = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    if (opening) {
        ModalBottomSheet(
            onDismissRequest = { opening = false },
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
                Text(
                    text = stringResource(R.string.circle_decision_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                SensitiveAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { actionName = action.name }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = actionName == action.name,
                            onClick = { actionName = action.name },
                            label = { Text(actionLabel(action)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { if (it.length <= Proposal.MAX_SUMMARY) summary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.circle_decision_summary)) },
                    placeholder = { Text(stringResource(R.string.circle_decision_summary_hint)) },
                    minLines = 2,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Button(
                    onClick = {
                        onPropose(
                            SensitiveAction.valueOf(actionName),
                            summary.ifBlank { null },
                        )
                        opening = false
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.circle_open_decision))
                }
            }
        }
    }
}

@Composable
private fun CircleError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun SeatCard(role: String, name: String, body: String, seated: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (seated) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (seated) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (seated) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (seated) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Eyebrow(role)
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: Proposal,
    primary: String,
    primaryName: String,
    co: String?,
    coName: String?,
    busy: Boolean,
    onApprove: (String, String) -> Unit,
    onVeto: (String, String) -> Unit,
) {
    val vetoed = proposal.vetoes.isNotEmpty()
    val approved = proposal.closed && !vetoed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = actionLabel(proposal.action),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                when {
                    approved -> Pill(
                        text = stringResource(R.string.circle_approved),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    vetoed -> Pill(
                        text = stringResource(R.string.circle_vetoed),
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    else -> Pill(text = stringResource(R.string.circle_waiting))
                }
            }
            Text(text = proposal.summary, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(
                        if (proposal.approvals.size >= 2) {
                            R.string.circle_signature_two
                        } else {
                            R.string.circle_signature_one
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatTimestamp(proposal.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!proposal.closed) {
                Button(
                    onClick = { onApprove(proposal.id, primary) },
                    enabled = !busy && primary !in proposal.approvals,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.circle_sign_primary, primaryName))
                }
                if (co != null) {
                    OutlinedButton(
                        onClick = { onApprove(proposal.id, co) },
                        enabled = !busy && co !in proposal.approvals,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.circle_sign_co, coName.orEmpty()))
                    }
                }
                TextButton(
                    onClick = { onVeto(proposal.id, primary) },
                    enabled = !busy && primary !in proposal.vetoes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.circle_veto))
                }
            }
        }
    }
}

@Composable
private fun actionLabel(action: SensitiveAction): String = when (action) {
    SensitiveAction.CHANGE_BANK_DETAILS -> stringResource(R.string.action_change_bank)
    SensitiveAction.DISSOLVE_DYAD -> stringResource(R.string.action_dissolve)
    SensitiveAction.GRANT_DELEGATION -> stringResource(R.string.action_grant_delegation)
    SensitiveAction.REVOKE_DELEGATION -> stringResource(R.string.action_revoke_delegation)
    SensitiveAction.EXPORT_SHARED_RECORD -> stringResource(R.string.action_export_shared)
}
