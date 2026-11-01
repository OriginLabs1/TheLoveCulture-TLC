package culture.thelove.tlc.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * SK-01 / INV-01 / INV-08 — the fee lifecycle as an executable contract.
 *
 * If any test here fails, the money path is wrong. These are not examples; they are invariants.
 */
class FeeMachineTest {

    private val alice = PartyId("party:alice")
    private val bob = PartyId("party:bob")
    private val outsider = PartyId("party:outsider")
    private val pioneer = Actor(PartyId("party:pioneer"), ActorKind.PIONEER)
    private val member = Actor(alice, ActorKind.MEMBER)
    private val t0 = 1_760_000_000_000L
    private val hashA = "a".repeat(64)
    private val hashB = "b".repeat(64)

    private fun activation(amount: Money = Money.naira(1_000)) = FeeObligation(
        id = ObligationId("ob:activation:001"),
        dyad = setOf(alice, bob),
        kind = TokenKind.ACTIVATION,
        amount = amount,
        period = FeePeriod.ONE_OFF,
    )

    private fun courtesy(amount: Money = Money.naira(200)) = FeeObligation(
        id = ObligationId("ob:courtesy:001"),
        dyad = setOf(alice, bob),
        kind = TokenKind.COURTESY,
        amount = amount,
        period = FeePeriod.ONE_OFF,
    )

    private fun issue(o: FeeObligation): FeeObligation =
        (FeeMachine.apply(o, FeeEvent.IssueInstruction(member, t0)) as Transition.Applied).next

    private fun submit(o: FeeObligation, amount: Money = o.amount, at: Long = t0 + 1_000, hash: String = hashA): Transition =
        FeeMachine.apply(o, FeeEvent.SubmitProof(member, at, hash, amount, "TRF-8891"))

    private fun attest(o: FeeObligation, amount: Money = o.amount, second: PartyId? = null): Transition =
        FeeMachine.apply(o, FeeEvent.Attest(pioneer, t0 + 2_000, amount, "ZENITH-STATEMENT-0001", second))

    // ---------------------------------------------------------------- happy path

    @Test
    fun `activation reaches ATTESTED only through proof, and asks for a receipt`() {
        val due = activation()
        val issued = issue(due)
        assertEquals(ObligationState.INSTRUCTION_ISSUED, issued.state)
        assertEquals(t0 + FeeEvent.INSTRUCTION_TTL_MILLIS, issued.instructionExpiresAt)

        val submitted = assertIs<Transition.Applied>(submit(issued)).next
        assertEquals(ObligationState.PROOF_SUBMITTED, submitted.state)

        val attested = assertIs<Transition.Applied>(attest(submitted))
        assertEquals(ObligationState.ATTESTED, attested.next.state)
        assertEquals(setOf(Effect.IssueReceipt(due.id), Effect.AnchorPending(due.id)), attested.effects.toSet())
    }

    @Test
    fun `revision increases exactly once per accepted transition`() {
        val h = assertIs<Transition.Applied>(attest(assertIs<Transition.Applied>(submit(issue(activation()))).next))
        assertEquals(3, h.next.revision)
    }

    // ---------------------------------------------------------------- INV-08: nobody self-settles

    @Test
    fun `a dyad member cannot attest their own fee`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        val selfSteward = Actor(alice, ActorKind.STEWARD)
        val result = FeeMachine.apply(
            submitted,
            FeeEvent.Attest(selfSteward, t0 + 2_000, submitted.amount, "SELF-001"),
        )
        assertEquals(Transition.Rejected(Rejection.SELF_ATTESTATION), result)
    }

    @Test
    fun `an ordinary member is not an authority`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        val result = FeeMachine.apply(
            submitted,
            FeeEvent.Attest(Actor(outsider, ActorKind.MEMBER), t0 + 2_000, submitted.amount, "X-1"),
        )
        assertEquals(Transition.Rejected(Rejection.WRONG_ACTOR), result)
    }

    @Test
    fun `automation can never attest`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        val result = FeeMachine.apply(
            submitted,
            FeeEvent.Attest(Actor(PartyId("system:cron"), ActorKind.SYSTEM), t0 + 2_000, submitted.amount, "X-1"),
        )
        assertEquals(Transition.Rejected(Rejection.SYSTEM_CANNOT_ATTEST), result)
    }

    @Test
    fun `attestation is impossible before evidence exists`() {
        assertEquals(
            Transition.Rejected(Rejection.ILLEGAL_TRANSITION),
            attest(activation()),
        )
    }

    // ---------------------------------------------------------------- three-way match

    @Test
    fun `a proof for the wrong amount becomes an exception, never an attestation`() {
        val issued = issue(activation())
        val routed = assertIs<Transition.Applied>(submit(issued, amount = Money.naira(999))).next
        assertEquals(ObligationState.EXCEPTION, routed.state)
        assertEquals(Rejection.AMOUNT_MISMATCH, routed.exceptionCode)
        assertEquals(
            Transition.Rejected(Rejection.ILLEGAL_TRANSITION),
            attest(routed),
        )
    }

    @Test
    fun `attesting a different amount than the proof is refused`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        assertEquals(Transition.Rejected(Rejection.AMOUNT_MISMATCH), attest(submitted, amount = Money.naira(1_001)))
    }

    @Test
    fun `an attestation without a statement reference is refused`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        val result = FeeMachine.apply(submitted, FeeEvent.Attest(pioneer, t0 + 2_000, submitted.amount, "  "))
        assertEquals(Transition.Rejected(Rejection.MISSING_STATEMENT_REF), result)
    }

    // ---------------------------------------------------------------- four eyes

    @Test
    fun `fees at or above the threshold need a second, different attestor`() {
        val big = activation(Money.naira(50_000))
        val submitted = assertIs<Transition.Applied>(submit(issue(big))).next
        assertEquals(Transition.Rejected(Rejection.FOUR_EYES_REQUIRED), attest(submitted))
        assertEquals(
            Transition.Rejected(Rejection.FOUR_EYES_REQUIRED),
            attest(submitted, second = pioneer.id),
        )
        val dual = assertIs<Transition.Applied>(attest(submitted, second = PartyId("party:steward-2")))
        assertEquals(ObligationState.ATTESTED, dual.next.state)
        assertEquals(PartyId("party:steward-2"), dual.next.attestation?.secondAttestor)
    }

    @Test
    fun `below the threshold one attestor is enough`() {
        val small = activation(Money.naira(49_999))
        val submitted = assertIs<Transition.Applied>(submit(issue(small))).next
        assertIs<Transition.Applied>(attest(submitted))
    }

    // ---------------------------------------------------------------- idempotency

    @Test
    fun `replaying the same proof is idempotent and does not advance the state machine`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        val replay = assertIs<Transition.Applied>(submit(submitted))
        assertEquals(submitted.state, replay.next.state)
        assertEquals(listOf<Effect>(Effect.AlreadyRecorded), replay.effects)
        assertEquals(submitted.revision, replay.next.revision)
    }

    @Test
    fun `a second, different proof is refused`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        assertEquals(Transition.Rejected(Rejection.PROOF_ALREADY_SUBMITTED), submit(submitted, hash = hashB))
    }

    @Test
    fun `proofs submitted after the instruction expires are refused`() {
        val issued = issue(activation())
        val late = t0 + FeeEvent.INSTRUCTION_TTL_MILLIS + 1
        assertEquals(Transition.Rejected(Rejection.INSTRUCTION_EXPIRED), submit(issued, at = late))
    }

    // ---------------------------------------------------------------- actors and gates

    @Test
    fun `only the dyad may request an instruction or submit proof`() {
        val notMember = Actor(outsider, ActorKind.MEMBER)
        assertEquals(
            Transition.Rejected(Rejection.NOT_A_DYAD_MEMBER),
            FeeMachine.apply(activation(), FeeEvent.IssueInstruction(notMember, t0)),
        )
        val issued = issue(activation())
        assertEquals(
            Transition.Rejected(Rejection.NOT_A_DYAD_MEMBER),
            FeeMachine.apply(issued, FeeEvent.SubmitProof(notMember, t0 + 10, hashA, issued.amount, "R-1")),
        )
    }

    @Test
    fun `only voluntary tokens can be waived`() {
        val issued = issue(activation())
        assertEquals(
            Transition.Rejected(Rejection.NOT_WAIVABLE),
            FeeMachine.apply(issued, FeeEvent.Waive(pioneer, t0 + 5, "hardship")),
        )
        val waived = assertIs<Transition.Applied>(
            FeeMachine.apply(issue(courtesy()), FeeEvent.Waive(pioneer, t0 + 5, "gift not taken")),
        )
        assertEquals(ObligationState.WAIVED, waived.next.state)
        assertEquals(listOf<Effect>(Effect.NoReceivableCreated), waived.effects)
    }

    // ---------------------------------------------------------------- totality

    @Test
    fun `every terminal state rejects every event`() {
        val attested = assertIs<Transition.Applied>(
            attest(assertIs<Transition.Applied>(submit(issue(activation()))).next),
        ).next
        val waived = assertIs<Transition.Applied>(
            FeeMachine.apply(issue(courtesy()), FeeEvent.Waive(pioneer, t0 + 5, "n/a")),
        ).next
        val expired = assertIs<Transition.Applied>(
            FeeMachine.apply(activation(), FeeEvent.Expire(Actor(PartyId("system:cron"), ActorKind.SYSTEM), t0 + 5)),
        ).next

        val events = listOf(
            FeeEvent.IssueInstruction(member, t0 + 10),
            FeeEvent.SubmitProof(member, t0 + 10, hashA, Money.naira(1_000), "R-1"),
            FeeEvent.Attest(pioneer, t0 + 10, Money.naira(1_000), "S-1"),
            FeeEvent.FlagException(pioneer, t0 + 10, Rejection.AMOUNT_MISMATCH),
            FeeEvent.Waive(pioneer, t0 + 10, "x"),
            FeeEvent.Expire(Actor(PartyId("system:cron"), ActorKind.SYSTEM), t0 + 10),
        )
        for (state in listOf(attested, waived, expired)) {
            for (event in events) {
                assertEquals(
                    Transition.Rejected(Rejection.ILLEGAL_TRANSITION),
                    FeeMachine.apply(state, event),
                    "terminal state ${state.state} accepted $event",
                )
            }
        }
    }

    @Test
    fun `the state machine is deterministic`() {
        val submitted = assertIs<Transition.Applied>(submit(issue(activation()))).next
        assertEquals(attest(submitted), attest(submitted))
    }

    @Test
    fun `no effect can require custody`() {
        val effects: List<Effect> = listOf(
            Effect.ReleasePioneerDetails(1),
            Effect.IssueReceipt(ObligationId("x")),
            Effect.AnchorPending(ObligationId("x")),
            Effect.NotifyDyad(ObligationId("x"), Rejection.AMOUNT_MISMATCH),
            Effect.AlreadyRecorded,
            Effect.NoReceivableCreated,
        )
        assertTrue(effects.none { it.requiresCustody }, "TLC effects must never imply custody (INV-01)")
    }

    @Test
    fun `every non initial state is reachable from DUE`() {
        val reached = mutableSetOf(ObligationState.DUE)
        fun visit(o: FeeObligation, depth: Int) {
            if (depth > 4) return
            val candidates = listOf(
                FeeEvent.IssueInstruction(member, t0),
                FeeEvent.SubmitProof(member, t0 + 1, hashA, o.amount, "R-1"),
                FeeEvent.Attest(pioneer, t0 + 2, o.amount, "S-1", PartyId("party:steward-2")),
                FeeEvent.FlagException(pioneer, t0 + 2, Rejection.AMOUNT_MISMATCH),
                FeeEvent.Waive(pioneer, t0 + 2, "x"),
                FeeEvent.Expire(Actor(PartyId("system:cron"), ActorKind.SYSTEM), t0 + 2),
            )
            for (e in candidates) {
                val r = FeeMachine.apply(o, e)
                if (r is Transition.Applied && r.next.state !in reached) {
                    reached += r.next.state
                    visit(r.next, depth + 1)
                }
            }
        }
        visit(courtesy(), 0)
        val missing = ObligationState.entries.toSet() - reached
        assertTrue(missing.isEmpty(), "unreachable states: $missing")
    }
}
