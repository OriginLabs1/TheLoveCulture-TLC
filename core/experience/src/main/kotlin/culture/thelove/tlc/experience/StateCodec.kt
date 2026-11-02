package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Rejection
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The app state file and the human-readable JSON export share one codec.
 *
 * Rules:
 *  - the format string is versioned; an unknown version is refused, never guessed at;
 *  - every field is validated by its type's constructor, so a hand-edited file cannot produce an
 *    impossible screen;
 *  - optional collections default to empty, which makes the first launch and a future migration
 *    the same operation;
 *  - encoding is deterministic (sets are sorted), so an export can be diffed.
 */
public sealed interface StateResult {
    public data class Ok(public val snapshot: AppSnapshot) : StateResult

    public data class Invalid(public val reason: String) : StateResult
}

public object StateCodec {

    public const val FORMAT: String = "tlc-state-v1"
    public const val MAX_CHARS: Int = 1_000_000

    public fun encode(snapshot: AppSnapshot): String = buildJsonObject {
        put("format", FORMAT)
        put("profile", buildJsonObject {
            put("display_name", snapshot.profile.displayName)
            put("circle_name", snapshot.profile.circleName)
            put("role", snapshot.profile.role.name)
            put("record_id", snapshot.profile.recordId)
            put("created_at", snapshot.profile.createdAt)
        })
        put("preferences", buildJsonObject {
            put("theme", snapshot.preferences.theme.name)
            put("motion", snapshot.preferences.motion)
            put("haptics", snapshot.preferences.haptics)
            put("receptive_mode", snapshot.preferences.receptiveMode)
            put("onboarded", snapshot.preferences.onboarded)
        })
        put("stewards", buildJsonArray {
            snapshot.stewards.forEach { steward ->
                add(buildJsonObject {
                    put("id", steward.id)
                    put("name", steward.name)
                    put("role", steward.role.name)
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
        if (format != FORMAT) return StateResult.Invalid("Unsupported state format: $format")

        val profileObject = root.objectOrNull("profile") ?: return StateResult.Invalid("The state has no profile.")
        val profile = Profile(
            displayName = profileObject.string("display_name"),
            circleName = profileObject.stringOr("circle_name", ""),
            role = profileObject.enum("role", Role.values()),
            recordId = profileObject.string("record_id"),
            createdAt = profileObject.long("created_at"),
        )

        val preferences = root.objectOrNull("preferences")?.let { objectValue ->
            Preferences(
                theme = objectValue.enumOr("theme", ThemeMode.values(), ThemeMode.SYSTEM),
                motion = objectValue.booleanOr("motion", true),
                haptics = objectValue.booleanOr("haptics", true),
                receptiveMode = objectValue.booleanOr("receptive_mode", false),
                onboarded = objectValue.booleanOr("onboarded", false),
            )
        } ?: Preferences()

        val stewards = root.arrayOrEmpty("stewards").map { element ->
            val item = element.jsonObject
            Steward(
                id = item.string("id"),
                name = item.stringOr("name", ""),
                role = item.enum("role", StewardRole.values()),
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

        StateResult.Ok(
            AppSnapshot(
                profile = profile,
                preferences = preferences,
                stewards = stewards,
                proposals = proposals,
                grants = grants,
                dues = dues,
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
