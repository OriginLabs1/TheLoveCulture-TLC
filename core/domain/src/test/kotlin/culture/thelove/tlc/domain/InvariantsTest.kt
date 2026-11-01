package culture.thelove.tlc.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** INV-03/INV-04/INV-05 — what a token is, who may sign, when support may be offered. */
class InvariantsTest {

    @Test
    fun `every token is a recognition signal and nothing else`() {
        assertEquals(1, TokenNature.entries.size, "A second token nature would break INV-03")
        TokenKind.entries.forEach { assertEquals(TokenNature.RECOGNITION_SIGNAL, it.nature) }
    }

    @Test
    fun `pioneer fees are exactly the platform tokens`() {
        assertEquals(TokenKind.platform, TokenKind.entries.filter { it.pioneerFee }.toSet())
        assertEquals(TokenKind.community, TokenKind.entries.filterNot { it.pioneerFee }.toSet())
        assertEquals(7, TokenKind.entries.size)
    }

    @Test
    fun `community tokens never route to the pioneer`() {
        TokenKind.community.forEach { assertTrue(it.rail != Rail.PIONEER_DIRECT, "$it routes to the pioneer") }
        TokenKind.platform.forEach { assertEquals(Rail.PIONEER_DIRECT, it.rail) }
    }

    @Test
    fun `money cannot be negative`() {
        assertTrue(runCatching { Money(-1) }.isFailure)
        assertEquals(0, Money.ZERO.kobo)
        assertEquals("NGN 1000.00", Money.naira(1_000).render())
        assertEquals("NGN 0.05", Money(5).render())
    }

    @Test
    fun `the financial notice states the non custodial position verbatim`() {
        val notice = DoctrineCopy.FINANCIAL_NOTICE
        listOf("does not hold, pool, escrow or move money", "directly to the intended recipient", "no cash value")
            .forEach { assertTrue(notice.contains(it), "notice is missing: $it") }
    }

    @Test
    fun `two signatures are required for irreversible acts`() {
        val dyad = Dyad("dyad:1", PartyId("alice"), PartyId("bob"))
        val action = SensitiveAction.CHANGE_BANK_DETAILS
        assertEquals(setOf(PartyId("bob")), assertIs<DyadDecision.Pending>(Stewardship.decide(dyad, action, setOf(PartyId("alice")))).missing)
        assertEquals(DyadDecision.Approved, Stewardship.decide(dyad, action, dyad.members))
    }

    @Test
    fun `veto is symmetric and single`() {
        val dyad = Dyad("dyad:1", PartyId("alice"), PartyId("bob"))
        for (vetoer in dyad.members) {
            val decision = Stewardship.decide(dyad, SensitiveAction.DISSOLVE_DYAD, dyad.members, setOf(vetoer))
            assertEquals(DyadDecision.Blocked(vetoer), decision, "veto by $vetoer was ignored")
        }
    }

    @Test
    fun `non members cannot participate in a dyad decision`() {
        val dyad = Dyad("dyad:1", PartyId("alice"), PartyId("bob"))
        val decision = Stewardship.decide(
            dyad,
            SensitiveAction.GRANT_DELEGATION,
            dyad.members,
            setOf(PartyId("mallory")),
        )
        assertEquals(DyadDecision.Blocked(PartyId("mallory")), decision)
    }

    @Test
    fun `receptive mode is required for singles and irrelevant for married models`() {
        val alice = PartyId("alice")
        val bob = PartyId("bob")
        assertEquals(
            SupportGate.Allowed,
            SupportPolicy.gate(Role.LOVE_MODEL, ReceptiveMode.OFF, alice, bob, TokenKind.SUPPORTIVE),
        )
        assertIs<SupportGate.Refused>(
            SupportPolicy.gate(Role.SINGLE, ReceptiveMode.OFF, alice, bob, TokenKind.SUPPORTIVE),
        )
        assertEquals(
            SupportGate.Allowed,
            SupportPolicy.gate(Role.SINGLE, ReceptiveMode.ON, alice, bob, TokenKind.SUPPORTIVE),
        )
        assertIs<SupportGate.Refused>(
            SupportPolicy.gate(Role.SINGLE, ReceptiveMode.ON, alice, alice, TokenKind.SUPPORTIVE),
        )
        assertIs<SupportGate.Refused>(
            SupportPolicy.gate(Role.LOVE_MODEL, ReceptiveMode.ON, alice, bob, TokenKind.ACTIVATION),
        )
    }
}
