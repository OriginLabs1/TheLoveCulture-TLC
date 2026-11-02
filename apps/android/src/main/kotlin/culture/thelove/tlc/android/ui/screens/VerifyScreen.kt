package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.InfoRow
import culture.thelove.tlc.android.ui.SealPill
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.StatTile
import culture.thelove.tlc.android.ui.hashPrefix
import culture.thelove.tlc.ledger.PersistentLedger
import culture.thelove.tlc.ledger.VerificationReport

@Composable
public fun VerifyScreen(
    entryCount: Int,
    headHash: String,
    merkleRoot: String,
    healthy: Boolean,
    sealLabel: String,
    report: VerificationReport?,
    status: PersistentLedger.Status?,
    onExport: () -> Unit,
    onShareSeal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.verify_lead),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SealPill(healthy = healthy, label = sealLabel)
            }
        }

        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(R.string.verify_entries),
                    value = entryCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.verify_status),
                    value = when {
                        status is PersistentLedger.Status.Damaged -> stringResource(R.string.status_damaged)
                        status is PersistentLedger.Status.Truncated -> stringResource(R.string.status_truncated)
                        status is PersistentLedger.Status.Recovered -> stringResource(R.string.status_recovered)
                        entryCount == 0 -> stringResource(R.string.status_empty)
                        else -> stringResource(R.string.status_ok)
                    },
                    modifier = Modifier.weight(1f),
                    accent = if (healthy) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }

        item(key = "hashes") {
            SectionCard(title = stringResource(R.string.verify_commitment_title)) {
                InfoRow(label = stringResource(R.string.verify_head), value = hashPrefix(headHash))
                InfoRow(label = stringResource(R.string.verify_root), value = hashPrefix(merkleRoot))
                InfoRow(
                    label = stringResource(R.string.verify_length),
                    value = stringResource(R.string.verify_length_value, entryCount),
                    mono = false,
                )
            }
        }

        item(key = "chain") {
            val failures = if (entryCount == 0) emptyList() else report?.failures.orEmpty()
            SectionCard(
                title = stringResource(R.string.verify_chain),
                container = if (healthy) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (healthy) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (healthy) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                    Text(
                        text = when {
                            entryCount == 0 -> stringResource(R.string.verify_empty)
                            healthy -> stringResource(R.string.verify_all_ok)
                            else -> stringResource(R.string.verify_failures, failures.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (failures.isNotEmpty()) {
                    Text(
                        text = failures.joinToString("\n") {
                            val where = if (it.index >= 0) "entry ${it.index + 1}" else "record"
                            "· ${it::class.simpleName} — $where"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                } else if (status is PersistentLedger.Status.Truncated) {
                    Text(
                        text = stringResource(R.string.verify_truncated_body, status.expected, status.found),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (status is PersistentLedger.Status.Recovered) {
                    Text(
                        text = stringResource(R.string.verify_recovered_body, status.entries),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = entryCount > 0,
                ) {
                    Text(stringResource(R.string.verify_export))
                }
                OutlinedButton(
                    onClick = onShareSeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = entryCount > 0,
                ) {
                    Text(stringResource(R.string.verify_share))
                }
            }
        }

        item(key = "cli") {
            SectionCard(title = stringResource(R.string.verify_outside_title)) {
                Text(
                    text = stringResource(R.string.verify_outside_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "ledger-verify --file <export>",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        item(key = "how") {
            SectionCard(title = stringResource(R.string.verify_how_title)) {
                Text(
                    text = stringResource(R.string.verify_how_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
