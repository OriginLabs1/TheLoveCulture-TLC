package culture.thelove.tlc.ledger.adapter

import culture.thelove.tlc.ledger.Hasher
import java.security.MessageDigest

/**
 * JVM/Android adapter. This is the single permitted platform dependency in the ledger module;
 * the Swift/iOS and embedded targets supply their own audited implementation of [Hasher].
 * Never replace with a hand-written digest in production (SK-09).
 */
public class PlatformSha256 : Hasher {
    override fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)
}
