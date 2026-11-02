package culture.thelove.tlc.ledger

/**
 * SK-04: where the record lives is a port, not an assumption.
 *
 * The ledger core never touches a filesystem, a database or Android. It asks for a
 * [LogStorage]; the JVM/Android adapter supplies one. That keeps the storage policy
 * (atomic replace, fsync, permissions) outside the rules, and makes the rules testable
 * against a simulated crash or a tampered file.
 *
 * Contract for implementors:
 *  - [appendLine] must be durable *before it returns*, not merely buffered;
 *  - [replaceLog] must be atomic, so a crash leaves either the old log or the new one;
 *  - reads return `null` when nothing has been written yet, never an empty string;
 *  - nothing here may reorder, merge or drop lines.
 */
public interface LogStorage {

    /** The whole log, or `null` if it has never been written. */
    public fun readLog(): String?

    /** Append exactly one line (no separators are added by the caller). Must be durable on return. */
    public fun appendLine(line: String)

    /** Replace the log atomically. Used only to repair a partial write or to import a record. */
    public fun replaceLog(text: String)

    /** The local seal (`length:head:root`), or `null`. It is a hint, not an authority. */
    public fun readSeal(): String?

    /** Persist the local seal. Must be durable on return. */
    public fun writeSeal(text: String)
}

/**
 * Storage that lives for the length of the process. It is the reference implementation for
 * tests, and it can simulate the two failures that matter: a failed durable write, and a
 * hard restart.
 */
public class InMemoryLogStorage(initialLog: String? = null, initialSeal: String? = null) : LogStorage {

    private var log: String? = initialLog
    private var seal: String? = initialSeal

    /** When true, the next write throws once and is then cleared — a simulated disk failure. */
    public var failNextWrite: Boolean = false

    /** Number of durable writes, for tests that care about ordering. */
    public var writes: Int = 0
        private set

    override fun readLog(): String? = log

    override fun appendLine(line: String) {
        failIfAsked()
        val current = log ?: ""
        require(current.isEmpty() || current.endsWith('\n')) {
            "appendLine called on a log whose last line is not terminated."
        }
        log = current + line + "\n"
        writes += 1
    }

    override fun replaceLog(text: String) {
        failIfAsked()
        log = text
        writes += 1
    }

    override fun readSeal(): String? = seal

    override fun writeSeal(text: String) {
        failIfAsked()
        seal = text
        writes += 1
    }

    private fun failIfAsked() {
        if (failNextWrite) {
            failNextWrite = false
            throw java.io.IOException("simulated storage failure")
        }
    }
}
