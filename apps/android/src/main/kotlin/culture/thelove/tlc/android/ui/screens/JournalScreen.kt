package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.EntryRow
import culture.thelove.tlc.android.ui.RecordHero
import culture.thelove.tlc.android.ui.SealPill
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.android.ui.hashPrefix
import culture.thelove.tlc.ledger.LedgerEntry

@Composable
public fun JournalScreen(
    entries: List<LedgerEntry>,
    headHash: String,
    healthy: Boolean,
    sealLabel: String,
    blockedReason: String?,
    onEntry: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = entries
        .asReversed()
        .filter { query.isBlank() || it.subject.contains(query.trim(), ignoreCase = true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "hero") {
            RecordHero(
                entryCount = entries.size,
                headHash = headHash,
                healthy = healthy,
                sealLabel = sealLabel,
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

        if (entries.isNotEmpty()) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text(stringResource(R.string.journal_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                SectionCard(title = stringResource(R.string.journal_empty_title)) {
                    Text(
                        text = if (entries.isEmpty()) {
                            stringResource(R.string.journal_empty_body)
                        } else {
                            stringResource(R.string.journal_filter_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(key = "list-label") {
                Column(
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.journal_list_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.journal_list_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(visible, key = { _, entry -> entry.hash }) { index, entry ->
                EntryRow(
                    entry = entry,
                    isLast = index == visible.lastIndex,
                    onClick = { onEntry(entry) },
                )
            }
        }

        item(key = "footer") {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entries.isNotEmpty()) {
                    SealPill(healthy = healthy, label = sealLabel)
                    Text(
                        text = stringResource(R.string.journal_footer, hashPrefix(headHash)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!healthy && blockedReason == null) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
