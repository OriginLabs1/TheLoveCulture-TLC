package culture.thelove.tlc.android.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.data.AppState
import culture.thelove.tlc.android.data.JournalState
import culture.thelove.tlc.android.data.SaveOutcome
import culture.thelove.tlc.android.ui.screens.AboutScreen
import culture.thelove.tlc.android.ui.screens.BankVaultScreen
import culture.thelove.tlc.android.ui.screens.CaptureScreen
import culture.thelove.tlc.android.ui.screens.CircleScreen
import culture.thelove.tlc.android.ui.screens.ContactScreen
import culture.thelove.tlc.android.ui.screens.DuesScreen
import culture.thelove.tlc.android.ui.screens.GrantsScreen
import culture.thelove.tlc.android.ui.screens.JournalScreen
import culture.thelove.tlc.android.ui.screens.OnboardingScreen
import culture.thelove.tlc.android.ui.screens.PeopleScreen
import culture.thelove.tlc.android.ui.screens.ProfileScreen
import culture.thelove.tlc.android.ui.screens.SettingsScreen
import culture.thelove.tlc.android.ui.screens.SupportScreen
import culture.thelove.tlc.android.ui.screens.TodayScreen
import culture.thelove.tlc.android.ui.screens.TokensScreen
import culture.thelove.tlc.android.ui.screens.VerifyScreen
import culture.thelove.tlc.android.ui.theme.TlcTheme
import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.domain.ContactIntent
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.experience.GrantEvent
import culture.thelove.tlc.experience.ReceiptAudience
import culture.thelove.tlc.experience.ThemeMode
import culture.thelove.tlc.ledger.LedgerEntry
import culture.thelove.tlc.ledger.Merkle
import culture.thelove.tlc.ledger.PersistentLedger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

public enum class Dest {
    TODAY,
    RECORD,
    PEOPLE,
    SETTINGS,
    CREATE,
    SUPPORT,
    CONTACT,
    DUES,
    BANK,
    CIRCLE,
    GRANTS,
    CAPTURE,
    VERIFY,
    ABOUT,
    TOKENS,
    PROFILE,
    ;

    public val isTab: Boolean
        get() = this == TODAY || this == RECORD || this == PEOPLE || this == SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TlcApp(filesDir: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state = remember(context) { AppState(context) }
    val mediaDir = remember(context) { File(context.filesDir, culture.thelove.tlc.android.data.MediaFiles.FOLDER) }
    val scope = rememberCoroutineScope()
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
    var processingPhoto by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var revealed by remember { mutableStateOf<BankAccount?>(null) }
    var supportFor by rememberSaveable { mutableStateOf<String?>(null) }
    val systemDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) { state.load() }

    val snapshot = state.snapshot
    LaunchedEffect(revealed) {
        val account = revealed ?: return@LaunchedEffect
        delay(((state.snapshot?.preferences?.revealSeconds ?: 60) * 1000L))
        if (revealed === account) revealed = null
    }
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

    fun act(success: Int?, block: suspend () -> String?) {
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

    fun openContact(
        channel: ContactChannel,
        purpose: ContactPurpose,
        category: String?,
        guidance: String?,
    ) {
        val profile = state.snapshot?.profile ?: return
        val contactIntent = ContactIntent(purpose, category, guidance)
        val signature = buildString {
            append("\n\n— ")
            append(profile.displayName)
            profile.handle?.let {
                append(" (")
                append(it.value)
                append(")")
            }
        }
        val message = contactIntent.opening(profile.role) + signature
        val uri = when (channel) {
            ContactChannel.TELEGRAM -> Uri.parse("https://t.me/CollinsRoyalMedium")
            ContactChannel.EMAIL -> Uri.parse(
                "mailto:tlcfounder@protonmail.com" +
                    "?subject=${Uri.encode(purpose.label)}" +
                    "&body=${Uri.encode(message)}",
            )
            ContactChannel.WEBSITE -> Uri.parse("https://sircollins.coreconcept1.net")
            ContactChannel.PHONE -> return
        }
        act(null) { state.logContact(channel, purpose, category) }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { notify(context.getString(R.string.contact_no_app)) }
    }

    fun revealAccount(refId: String) {
        scope.launch {
            val account = state.revealBankAccount(refId)
            if (account == null) {
                snackbar.showSnackbar(context.getString(R.string.vault_reveal_failed))
            } else {
                revealed = account
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

    fun shareReceipt(fileName: String) {
        val file = state.mediaFile(fileName)
        if (file == null) {
            notify(context.getString(R.string.receipt_missing))
            return
        }
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
        if (uri == null) {
            notify(context.getString(R.string.receipt_failed))
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.receipt_share_title)))
        }.onFailure { notify(context.getString(R.string.receipt_failed)) }
    }

    fun savePhoto(uri: Uri) {
        if (processingPhoto) return
        processingPhoto = true
        scope.launch {
            try {
                val error = state.setProfilePhoto(uri)
                if (error != null) {
                    snackbar.showSnackbar(error)
                } else {
                    snackbar.showSnackbar(context.getString(R.string.profile_photo_saved))
                }
            } finally {
                processingPhoto = false
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
                        go(Dest.RECORD)
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
        CompositionLocalProvider(
            LocalMotion provides (snapshot?.preferences?.motion ?: true),
            LocalMediaDir provides mediaDir,
        ) {
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
                        onFinish = { name, circle, role, handle ->
                            if (onboardingBusy) return@OnboardingScreen
                            scope.launch {
                                onboardingBusy = true
                                onboardingError = null
                                val error = state.onboard(name, circle, role, handle)
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
                                        selected = dest == Dest.RECORD,
                                        label = stringResource(R.string.nav_record),
                                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                                        onClick = { go(Dest.RECORD) },
                                    )
                                    TlcNavItem(
                                        selected = dest == Dest.PEOPLE,
                                        label = stringResource(R.string.nav_people),
                                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                        onClick = { go(Dest.PEOPLE) },
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
                            if (dest.isTab) {
                                ExtendedFloatingActionButton(
                                    onClick = { showCreate = true },
                                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.create_title)) },
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
                                        onRecord = { go(Dest.CAPTURE) },
                                        onSupport = { supportFor = null; go(Dest.SUPPORT) },
                                        onContact = { go(Dest.CONTACT) },
                                        onPioneerToken = { go(Dest.DUES) },
                                        onPeople = { go(Dest.PEOPLE) },
                                        onGrants = { go(Dest.GRANTS) },
                                        onCircle = { go(Dest.CIRCLE) },
                                        onDues = { go(Dest.DUES) },
                                        onVerify = { go(Dest.VERIFY) },
                                        onCheckpoint = { go(Dest.SETTINGS) },
                                        onAbout = { go(Dest.ABOUT) },
                                        onTokens = { go(Dest.TOKENS) },
                                        onProfile = { go(Dest.PROFILE) },
                                    )

                                    Dest.RECORD -> JournalScreen(
                                        entries = state.entries,
                                        headHash = state.headHash,
                                        healthy = state.healthy,
                                        sealLabel = sealLabel(state.status, state.entries.size, state.healthy),
                                        blockedReason = state.blockedReason,
                                        onEntry = { selected = it },
                                    )

                                    Dest.PEOPLE -> PeopleScreen(
                                        snapshot = sealed,
                                        busy = state.busy || actionBusy,
                                        onAddPerson = { handle, name, role, bankRefId ->
                                            act(R.string.people_added) {
                                                state.addPerson(handle, name, role, bankRefId)
                                            }
                                        },
                                        onAttachBank = { personId, bankRefId ->
                                            act(null) { state.attachBank(personId, bankRefId) }
                                        },
                                        onRemovePerson = { personId ->
                                            act(R.string.people_removed) { state.removePerson(personId) }
                                        },
                                        onSupport = { handle ->
                                            supportFor = handle.value
                                            go(Dest.SUPPORT)
                                        },
                                        onContact = { go(Dest.CONTACT) },
                                        onBankVault = { go(Dest.BANK) },
                                    )

                                    Dest.SUPPORT -> SupportScreen(
                                        people = sealed.people,
                                        supports = sealed.supports,
                                        busy = state.busy || actionBusy,
                                        initialHandle = supportFor?.let { Handle.parse(it) },
                                        onSend = { handle, kind, amountKobo ->
                                            act(R.string.support_sent) {
                                                state.sendSupport(handle, kind, amountKobo)
                                            }
                                        },
                                        onClaim = { id, reference ->
                                            act(R.string.support_claimed) { state.claimSupport(id, reference) }
                                        },
                                        onAcknowledge = { id ->
                                            act(R.string.support_acknowledged) { state.acknowledgeSupport(id) }
                                        },
                                    )

                                    Dest.CONTACT -> ContactScreen(
                                        role = sealed.profile.role,
                                        displayName = sealed.profile.displayName,
                                        handle = sealed.profile.handle,
                                        contactLog = sealed.contactLog,
                                        busy = state.busy || actionBusy,
                                        onOpen = { channel, purpose, category, guidance ->
                                            openContact(channel, purpose, category, guidance)
                                        },
                                        onClearLog = { act(null) { state.clearContactLog() } },
                                    )

                                    Dest.BANK -> BankVaultScreen(
                                        refs = sealed.bankAccounts,
                                        revealed = revealed,
                                        busy = state.busy || actionBusy,
                                        onAdd = { bankCode, number, holder ->
                                            act(R.string.vault_saved) {
                                                state.saveBankAccount(bankCode, number, holder)
                                            }
                                        },
                                        onRemove = { refId ->
                                            act(R.string.vault_removed) { state.removeBankAccount(refId) }
                                        },
                                        onReveal = { refId -> revealAccount(refId) },
                                        onHide = { revealed = null },
                                    )

                                    Dest.GRANTS -> GrantsScreen(
                                        grants = sealed.grants,
                                        busy = state.busy || actionBusy,
                                        blockedReason = state.blockedReason,
                                        onCreate = { beneficiary, purpose, amountKobo ->
                                            act(R.string.grants_created) {
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
                                            act(message) { state.advanceGrant(id, event) }
                                        },
                                    )

                                    Dest.CIRCLE -> CircleScreen(
                                        snapshot = sealed,
                                        busy = state.busy || actionBusy,
                                        blockedReason = state.blockedReason,
                                        onAddCoSteward = { name ->
                                            act(R.string.circle_add_co_done) { state.addCoSteward(name) }
                                        },
                                        onPropose = { action, summary ->
                                            act(R.string.circle_decision_opened) { state.propose(action, summary) }
                                        },
                                        onApprove = { id, actor ->
                                            act(R.string.circle_signed) { state.approve(id, actor) }
                                        },
                                        onVeto = { id, actor ->
                                            act(R.string.circle_vetoed_done) { state.veto(id, actor) }
                                        },
                                    )

                                    Dest.SETTINGS -> SettingsScreen(
                                        preferences = sealed.preferences,
                                        versionName = culture.thelove.tlc.android.BuildConfig.VERSION_NAME,
                                        versionCode = culture.thelove.tlc.android.BuildConfig.VERSION_CODE,
                                        busy = state.busy || actionBusy,
                                        handle = sealed.profile.handle,
                                        onPreferences = { next ->
                                            scope.launch {
                                                val error = state.updatePreferences(next)
                                                if (error != null) snackbar.showSnackbar(error)
                                            }
                                        },
                                        onPeople = { go(Dest.PEOPLE) },
                                        onBankVault = { go(Dest.BANK) },
                                        onContact = { go(Dest.CONTACT) },
                                        onCircle = { go(Dest.CIRCLE) },
                                        onGrants = { go(Dest.GRANTS) },
                                        onTokens = { go(Dest.TOKENS) },
                                        onProfile = { go(Dest.PROFILE) },
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
                                            act(R.string.dues_created) {
                                                state.addDue(kind, amountKobo, period)
                                            }
                                        },
                                        onIssue = { id ->
                                            act(R.string.dues_issue_done) { state.issueInstruction(id) }
                                        },
                                        onProof = { id, amountKobo, reference ->
                                            act(R.string.dues_proof_done) {
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

                                    Dest.TOKENS -> TokensScreen(
                                        snapshot = sealed,
                                        busy = state.busy || actionBusy,
                                        onOpenSupport = { supportFor = null; go(Dest.SUPPORT) },
                                        onOpenGrants = { go(Dest.GRANTS) },
                                        onOpenDues = { go(Dest.DUES) },
                                        onAttachReceipt = { subject, id, uri, audience ->
                                            act(R.string.receipt_saved) {
                                                state.attachReceipt(subject, id, uri, audience)
                                            }
                                        },
                                        onRemoveReceipt = { id ->
                                            act(R.string.receipt_removed) { state.removeReceipt(id) }
                                        },
                                        onSetAudience = { id, audience ->
                                            act(null) { state.setReceiptAudience(id, audience) }
                                        },
                                        onShareReceipt = { fileName -> shareReceipt(fileName) },
                                    )

                                    Dest.PROFILE -> ProfileScreen(
                                        profile = sealed.profile,
                                        busy = state.busy || actionBusy,
                                        processingPhoto = processingPhoto,
                                        onSave = { name, circle ->
                                            act(R.string.profile_saved) { state.updateProfile(name, circle) }
                                        },
                                        onPickPhoto = { uri -> savePhoto(uri) },
                                        onRemovePhoto = {
                                            act(R.string.profile_photo_removed) { state.removeProfilePhoto() }
                                        },
                                    )

                                    Dest.CREATE -> Unit
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

            if (showCreate) {
                ModalBottomSheet(
                    onDismissRequest = { showCreate = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Eyebrow(stringResource(R.string.create_eyebrow))
                        Text(
                            text = stringResource(R.string.create_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.create_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        CreateRow(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(R.string.create_recognise),
                            body = stringResource(R.string.create_recognise_body),
                            onClick = { showCreate = false; go(Dest.CAPTURE) },
                        )
                        CreateRow(
                            icon = Icons.Filled.Send,
                            title = stringResource(R.string.create_support),
                            body = stringResource(R.string.create_support_body),
                            onClick = { showCreate = false; supportFor = null; go(Dest.SUPPORT) },
                        )
                        CreateRow(
                            icon = Icons.Filled.DateRange,
                            title = stringResource(R.string.create_token),
                            body = stringResource(R.string.create_token_body),
                            onClick = { showCreate = false; go(Dest.DUES) },
                        )
                        CreateRow(
                            icon = Icons.Filled.List,
                            title = stringResource(R.string.create_tokens),
                            body = stringResource(R.string.create_tokens_body),
                            onClick = { showCreate = false; go(Dest.TOKENS) },
                        )
                        CreateRow(
                            icon = Icons.Filled.Favorite,
                            title = stringResource(R.string.create_grant),
                            body = stringResource(R.string.create_grant_body),
                            onClick = { showCreate = false; go(Dest.GRANTS) },
                        )
                        CreateRow(
                            icon = Icons.Filled.Person,
                            title = stringResource(R.string.create_person),
                            body = stringResource(R.string.create_person_body),
                            onClick = { showCreate = false; go(Dest.PEOPLE) },
                        )
                        CreateRow(
                            icon = Icons.Filled.Lock,
                            title = stringResource(R.string.create_bank),
                            body = stringResource(R.string.create_bank_body),
                            onClick = { showCreate = false; go(Dest.BANK) },
                        )
                        CreateRow(
                            icon = Icons.Filled.Info,
                            title = stringResource(R.string.create_desk),
                            body = stringResource(R.string.create_desk_body),
                            onClick = { showCreate = false; go(Dest.CONTACT) },
                        )
                    }
                }
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
}

@Composable
private fun Dest.title(): String = stringResource(
    when (this) {
        Dest.TODAY -> R.string.app_doctrine
        Dest.RECORD -> R.string.nav_record
        Dest.PEOPLE -> R.string.nav_people
        Dest.SETTINGS -> R.string.nav_settings
        Dest.CREATE -> R.string.create_title
        Dest.SUPPORT -> R.string.support_title
        Dest.CONTACT -> R.string.contact_title
        Dest.BANK -> R.string.vault_title
        Dest.GRANTS -> R.string.nav_grants
        Dest.CIRCLE -> R.string.nav_circle
        Dest.DUES -> R.string.dues_title
        Dest.CAPTURE -> R.string.capture_title
        Dest.VERIFY -> R.string.verify_title
        Dest.ABOUT -> R.string.about_title
        Dest.TOKENS -> R.string.tokens_title
        Dest.PROFILE -> R.string.profile_title
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
private fun CreateRow(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
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
