package culture.thelove.tlc.domain

/** Stable identifier of a person (DID/ULID in production; opaque here). */
public data class PartyId(public val value: String) {
    init {
        require(value.isNotBlank()) { "PartyId must not be blank." }
    }
}

/** Za three roles. Nothing else may ever be added without a doctrine review (CONTEXT §4). */
public enum class Role {
    SINGLE,
    I_PARTNER,
    LOVE_MODEL,
}

/**
 * The nature of every TLC token. There is exactly one possible value, and that is the point:
 * a token with a monetary nature cannot be constructed without deleting this enum member and
 * failing [culture.thelove.tlc.domain.TokenInvariants]. (INV-03)
 */
public enum class TokenNature {
    RECOGNITION_SIGNAL,
}

/** Where a token's value actually travels. TLC appears in none of these paths as a party (INV-02). */
public enum class Rail {
    /** Sender's bank -> recipient's bank, direct. */
    PEER_TO_PEER,

    /** Direct, but the community witnesses a milestone before the sender pays. TLC holds nothing. */
    PEER_TO_PEER_WITNESSED,

    /** Love Model / user -> Concept Pioneer's own account, for platform dues and gifts only. */
    PIONEER_DIRECT,
}

/** The seven token kinds of the Standard Economic Framework. */
public enum class TokenKind(
    public val nature: TokenNature,
    public val rail: Rail,
    public val pioneerFee: Boolean,
) {
    SUPPORTIVE(TokenNature.RECOGNITION_SIGNAL, Rail.PEER_TO_PEER, false),
    RECOGNITION(TokenNature.RECOGNITION_SIGNAL, Rail.PEER_TO_PEER, false),
    GROWTH_GRANT(TokenNature.RECOGNITION_SIGNAL, Rail.PEER_TO_PEER_WITNESSED, false),
    ACTIVATION(TokenNature.RECOGNITION_SIGNAL, Rail.PIONEER_DIRECT, true),
    REACTIVATION(TokenNature.RECOGNITION_SIGNAL, Rail.PIONEER_DIRECT, true),
    COURTESY(TokenNature.RECOGNITION_SIGNAL, Rail.PIONEER_DIRECT, true),
    JOY(TokenNature.RECOGNITION_SIGNAL, Rail.PIONEER_DIRECT, true),
    ;

    /** True when the token is a voluntary gift rather than an obligation. */
    public val voluntary: Boolean
        get() = this == COURTESY || this == JOY

    public companion object {
        public val community: Set<TokenKind> = setOf(SUPPORTIVE, RECOGNITION, GROWTH_GRANT)
        public val platform: Set<TokenKind> = setOf(ACTIVATION, REACTIVATION, COURTESY, JOY)
    }
}

/** Who is performing an action. Authority is a property of the actor, never of the link (INV-04). */
public enum class ActorKind {
    /** A person acting on their own account or their dyad. */
    MEMBER,

    /** Scoped delegate (Account Steward / Community Manager). */
    STEWARD,

    /** The Concept Pioneer: attestation of Pioneer fees only. */
    PIONEER,

    /** Automation (scheduler, anchor job). Never authorised to attest money. */
    SYSTEM,
}

public data class Actor(public val id: PartyId, public val kind: ActorKind) {

    public val isAuthority: Boolean
        get() = kind == ActorKind.STEWARD || kind == ActorKind.PIONEER
}
