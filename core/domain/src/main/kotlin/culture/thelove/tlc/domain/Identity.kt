package culture.thelove.tlc.domain

import java.security.MessageDigest

/**
 * A TLC handle: the address a person or a role is known by, on this device today and across
 * devices when accounts arrive. Handles are deliberately small and strict: lowercase ASCII,
 * digits, dots and underscores, never starting or ending with punctuation. The `@` is rendered,
 * never stored.
 *
 * Reserved handles belong to TLC itself. `@pioneer` is the official role address and
 * `@sircollins` is the person behind it; both are reserved so nobody can impersonate them, and
 * both resolve to the same persona.
 */
@ConsistentCopyVisibility
public data class Handle private constructor(public val value: String) {
    override fun toString(): String = "@$value"

    public companion object {
        public const val MIN_LENGTH: Int = 2
        public const val MAX_LENGTH: Int = 24

        private val PATTERN: Regex = Regex("[a-z0-9](?:[a-z0-9._]{0,22}[a-z0-9])?")

        /** Addresses TLC keeps for itself. A person may not claim one. */
        public val RESERVED: Set<String> = setOf(
            "pioneer",
            "sircollins",
            "tlc",
            "theloveculture",
            "loveculture",
            "conceptpioneer",
            "admin",
            "support",
            "system",
            "steward",
        )

        /** Official personas that exist whether or not a device has met them yet. */
        public val OFFICIAL: Set<String> = setOf("pioneer", "sircollins")

        public fun normalize(raw: String): String =
            raw.trim().removePrefix("@").removePrefix("@").lowercase()

        /** `null` when the text is not a usable handle; never throws on user input. */
        public fun parse(raw: String): Handle? {
            val clean = normalize(raw)
            if (clean.length !in MIN_LENGTH..MAX_LENGTH) return null
            if (!PATTERN.matches(clean)) return null
            return Handle(clean)
        }

        public fun of(raw: String): Handle =
            parse(raw) ?: throw IllegalArgumentException(
                "A handle is $MIN_LENGTH–$MAX_LENGTH lowercase letters, digits, dots or underscores, " +
                    "and cannot start or end with punctuation.",
            )

        public fun isReserved(raw: String): Boolean = normalize(raw) in RESERVED

        /** A deterministic starting point that the person can edit before finishing onboarding. */
        public fun suggest(displayName: String, fallbackId: String): Handle {
            val base = displayName.lowercase()
                .map { if (it.isLetterOrDigit()) it else '.' }
                .joinToString("")
                .trim('.')
                .replace(Regex("\\.{2,}"), ".")
                .take(MAX_LENGTH)
                .trim('.')
            val candidate = if (base.length >= MIN_LENGTH) base else "member.${fallbackId.take(6)}"
            val clean = candidate.take(MAX_LENGTH).trim('.')
            if (isReserved(clean)) return of("$clean.${fallbackId.take(4)}")
            return parse(clean) ?: of("member.${fallbackId.take(6)}")
        }
    }
}

public enum class PersonaKind {
    /** A person known to this circle. */
    PERSON,

    /** A steward seat inside this circle. */
    STEWARD,

    /** The Concept Pioneer, a role. */
    CONCEPT_PIONEER,
}

/**
 * One addressable identity. A role and the person behind it are linked through [aliases] so the
 * record can tell "the Pioneer (role)" from "Sir Collins (person)" without pretending they are
 * two different people.
 */
public data class Persona(
    public val id: String,
    public val handle: Handle,
    public val displayName: String,
    public val kind: PersonaKind = PersonaKind.PERSON,
    public val aliases: Set<Handle> = emptySet(),
    public val role: Role? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A persona needs a stable id." }
        require(displayName.isNotBlank() && displayName.length <= 80) {
            "A persona name is 1–80 characters."
        }
        require(aliases.none { it == handle }) { "A persona cannot alias itself." }
    }

    public fun answersTo(candidate: Handle): Boolean = candidate == handle || candidate in aliases

    public companion object {
        /** The official role address. */
        public val PIONEER: Persona = Persona(
            id = "concept-pioneer",
            handle = Handle.of("pioneer"),
            displayName = "Concept Pioneer",
            kind = PersonaKind.CONCEPT_PIONEER,
            aliases = setOf(Handle.of("sircollins")),
        )

        /** The person behind the role, reachable at either address. */
        public val SIR_COLLINS: Persona = Persona(
            id = "sir-collins",
            handle = Handle.of("sircollins"),
            displayName = "Sir Collins",
            kind = PersonaKind.CONCEPT_PIONEER,
            aliases = setOf(Handle.of("pioneer")),
        )

        public val OFFICIAL: List<Persona> = listOf(PIONEER, SIR_COLLINS)

        public fun resolve(candidate: Handle): Persona? =
            OFFICIAL.firstOrNull { it.answersTo(candidate) }
    }
}

public data class Bank(public val code: String, public val name: String) {
    init {
        require(code.matches(Regex("\\d{3}"))) { "A CBN bank code is three digits." }
        require(name.isNotBlank()) { "A bank needs a name." }
    }
}

/** A bundled, offline directory. No network lookup is ever made. */
public object BankDirectory {

    public val NIGERIA: List<Bank> = listOf(
        Bank("011", "First Bank of Nigeria"),
        Bank("023", "Citibank Nigeria"),
        Bank("030", "Heritage Bank"),
        Bank("032", "Union Bank of Nigeria"),
        Bank("033", "United Bank for Africa"),
        Bank("035", "Wema Bank"),
        Bank("044", "Access Bank"),
        Bank("050", "Ecobank Nigeria"),
        Bank("057", "Zenith Bank"),
        Bank("058", "Guaranty Trust Bank"),
        Bank("068", "Standard Chartered Bank"),
        Bank("070", "Fidelity Bank"),
        Bank("076", "Polaris Bank"),
        Bank("082", "Keystone Bank"),
        Bank("100", "SunTrust Bank"),
        Bank("101", "Providus Bank"),
        Bank("102", "Titan Trust Bank"),
        Bank("214", "First City Monument Bank"),
        Bank("215", "Unity Bank"),
        Bank("221", "Stanbic IBTC Bank"),
        Bank("232", "Sterling Bank"),
        Bank("301", "Jaiz Bank"),
    )

    public fun byCode(code: String): Bank? = NIGERIA.firstOrNull { it.code == code }

    public fun search(query: String): List<Bank> {
        val clean = query.trim()
        if (clean.isEmpty()) return NIGERIA
        return NIGERIA.filter {
            it.name.contains(clean, ignoreCase = true) || it.code.startsWith(clean)
        }
    }
}

/**
 * Nigerian Uniform Bank Account Number validation, done locally.
 *
 * A NUBAN is ten digits: the first three are the CBN bank code, the last is a check digit over
 * the first nine with the published weights. This catches typos before a person sends money to
 * the wrong place. It is not a name enquiry and it does not prove ownership.
 */
public object Nuban {

    private val WEIGHTS: IntArray = intArrayOf(3, 7, 3, 3, 7, 3, 3, 7, 3)

    public fun checkDigit(firstNine: String): Int? {
        if (!firstNine.matches(Regex("\\d{9}"))) return null
        var sum = 0
        firstNine.forEachIndexed { index, character ->
            sum += character.digitToInt() * WEIGHTS[index]
        }
        return (10 - (sum % 10)) % 10
    }

    public fun isValid(accountNumber: String, bankCode: String): Boolean {
        if (!accountNumber.matches(Regex("\\d{10}"))) return false
        if (!bankCode.matches(Regex("\\d{3}"))) return false
        if (!accountNumber.startsWith(bankCode)) return false
        val expected = checkDigit(accountNumber.take(9)) ?: return false
        return accountNumber.last().digitToInt() == expected
    }

    /** Builds a syntactically valid NUBAN for a bank code; used by tests and previews only. */
    public fun sample(bankCode: String, seed: String = "123456"): String {
        val firstNine = bankCode + seed.padEnd(6, '0').take(6)
        val check = checkDigit(firstNine) ?: 0
        return "$firstNine$check"
    }
}

/**
 * A full bank account, held only inside the encrypted vault. It is never written to the state
 * file, the bundle, the ledger, a log or a notification.
 */
public data class BankAccount(
    public val bank: Bank,
    public val accountNumber: String,
    public val holderName: String,
) {
    init {
        require(Nuban.isValid(accountNumber, bank.code)) {
            "That account number does not match ${bank.name}'s NUBAN check digit."
        }
        require(holderName.isNotBlank() && holderName.length <= 120) {
            "Name the account holder."
        }
    }

    public val last4: String get() = accountNumber.takeLast(4)

    /** A stable, non-reversible digest used to detect a changed destination. */
    public fun fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("tlc-bank-v1|${bank.code}|$accountNumber|${holderName.trim()}".toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    public fun ref(id: String, savedAt: Long): BankAccountRef = BankAccountRef(
        id = id,
        bankCode = bank.code,
        bankName = bank.name,
        last4 = last4,
        holderName = holderName.trim(),
        fingerprint = fingerprint(),
        savedAt = savedAt,
    )
}

/** The only bank shape that may enter app state, a bundle, an export or a log. */
public data class BankAccountRef(
    public val id: String,
    public val bankCode: String,
    public val bankName: String,
    public val last4: String,
    public val holderName: String,
    public val fingerprint: String,
    public val savedAt: Long,
) {
    init {
        require(id.isNotBlank() && id.length <= 64) { "A bank reference needs a stable id." }
        require(bankCode.matches(Regex("\\d{3}"))) { "A bank reference needs a CBN code." }
        require(bankName.isNotBlank()) { "A bank reference needs a bank name." }
        require(last4.matches(Regex("\\d{4}"))) { "Only the last four digits are kept." }
        require(holderName.isNotBlank() && holderName.length <= 120) { "A bank reference needs a holder." }
        require(fingerprint.matches(Regex("[0-9a-f]{32}"))) { "A fingerprint is 32 lowercase hex characters." }
        require(savedAt > 0) { "A bank reference needs a save time." }
    }

    /** What the UI may show without a deliberate reveal. */
    public val masked: String get() = "•••• •••• $last4"

    public val label: String get() = "$bankName · $masked"
}

/** Why a person is making contact. The purpose exists before the message does. */
public enum class ContactPurpose {
    ACTIVATION,
    REACTIVATION,
    PIONEER_TOKEN,
    SUPPORT,
    MENTORSHIP,
    COLLABORATION,
    PARTNERSHIP,
    REPORT,
    OTHER,
    ;

    public val label: String
        get() = when (this) {
            ACTIVATION -> "Activation help"
            REACTIVATION -> "Reactivation help"
            PIONEER_TOKEN -> "Pioneer's Token question"
            SUPPORT -> "Support or transfer help"
            MENTORSHIP -> "Mentorship request"
            COLLABORATION -> "Collaboration"
            PARTNERSHIP -> "Partnership or media"
            REPORT -> "Report a problem"
            OTHER -> "Something else"
        }
}

public enum class ContactChannel {
    TELEGRAM,
    EMAIL,
    PHONE,
    WEBSITE,
}

/**
 * The local slice of the Contact & Consent Framework: a request has a purpose and a category
 * before it has a body, and the body is shown to the sender before it leaves the app.
 */
public data class ContactIntent(
    public val purpose: ContactPurpose,
    public val category: String? = null,
    public val expectedGuidance: String? = null,
) {
    init {
        require(category == null || category.length <= 120) { "A category is at most 120 characters." }
        require(expectedGuidance == null || expectedGuidance.length <= 280) {
            "Expected guidance is at most 280 characters."
        }
    }

    /** The opening line that carries the intent into the message. */
    public fun opening(role: Role): String = when (purpose) {
        ContactPurpose.MENTORSHIP ->
            "Hello, I am writing as a ${roleWord(role)} to request mentorship. " +
                (expectedGuidance?.takeIf { it.isNotBlank() }?.let { "What I hope for: $it. " } ?: "")
        ContactPurpose.ACTIVATION ->
            "Hello, I am writing as a ${roleWord(role)} about Activation. "
        ContactPurpose.REACTIVATION ->
            "Hello, I am writing as a ${roleWord(role)} about Reactivation. "
        ContactPurpose.PIONEER_TOKEN ->
            "Hello, I have a question about the Pioneer's Token. "
        ContactPurpose.SUPPORT ->
            "Hello, I need help with a direct Support transfer. "
        ContactPurpose.COLLABORATION ->
            "Hello, I would like to explore a collaboration. "
        ContactPurpose.PARTNERSHIP ->
            "Hello, I am writing about a partnership or media request. "
        ContactPurpose.REPORT ->
            "Hello, I want to report a problem. "
        ContactPurpose.OTHER ->
            "Hello, I would like to ask something. "
    } + (category?.takeIf { it.isNotBlank() }?.let { "Category: $it. " } ?: "")

    private fun roleWord(role: Role): String = when (role) {
        Role.SINGLE -> "Single"
        Role.I_PARTNER -> "Intentional Partner"
        Role.LOVE_MODEL -> "Love Model"
    }
}
