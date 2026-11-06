package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.experience.Person
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.experience.SupportIntent
import culture.thelove.tlc.experience.SupportState
import kotlinx.coroutines.delay

/**
 * Send Support: a gift between two private accounts, recorded as an intention and then as a claim.
 * TLC never holds the money, so the screen never says "paid" — it says offered, sent, acknowledged.
 */
@Composable
public fun SupportScreen(
    people: List<Person>,
    supports: List<SupportIntent>,
    busy: Boolean,
    initialHandle: Handle?,
    onSend: (Handle, TokenKind, Long?) -> Unit,
    onClaim: (String, String) -> Unit,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var handleText by rememberSaveable { mutableStateOf(initialHandle?.value.orEmpty()) }
    var kindName by rememberSaveable { mutableStateOf(TokenKind.SUPPORTIVE.name) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var claimFor by rememberSaveable { mutableStateOf<String?>(null) }
    var claimReference by rememberSaveable { mutableStateOf("") }
    var methodSheet by rememberSaveable { mutableStateOf(false) }
    var methodId by rememberSaveable { mutableStateOf<String?>(null) }
    var deadline by rememberSaveable { mutableStateOf(0L) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    val kind = TokenKind.valueOf(kindName)
    val handle = Handle.parse(handleText)
    val amountKobo = amountText.filter { it.isDigit() }.toLongOrNull()?.times(100)
    val methods = transferMethods()
    val method = methods.firstOrNull { it.id == methodId }
    val needsRoute = kind != TokenKind.RECOGNITION
    val routeReady = !needsRoute || (method != null && secondsLeft > 0)
    val valid = handle != null &&
        (kind == TokenKind.RECOGNITION || (amountKobo != null && amountKobo > 0)) &&
        routeReady

    LaunchedEffect(methodId, deadline) {
        while (methodId != null) {
            val left = ((deadline - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
            secondsLeft = left
            if (left == 0) break
            delay(1000)
        }
    }

    if (methodSheet) {
        TransferMethodSheet(
            methods = methods,
            selectedId = methodId,
            onChoose = { chosen ->
                methodId = chosen.id
                deadline = System.currentTimeMillis() + ROUTE_WINDOW_MS
                secondsLeft = (ROUTE_WINDOW_MS / 1000L).toInt()
                methodSheet = false
            },
            onDismiss = { methodSheet = false },
        )
    }

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
                eyebrow = stringResource(R.string.support_eyebrow),
                title = stringResource(R.string.support_title),
                body = stringResource(R.string.support_body),
            )
        }

        item(key = "form") {
            SectionCard(title = stringResource(R.string.support_new)) {
                if (people.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.support_people),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        people.forEach { person ->
                            FilterChip(
                                selected = handleText == person.handle.value,
                                onClick = { handleText = person.handle.value },
                                label = { Text(person.handle.toString()) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = handleText,
                    onValueChange = { if (it.length <= 24) handleText = it.lowercase().removePrefix("@") },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.support_to)) },
                    prefix = { Text("@") },
                    singleLine = true,
                    isError = handleText.isNotEmpty() && handle == null,
                    supportingText = {
                        Text(
                            if (handle == null && handleText.isNotEmpty()) {
                                stringResource(R.string.people_handle_invalid)
                            } else {
                                stringResource(R.string.support_to_hint)
                            },
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                )

                Text(text = stringResource(R.string.support_kind), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == TokenKind.SUPPORTIVE,
                        onClick = { kindName = TokenKind.SUPPORTIVE.name },
                        label = { Text(stringResource(R.string.support_kind_supportive)) },
                        leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    )
                    FilterChip(
                        selected = kind == TokenKind.RECOGNITION,
                        onClick = { kindName = TokenKind.RECOGNITION.name },
                        label = { Text(stringResource(R.string.support_kind_recognition)) },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    )
                }

                if (kind != TokenKind.RECOGNITION) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { value -> amountText = value.filter { it.isDigit() }.take(9) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.support_amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large,
                    )
                }

                if (needsRoute) {
                    if (method != null) {
                        TransferMethodSummary(
                            method = method,
                            secondsLeft = secondsLeft,
                            onOpen = { methodSheet = true },
                        )
                    } else {
                        TransferMethodNone(onOpen = { methodSheet = true })
                    }
                }

                Text(
                    text = stringResource(R.string.support_rail_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        if (handle != null) {
                            onSend(handle, kind, if (kind == TokenKind.RECOGNITION) null else amountKobo)
                            amountText = ""
                        }
                    },
                    enabled = valid && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.support_send))
                }
            }
        }

        item(key = "history-label") {
            Eyebrow(stringResource(R.string.support_history))
        }

        if (supports.isEmpty()) {
            item(key = "empty") {
                SectionCard(title = stringResource(R.string.support_empty_title)) {
                    Text(
                        text = stringResource(R.string.support_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(supports, key = { it.id }) { support ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.support_line,
                                    kindLabel(support.kind),
                                    support.toHandle.toString(),
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = support.amountKobo?.let { Money(it).render() }
                                    ?: stringResource(R.string.support_no_amount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Pill(
                            text = stateLabel(support.state),
                            container = if (support.state == SupportState.ACKNOWLEDGED) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            content = if (support.state == SupportState.ACKNOWLEDGED) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    support.reference?.let { reference ->
                        Text(
                            text = reference,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when (support.state) {
                        SupportState.INTENT -> {
                            if (claimFor == support.id) {
                                OutlinedTextField(
                                    value = claimReference,
                                    onValueChange = { if (it.length <= 120) claimReference = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.support_reference)) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onClaim(support.id, claimReference)
                                            claimFor = null
                                            claimReference = ""
                                        },
                                        enabled = claimReference.isNotBlank() && !busy,
                                        shape = MaterialTheme.shapes.large,
                                    ) {
                                        Text(stringResource(R.string.support_claim_save))
                                    }
                                    TextButton(onClick = { claimFor = null }) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                }
                            } else {
                                TextButton(onClick = { claimFor = support.id }) {
                                    Text(stringResource(R.string.support_claim))
                                }
                            }
                        }
                        SupportState.CLAIMED -> {
                            TextButton(onClick = { onAcknowledge(support.id) }, enabled = !busy) {
                                Text(stringResource(R.string.support_acknowledge))
                            }
                        }
                        SupportState.ACKNOWLEDGED -> {
                            Text(
                                text = stringResource(R.string.support_acknowledged_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: TokenKind): String = when (kind) {
    TokenKind.SUPPORTIVE -> "Support"
    TokenKind.RECOGNITION -> "Recognition"
    TokenKind.GROWTH_GRANT -> "Growth Grant"
    TokenKind.ACTIVATION -> "Activation"
    TokenKind.REACTIVATION -> "Reactivation"
    TokenKind.COURTESY -> "Courtesy"
    TokenKind.JOY -> "Joy"
}

/**
 * The real routes between two private accounts. Descriptors are the honest, typical shape of each
 * route; the provider confirms the exact fee, timing and limit when the person leaves TLC. Routes
 * that need an integration this build does not have are shown disabled rather than invented.
 */
@Composable
private fun transferMethods(): List<TransferMethod> {
    val sender = stringResource(R.string.transfer_method_payer_sender)
    val unavailable = stringResource(R.string.transfer_method_unavailable)
    val direct = stringResource(R.string.transfer_method_direct_name)
    val fast = stringResource(R.string.transfer_method_fast_name)
    val card = stringResource(R.string.transfer_method_card_name)
    val agent = stringResource(R.string.transfer_method_agent_name)
    val world = stringResource(R.string.transfer_method_world_name)
    return listOf(
        TransferMethod(
            id = "direct-bank",
            name = direct,
            provider = stringResource(R.string.transfer_method_direct_provider),
            fee = stringResource(R.string.transfer_method_direct_fee),
            payer = sender,
            timing = stringResource(R.string.transfer_method_timing_varies),
            limit = stringResource(R.string.transfer_method_limit_bank),
            badge = stringResource(R.string.transfer_method_direct_badge),
            available = true,
            unavailableReason = null,
        ),
        TransferMethod(
            id = "instant-bank",
            name = fast,
            provider = stringResource(R.string.transfer_method_fast_provider),
            fee = stringResource(R.string.transfer_method_fast_fee),
            payer = sender,
            timing = stringResource(R.string.transfer_method_timing_seconds),
            limit = stringResource(R.string.transfer_method_limit_fast),
            badge = stringResource(R.string.transfer_method_fast_badge),
            available = true,
            unavailableReason = null,
        ),
        TransferMethod(
            id = "card",
            name = card,
            provider = stringResource(R.string.transfer_method_card_provider),
            fee = stringResource(R.string.transfer_method_fee_provider),
            payer = sender,
            timing = stringResource(R.string.transfer_method_timing_minutes),
            limit = stringResource(R.string.transfer_method_limit_provider),
            badge = null,
            available = false,
            unavailableReason = unavailable,
        ),
        TransferMethod(
            id = "agent",
            name = agent,
            provider = stringResource(R.string.transfer_method_agent_provider),
            fee = stringResource(R.string.transfer_method_agent_fee),
            payer = sender,
            timing = stringResource(R.string.transfer_method_timing_same_day),
            limit = stringResource(R.string.transfer_method_limit_agent),
            badge = null,
            available = false,
            unavailableReason = unavailable,
        ),
        TransferMethod(
            id = "international",
            name = world,
            provider = stringResource(R.string.transfer_method_world_provider),
            fee = stringResource(R.string.transfer_method_world_fee),
            payer = sender,
            timing = stringResource(R.string.transfer_method_timing_days),
            limit = stringResource(R.string.transfer_method_limit_world),
            badge = null,
            available = false,
            unavailableReason = unavailable,
        ),
    )
}

private const val ROUTE_WINDOW_MS = 120_000L

private fun stateLabel(state: SupportState): String = when (state) {
    SupportState.INTENT -> "Offered"
    SupportState.CLAIMED -> "Sent — awaiting acknowledgement"
    SupportState.ACKNOWLEDGED -> "Acknowledged"
}
