package culture.thelove.tlc.android.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.ActionTile
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.GlassPanel
import culture.thelove.tlc.android.ui.ModuleRow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.RecordHero
import culture.thelove.tlc.android.ui.SealPill
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.StatTile
import culture.thelove.tlc.android.ui.TlcAvatar
import culture.thelove.tlc.android.ui.tlcClickable
import culture.thelove.tlc.android.ui.theme.TlcPalette
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.GrantStage
import culture.thelove.tlc.experience.SupportState

/**
 * The home surface: who you are, that the record is intact, the four everyday verbs, and every
 * loop that is still open. The first thing a new person should see is a way to reach someone.
 */
@Composable
public fun TodayScreen(
    snapshot: AppSnapshot,
    entryCount: Int,
    headHash: String,
    healthy: Boolean,
    sealLabel: String,
    onRecord: () -> Unit,
    onSupport: () -> Unit,
    onContact: () -> Unit,
    onPioneerToken: () -> Unit,
    onPeople: () -> Unit,
    onGrants: () -> Unit,
    onCircle: () -> Unit,
    onDues: () -> Unit,
    onVerify: () -> Unit,
    onCheckpoint: () -> Unit,
    onAbout: () -> Unit,
    onTokens: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openProposals = snapshot.proposals.count { !it.closed }
    val openGrants = snapshot.grants.count { it.stage != GrantStage.PAID && it.stage != GrantStage.WITHDRAWN }
    val openDues = snapshot.dues.count {
        it.state == ObligationState.DUE ||
            it.state == ObligationState.INSTRUCTION_ISSUED ||
            it.state == ObligationState.PROOF_SUBMITTED
    }
    val openSupports = snapshot.supports.count { it.state != SupportState.ACKNOWLEDGED }
    val openTotal = openProposals + openGrants + openDues + openSupports

    val tokenTotal = snapshot.supports.size + snapshot.dues.size + snapshot.grants.size
    val tokenReceived = snapshot.supports.count { it.state == SupportState.ACKNOWLEDGED }
    val tokenTransferred = snapshot.supports.count { it.state != SupportState.INTENT } +
        snapshot.dues.count {
            it.state == ObligationState.ATTESTED ||
                it.state == ObligationState.PROOF_SUBMITTED
        } +
        snapshot.grants.count { it.stage == GrantStage.PAID }

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
        item(key = "hello") {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Eyebrow(stringResource(R.string.today_eyebrow))
                    Text(
                        text = stringResource(R.string.today_greeting, snapshot.profile.displayName),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    snapshot.profile.handle?.let { handle ->
                        Text(
                            text = handle.value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            text = stringResource(
                                R.string.today_circle,
                                snapshot.profile.circleName,
                                roleLabel(snapshot.profile.role),
                            ),
                        )
                        if (snapshot.hasDyad) {
                            Pill(
                                text = stringResource(R.string.circle_signature_two),
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
                TlcAvatar(
                    photoRef = snapshot.profile.photoRef,
                    size = 56.dp,
                    modifier = Modifier.tlcClickable(onClick = onProfile),
                )
            }
        }

        item(key = "record") {
            RecordHero(
                entryCount = entryCount,
                headHash = headHash,
                healthy = healthy,
                sealLabel = sealLabel,
            )
        }

        item(key = "circulation") {
            CirculationStrip(
                total = tokenTotal,
                transferred = tokenTransferred,
                received = tokenReceived,
                onOpen = onTokens,
            )
        }

        item(key = "quick-label") {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.today_quick),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.today_capabilities_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "quick") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = stringResource(R.string.today_support),
                        body = stringResource(R.string.today_support_body),
                        icon = Icons.Filled.Send,
                        onClick = onSupport,
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        title = stringResource(R.string.today_desk),
                        body = stringResource(R.string.today_desk_body),
                        icon = Icons.Filled.Person,
                        onClick = onContact,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = stringResource(R.string.today_recognise),
                        body = stringResource(R.string.today_recognise_body),
                        icon = Icons.Filled.CheckCircle,
                        onClick = onRecord,
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        title = stringResource(R.string.today_token),
                        body = stringResource(R.string.today_token_body),
                        icon = Icons.Filled.DateRange,
                        onClick = onPioneerToken,
                        modifier = Modifier.weight(1f),
                        enabled = snapshot.hasDyad,
                    )
                }
            }
        }

        item(key = "loops") {
            SectionCard(
                title = stringResource(R.string.today_open_loops),
                container = if (openTotal == 0) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                if (openTotal == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SealPill(healthy = true, label = stringResource(R.string.today_all_clear))
                        Text(
                            text = stringResource(R.string.today_open_loops_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                } else {
                    OpenLoopRow(
                        title = stringResource(R.string.module_support),
                        count = openSupports,
                        onClick = onSupport,
                    )
                    OpenLoopRow(
                        title = stringResource(R.string.module_grants),
                        count = openGrants,
                        onClick = onGrants,
                    )
                    OpenLoopRow(
                        title = stringResource(R.string.module_dues),
                        count = openDues,
                        onClick = onDues,
                    )
                    OpenLoopRow(
                        title = stringResource(R.string.module_circle),
                        count = openProposals,
                        onClick = onCircle,
                    )
                }
            }
        }

        item(key = "trust") {
            SectionCard(title = stringResource(R.string.today_trust_title)) {
                Text(
                    text = stringResource(R.string.today_trust_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ModuleRow(
                    index = 1,
                    title = stringResource(R.string.module_people),
                    body = stringResource(R.string.module_people_body),
                    onClick = onPeople,
                )
                ModuleRow(
                    index = 2,
                    title = stringResource(R.string.module_journal),
                    body = stringResource(R.string.module_journal_body),
                    onClick = onRecord,
                )
                ModuleRow(
                    index = 3,
                    title = stringResource(R.string.module_grants),
                    body = stringResource(R.string.module_grants_body),
                    onClick = onGrants,
                )
                ModuleRow(
                    index = 4,
                    title = stringResource(R.string.module_dues),
                    body = stringResource(R.string.module_dues_body),
                    onClick = onDues,
                )
                ModuleRow(
                    index = 5,
                    title = stringResource(R.string.module_circle),
                    body = stringResource(R.string.module_circle_body),
                    onClick = onCircle,
                )
                ModuleRow(
                    index = 6,
                    title = stringResource(R.string.module_verify),
                    body = stringResource(R.string.module_verify_body),
                    onClick = onVerify,
                )
                ModuleRow(
                    index = 7,
                    title = stringResource(R.string.module_backup),
                    body = stringResource(R.string.module_backup_body),
                    onClick = onCheckpoint,
                )
            }
        }

        item(key = "notice") {
            GlassPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.today_notice_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.today_notice_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onAbout) {
                    Text(stringResource(R.string.today_read_doctrine))
                }
            }
        }

        item(key = "footer") {
            OutlinedButton(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.settings_verify))
            }
        }
    }
}

/**
 * The live circulation strip. Three counts, no balance and no ranking: how many tokens exist,
 * how many have left the offer stage, and how many the other side has acknowledged.
 */
@Composable
private fun CirculationStrip(total: Int, transferred: Int, received: Int, onOpen: () -> Unit) {
    val colors = TlcPalette.colors
    SectionCard(title = stringResource(R.string.today_circulation)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedCounter(
                label = stringResource(R.string.tokens_segment_all),
                value = total,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            AnimatedCounter(
                label = stringResource(R.string.tokens_transferred),
                value = transferred,
                accent = colors.forest,
                modifier = Modifier.weight(1f),
            )
            AnimatedCounter(
                label = stringResource(R.string.tokens_received_short),
                value = received,
                accent = colors.royal,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(
                if (total == 0) R.string.today_circulation_none else R.string.today_tokens_body,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpen) {
            Text(stringResource(R.string.create_tokens))
        }
    }
}

@Composable
private fun AnimatedCounter(label: String, value: Int, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(650),
        label = "today-$label",
    )
    StatTile(label = label, value = animated.toString(), modifier = modifier, accent = accent)
}

@Composable
private fun OpenLoopRow(title: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Pill(text = count.toString())
        } else {
            Pill(
                text = stringResource(R.string.today_all_clear),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.today_view_all))
        }
    }
}

private fun roleLabel(role: Role): String = when (role) {
    Role.SINGLE -> "Single"
    Role.I_PARTNER -> "I-Partner"
    Role.LOVE_MODEL -> "Love Model"
}
