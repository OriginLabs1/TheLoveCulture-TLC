package culture.thelove.tlc.ledger

/** RFC 6962-style Merkle tree over entry hashes; odd nodes are duplicated at each level. */
public object Merkle {

    public data class Step(public val sibling: String, public val siblingOnRight: Boolean)

    public fun root(leaves: List<String>, hasher: Hasher): String {
        if (leaves.isEmpty()) return Ledger.GENESIS
        var level = leaves
        while (level.size > 1) {
            val next = ArrayList<String>((level.size + 1) / 2)
            var i = 0
            while (i < level.size) {
                val left = level[i]
                val right = if (i + 1 < level.size) level[i + 1] else left
                next += hashPair(left, right, hasher)
                i += 2
            }
            level = next
        }
        return level[0]
    }

    public fun proof(index: Int, leaves: List<String>, hasher: Hasher): List<Step> {
        require(index >= 0 && index < leaves.size) { "Leaf index out of range." }
        val steps = ArrayList<Step>()
        var level = leaves
        var idx = index
        while (level.size > 1) {
            val siblingIndex = if (idx % 2 == 0) idx + 1 else idx - 1
            val sibling = if (siblingIndex < level.size) level[siblingIndex] else level[idx]
            steps += Step(sibling = sibling, siblingOnRight = idx % 2 == 0)
            val next = ArrayList<String>((level.size + 1) / 2)
            var i = 0
            while (i < level.size) {
                val left = level[i]
                val right = if (i + 1 < level.size) level[i + 1] else left
                next += hashPair(left, right, hasher)
                i += 2
            }
            level = next
            idx /= 2
        }
        return steps
    }

    public fun verify(leaf: String, proof: List<Step>, expectedRoot: String, hasher: Hasher): Boolean {
        var current = leaf
        for (step in proof) {
            current = if (step.siblingOnRight) {
                hashPair(current, step.sibling, hasher)
            } else {
                hashPair(step.sibling, current, hasher)
            }
        }
        return current == expectedRoot
    }

    private fun hashPair(left: String, right: String, hasher: Hasher): String =
        Hex.encode(hasher.sha256(Hex.decode(left) + Hex.decode(right)))
}
