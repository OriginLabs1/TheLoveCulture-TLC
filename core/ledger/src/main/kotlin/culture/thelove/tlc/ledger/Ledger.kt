package culture.thelove.tlc.ledger

/**
 * Append-only, hash-chained record of obligations and attestations (SPECS §C-4, PART D).
 *
 * Invariants this class mechanically enforces:
 *  - sequence numbers are contiguous and start at 1;
 *  - each entry commits to its predecessor (`prevHash`);
 *  - recomputing a hash from the stored fields must reproduce it, so any edit is detectable;
 *  - an idempotency key can appear at most once, so a replayed proof cannot double-attest.
 *
 * It cannot and does not move money. There is no balance to query.
 */
public class Ledger(private val hasher: Hasher) {

    private val items = ArrayList<LedgerEntry>()
    private val idempotencyIndex = HashMap<String, LedgerEntry>()

    public fun entries(): List<LedgerEntry> = items.toList()

    public fun size(): Int = items.size

    public fun headHash(): String = items.lastOrNull()?.hash ?: GENESIS

    public fun append(draft: LedgerDraft): AppendResult {
        val key = draft.idempotencyKey
        if (key != null) {
            idempotencyIndex[key]?.let { return AppendResult.Duplicate(it) }
        }
        if (draft.amountKobo != null && draft.amountKobo < 0) {
            return AppendResult.Invalid("Negative amount: no such fact exists in TLC (INV-01).")
        }
        val seq = items.size.toLong() + 1
        val prev = headHash()
        val hash = LedgerEntry.computeHash(
            hasher = hasher,
            seq = seq,
            kind = draft.kind,
            subject = draft.subject,
            amountKobo = draft.amountKobo,
            occurredAt = draft.occurredAt,
            idempotencyKey = draft.idempotencyKey,
            prevHash = prev,
        )
        val entry = LedgerEntry(
            seq = seq,
            kind = draft.kind,
            subject = draft.subject,
            amountKobo = draft.amountKobo,
            occurredAt = draft.occurredAt,
            idempotencyKey = draft.idempotencyKey,
            prevHash = prev,
            hash = hash,
        )
        items += entry
        if (key != null) idempotencyIndex[key] = entry
        return AppendResult.Appended(entry)
    }

    public fun merkleRoot(): String = Merkle.root(items.map { it.hash }, hasher)

    /** A published commitment to the whole history at a point in time. */
    public fun anchor(createdAt: Long): Anchor = Anchor(
        length = items.size,
        headHash = headHash(),
        merkleRoot = merkleRoot(),
        createdAt = createdAt,
    )

    public companion object {
        /** 64 zeroes: the predecessor of entry 1. */
        public const val GENESIS: String =
            "0000000000000000000000000000000000000000000000000000000000000000"

        public fun verify(
            entries: List<LedgerEntry>,
            hasher: Hasher,
            anchor: Anchor? = null,
        ): VerificationReport {
            val failures = ArrayList<Failure>()
            if (entries.isEmpty()) {
                failures += Failure.EmptyLedger
            }
            val seenKeys = HashSet<String>()
            var expectedPrev = GENESIS
            entries.forEachIndexed { index, entry ->
                val expectedSeq = index.toLong() + 1
                if (entry.seq != expectedSeq) failures += Failure.SequenceGap(index, entry.seq)
                if (entry.prevHash != expectedPrev) failures += Failure.PrevHashMismatch(index)
                if (entry.recomputeHash(hasher) != entry.hash) failures += Failure.HashMismatch(index)
                entry.amountKobo?.let { if (it < 0) failures += Failure.NegativeAmount(index) }
                entry.idempotencyKey?.let { key ->
                    if (!seenKeys.add(key)) failures += Failure.DuplicateIdempotencyKey(index)
                }
                expectedPrev = entry.hash
            }
            if (anchor != null) {
                if (anchor.length != entries.size) failures += Failure.AnchorLengthMismatch(anchor.length, entries.size)
                if (entries.isNotEmpty() && anchor.headHash != entries.last().hash) {
                    failures += Failure.AnchorHeadMismatch
                }
                if (anchor.merkleRoot != Merkle.root(entries.map { it.hash }, hasher)) {
                    failures += Failure.AnchorRootMismatch
                }
            }
            return VerificationReport(
                ok = failures.isEmpty(),
                length = entries.size,
                headHash = entries.lastOrNull()?.hash ?: GENESIS,
                merkleRoot = Merkle.root(entries.map { it.hash }, hasher),
                failures = failures.toList(),
            )
        }
    }
}

/** A signed, publishable commitment: "history up to N has head H and root R at time T". */
public data class Anchor(
    public val length: Int,
    public val headHash: String,
    public val merkleRoot: String,
    public val createdAt: Long,
)

public data class VerificationReport(
    public val ok: Boolean,
    public val length: Int,
    public val headHash: String,
    public val merkleRoot: String,
    public val failures: List<Failure>,
)

public sealed interface Failure {
    public val index: Int

    public data class SequenceGap(public override val index: Int, public val found: Long) : Failure

    public data class PrevHashMismatch(public override val index: Int) : Failure

    public data class HashMismatch(public override val index: Int) : Failure

    public data class NegativeAmount(public override val index: Int) : Failure

    public data class DuplicateIdempotencyKey(public override val index: Int) : Failure

    public data object EmptyLedger : Failure {
        override val index: Int = -1
    }

    public data class AnchorLengthMismatch(public val anchored: Int, public val actual: Int) : Failure {
        override val index: Int = -1
    }

    public data object AnchorHeadMismatch : Failure {
        override val index: Int = -1
    }

    public data object AnchorRootMismatch : Failure {
        override val index: Int = -1
    }
}
