package culture.thelove.tlc.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Handles, the official personas, NUBAN checks and the shape that may leave the vault. */
class IdentityTest {

    @Test
    fun `a handle is lowercase, short and cannot start or end with punctuation`() {
        assertEquals("ada.okafor", Handle.parse("@Ada.Okafor")?.value)
        assertEquals("ada", Handle.parse("  ADA  ")?.value)
        assertNull(Handle.parse("a"))
        assertNull(Handle.parse(".ada"))
        assertNull(Handle.parse("ada."))
        assertNull(Handle.parse("ada okafor"))
        assertNull(Handle.parse("ada".repeat(10)))
    }

    @Test
    fun `official addresses are reserved and both resolve to the pioneer`() {
        assertTrue(Handle.isReserved("pioneer"))
        assertTrue(Handle.isReserved("@sircollins"))
        assertNull(Handle.parse("@pioneer")?.let { if (Handle.isReserved(it.value)) null else it })
        assertEquals(Persona.PIONEER, Persona.resolve(Handle.of("pioneer")))
        assertEquals(Persona.PIONEER, Persona.resolve(Handle.of("sircollins")))
        assertTrue(Persona.PIONEER.answersTo(Handle.of("sircollins")))
        assertTrue(Persona.SIR_COLLINS.answersTo(Handle.of("pioneer")))
        assertNotNull(Persona.OFFICIAL.firstOrNull { it.handle.value == "sircollins" })
    }

    @Test
    fun `a suggested handle is never reserved and is editable`() {
        val suggested = Handle.suggest("Ada Okafor", "0123456789abcdef")
        assertEquals("ada.okafor", suggested.value)
        assertFalse(Handle.isReserved(suggested.value))
        assertFalse(Handle.isReserved(Handle.suggest("Pioneer", "0123456789abcdef").value))
    }

    @Test
    fun `nuban check digit catches a typo before money moves`() {
        val sample = Nuban.sample("058")
        assertTrue(Nuban.isValid(sample, "058"))
        assertFalse(Nuban.isValid(sample.dropLast(1) + ((sample.last().digitToInt() + 1) % 10), "058"))
        assertFalse(Nuban.isValid(sample, "044"))
        assertFalse(Nuban.isValid("123", "058"))
    }

    @Test
    fun `only the last four digits and a fingerprint may leave the vault`() {
        val bank = BankDirectory.byCode("058")!!
        val account = BankAccount(bank, Nuban.sample("058"), "Ada Okafor")
        val ref = account.ref("bank-1", 1_700_000_000_000)
        assertEquals(account.accountNumber.takeLast(4), ref.last4)
        assertFalse(ref.toString().contains(account.accountNumber))
        assertFalse(ref.label.contains(account.accountNumber))
        assertEquals("•••• •••• ${account.accountNumber.takeLast(4)}", ref.masked)
        assertEquals(32, ref.fingerprint.length)
        assertEquals(ref.fingerprint, account.ref("bank-2", 5).fingerprint)
    }
}
