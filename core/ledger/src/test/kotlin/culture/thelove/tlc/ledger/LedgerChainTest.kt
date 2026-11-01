package culture.thelove.tlc.ledger

import culture.thelove.tlc.domain.Actor
import culture.thelove.tlc.domain.ActorKind
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeeMachine
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.FeeObligation
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.domain.ObligationId
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.PartyId
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.domain.Transition
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * SK-04 / INV-08 — the ledger is the accountability surface for every naira that reaches the
 * Concept Pioneer. If it can be edited, reordered or silently truncated without detection,
 * the product has no receipts and no argument.
 */
class LedgerChainTest {

    private val hasher = PlatformSha256()
    private val t0 = 1_760_000_000_000L

    private fun seed(): Ledger {
        val ledger = Ledger(hasher)
        ledger.append(LedgerDraft(EntryKind.FEE_INTENT, "ob:001", 100_000, t0, "idem-1"))
        ledger.append(LedgerDraft(EntryKind.PROOF_RECORDED, "ob:001", 100_000, t0 + 1, "idem-2"))
        ledger.append(LedgerDraft(EntryKind.ATTESTATION, "ob:001", 100_000, t0 + 2, "idem-3"))
        return ledger
    }

    @Test
    fun `a fresh chain verifies`() {
        val ledger = seed()
        val report = Ledger.verify(ledger.entries(), hasher)
        assertTrue(report.ok, "unexpected failures: ${report.failures}")
        assertEquals(3, report.length)
        assertEquals(ledger.headHash(), report.headHash)
    }

    @Test
    fun `entry one links to genesis`() {
        assertEquals(Ledger.GENESIS, seed().entries().first().prevHash)
        assertTrue(Ledger.GENESIS.all { it == '0' } && Ledger.GENESIS.length == 64)
    }

    @Test
    fun `editing an amount is detected`() {
        val entries = seed().entries().toMutableList()
        entries[1] = entries[1].copy(amountKobo = 1)
        val report = Ledger.verify(entries, hasher)
        assertFalse(report.ok)
        assertTrue(report.failures.any { it is Failure.HashMismatch && it.index == 1 }, "${report.failures}")
    }

    @Test
    fun `editing and rehashing still breaks the next link`() {
        val entries = seed().entries().toMutableList()
        val forged = entries[1].copy(amountKobo = 1)
        entries[1] = forged.copy(
            hash = forged.recomputeHash(hasher),
        )
        val report = Ledger.verify(entries, hasher)
        assertFalse(report.ok)
        assertTrue(report.failures.any { it is Failure.PrevHashMismatch && it.index == 2 }, "${report.failures}")
    }

    @Test
    fun `reordering is detected`() {
        val entries = seed().entries().toMutableList()
        val second = entries[1]
        entries[1] = entries[2]
        entries[2] = second
        assertFalse(Ledger.verify(entries, hasher).ok)
    }

    @Test
    fun `removing an entry is detected`() {
        val entries = seed().entries()
        val report = Ledger.verify(entries.filterIndexed { i, _ -> i != 1 }, hasher)
        assertFalse(report.ok)
        assertTrue(report.failures.any { it is Failure.SequenceGap || it is Failure.PrevHashMismatch })
    }

    @Test
    fun `truncation is only detectable against an anchor`() {
        val ledger = seed()
        val anchor = ledger.anchor(t0 + 10)
        val truncated = ledger.entries().dropLast(1)

        // A prefix is self-consistent, so re-verification alone is not enough…
        assertTrue(Ledger.verify(truncated, hasher).ok)
        // …the published anchor is what makes the tail tamper-evident.
        val report = Ledger.verify(truncated, hasher, anchor)
        assertFalse(report.ok)
        assertTrue(report.failures.any { it is Failure.AnchorLengthMismatch })
    }

    @Test
    fun `a replayed idempotency key cannot double record`() {
        val ledger = seed()
        val before = ledger.size()
        val result = ledger.append(LedgerDraft(EntryKind.PROOF_RECORDED, "ob:001", 100_000, t0 + 99, "idem-2"))
        assertIs<AppendResult.Duplicate>(result)
        assertEquals(before, ledger.size())
    }

    @Test
    fun `negative amounts are refused at the door`() {
        // Factories validate, and append re-validates (defence in depth for deserialised input).
        assertFailsWith<IllegalArgumentException> {
            LedgerDraft(EntryKind.FEE_INTENT, "ob:001", -1, t0)
        }
    }

    @Test
    fun `an empty ledger is not a valid history`() {
        assertFalse(Ledger.verify(emptyList(), hasher).ok)
    }

    /**
     * The whole point, end to end: the domain says "attested", the ledger records the evidence
     * chain, and the chain verifies offline. No step in this test moves or holds money.
     */
    @Test
    fun `a real activation ends as a verifiable receipt chain`() {
        val alice = PartyId("party:alice")
        val bob = PartyId("party:bob")
        val member = Actor(alice, ActorKind.MEMBER)
        val pioneer = Actor(PartyId("party:pioneer"), ActorKind.PIONEER)
        val obligation = FeeObligation(
            id = ObligationId("ob:activation:001"),
            dyad = setOf(alice, bob),
            kind = TokenKind.ACTIVATION,
            amount = Money.naira(1_000),
            period = FeePeriod.ONE_OFF,
        )

        val issued = assertIs<Transition.Applied>(
            FeeMachine.apply(obligation, FeeEvent.IssueInstruction(member, t0)),
        ).next
        val proofHash = "c".repeat(64)
        val submitted = assertIs<Transition.Applied>(
            FeeMachine.apply(issued, FeeEvent.SubmitProof(member, t0 + 60_000, proofHash, obligation.amount, "TRF-778812")),
        ).next
        val attested = assertIs<Transition.Applied>(
            FeeMachine.apply(submitted, FeeEvent.Attest(pioneer, t0 + 120_000, obligation.amount, "ZENITH-2026-11-0007")),
        )

        assertEquals(ObligationState.ATTESTED, attested.next.state)
        assertEquals(setOf("IssueReceipt", "AnchorPending"), attested.effects.map { it::class.simpleName }.toSet())

        val ledger = Ledger(hasher)
        ledger.append(LedgerDraft(EntryKind.FEE_INTENT, "ob:activation:001", 100_000, t0, "ob:activation:001:intent"))
        ledger.append(LedgerDraft(EntryKind.PROOF_RECORDED, "ob:activation:001", 100_000, t0 + 60_000, "ob:activation:001:proof"))
        ledger.append(LedgerDraft(EntryKind.ATTESTATION, "ob:activation:001", 100_000, t0 + 120_000, "ob:activation:001:attest"))
        ledger.append(LedgerDraft(EntryKind.RECEIPT, "ob:activation:001", 100_000, t0 + 120_001, "ob:activation:001:receipt"))

        val anchor = ledger.anchor(t0 + 200_000)
        val report = Ledger.verify(ledger.entries(), hasher, anchor)
        assertTrue(report.ok, "${report.failures}")
        assertEquals(4, report.length)
    }
}
