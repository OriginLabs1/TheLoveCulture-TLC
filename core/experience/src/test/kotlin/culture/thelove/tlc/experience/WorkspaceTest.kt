package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.ledger.EntryKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkspaceTest {

    private fun onboarded(): AppSnapshot =
        (Workspace.onboard("Ada", "Okafor Circle", Role.LOVE_MODEL, "0123456789abcdef0123456789abcdef", 1) as WorkspaceResult.Ok)
            .change.snapshot

    @Test
    fun `onboarding creates the primary steward and a traceable consent fact`() {
        val result = Workspace.onboard(" Ada ", "", Role.LOVE_MODEL, "0123456789abcdef0123456789abcdef", 5)
        assertIs<WorkspaceResult.Ok>(result)
        assertEquals("Ada", result.change.snapshot.profile.displayName)
        assertEquals("My circle", result.change.snapshot.profile.circleName)
        assertEquals(StewardRole.PRIMARY, result.change.snapshot.primary.role)
        assertTrue(result.change.snapshot.preferences.onboarded)
        assertEquals(
            listOf(EntryKind.CONSENT_GRANTED, EntryKind.HANDLE_CLAIMED),
            result.change.facts.map { it.kind },
        )
        assertEquals("@ada", result.change.snapshot.profile.handle.toString())
    }

    @Test
    fun `co-steward unlocks two-key decisions`() {
        val start = onboarded()
        val added = Workspace.addCoSteward(start, "pair-1", "Tunde")
        assertIs<WorkspaceResult.Ok>(added)
        assertTrue(added.change.snapshot.hasDyad)
        assertEquals("Tunde", added.change.snapshot.coSteward?.name)
        val second = Workspace.addCoSteward(added.change.snapshot, "pair-2", "Bisi")
        assertIs<WorkspaceResult.Rejected>(second)
    }

    @Test
    fun `a proposal needs both signatures and a veto closes it`() {
        val start = (Workspace.addCoSteward(onboarded(), "pair-1", "Tunde") as WorkspaceResult.Ok).change.snapshot
        val proposed = Workspace.propose(start, "p1", SensitiveAction.CHANGE_BANK_DETAILS, null, 10)
        assertIs<WorkspaceResult.Ok>(proposed)
        val pending = Workspace.approve(proposed.change.snapshot, "p1", start.primary.id, 11)
        assertIs<WorkspaceResult.Ok>(pending)
        assertTrue(pending.change.facts.isEmpty())
        val approved = Workspace.approve(pending.change.snapshot, "p1", "pair-1", 12)
        assertIs<WorkspaceResult.Ok>(approved)
        assertTrue(approved.change.snapshot.proposals.single().closed)
        assertEquals(EntryKind.CONSENT_GRANTED, approved.change.facts.single().kind)

        val proposed2 = Workspace.propose(approved.change.snapshot, "p2", SensitiveAction.DISSOLVE_DYAD, "Close", 13)
        assertIs<WorkspaceResult.Ok>(proposed2)
        val vetoed = Workspace.veto(proposed2.change.snapshot, "p2", start.primary.id, 14)
        assertIs<WorkspaceResult.Ok>(vetoed)
        assertTrue(vetoed.change.snapshot.proposals.first { it.id == "p2" }.closed)
        assertEquals(EntryKind.CONSENT_REVOKED, vetoed.change.facts.single().kind)
    }

    @Test
    fun `a proposal cannot be approved without a co-steward`() {
        val start = onboarded()
        val proposed = Workspace.propose(start, "p1", SensitiveAction.EXPORT_SHARED_RECORD, null, 10)
        assertIs<WorkspaceResult.Ok>(proposed)
        val result = Workspace.approve(proposed.change.snapshot, "p1", start.primary.id, 11)
        assertIs<WorkspaceResult.Rejected>(result)
        assertTrue(result.reason.contains("Co-Steward"))
    }

    @Test
    fun `growth grant lifecycle records evidence, verification and direct payment`() {
        val start = onboarded()
        val added = Workspace.addGrant(start, "g1", "Ada", "School fees", 250_000, 10)
        assertIs<WorkspaceResult.Ok>(added)
        assertEquals(EntryKind.GRANT_PLEDGE, added.change.facts.single().kind)

        val evidence = Workspace.advanceGrant(
            added.change.snapshot, "g1", GrantEvent.AttachEvidence("Signed receipt", 11),
        )
        assertIs<WorkspaceResult.Ok>(evidence)
        assertEquals(EntryKind.GRANT_EVIDENCE, evidence.change.facts.single().kind)

        val verified = Workspace.advanceGrant(
            evidence.change.snapshot, "g1", GrantEvent.Verify("Tunde", 12),
        )
        assertIs<WorkspaceResult.Ok>(verified)
        assertEquals(EntryKind.MILESTONE_VERIFIED, verified.change.facts.single().kind)

        val paid = Workspace.advanceGrant(
            verified.change.snapshot, "g1", GrantEvent.RecordPayment("BANK-123", 13),
        )
        assertIs<WorkspaceResult.Ok>(paid)
        assertEquals(GrantStage.PAID, paid.change.snapshot.grants.single().stage)
        assertEquals(EntryKind.PROOF_RECORDED, paid.change.facts.single().kind)
    }

    @Test
    fun `a due cannot be created before the dyad exists`() {
        val start = onboarded()
        val result = Workspace.addDue(start, "d1", TokenKind.ACTIVATION, 100_000, FeePeriod.ONE_OFF, 10)
        assertIs<WorkspaceResult.Rejected>(result)
        assertTrue(result.reason.contains("Co-Steward"))
    }

    @Test
    fun `a due reaches proof and awaits attestation without inventing a receipt`() {
        val start = (Workspace.addCoSteward(onboarded(), "pair-1", "Tunde") as WorkspaceResult.Ok).change.snapshot
        val added = Workspace.addDue(start, "d1", TokenKind.ACTIVATION, 100_000, FeePeriod.ONE_OFF, 10)
        assertIs<WorkspaceResult.Ok>(added)
        val due = added.change.snapshot.dues.single()
        val issued = Workspace.advanceDue(
            added.change.snapshot,
            "d1",
            FeeEvent.IssueInstruction(Dues.memberActor(start.primary.id), 11),
        )
        assertIs<WorkspaceResult.Ok>(issued)
        val proof = Workspace.advanceDue(
            issued.change.snapshot,
            "d1",
            Dues.submitProof(
                record = due,
                actor = Dues.memberActor(start.primary.id),
                at = 12,
                proofHash = "a".repeat(64),
                statedAmountKobo = 100_000,
                bankReference = "BANK-1",
            ),
        )
        assertIs<WorkspaceResult.Ok>(proof)
        assertEquals(ObligationState.PROOF_SUBMITTED, proof.change.snapshot.dues.single().state)
        assertEquals(EntryKind.PROOF_RECORDED, proof.change.facts.single().kind)
    }

    @Test
    fun `preferences are presentation only and never create a fact`() {
        val start = onboarded()
        val result = Workspace.updatePreferences(start, start.preferences.copy(theme = ThemeMode.DARK))
        assertIs<WorkspaceResult.Ok>(result)
        assertTrue(result.change.facts.isEmpty())
        assertEquals(ThemeMode.DARK, result.change.snapshot.preferences.theme)
    }
}
