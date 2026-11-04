package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.BankAccountRef
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Rejection
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * The app state file and the human-readable JSON export share one codec.
 *
 * Rules:
 *  - the format string is versioned; an unknown version is refused, never guessed at;
 *  - v1 and v2 are still read so a v0.0.5 device upgrades without losing its record; v3 is written;
 *  - every field is validated by its type's constructor, so a hand-edited file cannot produce an
 *    impossible screen;
 *  - optional collections default to empty, which makes the first launch and a future migration
 *    the same operation;
 *  - encoding is deterministic (sets are sorted), so an export can be diffed.
 *
 * A full bank account number is never encoded. State only ever holds [BankAccountRef]: bank,
 * last four digits, holder and a non-reversible fingerprint. A profile photo and a receipt are
 * file names, never bytes, so the state file stays small and readable.
 */
public sealed interface StateResult {
    public data class Ok(public val snapshot: AppSnapshot) : StateResult

    public data class Invalid(public val reason: String) : StateResult
}

public object StateCodec {

    public const val FORMAT: String = "tlc-state-v3"
    public const val PREVIOUS_FORMAT: String = "tlc-state-v2"
    public const val V1_FORMAT: String = "tlc-state-v1"
    public const val MAX_CHARS: Int = 1_000_000

    private val READ_FORMATS: Set<String> = setOf(V1_FORMAT, PREVIOUS_FORMAT, FORMAT)

    public fun encode(snapshot: AppSnapshot): String = buildJsonObject {
        put("format", FORMAT)
        put("profile", buildJsonObject {
            put("display_name", snapshot.profile.displayName)
            put("circle_name", snapshot.profile.circleName)
            put("role", snapshot.profile.role.name)
            put("record_id", snapshot.profile.recordId)
            put("created_at", snapshot.profile.createdAt)
            putNullable("handle", snapshot.profile.handle?.value)
            putNullable("photo_ref", snapshot.profile.photoRef)
        })
        put("preferences", buildJsonObject {
            put("theme", snapshot.preferences.theme.name)
            put("motion", snapshot.preferences.motion)
            put("haptics", snapshot.preferences.haptics)
            put("receptive_mode", snapshot.preferences.receptiveMode)
            put("onboarded", snapshot.preferences.onboarded)
            put("reveal_seconds", snapshot.preferences.revealSeconds)
            put("follow_up_reminders", snapshot.preferences.followUpReminders)
        })
        put("stewards", buildJsonArray {
            snapshot.stewards.forEach { steward ->
                add(buildJsonObject {
                    put("id", steward.id)
                    put("name", steward.name)
                    put("role", steward.role.name)
                    putNullable("handle", steward.handle?.value)
                })
            }
        })
        put("proposals", buildJsonArray {
            snapshot.proposals.forEach { proposal ->
                add(buildJsonObject {
                    put("id", proposal.id)
                    put("action", proposal.action.name)
                    put("summary", proposal.summary)
                    put("created_at", proposal.createdAt)
                    put("approvals", buildJsonArray { proposal.approvals.sorted().forEach { add(it) } })
                    put("vetoes", buildJsonArray { proposal.vetoes.sorted().forEach { add(it) } })
                    put("closed", proposal.closed)
                })
            }
        })
        put("grants", buildJsonArray {
            snapshot.grants.forEach { grant ->
                add(buildJsonObject {
                    put("id", grant.id)
                    put("beneficiary", grant.beneficiary)
                    put("purpose", grant.purpose)
                    putNullableNumber("amount_kobo", grant.amountKobo)
                    put("pledged_at", grant.pledgedAt)
                    put("stage", grant.stage.name)
                    putNullable("evidence", grant.evidence)
                    putNullableNumber("verified_at", grant.verifiedAt)
                    putNullable("witness", grant.witness)
                    putNullableNumber("paid_at", grant.paidAt)
                    putNullable("bank_reference", grant.bankReference)
                    putNullableNumber("withdrawn_at", grant.withdrawnAt)
                    putNullable("withdrawn_reason", grant.withdrawnReason)
                })
            }
        })
        put("dues", buildJsonArray {
            snapshot.dues.forEach { due ->
                add(buildJsonObject {
                    put("id", due.id)
                    put("kind", due.kind.name)
                    put("amount_kobo", due.amountKobo)
                    put("period", due.period.name)
                    put("members", buildJsonArray { due.members.sorted().forEach { add(it) } })
                    put("state", due.state.name)
                    put("created_at", due.createdAt)
                    putNullableNumber("instruction_issued_at", due.instructionIssuedAt)
                    putNullableNumber("instruction_expires_at", due.instructionExpiresAt)
                    putNullable("proof_hash", due.proofHash)
                    putNullableNumber("stated_amount_kobo", due.statedAmountKobo)
                    putNullable("bank_reference", due.bankReference)
                    putNullable("submitted_by", due.submittedBy)
                    putNullableNumber("proof_at", due.proofAt)
                    putNullable("attested_by", due.attestedBy)
                    putNullable("second_attestor", due.secondAttestor)
                    putNullable("statement_ref", due.statementRef)
                    putNullableNumber("attested_at", due.attestedAt)
                    putNullable("waived_by", due.waivedBy)
                    putNullable("waiver_note", due.waiverNote)
                    putNullableNumber("waived_at", due.waivedAt)
                    putNullable("exception", due.exception?.name)
                    put("revision", due.revision)
                })
            }
        })
        put("people", buildJsonArray {
            snapshot.people.forEach { person ->
                add(buildJsonObject {
                    put("id", person.id)
                    put("handle", person.handle.value)
                    put("display_name", person.displayName)
                    put("role", person.role.name)
                    putNullable("bank_ref_id", person.bankRefId)
                    put("receptive", person.receptive)
                    put("created_at", person.createdAt)
                })
            }
        })
        put("bank_accounts", buildJsonArray {
            snapshot.bankAccounts.forEach { account ->
                add(buildJsonObject {
                    put("id", account.id)
                    put("bank_code", account.bankCode)
                    put("bank_name", account.bankName)
                    put("last4", account.last4)
                    put("holder_name", account.holderName)
                    put("fingerprint", account.fingerprint)
                    put("saved_at", account.savedAt)
                })
            }
        })
        put("contact_log", buildJsonArray {
            snapshot.contactLog.forEach { event ->
                add(buildJsonObject {
                    put("id", event.id)
                    put("channel", event.channel.name)
                    put("purpose", event.purpose.name)
                    putNullable("category", event.category)
                    put("at", event.at)
                    putNullable("token_ref", event.tokenRef)
                })
            }
        })
        put("supports", buildJsonArray {
            snapshot.supports.forEach { support ->
                add(buildJsonObject {
                    put("id", support.id)
                    put("to_handle", support.toHandle.value)
                    put("kind", support.kind.name)
                    putNullableNumber("amount_kobo", support.amountKobo)
                    put("state", support.state.name)
                    put("created_at", support.createdAt)
                    putNullable("reference", support.reference)
                    putNullableNumber("claimed_at", support.claimedAt)
                    putNullableNumber("acknowledged_at", support.acknowledgedAt)
                })
            }
        })
        put("receipts", buildJsonArray {
            snapshot.receipts.forEach { receipt ->
                add(buildJsonObject {
                    put("id", receipt.id)
                    put("subject_kind", receipt.subjectKind.name)
                    put("subject_id", receipt.subjectId)
                    put("file_name", receipt.fileName)
                    put("hash", receipt.hash)
                    put("audience", receipt.audience.name)
                    put("added_at", receipt.addedAt)
                    putNullable("note", receipt.note)
                })
            }
        })
    }.toString()

    public fun decode(text: String): StateResult {
        if (text.length > MAX_CHARS) return StateResult.Invalid("The state file is unreasonably large.")
        return try {
            val root = Json.parseToJsonElement(text)
            if (root !is JsonObject) return StateResult.Invalid("The state file is not a JSON object.")
            decodeObject(root)
        } catch (e: Exception) {
            StateResult.Invalid(e.message ?: "The state file could not be read.")
        }
    }

    internal fun decodeObject(root: JsonObject): StateResult {
        return try {
            val format = root.string("format")
            if (format !in READ_FORMATS) return StateResult.Invalid("Unsupported state format: $format")
            val fromV1 = format == V1_FORMAT

            val profileObject = root.objectOrNull("profile")
                ?: return StateResult.Invalid("The state has no profile.")
            val recordId = profileObject.string("record_id")
            val displayName = profileObject.string("display_name")
            val storedHandle = profileObject.nullableString("handle")
            val handle = when {
                storedHandle != null -> Handle.of(storedHandle)
                fromV1 -> Handle.suggest(displayName, recordId)
                else -> null
            }
            val profile = Profile(
                displayName = displayName,
                circleName = profileObject.stringOr("circle_name", ""),
                role = profileObject.enum("role", Role.values()),
                recordId = recordId,
                createdAt = profileObject.long("created_at"),
                handle = handle,
                photoRef = profileObject.nullableString("photo_ref"),
            )

            val preferences = root.objectOrNull("preferences")?.let { objectValue ->
                Preferences(
                    theme = objectValue.enumOr("theme", ThemeMode.values(), ThemeMode.SYSTEM),
                    motion = objectValue.booleanOr("motion", true),
                    haptics = objectValue.booleanOr("haptics", true),
                    receptiveMode = objectValue.booleanOr("receptive_mode", false),
                    onboarded = objectValue.booleanOr("onboarded", false),
                    revealSeconds = objectValue.longOr("reveal_seconds", 60).toInt(),
                    followUpReminders = objectValue.booleanOr("follow_up_reminders", true),
                )
            } ?: Preferences()

            val stewards = root.arrayOrEmpty("stewards").map { element ->
                val item = element.jsonObject
                Steward(
                    id = item.string("id"),
                    name = item.stringOr("name", ""),
                    role = item.enum("role", StewardRole.values()),
                    handle = item.nullableString("handle")?.let { Handle.of(it) },
                )
            }

            val proposals = root.arrayOrEmpty("proposals").map { element ->
                val item = element.jsonObject
                Proposal(
                    id = item.string("id"),
                    action = item.enum("action", SensitiveAction.values()),
                    summary = item.string("summary"),
                    createdAt = item.long("created_at"),
                    approvals = item.stringSet("approvals"),
                    vetoes = item.stringSet("vetoes"),
                    closed = item.booleanOr("closed", false),
                )
            }

            val grants = root.arrayOrEmpty("grants").map { element ->
                val item = element.jsonObject
                GrowthGrant(
                    id = item.string("id"),
                    beneficiary = item.string("beneficiary"),
                    purpose = item.string("purpose"),
                    amountKobo = item.nullableLong("amount_kobo"),
                    pledgedAt = item.long("pledged_at"),
                    stage = item.enum("stage", GrantStage.values()),
                    evidence = item.nullableString("evidence"),
                    verifiedAt = item.nullableLong("verified_at"),
                    witness = item.nullableString("witness"),
                    paidAt = item.nullableLong("paid_at"),
                    bankReference = item.nullableString("bank_reference"),
                    withdrawnAt = item.nullableLong("withdrawn_at"),
                    withdrawnReason = item.nullableString("withdrawn_reason"),
                )
            }

            val dues = root.arrayOrEmpty("dues").map { element ->
                val item = element.jsonObject
                DueRecord(
                    id = item.string("id"),
                    kind = item.enum("kind", TokenKind.values()),
                    amountKobo = item.long("amount_kobo"),
                    period = item.enum("period", FeePeriod.values()),
                    members = item.stringSet("members").sorted(),
                    state = item.enum("state", ObligationState.values()),
                    createdAt = item.long("created_at"),
                    instructionIssuedAt = item.nullableLong("instruction_issued_at"),
                    instructionExpiresAt = item.nullableLong("instruction_expires_at"),
                    proofHash = item.nullableString("proof_hash"),
                    statedAmountKobo = item.nullableLong("stated_amount_kobo"),
                    bankReference = item.nullableString("bank_reference"),
                    submittedBy = item.nullableString("submitted_by"),
                    proofAt = item.nullableLong("proof_at"),
                    attestedBy = item.nullableString("attested_by"),
                    secondAttestor = item.nullableString("second_attestor"),
                    statementRef = item.nullableString("statement_ref"),
                    attestedAt = item.nullableLong("attested_at"),
                    waivedBy = item.nullableString("waived_by"),
                    waiverNote = item.nullableString("waiver_note"),
                    waivedAt = item.nullableLong("waived_at"),
                    exception = item.nullableString("exception")?.let { name ->
                        Rejection.values().firstOrNull { it.name == name }
                            ?: return StateResult.Invalid("Unknown exception code: $name")
                    },
                    revision = item.longOr("revision", 0).toInt(),
                )
            }

            val people = root.arrayOrEmpty("people").map { element ->
                val item = element.jsonObject
                Person(
                    id = item.string("id"),
                    handle = Handle.of(item.string("handle")),
                    displayName = item.string("display_name"),
                    role = item.enum("role", Role.values()),
                    bankRefId = item.nullableString("bank_ref_id"),
                    receptive = item.booleanOr("receptive", false),
                    createdAt = item.long("created_at"),
                )
            }

            val bankAccounts = root.arrayOrEmpty("bank_accounts").map { element ->
                val item = element.jsonObject
                BankAccountRef(
                    id = item.string("id"),
                    bankCode = item.string("bank_code"),
                    bankName = item.string("bank_name"),
                    last4 = item.string("last4"),
                    holderName = item.string("holder_name"),
                    fingerprint = item.string("fingerprint"),
                    savedAt = item.long("saved_at"),
                )
            }

            val contactLog = root.arrayOrEmpty("contact_log").map { element ->
                val item = element.jsonObject
                ContactEvent(
                    id = item.string("id"),
                    channel = item.enum("channel", ContactChannel.values()),
                    purpose = item.enum("purpose", ContactPurpose.values()),
                    category = item.nullableString("category"),
                    at = item.long("at"),
                    tokenRef = item.nullableString("token_ref"),
                )
            }

            val supports = root.arrayOrEmpty("supports").map { element ->
                val item = element.jsonObject
                SupportIntent(
                    id = item.string("id"),
                    toHandle = Handle.of(item.string("to_handle")),
                    kind = item.enum("kind", TokenKind.values()),
                    amountKobo = item.nullableLong("amount_kobo"),
                    state = item.enum("state", SupportState.values()),
                    createdAt = item.long("created_at"),
                    reference = item.nullableString("reference"),
                    claimedAt = item.nullableLong("claimed_at"),
                    acknowledgedAt = item.nullableLong("acknowledged_at"),
                )
            }

            val receipts = root.arrayOrEmpty("receipts").map { element ->
                val item = element.jsonObject
                TokenReceipt(
                    id = item.string("id"),
                    subjectKind = item.enum("subject_kind", ReceiptSubject.values()),
                    subjectId = item.string("subject_id"),
                    fileName = item.string("file_name"),
                    hash = item.string("hash"),
                    audience = item.enum("audience", ReceiptAudience.values()),
                    addedAt = item.long("added_at"),
                    note = item.nullableString("note"),
                )
            }

            StateResult.Ok(
                AppSnapshot(
                    profile = profile,
                    preferences = preferences,
                    stewards = stewards,
                    proposals = proposals,
                    grants = grants,
                    dues = dues,
                    people = people,
                    bankAccounts = bankAccounts,
                    contactLog = contactLog,
                    supports = supports,
                    receipts = receipts,
                ),
            )
        } catch (e: Exception) {
            StateResult.Invalid(e.message ?: "The state file could not be read.")
        }
    }
}

private fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: error("Missing text field '$key'.")

private fun JsonObject.stringOr(key: String, fallback: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: fallback

private fun JsonObject.nullableString(key: String): String? =
    this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

private fun JsonObject.long(key: String): Long =
    this[key]?.jsonPrimitive?.longOrNull ?: error("Missing number field '$key'.")

private fun JsonObject.longOr(key: String, fallback: Long): Long =
    this[key]?.jsonPrimitive?.longOrNull ?: fallback

private fun JsonObject.nullableLong(key: String): Long? =
    this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull

private fun JsonObject.booleanOr(key: String, fallback: Boolean): Boolean =
    this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: fallback

private fun JsonObject.objectOrNull(key: String): JsonObject? =
    this[key]?.takeUnless { it is JsonNull }?.jsonObject

private fun JsonObject.arrayOrEmpty(key: String): JsonArray =
    this[key] as? JsonArray ?: JsonArray(emptyList())

private fun JsonObject.stringSet(key: String): Set<String> =
    arrayOrEmpty(key).map { it.jsonPrimitive.content }.toSet()

private fun <T : Enum<T>> JsonObject.enum(key: String, values: Array<T>): T {
    val name = string(key)
    return values.firstOrNull { it.name == name } ?: error("Unknown value '$name' for '$key'.")
}

private fun <T : Enum<T>> JsonObject.enumOr(key: String, values: Array<T>, fallback: T): T {
    val name = this[key]?.jsonPrimitive?.contentOrNull ?: return fallback
    return values.firstOrNull { it.name == name } ?: fallback
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?): Unit {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableNumber(key: String, value: Long?): Unit {
    if (value == null) put(key, JsonNull) else put(key, value)
}
