package culture.thelove.tlc.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.MediaImage
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.Reveal
import culture.thelove.tlc.android.ui.ScreenHeader
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.StatTile
import culture.thelove.tlc.android.ui.formatTimestamp
import culture.thelove.tlc.android.ui.relativeTime
import culture.thelove.tlc.android.ui.tlcClickable
import culture.thelove.tlc.android.ui.theme.TlcPalette
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.DueRecord
import culture.thelove.tlc.experience.GrantStage
import culture.thelove.tlc.experience.GrowthGrant
import culture.thelove.tlc.experience.ReceiptAudience
import culture.thelove.tlc.experience.ReceiptSubject
import culture.thelove.tlc.experience.SupportIntent
import culture.thelove.tlc.experience.SupportState
import culture.thelove.tlc.experience.TokenReceipt
import java.io.File

/** Which side of the circulation a token is seen from. */
private enum class Direction { SENT, RECEIVED }

private enum class TokenFamily { SUPPORT, PIONEER, GRANT }

/** One row in the shared circulation surface. Built only from facts already on the device. */
private data class TokenItem(
    val id: String,
    val family: TokenFamily,
    val direction: Direction,
    val counterparty: String,
    val amountKobo: Long?,
    val state: String,
    val statusKind: StatusKind,
    val createdAt: Long,
    val reference: String?,
    val subjectKind: ReceiptSubject,
    val subjectId: String,
    val steps: List<TokenStep>,
    val next: String,
)

private data class TokenStep(val label: String, val at: Long?, val done: Boolean)

private enum class StatusKind { OFFERED, MOVING, RECEIVED, ATTENTION, CLOSED }

/**
 * The shared token surface: every Support gift, Pioneer's Token and Growth Grant on one page,
 * with the same state the other side would see once the shared plane exists.
 *
 * There is no balance, no leaderboard and no pooled total. The three counters describe movement:
 * how many tokens exist, how many have left the offer stage, and how many have been acknowledged.
 * A receipt image belongs to its token; sharing it is one explicit, revocable choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TokensScreen(
    snapshot: AppSnapshot,
    busy: Boolean,
    onOpenSupport: () -> Unit,
    onOpenGrants: () -> Unit,
    onOpenDues: () -> Unit,
    onAttachReceipt: (ReceiptSubject, String, Uri, ReceiptAudience) -> Unit,
    onRemoveReceipt: (String) -> Unit,
    onSetAudience: (String, ReceiptAudience) -> Unit,
    onShareReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var segment by rememberSaveable { mutableStateOf("ALL") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingReceipt by remember { mutableStateOf<TokenReceipt?>(null) }
    var confirmRemove by remember { mutableStateOf<TokenReceipt?>(null) }
    var pending by remember { mutableStateOf<PendingCapture?>(null) }
    var pendingAudience by remember { mutableStateOf(ReceiptAudience.THIS_TRANSFER) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pending
        pending = null
        if (uri != null && target != null) {
            onAttachReceipt(target.subject, target.subjectId, uri, pendingAudience)
        }
    }

    val items = buildTokens(snapshot)
    val all = items
    val sent = items.filter { it.direction == Direction.SENT }
    val received = items.filter { it.direction == Direction.RECEIVED }
    val transferred = items.count { it.statusKind == StatusKind.MOVING || it.statusKind == StatusKind.RECEIVED }
    val visible = when (segment) {
        "SENT" -> sent
        "RECEIVED" -> received
        else -> all
    }
    val selected = items.firstOrNull { it.id == selectedId }

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
                eyebrow = stringResource(R.string.tokens_eyebrow),
                title = stringResource(R.string.tokens_title),
                body = stringResource(R.string.tokens_body),
                trailing = { LivePill() },
            )
        }

        item(key = "counters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveCounter(
                    label = stringResource(R.string.today_circulation),
                    value = all.size,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                LiveCounter(
                    label = stringResource(R.string.tokens_transferred),
                    value = transferred,
                    accent = TlcPalette.colors.forest,
                    modifier = Modifier.weight(1f),
                )
                LiveCounter(
                    label = stringResource(R.string.tokens_received_short),
                    value = received.size,
                    accent = TlcPalette.colors.royal,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "segments") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentChip(
                    label = stringResource(R.string.tokens_segment_all),
                    count = all.size,
                    selected = segment == "ALL",
                    onClick = { segment = "ALL" },
                )
                SegmentChip(
                    label = stringResource(R.string.tokens_segment_sent),
                    count = sent.size,
                    selected = segment == "SENT",
                    onClick = { segment = "SENT" },
                )
                SegmentChip(
                    label = stringResource(R.string.tokens_segment_received),
                    count = received.size,
                    selected = segment == "RECEIVED",
                    onClick = { segment = "RECEIVED" },
                )
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyTokens(segment = segment, onSupport = onOpenSupport)
            }
        }

        itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
            Reveal(index) {
                TokenCard(
                    item = item,
                    receipt = snapshot.receipt(item.subjectKind, item.subjectId),
                    onClick = { selectedId = item.id },
                )
            }
        }

        if (visible.isNotEmpty()) {
            item(key = "note") {
                SectionCard(
                    title = stringResource(R.string.tokens_received_note_title),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.tokens_received_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }

    if (selected != null) {
        val receipt = snapshot.receipt(selected.subjectKind, selected.subjectId)
        ModalBottomSheet(
            onDismissRequest = { selectedId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            TokenDetail(
                item = selected,
                receipt = receipt,
                busy = busy,
                onOpenSource = {
                    selectedId = null
                    when (selected.family) {
                        TokenFamily.SUPPORT -> onOpenSupport()
                        TokenFamily.GRANT -> onOpenGrants()
                        TokenFamily.PIONEER -> onOpenDues()
                    }
                },
                onAddReceipt = { audience ->
                    pendingAudience = audience
                    pending = PendingCapture(selected.subjectKind, selected.subjectId)
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onReplaceReceipt = { audience ->
                    pendingAudience = audience
                    pending = PendingCapture(selected.subjectKind, selected.subjectId)
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemoveReceipt = { confirmRemove = receipt },
                onSetAudience = { audience -> receipt?.let { onSetAudience(it.id, audience) } },
                onView = { viewingReceipt = receipt },
                onShare = { receipt?.let { onShareReceipt(it.fileName) } },
            )
        }
    }

    val full = viewingReceipt
    if (full != null) {
        ImageViewer(receipt = full, onClose = { viewingReceipt = null })
    }

    val removal = confirmRemove
    if (removal != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.receipt_remove)) },
            text = { Text(stringResource(R.string.receipt_remove_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveReceipt(removal.id)
                        confirmRemove = null
                        viewingReceipt = null
                    },
                ) {
                    Text(stringResource(R.string.receipt_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private data class PendingCapture(val subject: ReceiptSubject, val subjectId: String)

/**
 * Everything below is derived from the durable snapshot. No network, no guess: a Support that has
 * not been claimed says exactly that, and a grant that has not been paid says exactly that.
 */
@Composable
private fun buildTokens(snapshot: AppSnapshot): List<TokenItem> {
    val out = mutableListOf<TokenItem>()

    snapshot.supports.forEach { support ->
        val received = support.state == SupportState.ACKNOWLEDGED
        out += TokenItem(
            id = "support:${support.id}",
            family = TokenFamily.SUPPORT,
            direction = if (received) Direction.RECEIVED else Direction.SENT,
            counterparty = support.toHandle.toString(),
            amountKobo = support.amountKobo,
            state = supportStateLabel(support.state),
            statusKind = when (support.state) {
                SupportState.INTENT -> StatusKind.OFFERED
                SupportState.CLAIMED -> StatusKind.MOVING
                SupportState.ACKNOWLEDGED -> StatusKind.RECEIVED
            },
            createdAt = support.createdAt,
            reference = support.reference,
            subjectKind = ReceiptSubject.SUPPORT,
            subjectId = support.id,
            steps = listOf(
                TokenStep(stringResource(R.string.tokens_status_offered), support.createdAt, true),
                TokenStep(
                    stringResource(R.string.tokens_status_sent),
                    support.claimedAt,
                    support.state != SupportState.INTENT,
                ),
                TokenStep(
                    stringResource(R.string.tokens_status_received),
                    support.acknowledgedAt,
                    support.state == SupportState.ACKNOWLEDGED,
                ),
            ),
            next = when (support.state) {
                SupportState.INTENT -> stringResource(R.string.tokens_next_claim)
                SupportState.CLAIMED -> stringResource(R.string.tokens_next_acknowledge)
                SupportState.ACKNOWLEDGED -> stringResource(R.string.tokens_next_receipt)
            },
        )
    }

    snapshot.dues.forEach { due ->
        out += dueToken(due)
    }

    snapshot.grants.forEach { grant ->
        out += grantToken(grant)
    }

    return out.sortedByDescending { it.createdAt }
}

@Composable
private fun dueToken(due: DueRecord): TokenItem {
    val final = due.state == ObligationState.ATTESTED ||
        due.state == ObligationState.WAIVED ||
        due.state == ObligationState.EXPIRED
    return TokenItem(
        id = "due:${due.id}",
        family = TokenFamily.PIONEER,
        direction = Direction.SENT,
        counterparty = due.members.joinToString(" · ") { it.maskHandle() },
        amountKobo = due.amountKobo,
        state = dueStateLabel(due.state),
        statusKind = when (due.state) {
            ObligationState.WAIVED, ObligationState.EXPIRED -> StatusKind.CLOSED
            ObligationState.EXCEPTION -> StatusKind.ATTENTION
            ObligationState.ATTESTED -> StatusKind.RECEIVED
            ObligationState.DUE -> StatusKind.OFFERED
            else -> StatusKind.MOVING
        },
        createdAt = due.createdAt,
        reference = due.bankReference,
        subjectKind = ReceiptSubject.DUE,
        subjectId = due.id,
        steps = listOf(
            TokenStep(stringResource(R.string.tokens_status_pledged), due.createdAt, true),
            TokenStep(
                stringResource(R.string.tokens_status_issued),
                due.instructionIssuedAt,
                due.instructionIssuedAt != null,
            ),
            TokenStep(
                stringResource(R.string.tokens_status_proof),
                due.proofAt,
                due.proofAt != null,
            ),
            TokenStep(
                stringResource(R.string.tokens_status_attested),
                due.attestedAt,
                due.state == ObligationState.ATTESTED,
            ),
            TokenStep(
                stringResource(R.string.tokens_status_waived),
                due.waivedAt,
                due.state == ObligationState.WAIVED,
            ),
        ),
        next = if (final) {
            stringResource(R.string.tokens_next_receipt)
        } else {
            stringResource(R.string.tokens_next_open)
        },
    )
}

@Composable
private fun grantToken(grant: GrowthGrant): TokenItem = TokenItem(
    id = "grant:${grant.id}",
    family = TokenFamily.GRANT,
    direction = Direction.SENT,
    counterparty = grant.beneficiary,
    amountKobo = grant.amountKobo,
    state = grantStateLabel(grant.stage),
    statusKind = when (grant.stage) {
        GrantStage.PLEDGED -> StatusKind.OFFERED
        GrantStage.EVIDENCE_RECORDED -> StatusKind.MOVING
        GrantStage.VERIFIED -> StatusKind.MOVING
        GrantStage.PAID -> StatusKind.RECEIVED
        GrantStage.WITHDRAWN -> StatusKind.CLOSED
    },
    createdAt = grant.pledgedAt,
    reference = grant.bankReference,
    subjectKind = ReceiptSubject.GRANT,
    subjectId = grant.id,
    steps = listOf(
        TokenStep(stringResource(R.string.tokens_status_pledged), grant.pledgedAt, true),
        TokenStep(
            stringResource(R.string.tokens_status_evidence),
            grant.verifiedAt,
            grant.stage != GrantStage.PLEDGED && grant.stage != GrantStage.WITHDRAWN,
        ),
        TokenStep(
            stringResource(R.string.tokens_status_verified),
            grant.verifiedAt,
            grant.verifiedAt != null,
        ),
        TokenStep(
            stringResource(R.string.tokens_status_paid),
            grant.paidAt,
            grant.stage == GrantStage.PAID,
        ),
        TokenStep(
            stringResource(R.string.tokens_status_withdrawn),
            grant.withdrawnAt,
            grant.stage == GrantStage.WITHDRAWN,
        ),
    ),
    next = when (grant.stage) {
        GrantStage.PLEDGED -> stringResource(R.string.tokens_next_evidence)
        GrantStage.EVIDENCE_RECORDED -> stringResource(R.string.tokens_next_verify)
        GrantStage.VERIFIED -> stringResource(R.string.tokens_next_pay)
        GrantStage.PAID -> stringResource(R.string.tokens_next_receipt)
        GrantStage.WITHDRAWN -> stringResource(R.string.tokens_next_closed)
    },
)

@Composable
private fun LivePill() {
    val colors = TlcPalette.colors
    val motion = culture.thelove.tlc.android.ui.LocalMotion.current
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = if (motion) 0.35f else 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "live-alpha",
    )
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.forestContainer,
        contentColor = colors.onForestContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.forest.copy(alpha = alpha)),
            )
            Text(
                text = stringResource(R.string.tokens_live),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LiveCounter(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(650),
        label = "counter-$label",
    )
    StatTile(
        label = label,
        value = animated.toString(),
        modifier = modifier,
        accent = accent,
    )
}

@Composable
private fun SegmentChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label · $count") },
    )
}

@Composable
private fun EmptyTokens(segment: String, onSupport: () -> Unit) {
    val (title, body) = when (segment) {
        "SENT" -> R.string.tokens_empty_sent_title to R.string.tokens_empty_sent_body
        "RECEIVED" -> R.string.tokens_empty_received_title to R.string.tokens_empty_received_body
        else -> R.string.tokens_empty_all_title to R.string.tokens_empty_all_body
    }
    SectionCard(title = stringResource(title)) {
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (segment != "RECEIVED") {
            OutlinedButton(
                onClick = onSupport,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.today_support))
            }
        }
    }
}

@Composable
private fun TokenCard(item: TokenItem, receipt: TokenReceipt?, onClick: () -> Unit) {
    val colors = TlcPalette.colors
    val accent = when (item.family) {
        TokenFamily.SUPPORT -> MaterialTheme.colorScheme.primary
        TokenFamily.PIONEER -> colors.royal
        TokenFamily.GRANT -> colors.forest
    }
    val container = when (item.statusKind) {
        StatusKind.RECEIVED -> colors.forestContainer
        StatusKind.ATTENTION -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when (item.statusKind) {
        StatusKind.RECEIVED -> colors.onForestContainer
        StatusKind.ATTENTION -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tlcClickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (item.family) {
                            TokenFamily.SUPPORT -> Icons.Filled.Favorite
                            TokenFamily.PIONEER -> Icons.Filled.DateRange
                            TokenFamily.GRANT -> Icons.Filled.Star
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = familyLabel(item.family),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.counterparty,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (item.statusKind == StatusKind.RECEIVED) {
                            colors.onForestContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(item)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.amountKobo?.let { culture.thelove.tlc.domain.Money(it).render() }
                        ?: stringResource(R.string.tokens_no_amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = relativeTime(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (receipt != null) {
                    Pill(
                        text = stringResource(R.string.tokens_status_receipt),
                        container = colors.forestContainer,
                        content = colors.onForestContainer,
                    )
                } else {
                    Pill(
                        text = stringResource(R.string.receipt_none_title),
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.reference?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(item: TokenItem) {
    val colors = TlcPalette.colors
    val (container, content) = when (item.statusKind) {
        StatusKind.RECEIVED -> colors.forestContainer to colors.onForestContainer
        StatusKind.ATTENTION -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusKind.OFFERED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        StatusKind.MOVING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusKind.CLOSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Pill(text = item.state, container = container, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenDetail(
    item: TokenItem,
    receipt: TokenReceipt?,
    busy: Boolean,
    onOpenSource: () -> Unit,
    onAddReceipt: (ReceiptAudience) -> Unit,
    onReplaceReceipt: (ReceiptAudience) -> Unit,
    onRemoveReceipt: () -> Unit,
    onSetAudience: (ReceiptAudience) -> Unit,
    onView: () -> Unit,
    onShare: () -> Unit,
) {
    var audience by remember(receipt?.id) {
        mutableStateOf(receipt?.audience ?: ReceiptAudience.THIS_TRANSFER)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow(stringResource(R.string.tokens_detail_title), color = TlcPalette.colors.royal)
        Text(text = familyLabel(item.family), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = item.counterparty,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusPill(item)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailFact(
                label = stringResource(R.string.tokens_reference),
                value = item.reference ?: stringResource(R.string.tokens_no_reference),
                modifier = Modifier.weight(1f),
            )
            DetailFact(
                label = stringResource(R.string.support_amount),
                value = item.amountKobo?.let { culture.thelove.tlc.domain.Money(it).render() }
                    ?: stringResource(R.string.tokens_no_amount),
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(text = stringResource(R.string.tokens_timeline), style = MaterialTheme.typography.titleSmall)
        item.steps.forEach { step -> TimelineStep(step) }

        SectionCard(title = stringResource(R.string.tokens_next)) {
            Text(
                text = item.next,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenSource, shape = MaterialTheme.shapes.large) {
                Text(stringResource(R.string.tokens_open_source))
            }
        }

        ReceiptSection(
            item = item,
            receipt = receipt,
            audience = audience,
            busy = busy,
            onAudience = { choice ->
                audience = choice
                if (receipt != null) onSetAudience(choice)
            },
            onAdd = { onAddReceipt(audience) },
            onReplace = { onReplaceReceipt(audience) },
            onRemove = onRemoveReceipt,
            onView = onView,
            onShare = onShare,
        )
    }
}

@Composable
private fun DetailFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimelineStep(step: TokenStep) {
    val colors = TlcPalette.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (step.done) colors.forestContainer else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (step.done) colors.forest else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = step.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (step.done) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            color = if (step.done) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = step.at?.let { formatTimestamp(it) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReceiptSection(
    item: TokenItem,
    receipt: TokenReceipt?,
    audience: ReceiptAudience,
    busy: Boolean,
    onAudience: (ReceiptAudience) -> Unit,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onView: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = TlcPalette.colors
    SectionCard(title = stringResource(R.string.receipt_title)) {
        if (receipt == null) {
            Text(
                text = stringResource(R.string.receipt_none_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val file = culture.thelove.tlc.android.ui.LocalMediaDir.current
                ?.let { File(it, receipt.fileName) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .tlcClickable(onClick = onView),
            ) {
                MediaImage(
                    file = file,
                    contentDescription = stringResource(R.string.receipt_title),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = stringResource(R.string.receipt_hash),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = receipt.hash,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.forest,
            )
        }

        Text(
            text = stringResource(R.string.receipt_audience),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.receipt_audience_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReceiptAudience.entries.forEach { choice ->
                FilterChip(
                    selected = audience == choice,
                    enabled = !busy,
                    onClick = { onAudience(choice) },
                    label = { Text(audienceLabel(choice)) },
                )
            }
        }
        Text(
            text = stringResource(
                if (audience.isShared) R.string.receipt_shared_note else R.string.receipt_private_note,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (receipt == null) {
                Button(onClick = onAdd, enabled = !busy, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.receipt_add))
                }
            } else {
                OutlinedButton(onClick = onReplace, enabled = !busy, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.receipt_replace))
                }
                OutlinedButton(onClick = onShare, enabled = !busy, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.receipt_share))
                }
            }
        }
        if (receipt != null) {
            TextButton(onClick = onRemove, enabled = !busy) {
                Text(
                    text = stringResource(R.string.receipt_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(
            text = stringResource(R.string.profile_photo_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImageViewer(receipt: TokenReceipt, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val file = culture.thelove.tlc.android.ui.LocalMediaDir.current
                    ?.let { File(it, receipt.fileName) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(MaterialTheme.shapes.large),
                ) {
                    MediaImage(
                        file = file,
                        contentDescription = stringResource(R.string.receipt_title),
                        modifier = Modifier.fillMaxSize(),
                        maxDimension = 1600,
                    )
                }
                Text(
                    text = receipt.hash,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.receipt_close))
                }
            }
        }
    }
}

@Composable
private fun familyLabel(family: TokenFamily): String = stringResource(
    when (family) {
        TokenFamily.SUPPORT -> R.string.tokens_kind_support
        TokenFamily.PIONEER -> R.string.tokens_kind_pioneer
        TokenFamily.GRANT -> R.string.tokens_kind_grant
    },
)

@Composable
private fun supportStateLabel(state: SupportState): String = stringResource(
    when (state) {
        SupportState.INTENT -> R.string.tokens_status_offered
        SupportState.CLAIMED -> R.string.tokens_status_sent
        SupportState.ACKNOWLEDGED -> R.string.tokens_status_received
    },
)

@Composable
private fun dueStateLabel(state: ObligationState): String = stringResource(
    when (state) {
        ObligationState.DUE -> R.string.tokens_status_offered
        ObligationState.INSTRUCTION_ISSUED -> R.string.tokens_status_instructed
        ObligationState.PROOF_SUBMITTED -> R.string.tokens_status_sent
        ObligationState.ATTESTED -> R.string.tokens_status_verified
        ObligationState.EXCEPTION -> R.string.tokens_status_attention
        ObligationState.WAIVED -> R.string.tokens_status_withdrawn
        ObligationState.EXPIRED -> R.string.tokens_status_withdrawn
    },
)

@Composable
private fun grantStateLabel(stage: GrantStage): String = stringResource(
    when (stage) {
        GrantStage.PLEDGED -> R.string.tokens_status_pledged
        GrantStage.EVIDENCE_RECORDED -> R.string.tokens_status_recorded
        GrantStage.VERIFIED -> R.string.tokens_status_verified
        GrantStage.PAID -> R.string.tokens_status_paid
        GrantStage.WITHDRAWN -> R.string.tokens_status_withdrawn
    },
)

@Composable
private fun audienceLabel(audience: ReceiptAudience): String = stringResource(
    when (audience) {
        ReceiptAudience.ONLY_ME -> R.string.receipt_audience_only_me
        ReceiptAudience.THIS_TRANSFER -> R.string.receipt_audience_transfer
        ReceiptAudience.MY_CIRCLE -> R.string.receipt_audience_circle
        ReceiptAudience.ANYONE_WITH_LINK -> R.string.receipt_audience_link
        ReceiptAudience.PUBLIC_IN_RECORD -> R.string.receipt_audience_public
    },
)

private fun String.maskHandle(): String =
    if (length <= 3) this else take(2) + "…" + takeLast(1)
