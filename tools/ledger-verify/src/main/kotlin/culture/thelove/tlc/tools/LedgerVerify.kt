package culture.thelove.tlc.tools

import culture.thelove.tlc.ledger.Anchor
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerCodec
import culture.thelove.tlc.ledger.LedgerDraft
import culture.thelove.tlc.ledger.LedgerEntry
import culture.thelove.tlc.ledger.VerificationReport
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.system.exitProcess

/**
 * `ledger-verify` — the nightly, machine-checkable proof that nothing was rewritten (SPECS §B-3).
 *
 * Usage:
 *   ledger-verify --demo
 *   ledger-verify --file chain.tsv [--anchor <length>:<head>:<root>:<createdAtMillis>]
 *
 * TSV columns, no header:
 *   seq \t kind \t subject \t amountKobo|- \t occurredAt \t idempotencyKey|- \t prevHash \t hash
 * Exit codes: 0 verified · 1 verification failed · 2 usage/IO error.
 */
public fun main(args: Array<String>) {
    exitProcess(run(args))
}

internal fun run(args: Array<String>): Int {
    val hasher = PlatformSha256()
    if (args.isEmpty()) {
        println(USAGE)
        return 2
    }
    if (args[0] == "--help" || args[0] == "-h") {
        println(USAGE)
        return 0
    }
    return when (args[0]) {
        "--demo" -> demo(hasher)
        "--file" -> {
            val path = args.getOrNull(1)
            if (path == null) {
                println("✗ --file needs a path")
                return 2
            }
            val anchor = if ("--anchor" in args) {
                runCatching { parseAnchor(args[args.indexOf("--anchor") + 1]) }
                    .getOrElse {
                        println("✗ bad anchor: ${it.message}")
                        return 2
                    }
            } else {
                null
            }
            verifyFile(java.io.File(path), anchor, hasher)
        }
        else -> {
            println("✗ unknown mode: ${args[0]}\n$USAGE")
            2
        }
    }
}

private const val USAGE =
    "usage: ledger-verify --demo | --file <record> [--anchor <len>:<head>:<root>:<createdAt>]\n" +
        "  <record> is either an app export (starts with #tlc-ledger-v1) or a legacy TSV table."

private fun demo(hasher: PlatformSha256): Int {
    val ledger = Ledger(hasher)
    val t0 = 1_760_000_000_000L
    ledger.append(LedgerDraft(EntryKind.FEE_INTENT, "ob:demo", 100_000, t0, "demo:intent"))
    ledger.append(LedgerDraft(EntryKind.PROOF_RECORDED, "ob:demo", 100_000, t0 + 1, "demo:proof"))
    ledger.append(LedgerDraft(EntryKind.ATTESTATION, "ob:demo", 100_000, t0 + 2, "demo:attest"))
    ledger.append(LedgerDraft(EntryKind.RECEIPT, "ob:demo", 100_000, t0 + 3, "demo:receipt"))

    val entries = ledger.entries()
    val anchor = ledger.anchor(t0 + 10)
    val clean = Ledger.verify(entries, hasher, anchor)
    printReport("clean chain", clean)

    val tampered = entries.toMutableList().also { it[1] = it[1].copy(amountKobo = 1) }
    val dirty = Ledger.verify(tampered, hasher, anchor)
    printReport("tampered chain (must fail)", dirty)

    val truncated = Ledger.verify(entries.dropLast(1), hasher, anchor)
    printReport("truncated chain (must fail)", truncated)

    val ok = clean.ok && !dirty.ok && !truncated.ok
    println(if (ok) "✓ invariants hold" else "✗ INVARIANT VIOLATION")
    return if (ok) 0 else 1
}

private fun verifyFile(file: java.io.File, anchor: Anchor?, hasher: PlatformSha256): Int {
    if (!file.isFile) {
        println("✗ no such file: ${file.path}")
        return 2
    }
    val text = runCatching { file.readText() }.getOrElse {
        println("✗ cannot read ${file.path}: ${it.message}")
        return 2
    }

    // A file written by the app is self-describing. The TSV branch keeps the original CLI shape
    // working for anyone who scripted it; both are verified by the same rules.
    val entries = if (text.startsWith(LedgerCodec.HEADER)) {
        when (val decoded = LedgerCodec.decode(text)) {
            is LedgerCodec.Decoded.Ok -> decoded.entries
            is LedgerCodec.Decoded.Unreadable -> {
                println("✗ malformed record: line ${decoded.lineNumber}: ${decoded.reason}")
                return 2
            }
        }
    } else {
        runCatching { parse(text.lines()) }.getOrElse {
            println("✗ malformed chain: ${it.message}")
            return 2
        }
    }

    val report = Ledger.verify(entries, hasher, anchor)
    printReport(file.name, report)
    return if (report.ok) 0 else 1
}

internal fun parse(lines: List<String>): List<LedgerEntry> {
    val out = ArrayList<LedgerEntry>()
    lines.forEachIndexed { i, line ->
        if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
        val cells = line.split('\t')
        require(cells.size == 8) { "line ${i + 1}: expected 8 tab-separated columns, found ${cells.size}" }
        out += LedgerEntry(
            seq = cells[0].toLong(),
            kind = EntryKind.valueOf(cells[1]),
            subject = cells[2],
            amountKobo = cells[3].takeIf { it != "-" }?.toLong(),
            occurredAt = cells[4].toLong(),
            idempotencyKey = cells[5].takeIf { it != "-" },
            prevHash = cells[6],
            hash = cells[7],
        )
    }
    return out
}

internal fun parseAnchor(raw: String): Anchor {
    val parts = raw.split(':')
    require(parts.size == 4) { "anchor must be <length>:<head>:<root>:<createdAt>" }
    return Anchor(
        length = parts[0].toInt(),
        headHash = parts[1],
        merkleRoot = parts[2],
        createdAt = parts[3].toLong(),
    )
}

private fun printReport(label: String, report: VerificationReport) {
    val mark = if (report.ok) "✓" else "✗"
    println("$mark $label: entries=${report.length} head=${report.headHash.take(12)}… root=${report.merkleRoot.take(12)}…")
    report.failures.forEach { println("    - ${it::class.simpleName} @${it.index}") }
}
