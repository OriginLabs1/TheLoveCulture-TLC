package culture.thelove.tlc.ledger.adapter

import culture.thelove.tlc.ledger.LogStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * File-backed [LogStorage] for Android and the JVM.
 *
 * Two files, both inside the app's private directory:
 *
 *   ledger.v1.log    the hash chain, one entry per line (human-readable on purpose)
 *   ledger.v1.seal   "length:head:root" — local evidence against a truncated tail
 *
 * Every write is `write -> flush -> fd.sync()` before it returns, so the durable write happens
 * before the app claims the fact happened. A replacement is written to a temporary file, synced,
 * then renamed over the old one, so a crash exposes either the old log or the new one — never a
 * half-written mix.
 *
 * The directory must be private to the app (`context.filesDir`), never shared storage: the record
 * is personal, and Android's per-app sandbox is the access control (SK-07).
 */
public class FileLogStorage(private val directory: File) : LogStorage {

    private val logFile = File(directory, LOG_NAME)
    private val sealFile = File(directory, SEAL_NAME)

    init {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create the record directory: ${directory.path}")
        }
        if (!directory.isDirectory) throw IOException("Not a directory: ${directory.path}")
    }

    public val logPath: String get() = logFile.path

    public val sealPath: String get() = sealFile.path

    override fun readLog(): String? = read(logFile)

    override fun appendLine(line: String) {
        require(!line.contains('\n')) { "An entry line must not contain a newline." }
        FileOutputStream(logFile, true).use { out ->
            out.write((line + "\n").toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
    }

    override fun replaceLog(text: String) {
        replace(logFile, text)
    }

    override fun readSeal(): String? = read(sealFile)

    override fun writeSeal(text: String) {
        replace(sealFile, text)
    }

    private fun read(file: File): String? {
        if (!file.isFile) return null
        val bytes = file.readBytes()
        return if (bytes.isEmpty()) null else String(bytes, Charsets.UTF_8)
    }

    private fun replace(file: File, text: String) {
        val temp = File(directory, "${file.name}.tmp")
        FileOutputStream(temp, false).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (!temp.renameTo(file)) {
            // rename(2) replaces on every platform TLC targets; if it did not, fall back to a
            // delete+rename and let the caller see the failure rather than silently succeed.
            if (!file.delete() || !temp.renameTo(file)) {
                temp.delete()
                throw IOException("Cannot replace ${file.path}")
            }
        }
    }

    private companion object {
        const val LOG_NAME = "ledger.v1.log"
        const val SEAL_NAME = "ledger.v1.seal"
    }
}
