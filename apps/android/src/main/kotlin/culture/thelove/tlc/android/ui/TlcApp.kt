package culture.thelove.tlc.android.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.AppState
import culture.thelove.tlc.android.data.JournalState
import culture.thelove.tlc.android.data.SaveOutcome
import culture.thelove.tlc.android.ui.screens.AboutScreen
import culture.thelove.tlc.android.ui.screens.CaptureScreen
import culture.thelove.tlc.android.ui.screens.CircleScreen
import culture.thelove.tlc.android.ui.screens.DuesScreen
import culture.thelove.tlc.android.ui.screens.GrantsScreen
import culture.thelove.tlc.android.ui.screens.JournalScreen
import culture.thelove.tlc.android.ui.screens.OnboardingScreen
import culture.thelove.tlc.android.ui.screens.SettingsScreen
import culture.thelove.tlc.android.ui.screens.TodayScreen
import culture.thelove.tlc.android.ui.screens.VerifyScreen
import culture.thelove.tlc.android.ui.theme.TlcTheme
import culture.thelove.tlc.experience.GrantEvent
import culture.thelove.tlc.experience.ThemeMode
import culture.thelove.tlc.ledger.LedgerEntry
import culture.thelove.tlc.ledger.Merkle
import culture.thelove.tlc.ledger.PersistentLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

public enum class Dest {
    TODAY,
    JOURNAL,
    GRANTS,
    CIRCLE,
    SETTINGS,
    CAPTURE,
    DUES,
    VERIFY,
    ABOUT,
    ;

    public val isTab: Boolean
        get() = this == TODAY || this == JOURNAL || this == GRANTS || this == CIRCLE || this == SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TlcApp(filesDir: File, modifier: Modifier = Modifier) {
    val state = remember(filesDir) { AppState(filesDir) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }

    var destName by rememberSaveable { mutableStateOf(Dest.TODAY.name) }
    val dest = Dest.valueOf(destName)
    var lastTabName by rememberSaveable { mutableStateOf(Dest.TODAY.name) }
    var selected by remember { mutableStateOf<LedgerEntry?>(null) }
    var onboardingBusy by rememberSaveable { mutableStateOf(false) }
    var onboardingError by rememberSaveable { mutableStateOf<String?>(null) }
    var actionBusy by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) { state.load() }

    val snapshot = state.snapshot
    val darkTheme = when (snapshot?.preferences?.theme ?: ThemeMode.SYSTEM) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun go(next: Dest) {
        if (next.isTab) lastTabName = next.name
        destName = next.name
    }

    fun back() {
        go(Dest.valueOf(lastTabName))
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun run(success: Int?, block: suspend () -> String?) {
        if (actionBusy) return
        actionBusy = true
        scope.launch {
            try {
                val error = block()
                if (error != null) {
                    snackbar.showSnackbar(error)
                } else {
                    if (snapshot?.preferences?.haptics != false) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (success != null) snackbar.showSnackbar(context.getString(success))
                }
            } finally {
                actionBusy = false
            }
        }
    }

    fun sealText(): String? {
        val anchor = state.journal.anchor() ?: return null
        return context.getString(
            R.string.seal_share_body,
            anchor.length,
            anchor.headHash,
            anchor.merkleRoot,
            formatTimestamp(anchor.createdAt),
        )
    }

    fun shareSeal() {
        val body = sealText() ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.seal_share_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.verify_share)))
    }

    val exportRecord = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = state.exportRecord()
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(text.toByteArray(Charsets.UTF_8))
                        } ?: error("no output stream")
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

    val exportBundle = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = state.exportBundle()
                if (text == null) {
                    snackbar.showSnackbar(context.getString(R.string.settings_export_failed))
                    return@launch
                }
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(text.toByteArray(Charsets.UTF_8))
                        } ?: error("no output stream")
                    }
                }
                snackbar.showSnackbar(
                    context.getString(
                        if (written.isSuccess) R.string.settings_export_done else R.string.settings_export_failed,
                    ),
                )
            }
        }
    }

    val importBundle = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (text == null) {
                    snackbar.showSnackbar(context.getString(R.string.settings_import_failed))
                    return@launch
                }
                val error = state.importBundle(text)
                snackbar.showSnackbar(
                    error ?: context.getString(R.string.settings_import_done),
                )
            }
        }
    }

    fun capture(who: String, what: String) {
        if (capturing) return
        capturing = true
        scope.launch {
            try {
                when (val outcome = state.addRecognition(who, what)) {
                    is SaveOutcome.Saved -> {
                        if (snapshot?.preferences?.haptics != false) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        go(Dest.JOURNAL)
                        snackbar.showSnackbar(context.getString(R.string.saved_message))
                    }
                    is SaveOutcome.Failed -> snackbar.showSnackbar(outcome.reason)
                }
            } finally {
                capturing = false
            }
        }
    }

    TlcTheme(darkTheme = darkTheme) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.loading -> LoadingPane()

                state.loadError != null -> LoadErrorPane(
                    reason = state.loadError.orEmpty(),
                    onRetry = { scope.launch { state.load() } },
                    onReset = { confirmReset = true },
                )

                snapshot == null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) {
                    OnboardingScreen(
                        submitting = onboardingBusy,
                        error = onboardingError,
                        onFinish = { name, circle, role ->
                            if (onboardingBusy) return@OnboardingScreen
                            scope.launch {
                                onboardingBusy = true
                                onboardingError = null
                                val error = state.onboard(name, circle, role)
                                onboardingBusy = false
                                if (error != null) {
                                    onboardingError = error
                                } else if (state.snapshot?.preferences?.haptics != false) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                    )
                }

                else -> {
                    val sealed = snapshot
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbar) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = dest.title(),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                },
                                navigationIcon = {
                                    if (!dest.isTab) {
                                        IconButton(onClick = { back() }) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_back),
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    if (dest.isTab) {
                                        SealPill(
                                            healthy = state.healthy,
                                            label = sealLabel(state.status, state.entries.size, state.healthy),
                                            modifier = Modifier.padding(end = 12.dp),
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                ),
                            )
                        },
                        bottomBar = {
                            if (dest.isTab) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                ) {
                                    TlcNavItem(
                                        selected = dest == Dest.TODAY,
                                        label = stringResource(R.string.nav_today),
                                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                        onClick = { go(Dest.TODAY) },
                                    )
                                    TlcNavItem(
                                        selected = dest == Dest.JOURNAL,
                                        label = stringResource(R.string.nav_journal),
                                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                                        onClick = { go(Dest.JOURNAL) },
                                    )
                                    TlcNavItem(
                                        selected = dest == Dest.GRANTS,
                                        label = stringResource(R.string.nav_grants),
                                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                                        onClick = { go(Dest.GRANTS) },
                                    )
                                    TlcNavItem(
                                        selected = dest == Dest.CIRCLE,
                                        label = stringResource(R.string.nav_circle),
                                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                        onClick = { go(Dest.CIRCLE) },
                                    )
                                    TlcNavItem(
                                        selected = dest == Dest.SETTINGS,
                                        label = stringResource(R.string.nav_settings),
                                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                        onClick = { go(Dest.SETTINGS) },
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            if (dest == Dest.JOURNAL) {
                                ExtendedFloatingActionButton(
                                    onClick = { go(Dest.CAPTURE) },
                                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.journal_add)) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        },
                    ) { inner ->
                        Box(modifier = Modifier.padding(inner)) {
                            AnimatedContent(
                                targetState = dest,
                                transitionSpec = {
                                    val forward = targetState.ordinal > initialState.ordinal
                                    (
                                        fadeIn(tween(180)) +
                                            slideInHorizontally(tween(220)) { if (forward) it / 10 else -it / 10 }
                                        ) togetherWith (
                                        fadeOut(tween(120)) +
                                            slideOutHorizontally(tween(160)) { if (forward) -it / 10 else it / 10 }
                                        )
                                },
                                label = "screen",
                            ) { target ->
                                when (target) {
                                    Dest.TODAY -> TodayScreen(
                                        snapshot = sealed,
                                        entryCount = state.entries.size,
                                        headHash = state.headHash,
                                        healthy = state.healthy,
                                        sealLabel = sealLabel(state.status, state.entries.size, state.healthy),
                                        onRecognise = { go(Dest.CAPTURE) },
                                        onPledge = { go(Dest.GRANTS) },
                                        onRecordDue = { go(Dest.DUES) },
                                        onJournal = { go(Dest.JOURNAL) },
                                        onCircle = { go(Dest.CIRCLE) },
                                        onGrants = { go(Dest.GRANTS) },
                                        onDues = { go(Dest.DUES) },
                                        onVerify = { go(Dest.VERIFY) },
                                        onCheckpoint = { go(Dest.SETTINGS) },
                                        onAbout = { go(Dest.ABOUT) },
                                    )

                                    Dest.JOURNAL -> JournalScreen(
                                        entries = state.entries,
                                        headHash = state.headHash,
                                        healthy = state.healthy,
                                        sealLabel = sealLabel(state.status, state.entries.size, state.healthy),
                                        blockedReason = state.blockedReason,
                                        onEntry = { selected = it },
                                    )

                                    Dest.GRANTS -> GrantsScreen(
                                        grants = sealed.grants,
                                        busy = state.busy || actionBusy,
                                        blockedReason = state.blockedReason,
                                        onCreate = { beneficiary, purpose, amountKobo ->
                                            run(R.string.grants_created) {
                                                state.addGrant(beneficiary, purpose, amountKobo)
                                            }
                                        },
                                        onAdvance = { id, event ->
                                            val message = when (event) {
                                                is GrantEvent.AttachEvidence -> R.string.grants_attach_done
                                                is GrantEvent.Verify -> R.string.grants_verify_done
                                                is GrantEvent.RecordPayment -> R.string.grants_pay_done
                                                is GrantEvent.Withdraw -> R.string.grants_withdraw_done
                                            }
                                            run(message) { state.advanceGrant(id, event) }
                                        },
                                    )

                                    Dest.CIRCLE -> CircleScreen(
                                        snapshot = sealed,
                                        busy = state.busy || actionBusy,
                                        blockedReason = state.blockedReason,
                                        onAddCoSteward = { name ->
                                            run(R.string.circle_add_co_done) { state.addCoSteward(name) }
                                        },
                                        onPropose = { action, summary ->
                                            run(R.string.circle_decision_opened) { state.propose(action, summary) }
                                        },
                                        onApprove = { id, actor ->
                                            run(R.string.circle_signed) { state.approve(id, actor) }
                                        },
                                        onVeto = { id, actor ->
                                            run(R.string.circle_vetoed_done) { state.veto(id, actor) }
                                        },
                                    )

                                    Dest.SETTINGS -> SettingsScreen(
                                        preferences = sealed.preferences,
                                        versionName = culture.thelove.tlc.android.BuildConfig.VERSION_NAME,
                                        versionCode = culture.thelove.tlc.android.BuildConfig.VERSION_CODE,
                                        busy = state.busy || actionBusy,
                                        onPreferences = { next ->
                                            scope.launch {
                                                val error = state.updatePreferences(next)
                                                if (error != null) snackbar.showSnackbar(error)
                                            }
                                        },
                                        onExportRecord = {
                                            exportRecord.launch(
                                                context.getString(
                                                    R.string.settings_record_file_name,
                                                    dayStamp(),
                                                ),
                                            )
                                        },
                                        onExportBundle = {
                                            exportBundle.launch(
                                                context.getString(
                                                    R.string.settings_bundle_file_name,
                                                    dayStamp(),
                                                ),
                                            )
                                        },
                                        onImportBundle = { confirmImport = true },
                                        onShareSeal = { shareSeal() },
                                        onVerify = { go(Dest.VERIFY) },
                                        onAbout = { go(Dest.ABOUT) },
                                    )

                                    Dest.CAPTURE -> CaptureScreen(
                                        saving = capturing,
                                        blockedReason = state.blockedReason,
                                        onSave = { who, what -> capture(who, what) },
                                        onCancel = { back() },
                                    )

                                    Dest.DUES -> DuesScreen(
                                        dues = sealed.dues,
                                        hasDyad = sealed.hasDyad,
                                        busy = state.busy || actionBusy,
                                        blockedReason = state.blockedReason,
                                        onRecordDue = { kind, amountKobo, period ->
                                            run(R.string.dues_created) {
                                                state.addDue(kind, amountKobo, period)
                                            }
                                        },
                                        onIssue = { id ->
                                            run(R.string.dues_issue_done) { state.issueInstruction(id) }
                                        },
                                        onProof = { id, amountKobo, reference ->
                                            run(R.string.dues_proof_done) {
                                                state.recordProof(id, amountKobo, reference)
                                            }
                                        },
                                        onOpenCircle = { go(Dest.CIRCLE) },
                                    )

                                    Dest.VERIFY -> VerifyScreen(
                                        entryCount = state.entries.size,
                                        headHash = state.headHash,
                                        merkleRoot = state.merkleRoot,
                                        healthy = state.healthy,
                                        sealLabel = sealLabel(state.status, state.entries.size, state.healthy),
                                        report = state.report,
                                        status = state.status,
                                        onExport = {
                                            exportRecord.launch(
                                                context.getString(R.string.record_file_name, dayStamp()),
                                            )
                                        },
                                        onShareSeal = { shareSeal() },
                                    )

                                    Dest.ABOUT -> AboutScreen()
                                }
                            }
                        }
                    }

                    BackHandler(enabled = !dest.isTab || dest != Dest.TODAY) {
                        if (!dest.isTab) back() else go(Dest.TODAY)
                    }
                }
            }

            if (confirmReset) {
                AlertDialog(
                    onDismissRequest = { confirmReset = false },
                    title = { Text(stringResource(R.string.load_failed)) },
                    text = { Text(stringResource(R.string.settings_import_confirm)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmReset = false
                                scope.launch {
                                    val error = state.startFresh()
                                    if (error != null) snackbar.showSnackbar(error)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.onboard_begin))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmReset = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            if (confirmImport) {
                AlertDialog(
                    onDismissRequest = { confirmImport = false },
                    title = { Text(stringResource(R.string.settings_import_bundle)) },
                    text = { Text(stringResource(R.string.settings_import_confirm)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmImport = false
                                importBundle.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                        ) {
                            Text(stringResource(R.string.action_continue))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmImport = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            val detail = selected
            if (detail != null) {
                EntryDetailSheet(
                    entry = detail,
                    state = state.journal,
                    onDismiss = { selected = null },
                    onCopied = { notify(context.getString(R.string.detail_copied)) },
                )
            }
        }
    }
}

@Composable
private fun Dest.title(): String = stringResource(
    when (this) {
        Dest.TODAY -> R.string.app_doctrine
        Dest.JOURNAL -> R.string.nav_journal
        Dest.GRANTS -> R.string.nav_grants
        Dest.CIRCLE -> R.string.nav_circle
        Dest.SETTINGS -> R.string.nav_settings
        Dest.CAPTURE -> R.string.capture_title
        Dest.DUES -> R.string.dues_title
        Dest.VERIFY -> R.string.verify_title
        Dest.ABOUT -> R.string.about_title
    },
)

@Composable
private fun sealLabel(status: PersistentLedger.Status?, entryCount: Int, healthy: Boolean): String = when {
    status is PersistentLedger.Status.Truncated -> stringResource(R.string.status_truncated)
    status is PersistentLedger.Status.Damaged -> stringResource(R.string.status_damaged)
    status is PersistentLedger.Status.Recovered -> stringResource(R.string.status_recovered)
    entryCount == 0 && healthy -> stringResource(R.string.status_empty)
    healthy -> stringResource(R.string.status_ok)
    else -> stringResource(R.string.status_opening)
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
private fun LoadErrorPane(reason: String, onRetry: () -> Unit, onReset: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        SectionCard(title = stringResource(R.string.load_failed)) {
            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.settings_data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.retry))
                }
                Button(onClick = onReset, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.onboard_begin))
                }
            }
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
            InfoRow(
                label = stringResource(R.string.detail_kind),
                value = entry.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                mono = false,
            )
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
