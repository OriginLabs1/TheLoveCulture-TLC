package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.ObligationState
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StateCodecTest {

    private fun snapshot(): AppSnapshot = AppSnapshot(
        profile = Profile("Ada", "The Okafor Circle", Role.LOVE_MODEL, "0123456789abcdef0123456789abcdef", 1_700_000_000_000),
        preferences = Preferences(
            theme = ThemeMode.DARK,
            motion = false,
            haptics = true,
            receptiveMode = true,
            onboarded = true,
        ),
        stewards = listOf(
            Steward("0123456789abcdef0123456789abcdef", "Ada", StewardRole.PRIMARY),
            Steward("pair-1", "Tunde", StewardRole.CO),
        ),
        proposals = listOf(
            Proposal(
                id = "p1",
                action = SensitiveAction.CHANGE_BANK_DETAILS,
                summary = "Change verified details",
                createdAt = 1_700_000_000_100,
                approvals = setOf("pair-1"),
            ),
        ),
        grants = listOf(
            GrowthGrant(
                id = "g1",
                beneficiary = "Ada",
                purpose = "School fees",
                amountKobo = 25_000_00,
                pledgedAt = 1_700_000_000_200,
                stage = GrantStage.VERIFIED,
                evidence = "Receipt from the school",
                verifiedAt = 1_700_000_000_300,
                witness = "Tunde",
            ),
        ),
        dues = listOf(
            DueRecord(
                id = "d1",
                kind = TokenKind.ACTIVATION,
                amountKobo = 1000_00,
                period = FeePeriod.ONE_OFF,
                members = listOf("pair-1", "0123456789abcdef0123456789abcdef"),
                state = ObligationState.DUE,
                createdAt = 1_700_000_000_400,
            ),
        ),
    )

    @Test
    fun `encoding is deterministic and round trips every field`() {
        val original = snapshot()
        val encoded = StateCodec.encode(original)
        assertEquals(encoded, StateCodec.encode(original))

        val decoded = StateCodec.decode(encoded)
        assertIs<StateResult.Ok>(decoded)
        assertEquals(original.profile, decoded.snapshot.profile)
        assertEquals(original.preferences, decoded.snapshot.preferences)
        assertEquals(original.stewards, decoded.snapshot.stewards)
        assertEquals(original.proposals, decoded.snapshot.proposals)
        assertEquals(original.grants, decoded.snapshot.grants)
        // members are sorted by the codec; compare content, not order.
        assertEquals(original.dues.first().copy(members = original.dues.first().members.sorted()), decoded.snapshot.dues.first())
    }

    @Test
    fun `unknown format is refused rather than guessed`() {
        val text = StateCodec.encode(snapshot()).replace("tlc-state-v1", "tlc-state-v999")
        val decoded = StateCodec.decode(text)
        assertIs<StateResult.Invalid>(decoded)
        assertTrue(decoded.reason.contains("Unsupported state format"))
    }

    @Test
    fun `a hand-edited invalid profile cannot become a screen`() {
        val text = StateCodec.encode(snapshot())
            .replace("\"record_id\":\"0123456789abcdef0123456789abcdef\"", "\"record_id\":\"nope\"")
        val decoded = StateCodec.decode(text)
        assertIs<StateResult.Invalid>(decoded)
    }

    @Test
    fun `optional collections default to empty for a forward migration`() {
        val minimal = """
            {"format":"tlc-state-v1",
             "profile":{"display_name":"Ada","role":"SINGLE","record_id":"0123456789abcdef0123456789abcdef","created_at":1},
             "stewards":[{"id":"0123456789abcdef0123456789abcdef","name":"Ada","role":"PRIMARY"}]}
        """.trimIndent()
        val decoded = StateCodec.decode(minimal)
        assertIs<StateResult.Ok>(decoded)
        assertTrue(decoded.snapshot.grants.isEmpty())
        assertTrue(decoded.snapshot.dues.isEmpty())
        assertEquals(Preferences(), decoded.snapshot.preferences)
    }

    @Test
    fun `oversized state is refused`() {
        val decoded = StateCodec.decode("x".repeat(StateCodec.MAX_CHARS + 1))
        assertIs<StateResult.Invalid>(decoded)
    }
}
