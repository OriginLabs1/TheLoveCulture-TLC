package culture.thelove.tlc.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import culture.thelove.tlc.android.BuildConfig
import culture.thelove.tlc.shared.PresenceProjection
import culture.thelove.tlc.shared.SealedMessage
import culture.thelove.tlc.shared.SharedCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.PrivateKey
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The honest state of the live connection. "Live" is never shown while the socket is down. */
public enum class SharedStatus {
    OFFLINE,
    CONNECTING,
    LIVE,
    ERROR,
}

public enum class SharedMessageState {
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

public data class SharedContact(
    public val handle: String,
    public val display: String,
    public val state: String,
    public val signingKey: String? = null,
    public val messagingKey: String? = null,
    public val online: Boolean = false,
    public val projection: PresenceProjection? = null,
    public val keyChanged: Boolean = false,
    public val lastSeen: Long = 0L,
) {
    public val accepted: Boolean get() = state == "ACCEPTED"
    public val pending: Boolean get() = state == "PENDING" || state == "REQUESTED"
}

public data class SharedMessage(
    public val localId: String,
    public val serverId: String? = null,
    public val from: String,
    public val to: String,
    public val text: String,
    public val at: Long,
    public val state: SharedMessageState,
    public val mine: Boolean,
    public val verified: Boolean = true,
)

public data class SharedRequest(
    public val handle: String,
    public val display: String,
    public val incoming: Boolean,
)

private data class PendingEnvelope(
    public val localId: String,
    public val clientId: String,
    public val to: String,
    public val conversation: String,
    public val aad: String,
    public val ephemeral: String,
    public val nonce: String,
    public val ciphertext: String,
    public val signature: String,
    public val text: String,
    public val at: Long,
)

/**
 * The live shared plane.
 *
 * It is deliberately a separate layer from the private record: connecting never makes the ledger,
 * vault, drafts, photos or receipts wait for the network. The relay carries handle/public-key
 * directory entries, consent records, connection state and opaque message envelopes. Local message
 * copies live in app-private storage; private keys are wrapped by an Android Keystore AES key and
 * never leave the device.
 */
public class SharedPlane(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("tlc.shared", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private val sockets = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    public var status: SharedStatus by mutableStateOf(SharedStatus.OFFLINE)
        private set
    public var error: String? by mutableStateOf(null)
        private set
    public var relayUrl: String by mutableStateOf(
        prefs.getString(KEY_RELAY, BuildConfig.TLC_RELAY_URL).orEmpty(),
    )
        private set
    public var me: String? by mutableStateOf(null)
        private set
    public var contacts: List<SharedContact> by mutableStateOf(emptyList())
        private set
    public var requests: List<SharedRequest> by mutableStateOf(emptyList())
        private set
    public var messages: Map<String, List<SharedMessage>> by mutableStateOf(emptyMap())
        private set
    public var typingFrom: String? by mutableStateOf(null)
        private set
    public var lastNotice: String? by mutableStateOf(null)
        private set

    private var socket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var manualStop = true
    private var currentHandle: String? = null
    private var currentDisplay: String = ""
    private var pendingPresence: JSONObject? = null
    private var currentPresence: PresenceSettings = PresenceSettings.PRIVATE
    private var cursor: Int = prefs.getInt(KEY_CURSOR, 0)
    private var activeChat: String? = null

    init {
        me = prefs.getString(KEY_HANDLE, null)
        loadCachedContacts()
        loadCachedMessages()
        status = SharedStatus.OFFLINE
    }

    public fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    public fun canConnect(): Boolean = status != SharedStatus.CONNECTING && status != SharedStatus.LIVE

    public fun updateRelay(url: String) {
        val clean = url.trim().trimEnd('/')
        if (!clean.startsWith("https://")) {
            error = "The relay must use https."
            return
        }
        relayUrl = clean
        prefs.edit().putString(KEY_RELAY, clean).apply()
        error = null
    }

    public fun startIfEnabled(handle: String, display: String, presence: PresenceSettings) {
        if (isEnabled()) connect(handle, display, presence)
    }

    public fun connect(handle: String, display: String, presence: PresenceSettings) {
        if (!canConnect()) return
        if (messages.isEmpty()) loadCachedMessages()
        currentHandle = handle.lowercase()
        currentDisplay = display
        currentPresence = presence
        manualStop = false
        status = SharedStatus.CONNECTING
        error = null
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        scope.launch {
            runCatching {
                ensureKeys()
                register(currentHandle.orEmpty(), currentDisplay)
                me = currentHandle
                prefs.edit().putString(KEY_HANDLE, me).apply()
                syncPresence(presence)
                refreshAll()
                openSocket()
            }.onFailure {
                status = SharedStatus.ERROR
                error = it.message ?: "The relay could not be reached."
                scheduleReconnect()
            }
        }
    }

    public fun stop() {
        manualStop = true
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        reconnectJob?.cancel()
        pingJob?.cancel()
        socket?.close(1000, "closed by the person")
        socket = null
        status = SharedStatus.OFFLINE
        typingFrom = null
    }

    /** Locking the door closes the live socket and forgets decrypted messages in memory. */
    public fun lock() {
        reconnectJob?.cancel()
        pingJob?.cancel()
        socket?.close(1000, "locked")
        socket = null
        status = SharedStatus.OFFLINE
        messages = emptyMap()
        typingFrom = null
    }

    public fun clear() {
        stop()
        prefs.edit().clear().apply()
        relayUrl = BuildConfig.TLC_RELAY_URL
        me = null
        contacts = emptyList()
        requests = emptyList()
        messages = emptyMap()
        cursor = 0
        error = null
    }

    public fun setActiveChat(handle: String?) {
        activeChat = handle?.lowercase()
        if (activeChat != null) markRead(activeChat!!)
    }

    public fun noticeSeen() {
        lastNotice = null
    }

    public fun syncPresence(settings: PresenceSettings) {
        currentPresence = settings
        val body = JSONObject()
            .put("onlineNow", settings.onlineNow.name)
            .put("lastSeen", settings.lastSeen.name)
            .put("typing", settings.typing.name)
            .put("readReceipts", settings.readReceipts.name)
            .put("invisible", settings.invisible)
            .put("exactLastSeen", settings.exactLastSeen)
            .put("selected", JSONArray(settings.selectedPeople.toList()))
            .put("hidden", JSONArray(settings.hiddenFrom.toList()))
        pendingPresence = body
        if (status != SharedStatus.LIVE) return
        scope.launch {
            runCatching { request("PUT", "/v1/presence", JSONObject().put("consent", body)) }
                .onFailure { error = it.message }
        }
    }

    public fun requestConnection(handle: String) {
        val clean = IdentityStore.normalizeUsername(handle) ?: run {
            lastNotice = "That username is not valid."
            return
        }
        scope.launch {
            runCatching {
                request("POST", "/v1/connections", JSONObject().put("handle", clean))
                lastNotice = "Request sent to @$clean."
                refreshAll()
            }.onFailure { lastNotice = it.message ?: "The request could not be sent." }
        }
    }

    public fun respondConnection(handle: String, accept: Boolean) {
        scope.launch {
            runCatching {
                request(
                    "POST",
                    "/v1/connections/respond",
                    JSONObject().put("handle", handle).put("accept", accept),
                )
                lastNotice = if (accept) "Connection accepted." else "Request declined."
                refreshAll()
            }.onFailure { lastNotice = it.message ?: "The response could not be sent." }
        }
    }

    public fun block(handle: String) {
        scope.launch {
            runCatching {
                request("POST", "/v1/connections/block", JSONObject().put("handle", handle))
                lastNotice = "Blocked @$handle."
                refreshAll()
            }.onFailure { lastNotice = it.message ?: "The block could not be sent." }
        }
    }

    public fun refresh() {
        scope.launch {
            runCatching { refreshAll() }.onFailure { lastNotice = it.message ?: "Refresh failed." }
        }
    }

    /** Ask the relay to erase this handle and every row attached to it, then clear local state. */
    public fun deleteAccount() {
        scope.launch {
            val removed = runCatching { request("DELETE", "/v1/account", null) }.isSuccess
            if (removed) {
                clear()
                lastNotice = "The relay copy of this live account was deleted."
            } else {
                lastNotice = "The relay account could not be deleted."
            }
        }
    }

    public fun sendTyping(handle: String, typing: Boolean) {
        if (status != SharedStatus.LIVE) return
        scope.launch {
            runCatching {
                request("POST", "/v1/typing", JSONObject().put("handle", handle).put("typing", typing))
            }
        }
    }

    public fun send(handle: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val target = contacts.firstOrNull { it.handle == handle && it.accepted }
        if (target == null) {
            lastNotice = "Accept the connection before sending."
            return
        }
        if (target.keyChanged) {
            lastNotice = "The key for @$handle changed. Verify it before sending."
            return
        }
        val messagingKey = target.messagingKey
        if (messagingKey.isNullOrBlank()) {
            lastNotice = "That person's key is not available yet."
            return
        }
        val sender = me ?: run {
            lastNotice = "Connect to the shared plane first."
            return
        }
        val clientId = UUID.randomUUID().toString()
        val conversation = listOf(sender, handle).sorted().joinToString("~")
        val aad = "$conversation|$sender|$handle|$clientId"
        val sealed = SharedCrypto.seal(messagingKey, clean, aad)
        val signatureData = listOf(aad, sealed.ephemeralPublicKey, sealed.nonce, sealed.ciphertext)
            .joinToString("|")
        val signature = SharedCrypto.sign(privateKey(KEY_SIGN_PRIVATE), signatureData)
        val at = System.currentTimeMillis()
        val local = SharedMessage(
            localId = clientId,
            from = sender,
            to = handle,
            text = clean,
            at = at,
            state = SharedMessageState.QUEUED,
            mine = true,
        )
        appendMessage(local)
        val pending = PendingEnvelope(
            localId = clientId,
            clientId = clientId,
            to = handle,
            conversation = conversation,
            aad = aad,
            ephemeral = sealed.ephemeralPublicKey,
            nonce = sealed.nonce,
            ciphertext = sealed.ciphertext,
            signature = signature,
            text = clean,
            at = at,
        )
        saveOutbox(outbox() + pending)
        if (status == SharedStatus.LIVE) flushOutbox()
    }

    public fun messageKeyState(message: SharedMessage): String = when (message.state) {
        SharedMessageState.QUEUED -> "Queued"
        SharedMessageState.SENT -> "Sent"
        SharedMessageState.DELIVERED -> "Delivered"
        SharedMessageState.READ -> "Read"
        SharedMessageState.FAILED -> "Failed"
    }

    // -----------------------------------------------------------------------------------------
    // Transport
    // -----------------------------------------------------------------------------------------

    private fun openSocket() {
        val token = prefs.getString(KEY_TOKEN, null) ?: run {
            status = SharedStatus.ERROR
            error = "The relay token is missing."
            return
        }
        val wsUrl = relayUrl.removePrefix("https://").removePrefix("http://")
        val request = Request.Builder()
            .url("wss://$wsUrl/v1/stream")
            .header("Authorization", "Bearer $token")
            .build()
        socket?.close(1000, "reconnecting")
        socket = sockets.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch {
                        runCatching { handleEvent(JSONObject(text)) }
                            .onFailure { lastNotice = "A live event could not be read." }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket !== webSocket) return
                    socket = null
                    pingJob?.cancel()
                    if (!manualStop) {
                        status = SharedStatus.OFFLINE
                        scheduleReconnect()
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket !== webSocket) return
                    socket = null
                    pingJob?.cancel()
                    if (!manualStop) {
                        status = SharedStatus.OFFLINE
                        scheduleReconnect()
                    }
                }
            },
        )
    }

    private fun scheduleReconnect() {
        if (manualStop || !isEnabled()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(4_000)
            if (!manualStop && status != SharedStatus.LIVE && status != SharedStatus.CONNECTING) {
                connect(
                    currentHandle ?: return@launch,
                    currentDisplay,
                    currentPresence,
                )
            }
        }
    }

    private suspend fun handleEvent(event: JSONObject) {
        when (event.optString("type")) {
            "ready" -> {
                status = SharedStatus.LIVE
                error = null
                pingJob?.cancel()
                pingJob = scope.launch {
                    while (status == SharedStatus.LIVE) {
                        delay(20_000)
                        socket?.send(JSONObject().put("type", "ping").toString())
                    }
                }
                pendingPresence?.let {
                    runCatching { request("PUT", "/v1/presence", JSONObject().put("consent", it)) }
                    pendingPresence = null
                }
                flushOutbox()
                runCatching { refreshAll() }
            }

            "presence" -> {
                val handle = event.optString("handle")
                if (handle.isNotBlank()) updatePresence(handle, event.optJSONObject("projection"))
            }

            "message" -> {
                val message = event.optJSONObject("message") ?: return
                receive(message)
            }

            "sent" -> {
                val clientId = event.optString("clientId")
                val serverId = event.optString("id")
                updateMessageState(clientId, SharedMessageState.SENT, serverId)
                removeFromOutbox(clientId)
            }

            "delivered" -> updateMessageState(event.optString("id"), SharedMessageState.DELIVERED, null)
            "read" -> updateMessageState(event.optString("id"), SharedMessageState.READ, null)

            "typing" -> {
                val handle = event.optString("handle")
                typingFrom = if (event.optBoolean("typing", false)) handle else null
                if (typingFrom != null) {
                    scope.launch {
                        delay(5_000)
                        if (typingFrom == handle) typingFrom = null
                    }
                }
            }

            "connection" -> runCatching { refreshAll() }
        }
    }

    private suspend fun receive(payload: JSONObject) {
        val from = payload.optString("from")
        val to = payload.optString("to")
        val aad = payload.optString("aad")
        if (from.isBlank() || to != me) return
        val sealed = SealedMessage(
            ephemeralPublicKey = payload.optString("ephemeralPublicKey"),
            nonce = payload.optString("nonce"),
            ciphertext = payload.optString("ciphertext"),
        )
        val signatureData = listOf(aad, sealed.ephemeralPublicKey, sealed.nonce, sealed.ciphertext)
            .joinToString("|")
        val signature = payload.optString("signature")
        val contact = ensureContactKeys(from)
        val verified = contact?.signingKey?.let {
            SharedCrypto.verify(it, signatureData, signature)
        } ?: false
        val text = SharedCrypto.open(privateKey(KEY_MESSAGE_PRIVATE), sealed, aad)
        val id = payload.optString("id")
        val seq = payload.optInt("seq", 0)
        if (seq > cursor) {
            cursor = seq
            prefs.edit().putInt(KEY_CURSOR, cursor).apply()
        }
        if (text == null) {
            ack(id, "delivered")
            return
        }
        val message = SharedMessage(
            localId = id,
            serverId = id,
            from = from,
            to = to,
            text = if (verified) text else "This message could not be verified.",
            at = payload.optLong("createdAt", System.currentTimeMillis()),
            state = SharedMessageState.DELIVERED,
            mine = false,
            verified = verified,
        )
        if (messages[from]?.any { it.localId == id } != true) appendMessage(message)
        ack(id, "delivered")
        if (activeChat == from) {
            markRead(from)
            ack(id, "read")
        }
    }

    private fun ack(id: String, kind: String) {
        if (id.isBlank()) return
        scope.launch {
            runCatching { request("POST", "/v1/messages/$id/$kind", JSONObject()) }
        }
    }

    private fun markRead(handle: String) {
        val incoming = messages[handle].orEmpty().filter { !it.mine && it.state != SharedMessageState.READ }
        if (incoming.isEmpty()) return
        incoming.forEach { ack(it.serverId ?: it.localId, "read") }
        messages = messages + (handle to messages[handle].orEmpty().map {
            if (!it.mine) it.copy(state = SharedMessageState.READ) else it
        })
        persistMessages()
    }

    private fun flushOutbox() {
        if (status != SharedStatus.LIVE) return
        scope.launch {
            for (pending in outbox()) {
                runCatching {
                    val body = JSONObject()
                        .put("to", pending.to)
                        .put("clientId", pending.clientId)
                        .put("ephemeralPublicKey", pending.ephemeral)
                        .put("nonce", pending.nonce)
                        .put("ciphertext", pending.ciphertext)
                        .put("aad", pending.aad)
                        .put("signature", pending.signature)
                    val response = request("POST", "/v1/messages", body)
                    updateMessageState(pending.clientId, SharedMessageState.SENT, response.optString("id"))
                    removeFromOutbox(pending.clientId)
                }.onFailure {
                    updateMessageState(pending.clientId, SharedMessageState.QUEUED, null)
                }
            }
        }
    }

    private suspend fun refreshAll() {
        val list = request("GET", "/v1/connections", null)
        val fresh = mutableListOf<SharedContact>()
        val remote = list.optJSONArray("connections") ?: JSONArray()
        for (index in 0 until remote.length()) {
            val item = remote.getJSONObject(index)
            val handle = item.optString("handle")
            if (handle.isBlank()) continue
            val previous = contacts.firstOrNull { it.handle == handle }
            val signing = if (item.isNull("signingKey")) null else item.optString("signingKey")
            val messaging = if (item.isNull("messagingKey")) null else item.optString("messagingKey")
            val changed = previous != null &&
                ((previous.signingKey != null && signing != null && previous.signingKey != signing) ||
                    (previous.messagingKey != null && messaging != null && previous.messagingKey != messaging))
            fresh += SharedContact(
                handle = handle,
                display = item.optString("display", handle),
                state = item.optString("state", "NONE"),
                signingKey = if (changed) previous?.signingKey else signing,
                messagingKey = if (changed) previous?.messagingKey else messaging,
                online = item.optBoolean("online", false),
                projection = previous?.projection,
                keyChanged = changed,
                lastSeen = item.optLong("lastSeen", 0L),
            )
        }
        contacts = fresh.sortedWith(compareBy({ it.state != "ACCEPTED" }, { it.display.lowercase() }))
        requests = buildList {
            contacts.filter { it.state == "PENDING" }.forEach {
                add(SharedRequest(it.handle, it.display, incoming = true))
            }
        }
        persistContacts()

        for (contact in contacts.filter { it.accepted }) {
            runCatching {
                val presence = request("GET", "/v1/presence/${contact.handle}", null)
                updatePresence(contact.handle, presence.optJSONObject("projection"))
            }
        }
        pullMessages()
    }

    private suspend fun pullMessages() {
        val response = request("GET", "/v1/messages?since=$cursor", null)
        val list = response.optJSONArray("messages") ?: JSONArray()
        for (index in 0 until list.length()) {
            receive(list.getJSONObject(index))
        }
    }

    private suspend fun register(handle: String, display: String) {
        val token = prefs.getString(KEY_TOKEN, null) ?: newToken().also {
            prefs.edit().putString(KEY_TOKEN, it).apply()
        }
        val body = JSONObject()
            .put("handle", handle)
            .put("display", display)
            .put("token", token)
            .put("signingKey", publicKey(KEY_SIGN_PUBLIC))
            .put("messagingKey", publicKey(KEY_MESSAGE_PUBLIC))
        val response = request("POST", "/v1/accounts", body, authenticated = false)
        if (response.has("error")) error = response.optString("error")
    }

    private suspend fun ensureContactKeys(handle: String): SharedContact? {
        val existing = contacts.firstOrNull { it.handle == handle }
        if (existing?.signingKey != null) return existing
        return runCatching {
            val directory = request("GET", "/v1/directory/$handle", null)
            val signing = directory.optString("signingKey")
            val messaging = directory.optString("messagingKey")
            val updated = (existing ?: SharedContact(handle, handle, "NONE")).copy(
                signingKey = signing,
                messagingKey = messaging,
            )
            contacts = contacts.map { if (it.handle == handle) updated else it } +
                if (contacts.none { it.handle == handle }) listOf(updated) else emptyList()
            persistContacts()
            updated
        }.getOrNull()
    }

    private fun updatePresence(handle: String, projection: JSONObject?) {
        if (projection == null) return
        contacts = contacts.map {
            if (it.handle != handle) it else it.copy(
                projection = PresenceProjection(
                    onlineNow = if (projection.isNull("onlineNow")) null else projection.optBoolean("onlineNow"),
                    lastSeen = if (projection.isNull("lastSeen")) null else projection.optLong("lastSeen"),
                    lastSeenCoarse = projection.optBoolean("lastSeenCoarse"),
                    typing = if (projection.isNull("typing")) null else projection.optBoolean("typing"),
                    readReceipts = if (projection.isNull("readReceipts")) null else projection.optBoolean("readReceipts"),
                ),
                online = projection.optBoolean("onlineNow", false),
            )
        }
        persistContacts()
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        authenticated: Boolean = true,
    ): JSONObject {
        val builder = Request.Builder().url(relayUrl + path)
        if (authenticated) {
            val token = prefs.getString(KEY_TOKEN, null)
                ?: throw IllegalStateException("Join the shared plane first.")
            builder.header("Authorization", "Bearer $token")
        }
        val payload = body?.toString()?.toRequestBody(JSON)
        when (method) {
            "GET" -> builder.get()
            "PUT" -> builder.put(payload ?: "{}".toRequestBody(JSON))
            else -> builder.post(payload ?: "{}".toRequestBody(JSON))
        }
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    when (json.optString("error")) {
                        "handle_taken" -> "That username is already registered on this relay."
                        "keys_already_registered" -> "This username is registered with different keys."
                        "not_connected" -> "Accept the connection before sending."
                        "rate_limited" -> "Too many messages just now."
                        else -> "The relay answered ${response.code}."
                    },
                )
            }
            return json
        }
    }

    // -----------------------------------------------------------------------------------------
    // Local persistence and keys
    // -----------------------------------------------------------------------------------------

    private fun ensureKeys() {
        if (prefs.getString(KEY_SIGN_PRIVATE, null) == null) {
            val signing = SharedCrypto.generateKeyPair()
            val messaging = SharedCrypto.generateKeyPair()
            prefs.edit()
                .putString(KEY_SIGN_PRIVATE, wrap(SharedCrypto.encodePrivate(signing.private)))
                .putString(KEY_SIGN_PUBLIC, SharedCrypto.encodePublic(signing.public))
                .putString(KEY_MESSAGE_PRIVATE, wrap(SharedCrypto.encodePrivate(messaging.private)))
                .putString(KEY_MESSAGE_PUBLIC, SharedCrypto.encodePublic(messaging.public))
                .putString(KEY_TOKEN, newToken())
                .apply()
        }
    }

    private fun publicKey(name: String): String = prefs.getString(name, null).orEmpty()

    private fun privateKey(name: String): PrivateKey {
        val wrapped = prefs.getString(name, null) ?: throw IllegalStateException("The device key is missing.")
        return SharedCrypto.decodePrivate(unwrap(wrapped))
    }

    private fun newToken(): String {
        val bytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun wrap(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun unwrap(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun wrappingKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_WRAP, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_WRAP,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun appendMessage(message: SharedMessage) {
        val list = messages[message.from.takeIf { !message.mine } ?: message.to].orEmpty()
        messages = messages + ((message.from.takeIf { !message.mine } ?: message.to) to (list + message))
        persistMessages()
    }

    private fun updateMessageState(id: String, state: SharedMessageState, serverId: String?) {
        messages = messages.mapValues { (_, list) ->
            list.map {
                if (it.localId == id || (serverId != null && it.serverId == serverId)) {
                    it.copy(state = state, serverId = serverId ?: it.serverId)
                } else {
                    it
                }
            }
        }
        persistMessages()
    }

    private fun persistMessages() {
        val root = JSONObject()
        for ((handle, list) in messages) {
            val array = JSONArray()
            list.takeLast(200).forEach { message ->
                array.put(
                    JSONObject()
                        .put("localId", message.localId)
                        .put("serverId", message.serverId)
                        .put("from", message.from)
                        .put("to", message.to)
                        .put("text", message.text)
                        .put("at", message.at)
                        .put("state", message.state.name)
                        .put("mine", message.mine)
                        .put("verified", message.verified),
                )
            }
            root.put(handle, array)
        }
        prefs.edit().putString(KEY_MESSAGES, root.toString()).apply()
    }

    private fun loadCachedMessages() {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return
        runCatching {
            val root = JSONObject(raw)
            val loaded = mutableMapOf<String, List<SharedMessage>>()
            root.keys().forEach { handle ->
                val array = root.optJSONArray(handle) ?: return@forEach
                loaded[handle] = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(
                            SharedMessage(
                                localId = item.optString("localId"),
                                serverId = item.optString("serverId").ifBlank { null },
                                from = item.optString("from"),
                                to = item.optString("to"),
                                text = item.optString("text"),
                                at = item.optLong("at"),
                                state = runCatching {
                                    SharedMessageState.valueOf(item.optString("state"))
                                }.getOrDefault(SharedMessageState.SENT),
                                mine = item.optBoolean("mine"),
                                verified = item.optBoolean("verified", true),
                            ),
                        )
                    }
                }
            }
            messages = loaded
        }
    }

    private fun persistContacts() {
        val array = JSONArray()
        contacts.forEach { contact ->
            array.put(
                JSONObject()
                    .put("handle", contact.handle)
                    .put("display", contact.display)
                    .put("state", contact.state)
                    .put("signingKey", contact.signingKey)
                    .put("messagingKey", contact.messagingKey)
                    .put("keyChanged", contact.keyChanged)
                    .put("lastSeen", contact.lastSeen),
            )
        }
        prefs.edit().putString(KEY_CONTACTS, array.toString()).apply()
    }

    private fun loadCachedContacts() {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            contacts = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        SharedContact(
                            handle = item.optString("handle"),
                            display = item.optString("display"),
                            state = item.optString("state"),
                            signingKey = item.optString("signingKey").ifBlank { null },
                            messagingKey = item.optString("messagingKey").ifBlank { null },
                            keyChanged = item.optBoolean("keyChanged"),
                            lastSeen = item.optLong("lastSeen"),
                        ),
                    )
                }
            }
        }
    }

    private fun outbox(): List<PendingEnvelope> {
        val raw = prefs.getString(KEY_OUTBOX, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        PendingEnvelope(
                            localId = item.optString("localId"),
                            clientId = item.optString("clientId"),
                            to = item.optString("to"),
                            conversation = item.optString("conversation"),
                            aad = item.optString("aad"),
                            ephemeral = item.optString("ephemeral"),
                            nonce = item.optString("nonce"),
                            ciphertext = item.optString("ciphertext"),
                            signature = item.optString("signature"),
                            text = item.optString("text"),
                            at = item.optLong("at"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveOutbox(items: List<PendingEnvelope>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("localId", item.localId)
                    .put("clientId", item.clientId)
                    .put("to", item.to)
                    .put("conversation", item.conversation)
                    .put("aad", item.aad)
                    .put("ephemeral", item.ephemeral)
                    .put("nonce", item.nonce)
                    .put("ciphertext", item.ciphertext)
                    .put("signature", item.signature)
                    .put("text", item.text)
                    .put("at", item.at),
            )
        }
        prefs.edit().putString(KEY_OUTBOX, array.toString()).apply()
    }

    private fun removeFromOutbox(clientId: String) {
        saveOutbox(outbox().filterNot { it.clientId == clientId })
    }

    private companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val KEY_WRAP = "tlc.shared.wrap.v1"
        private const val KEY_RELAY = "relay"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HANDLE = "handle"
        private const val KEY_TOKEN = "token"
        private const val KEY_SIGN_PRIVATE = "sign.private"
        private const val KEY_SIGN_PUBLIC = "sign.public"
        private const val KEY_MESSAGE_PRIVATE = "message.private"
        private const val KEY_MESSAGE_PUBLIC = "message.public"
        private const val KEY_CURSOR = "cursor"
        private const val KEY_MESSAGES = "messages"
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_OUTBOX = "outbox"
    }
}
