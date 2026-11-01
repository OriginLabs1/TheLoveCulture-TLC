package culture.thelove.tlc.ledger

import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The daily anchor is only trustworthy if the tree, the proofs and the root agree. */
class MerkleTest {

    private val hasher = PlatformSha256()

    private fun leaves(n: Int): List<String> = (1..n).map { i ->
        Hex.encode(hasher.sha256("leaf-$i".encodeToByteArray()))
    }

    @Test
    fun `single leaf is its own root`() {
        val leaf = leaves(1)
        assertEquals(leaf[0], Merkle.root(leaf, hasher))
        assertTrue(Merkle.verify(leaf[0], Merkle.proof(0, leaf, hasher), Merkle.root(leaf, hasher), hasher))
    }

    @Test
    fun `every leaf has a verifying proof for 1 to 32 leaves`() {
        for (n in 1..32) {
            val level = leaves(n)
            val root = Merkle.root(level, hasher)
            for (i in level.indices) {
                val proof = Merkle.proof(i, level, hasher)
                assertTrue(Merkle.verify(level[i], proof, root, hasher), "n=$n i=$i")
            }
        }
    }

    @Test
    fun `a tampered leaf fails its proof`() {
        val level = leaves(8)
        val root = Merkle.root(level, hasher)
        val proof = Merkle.proof(3, level, hasher)
        assertFalse(Merkle.verify(leaves(9)[8], proof, root, hasher))
    }

    @Test
    fun `the root changes when any leaf changes`() {
        val level = leaves(5).toMutableList()
        val before = Merkle.root(level, hasher)
        level[4] = Hex.encode(hasher.sha256("tampered".encodeToByteArray()))
        assertTrue(before != Merkle.root(level, hasher))
    }

    @Test
    fun `an empty tree commits to genesis`() {
        assertEquals(Ledger.GENESIS, Merkle.root(emptyList(), hasher))
    }
}
