package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.BankDirectory
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.domain.ContactIntent
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.Nuban
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.ledger.EntryKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Trust & Contact: a handle, a person, a vault reference, a Support intention and a contact note.
 * None of these may become a balance, and no full account number may reach the state file.
 */
class TrustContactTest {

    private val recordId = "0123456789abcdef0123456789abcdef"

    private fun onboarded(): AppSnapshot =
        (Workspace.onboard("Ada Okafor", "Okafor Circle", Role.LOVE_MODEL, recordId, 1) as WorkspaceResult.Ok)
            .change.snapshot

    private fun account(): BankAccount =
        BankAccount(BankDirectory.byCode("058")!!, Nuban.sample("058"), "Ada Okafor")

    @Test
    fun `onboarding claims an editable handle and links the steward`() {
        val snapshot = onboarded()
        assertEquals(Handle.of("ada.okafor"), snapshot.profile.handle)
        assertEquals(Handle.of("ada.okafor"), snapshot.primary.handle)
    }

    @Test
    fun `a reserved handle cannot be claimed by a person`() {
        val snapshot = onboarded()
        val result = Workspace.claimHandle(snapshot, Handle.of("pioneer"), 2)
        assertIs<WorkspaceResult.Rejected>(result)

        val person = Workspace.addPerson(
            snapshot = snapshot,
            id = "person-1",
            handle = Handle.of("sircollins"),
            displayName = "Impostor",
            role = Role.SINGLE,
            now = 3,
        )
        assertIs<WorkspaceResult.Rejected>(person)
    }

    @Test
    fun `a person is addressed by handle and never by an account number`() {
        val snapshot = onboarded()
        val saved = Workspace.saveBankAccount(snapshot, "bank-1", account(), 10)
        assertIs<WorkspaceResult.Ok>(saved)
        val ref = saved.change.snapshot.bankAccounts.single()
        val added = Workspace.addPerson(
            snapshot = saved.change.snapshot,
            id = "person-1",
            handle = Handle.of("tunde"),
            displayName = "Tunde",
            role = Role.I_PARTNER,
            now = 11,
            bankRefId = ref.id,
        )
        assertIs<WorkspaceResult.Ok>(added)
        val person = added.change.snapshot.people.single()
        assertEquals(Handle.of("tunde"), person.handle)
        assertEquals(ref.id, person.bankRefId)
        assertFalse(person.toString().contains(account().accountNumber))
    }

    @Test
    fun `state carries a vault reference, never the full account number`() {
        val snapshot = (Workspace.saveBankAccount(onboarded(), "bank-1", account(), 10) as WorkspaceResult.Ok)
            .change.snapshot
        val encoded = StateCodec.encode(snapshot)
        assertFalse(encoded.contains(account().accountNumber))
        val decoded = StateCodec.decode(encoded)
        assertIs<StateResult.Ok>(decoded)
        assertEquals(snapshot.bankAccounts, decoded.snapshot.bankAccounts)
    }

    @Test
    fun `revealing details is an event, not a stored secret`() {
        val snapshot = (Workspace.saveBankAccount(onboarded(), "bank-1", account(), 10) as WorkspaceResult.Ok)
            .change.snapshot
        val revealed = Workspace.markBankRevealed(snapshot, "bank-1", 12)
        assertIs<WorkspaceResult.Ok>(revealed)
        assertEquals(EntryKind.BANK_DETAILS_REVEALED, revealed.change.facts.single().kind)
        assertFalse(revealed.change.facts.single().subject.contains(account().accountNumber))
    }

    @Test
    fun `support is an intention, a claim, then an acknowledgement`() {
        val snapshot = onboarded()
        val sent = Workspace.sendSupport(
            snapshot = snapshot,
            id = "support-1",
            toHandle = Handle.of("tunde"),
            kind = TokenKind.SUPPORTIVE,
            amountKobo = 500_000,
            now = 20,
        )
        assertIs<WorkspaceResult.Ok>(sent)
        val support = sent.change.snapshot.supports.single()
        assertEquals(SupportState.INTENT, support.state)
        assertTrue(support.reference!!.startsWith("TLC-"))
        assertEquals(EntryKind.SUPPORT_INTENT, sent.change.facts.single().kind)

        val claimed = Workspace.claimSupport(sent.change.snapshot, "support-1", "GTB-123", 21)
        assertIs<WorkspaceResult.Ok>(claimed)
        assertEquals(SupportState.CLAIMED, claimed.change.snapshot.supports.single().state)
        assertEquals(21L, claimed.change.snapshot.supports.single().claimedAt)

        val acknowledged = Workspace.acknowledgeSupport(claimed.change.snapshot, "support-1", 22)
        assertIs<WorkspaceResult.Ok>(acknowledged)
        assertEquals(SupportState.ACKNOWLEDGED, acknowledged.change.snapshot.supports.single().state)
        assertEquals(EntryKind.SUPPORT_ACKNOWLEDGED, acknowledged.change.facts.single().kind)
    }

    @Test
    fun `recognition carries no amount and platform tokens cannot be Support`() {
        val snapshot = onboarded()
        assertIs<WorkspaceResult.Rejected>(
            Workspace.sendSupport(snapshot, "s1", Handle.of("tunde"), TokenKind.RECOGNITION, 100, 1),
        )
        assertIs<WorkspaceResult.Rejected>(
            Workspace.sendSupport(snapshot, "s2", Handle.of("tunde"), TokenKind.SUPPORTIVE, null, 1),
        )
        assertIs<WorkspaceResult.Rejected>(
            Workspace.sendSupport(snapshot, "s3", Handle.of("tunde"), TokenKind.ACTIVATION, 100, 1),
        )
    }

    @Test
    fun `a profile name and photo are saved as a fact, not an image`() {
        val updated = Workspace.updateProfile(
            snapshot = onboarded(),
            displayName = "  Ada New  ",
            circleName = "  New Circle  ",
            photoRef = "avatar-abc123.jpg",
            now = 40,
        )
        assertIs<WorkspaceResult.Ok>(updated)
        assertEquals("Ada New", updated.change.snapshot.profile.displayName)
        assertEquals("New Circle", updated.change.snapshot.profile.circleName)
        assertEquals("avatar-abc123.jpg", updated.change.snapshot.profile.photoRef)
        assertEquals("Ada New", updated.change.snapshot.primary.name)
        assertEquals(EntryKind.PROFILE_UPDATED, updated.change.facts.single().kind)
    }

    @Test
    fun `a receipt belongs to one token and can be replaced, revoked and removed`() {
        val withSupport = (
            Workspace.sendSupport(
                snapshot = onboarded(),
                id = "support-1",
                toHandle = Handle.of("tunde"),
                kind = TokenKind.SUPPORTIVE,
                amountKobo = 500_000,
                now = 20,
            ) as WorkspaceResult.Ok
            ).change.snapshot

        fun receipt(id: String, name: String): TokenReceipt = TokenReceipt(
            id = id,
            subjectKind = ReceiptSubject.SUPPORT,
            subjectId = "support-1",
            fileName = name,
            hash = "a".repeat(64),
            audience = ReceiptAudience.THIS_TRANSFER,
            addedAt = 50,
        )

        val attached = Workspace.attachReceipt(withSupport, receipt("receipt-1", "receipt-one.jpg"), 50)
        assertIs<WorkspaceResult.Ok>(attached)
        assertEquals(EntryKind.RECEIPT_ADDED, attached.change.facts.single().kind)
        assertEquals(1, attached.change.snapshot.receipts.size)

        // Replacing keeps one current image per token: the old one is not a second receipt.
        val replaced = Workspace.attachReceipt(
            attached.change.snapshot,
            receipt("receipt-2", "receipt-two.jpg"),
            51,
        )
        assertIs<WorkspaceResult.Ok>(replaced)
        assertEquals(listOf("receipt-2"), replaced.change.snapshot.receipts.map { it.id })

        val shared = Workspace.setReceiptAudience(replaced.change.snapshot, "receipt-2", ReceiptAudience.MY_CIRCLE)
        assertIs<WorkspaceResult.Ok>(shared)
        assertEquals(ReceiptAudience.MY_CIRCLE, shared.change.snapshot.receipts.single().audience)
        assertEquals(EntryKind.RECEIPT_AUDIENCE_CHANGED, shared.change.facts.single().kind)

        val removed = Workspace.removeReceipt(shared.change.snapshot, "receipt-2")
        assertIs<WorkspaceResult.Ok>(removed)
        assertTrue(removed.change.snapshot.receipts.isEmpty())
        assertEquals(EntryKind.RECEIPT_REMOVED, removed.change.facts.single().kind)
    }

    @Test
    fun `a receipt cannot attach to a token that is not here`() {
        val orphan = TokenReceipt(
            id = "receipt-1",
            subjectKind = ReceiptSubject.GRANT,
            subjectId = "missing-grant",
            fileName = "receipt-one.jpg",
            hash = "a".repeat(64),
            audience = ReceiptAudience.ONLY_ME,
            addedAt = 50,
        )
        assertIs<WorkspaceResult.Rejected>(Workspace.attachReceipt(onboarded(), orphan, 50))
    }

    @Test
    fun `contact records purpose and channel but never the message body`() {
        val snapshot = onboarded()
        val logged = Workspace.logContact(
            snapshot = snapshot,
            id = "contact-1",
            channel = ContactChannel.TELEGRAM,
            purpose = ContactPurpose.MENTORSHIP,
            category = "Mentorship",
            now = 30,
        )
        assertIs<WorkspaceResult.Ok>(logged)
        val event = logged.change.snapshot.contactLog.single()
        assertEquals(ContactPurpose.MENTORSHIP, event.purpose)
        assertEquals(EntryKind.CONTACT_RECORDED, logged.change.facts.single().kind)
        assertEquals(1, StateCodec.encode(logged.change.snapshot).split("contact_log").size - 1)
        assertTrue(Workspace.clearContactLog(logged.change.snapshot).let {
            (it as WorkspaceResult.Ok).change.snapshot.contactLog.isEmpty()
        })
    }

    @Test
    fun `contact opening carries the intent before the body`() {
        val intent = ContactIntent(
            purpose = ContactPurpose.MENTORSHIP,
            category = "Mentorship",
            expectedGuidance = "How to structure a first circle",
        )
        val opening = intent.opening(Role.LOVE_MODEL)
        assertTrue(opening.contains("Love Model"))
        assertTrue(opening.contains("Mentorship"))
        assertTrue(opening.contains("How to structure"))
    }

    @Test
    fun `a v1 state gains a suggested handle on migration`() {
        val v1 = """
            {"format":"tlc-state-v1",
             "profile":{"display_name":"Ada","role":"SINGLE","record_id":"$recordId","created_at":1},
             "stewards":[{"id":"$recordId","name":"Ada","role":"PRIMARY"}]}
        """.trimIndent()
        val decoded = StateCodec.decode(v1)
        assertIs<StateResult.Ok>(decoded)
        assertEquals(Handle.of("ada"), decoded.snapshot.profile.handle)
    }
}
