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

// ---------------------------------------------------------------------------------------------
// The shell's durable state. None of it is money, and none of it leaves the device on its own.
// Every type is small, explicit and validated at construction, so a hand-edited state file cannot
// turn into a silent, structurally impossible screen. (INV-01, INV-05)
// ---------------------------------------------------------------------------------------------

/** Who this device belongs to. [recordId] names the installation, never the person. */
public data class Profile(
    public val displayName: String,
    public val circleName: String,
    public val role: Role,
    public val recordId: String,
    public val createdAt: Long,
    public val handle: Handle? = null,
) {
    init {
        require(displayName.isNotBlank() && displayName.length <= MAX_NAME) {
            "A display name is 1–$MAX_NAME characters."
        }
        require(circleName.length <= MAX_CIRCLE) { "A circle name is at most $MAX_CIRCLE characters." }
        require(recordId.matches(RECORD_ID)) { "A record id is 32 lowercase hex characters." }
        require(createdAt > 0) { "A profile needs a real creation time." }
        require(handle == null || !Handle.isReserved(handle.value)) {
            "That handle belongs to TLC and cannot be claimed by a person."
        }
    }

    public companion object {
        public const val MAX_NAME: Int = 80
        public const val MAX_CIRCLE: Int = 80
        private val RECORD_ID: Regex = Regex("[0-9a-f]{32}")
    }
}

public enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Presentation preferences only. Changing one never changes the record. */
public data class Preferences(
    public val theme: ThemeMode = ThemeMode.SYSTEM,
    public val motion: Boolean = true,
    public val haptics: Boolean = true,
    public val receptiveMode: Boolean = false,
    public val onboarded: Boolean = false,
    public val revealSeconds: Int = 60,
    public val followUpReminders: Boolean = true,
) {
    init {
        require(revealSeconds in 15..300) { "A reveal lasts 15–300 seconds." }
    }
}

public enum class StewardRole { PRIMARY, CO }

/** A person in this space. The Co-Steward may be absent until one is named. */
public data class Steward(
    public val id: String,
    public val name: String,
    public val role: StewardRole,
    public val handle: Handle? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A steward needs a stable id." }
        require(name.length <= Profile.MAX_NAME) { "A steward name is at most ${Profile.MAX_NAME} characters." }
        require(handle == null || !Handle.isReserved(handle.value)) {
            "That handle belongs to TLC and cannot be claimed by a steward."
        }
    }
}

/** A person the circle can support. They are addressed by handle, never by a raw account number. */
public data class Person(
    public val id: String,
    public val handle: Handle,
    public val displayName: String,
    public val role: Role,
    public val bankRefId: String? = null,
    public val receptive: Boolean = false,
    public val createdAt: Long,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A person needs a stable id." }
        require(displayName.isNotBlank() && displayName.length <= Profile.MAX_NAME) {
            "A person name is 1–${Profile.MAX_NAME} characters."
        }
        require(!Handle.isReserved(handle.value)) {
            "That handle belongs to TLC and cannot be given to a person."
        }
        require(createdAt > 0) { "A person needs a real creation time." }
    }

    public val badge: String
        get() = when (role) {
            Role.SINGLE -> "Single"
            Role.I_PARTNER -> "Intentional Partner"
            Role.LOVE_MODEL -> "Love Model"
        }
}

public enum class SupportState { INTENT, CLAIMED, ACKNOWLEDGED }

/**
 * A Support offer recorded on this device. It is an intention, then a claim, then an
 * acknowledgement. It is never a balance, a payment or a settlement (INV-01).
 */
public data class SupportIntent(
    public val id: String,
    public val toHandle: Handle,
    public val kind: TokenKind,
    public val amountKobo: Long?,
    public val state: SupportState,
    public val createdAt: Long,
    public val reference: String? = null,
    public val acknowledgedAt: Long? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A support intent needs a stable id." }
        require(kind in TokenKind.community) { "Only community tokens can be sent as Support: $kind." }
        require(amountKobo == null || amountKobo > 0) { "A stated amount must be positive." }
        require(kind != TokenKind.RECOGNITION || amountKobo == null) {
            "Recognition carries no amount."
        }
        require(createdAt > 0) { "A support intent needs a real time." }
        require(reference == null || reference.length <= 120) { "A reference is at most 120 characters." }
        require(state != SupportState.ACKNOWLEDGED || acknowledgedAt != null) {
            "An acknowledged support needs the acknowledgement time."
        }
    }
}

/** A local record that contact happened. The message body is deliberately absent. */
public data class ContactEvent(
    public val id: String,
    public val channel: ContactChannel,
    public val purpose: ContactPurpose,
    public val category: String?,
    public val at: Long,
    public val tokenRef: String? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A contact event needs a stable id." }
        require(at > 0) { "A contact event needs a real time." }
        require(category == null || category.length <= 120) { "A category is at most 120 characters." }
        require(tokenRef == null || tokenRef.length <= 64) { "A token reference is at most 64 characters." }
    }
}

/** One irreversible action waiting for two signatures, or stopped by either one. */
public data class Proposal(
    public val id: String,
    public val action: SensitiveAction,
    public val summary: String,
    public val createdAt: Long,
    public val approvals: Set<String> = emptySet(),
    public val vetoes: Set<String> = emptySet(),
    public val closed: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "A proposal needs an id." }
        require(summary.isNotBlank() && summary.length <= MAX_SUMMARY) {
            "A proposal summary is 1–$MAX_SUMMARY characters."
        }
        require(createdAt > 0) { "A proposal needs a real creation time." }
    }

    public companion object {
        public const val MAX_SUMMARY: Int = 280
    }
}

public enum class GrantStage { PLEDGED, EVIDENCE_RECORDED, VERIFIED, PAID, WITHDRAWN }

/**
 * A Growth Grant is a witnessed pledge between two people. TLC records the promise, the evidence
 * and the verification; the money still moves directly from donor to beneficiary (INV-01/INV-02).
 */
public data class GrowthGrant(
    public val id: String,
    public val beneficiary: String,
    public val purpose: String,
    public val amountKobo: Long?,
    public val pledgedAt: Long,
    public val stage: GrantStage = GrantStage.PLEDGED,
    public val evidence: String? = null,
    public val verifiedAt: Long? = null,
    public val witness: String? = null,
    public val paidAt: Long? = null,
    public val bankReference: String? = null,
    public val withdrawnAt: Long? = null,
    public val withdrawnReason: String? = null,
) {
    init {
        require(id.isNotBlank()) { "A grant needs an id." }
        require(beneficiary.isNotBlank() && beneficiary.length <= MAX_BENEFICIARY) {
            "A beneficiary is 1–$MAX_BENEFICIARY characters."
        }
        require(purpose.isNotBlank() && purpose.length <= MAX_PURPOSE) {
            "A purpose is 1–$MAX_PURPOSE characters."
        }
        require(amountKobo == null || amountKobo > 0) { "A stated amount must be positive." }
        require(pledgedAt > 0) { "A pledge needs a real time." }
        require(stage != GrantStage.EVIDENCE_RECORDED || evidence != null) {
            "Evidence must exist before the stage says it does."
        }
        require(stage != GrantStage.VERIFIED || (verifiedAt != null && witness != null)) {
            "A verified grant needs its witness."
        }
        require(stage != GrantStage.PAID || (paidAt != null && bankReference != null)) {
            "A paid grant needs the receipt reference."
        }
        require(stage != GrantStage.WITHDRAWN || withdrawnAt != null) {
            "A withdrawn grant needs a time."
        }
    }

    public companion object {
        public const val MAX_BENEFICIARY: Int = 120
        public const val MAX_PURPOSE: Int = 280
        public const val MAX_EVIDENCE: Int = 500
        public const val MAX_REFERENCE: Int = 120
    }
}

/**
 * A Pioneer due recorded on this device. The obligation is an assertion, not a balance: the money
 * is sent directly to the Concept Pioneer, and the app only keeps the evidence (INV-01).
 */
public data class DueRecord(
    public val id: String,
    public val kind: TokenKind,
    public val amountKobo: Long,
    public val period: FeePeriod,
    public val members: List<String>,
    public val state: ObligationState,
    public val createdAt: Long,
    public val instructionIssuedAt: Long? = null,
    public val instructionExpiresAt: Long? = null,
    public val proofHash: String? = null,
    public val statedAmountKobo: Long? = null,
    public val bankReference: String? = null,
    public val submittedBy: String? = null,
    public val proofAt: Long? = null,
    public val attestedBy: String? = null,
    public val secondAttestor: String? = null,
    public val statementRef: String? = null,
    public val attestedAt: Long? = null,
    public val waivedBy: String? = null,
    public val waiverNote: String? = null,
    public val waivedAt: Long? = null,
    public val exception: Rejection? = null,
    public val revision: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "A due needs an id." }
        require(kind.pioneerFee) { "Only platform tokens create Pioneer dues: $kind." }
        require(amountKobo > 0) { "A due must be greater than zero." }
        require(members.size == 2 && members.toSet().size == 2) { "A due belongs to two distinct people." }
        require(createdAt > 0) { "A due needs a real creation time." }
        require(revision >= 0) { "Revision cannot be negative." }
        require(state !in STATES_WITH_PROOF || (proofHash != null && proofHash.length == 64 && bankReference != null)) {
            "This state needs a proof hash and a bank reference."
        }
        require(state != ObligationState.ATTESTED || (attestedBy != null && statementRef != null && attestedAt != null)) {
            "An attested due needs the attestation it is recording."
        }
        require(state != ObligationState.WAIVED || waiverNote != null) { "A waiver needs a note." }
        require(state != ObligationState.EXCEPTION || exception != null) { "An exception needs a code." }
    }

    public companion object {
        public val STATES_WITH_PROOF: Set<ObligationState> =
            setOf(ObligationState.PROOF_SUBMITTED, ObligationState.ATTESTED, ObligationState.EXCEPTION)
    }
}

/** Everything the shell remembers between launches, except the hash-chained record itself. */
public data class AppSnapshot(
    public val profile: Profile,
    public val preferences: Preferences,
    public val stewards: List<Steward>,
    public val proposals: List<Proposal> = emptyList(),
    public val grants: List<GrowthGrant> = emptyList(),
    public val dues: List<DueRecord> = emptyList(),
    public val people: List<Person> = emptyList(),
    public val bankAccounts: List<BankAccountRef> = emptyList(),
    public val contactLog: List<ContactEvent> = emptyList(),
    public val supports: List<SupportIntent> = emptyList(),
) {
    init {
        require(stewards.isNotEmpty()) { "A space needs at least one steward." }
        require(stewards.size <= 2) { "A dyad has at most a Primary and a Co-Steward." }
        require(stewards.map { it.role }.distinct().size == stewards.size) {
            "Each steward role appears at most once."
        }
        require(stewards.map { it.id }.distinct().size == stewards.size) {
            "Two stewards cannot share an id."
        }
        require(stewards.any { it.role == StewardRole.PRIMARY }) { "Every space has a Primary Steward." }
        require(people.map { it.id }.distinct().size == people.size) { "Two people cannot share an id." }
        require(people.map { it.handle }.distinct().size == people.size) { "Two people cannot share a handle." }
        require(bankAccounts.map { it.id }.distinct().size == bankAccounts.size) {
            "Two bank references cannot share an id."
        }
        require(supports.map { it.id }.distinct().size == supports.size) {
            "Two support intents cannot share an id."
        }
        require(contactLog.map { it.id }.distinct().size == contactLog.size) {
            "Two contact events cannot share an id."
        }
        require(people.none { person -> person.bankRefId != null && bankAccounts.none { it.id == person.bankRefId } }) {
            "A person cannot point at a bank reference that is not here."
        }
    }

    public val primary: Steward get() = stewards.first { it.role == StewardRole.PRIMARY }

    public val coSteward: Steward? get() = stewards.firstOrNull { it.role == StewardRole.CO }

    /** True when a second signature is possible. Proposals stay disabled until then. */
    public val hasDyad: Boolean get() = stewards.size == 2

    public fun person(handle: Handle): Person? = people.firstOrNull { it.handle == handle }

    public fun bankRef(id: String?): BankAccountRef? = id?.let { key -> bankAccounts.firstOrNull { it.id == key } }
}
