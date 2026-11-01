package culture.thelove.tlc.ledger

/**
 * The only kinds of fact TLC is allowed to commit to its ledger.
 *
 * Note what is absent: there is no BALANCE, TRANSFER, ESCROW, POOL or SETTLEMENT entry.
 * The ledger records *claims and attestations about direct transfers that already happened
 * between two private bank accounts* — never the movement of value itself (INV-01, INV-02, INV-08).
 * Adding a kind requires updating [LedgerContractTest.allowedEntryKinds], so it cannot happen quietly.
 */
public enum class EntryKind {
    FEE_INTENT,
    PROOF_RECORDED,
    ATTESTATION,
    RECEIPT,
    WAIVER,
    GRANT_PLEDGE,
    MILESTONE_VERIFIED,
    CONSENT_GRANTED,
    CONSENT_REVOKED,
}

public data class LedgerEntry(
    public val seq: Long,
    public val kind: EntryKind,
    public val subject: String,
    public val amountKobo: Long?,
    public val occurredAt: Long,
    public val idempotencyKey: String?,
    public val prevHash: String,
    public val hash: String,
) {
    init {
        require(seq >= 1) { "Sequence numbers start at 1." }
        require(subject.isNotBlank()) { "Every entry names its subject." }
    }

    public fun recomputeHash(hasher: Hasher): String = computeHash(
        hasher = hasher,
        seq = seq,
        kind = kind,
        subject = subject,
        amountKobo = amountKobo,
        occurredAt = occurredAt,
        idempotencyKey = idempotencyKey,
        prevHash = prevHash,
    )

    public companion object {
        public fun computeHash(
            hasher: Hasher,
            seq: Long,
            kind: EntryKind,
            subject: String,
            amountKobo: Long?,
            occurredAt: Long,
            idempotencyKey: String?,
            prevHash: String,
        ): String = Hex.encode(
            hasher.sha256(
                Canonical.of(
                    "tlc.ledger.v1",
                    seq.toString(),
                    kind.name,
                    subject,
                    amountKobo?.toString(),
                    occurredAt.toString(),
                    idempotencyKey,
                    prevHash,
                ),
            ),
        )
    }
}

/** An entry that has not been sequenced yet. */
public data class LedgerDraft(
    public val kind: EntryKind,
    public val subject: String,
    public val amountKobo: Long? = null,
    public val occurredAt: Long,
    public val idempotencyKey: String? = null,
) {
    init {
        require(subject.isNotBlank()) { "Every entry names its subject." }
        require(amountKobo == null || amountKobo >= 0) { "Amounts are non-negative or absent." }
    }
}

public sealed interface AppendResult {
    public data class Appended(public val entry: LedgerEntry) : AppendResult

    /** Same idempotency key already present: the original is returned, nothing is duplicated. */
    public data class Duplicate(public val existing: LedgerEntry) : AppendResult

    public data class Invalid(public val reason: String) : AppendResult
}
