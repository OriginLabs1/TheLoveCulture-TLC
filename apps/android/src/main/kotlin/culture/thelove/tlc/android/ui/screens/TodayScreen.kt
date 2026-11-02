package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.GrantStage

/**
 * The home surface: who you are, that the record is intact, the three everyday verbs, and every
 * loop that is still open. Nothing is decorative for its own sake; each block leads somewhere.
 */
@Composable
public fun TodayScreen(
    snapshot: AppSnapshot,
    entryCount: Int,
    headHash: String,
    healthy: Boolean,
    sealLabel: String,
    onRecognise: () -> Unit,
    onPledge: () -> Unit,
    onRecordDue: () -> Unit,
    onJournal: () -> Unit,
    onCircle: () -> Unit,
    onGrants: () -> Unit,
    onDues: () -> Unit,
    onVerify: () -> Unit,
    onCheckpoint: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openProposals = snapshot.proposals.count { !it.closed }
    val openGrants = snapshot.grants.count { it.stage != GrantStage.PAID && it.stage != GrantStage.WITHDRAWN }
    val openDues = snapshot.dues.count {
        it.state == ObligationState.DUE ||
            it.state == ObligationState.INSTRUCTION_ISSUED ||
            it.state == ObligationState.PROOF_SUBMITTED
    }
    val openTotal = openProposals + openGrants + openDues

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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(R.string.today_eyebrow))
                Text(
                    text = stringResource(R.string.today_greeting, snapshot.profile.displayName),
                    style = MaterialTheme.typography.displaySmall,
                )
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
        }

        item(key = "record") {
            RecordHero(
                entryCount = entryCount,
                headHash = headHash,
                healthy = healthy,
                sealLabel = sealLabel,
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
                        title = stringResource(R.string.today_recognise),
                        body = stringResource(R.string.today_recognise_body),
                        icon = Icons.Filled.Person,
                        onClick = onRecognise,
                        modifier = Modifier.weight(1f),
                    )
                    ActionTile(
                        title = stringResource(R.string.today_pledge),
                        body = stringResource(R.string.today_pledge_body),
                        icon = Icons.Filled.Favorite,
                        onClick = onPledge,
                        modifier = Modifier.weight(1f),
                    )
                }
                ActionTile(
                    title = stringResource(R.string.today_due),
                    body = stringResource(R.string.today_due_body),
                    icon = Icons.Filled.DateRange,
                    onClick = onRecordDue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = snapshot.hasDyad,
                )
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

        item(key = "modules") {
            SectionCard(title = stringResource(R.string.today_capabilities)) {
                Text(
                    text = stringResource(R.string.today_capabilities_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ModuleRow(
                    index = 1,
                    title = stringResource(R.string.module_journal),
                    body = stringResource(R.string.module_journal_body),
                    onClick = onJournal,
                )
                ModuleRow(
                    index = 2,
                    title = stringResource(R.string.module_grants),
                    body = stringResource(R.string.module_grants_body),
                    onClick = onGrants,
                )
                ModuleRow(
                    index = 3,
                    title = stringResource(R.string.module_dues),
                    body = stringResource(R.string.module_dues_body),
                    onClick = onDues,
                )
                ModuleRow(
                    index = 4,
                    title = stringResource(R.string.module_circle),
                    body = stringResource(R.string.module_circle_body),
                    onClick = onCircle,
                )
                ModuleRow(
                    index = 5,
                    title = stringResource(R.string.module_verify),
                    body = stringResource(R.string.module_verify_body),
                    onClick = onVerify,
                )
                ModuleRow(
                    index = 6,
                    title = stringResource(R.string.module_backup),
                    body = stringResource(R.string.module_backup_body),
                    onClick = onCheckpoint,
                )
            }
        }

        item(key = "notice") {
            GlassPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(
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
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.settings_verify))
            }
        }
    }
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
