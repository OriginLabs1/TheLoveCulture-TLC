package culture.thelove.tlc.android.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.JournalState
import culture.thelove.tlc.android.data.SaveOutcome
import culture.thelove.tlc.android.ui.screens.AboutScreen
import culture.thelove.tlc.android.ui.screens.CaptureScreen
import culture.thelove.tlc.android.ui.screens.JournalScreen
import culture.thelove.tlc.android.ui.screens.VerifyScreen
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.LedgerEntry
import culture.thelove.tlc.ledger.Merkle
import culture.thelove.tlc.ledger.PersistentLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

public enum class Screen { Journal, Verify, About, Capture }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TlcApp(filesDir: File, modifier: Modifier = Modifier) {
    val state = remember { JournalState(filesDir) }
    var screenName by rememberSaveable { mutableStateOf(Screen.Journal.name) }
    val screen = Screen.valueOf(screenName)
    var selected by remember { mutableStateOf<LedgerEntry?>(null) }
    var saving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) { state.load() }

    val sealLabel = when {
        state.status is PersistentLedger.Status.Truncated -> stringResource(R.string.status_truncated)
        state.status is PersistentLedger.Status.Damaged -> stringResource(R.string.status_damaged)
        state.status is PersistentLedger.Status.Recovered -> stringResource(R.string.status_recovered)
        state.entries.isEmpty() && state.healthy -> stringResource(R.string.status_empty)
        state.healthy -> stringResource(R.string.status_ok)
        else -> stringResource(R.string.status_opening)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = state.exportText()
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                            ?: error("no output stream")
                    }
                }
                snackbar.showSnackbar(
                    context.getString(
                        if (written.isSuccess) R.string.export_done else R.string.export_failed,
                    ),
                )
            }
        }
    }

    fun shareSeal() {
        val anchor = state.anchor() ?: return
        val body = context.getString(
            R.string.seal_share_body,
            anchor.length,
            anchor.headHash,
            anchor.merkleRoot,
            formatTimestamp(anchor.createdAt),
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.seal_share_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.verify_share)),
        )
    }

    fun save(who: String, what: String) {
        if (saving) return
        scope.launch {
            saving = true
            when (val outcome = state.addRecognition(who, what)) {
                is SaveOutcome.Saved -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    saving = false
                    screenName = Screen.Journal.name
                    snackbar.showSnackbar(context.getString(R.string.saved_message))
                }
                is SaveOutcome.Failed -> {
                    saving = false
                    snackbar.showSnackbar(outcome.reason)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screen) {
                            Screen.Journal -> stringResource(R.string.app_name)
                            Screen.Verify -> stringResource(R.string.verify_title)
                            Screen.About -> stringResource(R.string.about_title)
                            Screen.Capture -> stringResource(R.string.capture_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (screen == Screen.Capture) {
                        IconButton(onClick = { screenName = Screen.Journal.name }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (!state.loading && screen != Screen.Capture) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    TlcNavItem(
                        selected = screen == Screen.Journal,
                        label = stringResource(R.string.nav_journal),
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        onClick = { screenName = Screen.Journal.name },
                    )
                    TlcNavItem(
                        selected = screen == Screen.Verify,
                        label = stringResource(R.string.nav_verify),
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                        onClick = { screenName = Screen.Verify.name },
                    )
                    TlcNavItem(
                        selected = screen == Screen.About,
                        label = stringResource(R.string.nav_about),
                        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = { screenName = Screen.About.name },
                    )
                }
            }
        },
        floatingActionButton = {
            if (!state.loading && screen == Screen.Journal) {
                ExtendedFloatingActionButton(
                    onClick = { screenName = Screen.Capture.name },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.journal_add)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { inner ->
        Box(modifier = Modifier.padding(inner)) {
            when {
                state.loading -> LoadingPane()
                state.loadError != null -> LoadErrorPane(state.loadError!!)
                else -> AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (
                            fadeIn(tween(220)) +
                                slideInHorizontally(tween(260)) { if (forward) it / 8 else -it / 8 }
                            ) togetherWith (
                            fadeOut(tween(140)) +
                                slideOutHorizontally(tween(180)) { if (forward) -it / 8 else it / 8 }
                            )
                    },
                    label = "screen",
                ) { target ->
                    when (target) {
                        Screen.Journal -> JournalScreen(
                            entries = state.entries,
                            headHash = state.headHash,
                            healthy = state.healthy,
                            sealLabel = sealLabel,
                            blockedReason = state.blockedReason,
                            onEntry = { selected = it },
                        )
                        Screen.Verify -> VerifyScreen(
                            entryCount = state.entries.size,
                            headHash = state.headHash,
                            merkleRoot = state.merkleRoot,
                            healthy = state.healthy,
                            sealLabel = sealLabel,
                            report = state.report,
                            status = state.status,
                            onExport = {
                                exportLauncher.launch(
                                    context.getString(R.string.record_file_name, dayStamp()),
                                )
                            },
                            onShareSeal = { shareSeal() },
                        )
                        Screen.About -> AboutScreen()
                        Screen.Capture -> CaptureScreen(
                            saving = saving,
                            blockedReason = state.blockedReason,
                            onSave = { who, what -> save(who, what) },
                            onCancel = { screenName = Screen.Journal.name },
                        )
                    }
                }
            }
        }
    }

    val detail = selected
    if (detail != null) {
        EntryDetailSheet(
            entry = detail,
            state = state,
            onDismiss = { selected = null },
            onCopied = { scope.launch { snackbar.showSnackbar(context.getString(R.string.detail_copied)) } },
        )
    }
}

@Composable
private fun RowScope.TlcNavItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) },
    )
}

@Composable
private fun LoadingPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadErrorPane(reason: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        SectionCard(title = stringResource(R.string.load_failed)) {
            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailSheet(
    entry: LedgerEntry,
    state: JournalState,
    onDismiss: () -> Unit,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val index = state.entries.indexOfFirst { it.hash == entry.hash }
    val steps: List<Merkle.Step> = if (index >= 0) state.proof(index).orEmpty() else emptyList()
    // A one-entry record has a valid *empty* proof: the leaf is the root. Do not call that a
    // failure just because no sibling hashes are needed.
    val proofOk = index >= 0 && state.proofVerifies(entry.hash, steps)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.detail_title, entry.seq),
                    style = MaterialTheme.typography.headlineSmall,
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                KindLabel(entry.kind)
            }
            Text(text = entry.subject, style = MaterialTheme.typography.bodyLarge)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            InfoRow(label = stringResource(R.string.detail_time), value = formatTimestamp(entry.occurredAt), mono = false)
            InfoRow(label = stringResource(R.string.detail_kind), value = kindName(entry.kind), mono = false)
            InfoRow(label = stringResource(R.string.detail_prev), value = entry.prevHash)
            InfoRow(label = stringResource(R.string.detail_hash), value = entry.hash)
            entry.idempotencyKey?.let {
                InfoRow(label = stringResource(R.string.detail_key), value = it)
            }

            SectionCard(
                title = stringResource(R.string.detail_proof),
                container = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = if (proofOk) {
                        stringResource(R.string.detail_proof_ok)
                    } else {
                        stringResource(R.string.detail_proof_bad)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (steps.isEmpty()) {
                        stringResource(R.string.detail_proof_single)
                    } else {
                        stringResource(R.string.detail_proof_steps, steps.size, state.merkleRoot.take(16))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(entry.hash))
                    onCopied()
                },
            ) {
                Text(stringResource(R.string.detail_copy))
            }
        }
    }
}

private fun kindName(kind: EntryKind): String =
    kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
