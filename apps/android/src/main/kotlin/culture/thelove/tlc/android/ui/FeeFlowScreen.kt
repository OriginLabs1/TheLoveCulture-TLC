package culture.thelove.tlc.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.domain.Actor
import culture.thelove.tlc.domain.ActorKind
import culture.thelove.tlc.domain.DoctrineCopy
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeeMachine
import culture.thelove.tlc.domain.FeeObligation
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.domain.ObligationId
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.PartyId
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.domain.Transition
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerDraft
import culture.thelove.tlc.ledger.VerificationReport
import culture.thelove.tlc.ledger.adapter.PlatformSha256

private val alice = PartyId("party:alice")
private val bob = PartyId("party:bob")
private val member = Actor(alice, ActorKind.MEMBER)
private val pioneer = Actor(PartyId("party:pioneer"), ActorKind.PIONEER)

private fun seedObligation() = FeeObligation(
    id = ObligationId("ob:activation:demo"),
    dyad = setOf(alice, bob),
    kind = TokenKind.ACTIVATION,
    amount = Money.naira(1_000),
    period = FeePeriod.ONE_OFF,
)

/**
 * The shell renders; the core decides (SK-01/SK-07).
 * Every button here calls [FeeMachine.apply] — there is no second implementation of the rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeFlowScreen(modifier: Modifier = Modifier) {
    var obligation by remember { mutableStateOf(seedObligation()) }
    var log by remember { mutableStateOf(emptyList<String>()) }
    val report: VerificationReport = remember { demoLedgerReport() }

    fun advance(event: FeeEvent) {
        when (val result = FeeMachine.apply(obligation, event)) {
            is Transition.Applied -> {
                obligation = result.next
                log = result.effects.map { it::class.simpleName ?: "Effect" }
            }
            is Transition.Rejected -> {
                log = listOf(result.reason.name)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.fee_flow_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.fee_flow_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.label_token, obligation.kind.name))
                    Text(stringResource(R.string.label_amount, obligation.amount.render()))
                    Text(stringResource(R.string.label_state, obligation.state.name))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.label_effects))
                    if (log.isEmpty()) {
                        Text("—", fontFamily = FontFamily.Monospace)
                    } else {
                        log.forEach { Text(it, fontFamily = FontFamily.Monospace) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { advance(FeeEvent.IssueInstruction(member, now())) },
                    enabled = obligation.state == ObligationState.DUE,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_instruction))
                }
                Button(
                    onClick = {
                        advance(
                            FeeEvent.SubmitProof(
                                actor = member,
                                at = now(),
                                proofHash = PROOF_HASH,
                                statedAmount = obligation.amount,
                                bankReference = "TRF-DEMO-0001",
                            ),
                        )
                    },
                    enabled = obligation.state == ObligationState.INSTRUCTION_ISSUED,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_proof))
                }
            }

            Button(
                onClick = {
                    advance(
                        FeeEvent.Attest(
                            actor = pioneer,
                            at = now(),
                            amount = obligation.amount,
                            statementRef = "ZENITH-DEMO-0001",
                        ),
                    )
                },
                enabled = obligation.state == ObligationState.PROOF_SUBMITTED,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_attest))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (report.ok) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (report.ok) {
                            stringResource(R.string.ledger_ok, report.length)
                        } else {
                            stringResource(R.string.ledger_failed, report.failures.size)
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.ledger_head, report.headHash.take(16)),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.notice_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = DoctrineCopy.FINANCIAL_NOTICE,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun now(): Long = System.currentTimeMillis()

private val PROOF_HASH: String = "0123456789abcdef".repeat(4)

/** A miniature, in-memory history: intent -> proof -> attestation -> receipt, anchored. */
private fun demoLedgerReport(): VerificationReport {
    val hasher = PlatformSha256()
    val ledger = Ledger(hasher)
    val t = now()
    ledger.append(LedgerDraft(EntryKind.FEE_INTENT, "ob:activation:demo", 100_000, t, "demo:intent"))
    ledger.append(LedgerDraft(EntryKind.PROOF_RECORDED, "ob:activation:demo", 100_000, t + 1, "demo:proof"))
    ledger.append(LedgerDraft(EntryKind.ATTESTATION, "ob:activation:demo", 100_000, t + 2, "demo:attest"))
    ledger.append(LedgerDraft(EntryKind.RECEIPT, "ob:activation:demo", 100_000, t + 3, "demo:receipt"))
    return Ledger.verify(ledger.entries(), hasher, ledger.anchor(t + 4))
}
