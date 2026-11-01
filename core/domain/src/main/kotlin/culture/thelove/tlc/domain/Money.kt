package culture.thelove.tlc.domain

/**
 * Value in Nigerian kobo. Integer only — never [Double] (SK-01).
 *
 * Negative amounts are unrepresentable by construction: TLC has no balances, no debt and no
 * float, so a negative quantity has no meaning anywhere in the domain (INV-01, INV-03).
 */
public data class Money(public val kobo: Long) : Comparable<Money> {

    init {
        require(kobo >= 0) {
            "Money must be non-negative: TLC models no balance, debt, float or credit (INV-01/INV-03)."
        }
    }

    public operator fun plus(other: Money): Money {
        require(other.kobo <= Long.MAX_VALUE - kobo) { "Money overflow." }
        return Money(kobo + other.kobo)
    }

    public operator fun minus(other: Money): Money = Money(kobo - other.kobo)

    override fun compareTo(other: Money): Int = kobo.compareTo(other.kobo)

    /** Plain, deterministic, locale-free rendering. UI layers format for humans. */
    public fun render(): String {
        val major = kobo / 100
        val minor = (kobo % 100).toString().padStart(2, '0')
        return "NGN $major.$minor"
    }

    public companion object {
        public val ZERO: Money = Money(0)

        public fun naira(amount: Long): Money {
            require(amount >= 0) { "Money must be non-negative." }
            require(amount <= Long.MAX_VALUE / 100) { "Money overflow." }
            return Money(amount * 100)
        }
    }
}
