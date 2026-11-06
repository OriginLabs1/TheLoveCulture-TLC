package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.Eyebrow
import culture.thelove.tlc.android.ui.Pill
import culture.thelove.tlc.android.ui.theme.TlcPalette

/**
 * A Transfer Method is the real-world route by which value moves between two people's own accounts.
 *
 * It is chosen inside the transfer, never buried in Settings. The descriptor states the provider,
 * fee, who pays, the timing, the limit and whether it is genuinely available. TLC never debits an
 * account and never holds money: the person leaves for their own bank or licensed provider.
 */
public data class TransferMethod(
    public val id: String,
    public val name: String,
    public val provider: String,
    public val fee: String,
    public val payer: String,
    public val timing: String,
    public val limit: String,
    public val badge: String?,
    public val available: Boolean,
    public val unavailableReason: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TransferMethodSheet(
    methods: List<TransferMethod>,
    selectedId: String?,
    onChoose: (TransferMethod) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Eyebrow(stringResource(R.string.transfer_method_eyebrow), color = TlcPalette.colors.royal)
            Text(
                text = stringResource(R.string.transfer_method_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.transfer_method_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            methods.forEach { method ->
                TransferMethodRow(
                    method = method,
                    selected = method.id == selectedId,
                    onChoose = { onChoose(method) },
                )
            }
            Text(
                text = stringResource(R.string.transfer_method_notice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferMethodRow(
    method: TransferMethod,
    selected: Boolean,
    onChoose: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (method.available) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        onClick = { if (method.available) onChoose() },
        enabled = method.available,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (method.available) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (method.available) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (method.available) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Pill(
                        text = stringResource(R.string.transfer_method_selected),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                    )
                } else if (method.badge != null) {
                    Pill(text = method.badge)
                }
            }
            Text(
                text = method.provider,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MethodFact(stringResource(R.string.transfer_method_fee), method.fee)
                MethodFact(stringResource(R.string.transfer_method_timing), method.timing)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MethodFact(stringResource(R.string.transfer_method_payer), method.payer)
                MethodFact(stringResource(R.string.transfer_method_limit), method.limit)
            }
            if (!method.available) {
                Text(
                    text = method.unavailableReason.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MethodFact(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

/** The chosen method, always visible inside the transfer with its live quote window. */
@Composable
public fun TransferMethodSummary(
    method: TransferMethod,
    secondsLeft: Int,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.transfer_method_chosen),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (secondsLeft > 0) {
                        stringResource(R.string.transfer_method_quote_left, secondsLeft / 60, secondsLeft % 60)
                    } else {
                        stringResource(R.string.transfer_method_quote_expired)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(text = method.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.transfer_method_summary_line,
                    method.fee,
                    method.timing,
                    method.limit,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onOpen, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(stringResource(R.string.transfer_method_switch))
            }
        }
    }
}

/** A compact, honest preview of what the recipient will see. */
@Composable
public fun TransferMethodNone(onOpen: () -> Unit) {
    Button(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(stringResource(R.string.transfer_method_choose))
    }
}
