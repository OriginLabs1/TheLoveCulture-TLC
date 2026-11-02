package culture.thelove.tlc.experience

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The app's private state file.
 *
 * The file is written to a sibling temporary path, flushed to the device, then atomically moved
 * over the old one. A crash therefore leaves either the old state or the new one, never a half
 * state. The format lives in [StateCodec] so the backup bundle and the working file cannot drift.
 */
public class StateFile(private val directory: File) {

    private val file: File = File(directory, FILE_NAME)
    private val temporary: File = File(directory, "$FILE_NAME.tmp")

    /** `null` means "no state yet": a first launch, not an error. */
    public fun read(): StateResult? {
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrElse {
            return StateResult.Invalid("The state file could not be read: ${it.message ?: "I/O error"}")
        }
        return StateCodec.decode(text)
    }

    public fun write(snapshot: AppSnapshot) {
        if (!directory.exists()) directory.mkdirs()
        val text = StateCodec.encode(snapshot)
        FileOutputStream(temporary).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            // Some Android filesystems refuse ATOMIC_MOVE between paths. A rename is still
            // atomic on the same filesystem; fall back rather than refusing to save.
            if (!temporary.renameTo(file)) {
                file.writeText(text)
                temporary.delete()
            }
        }
    }

    public fun delete() {
        file.delete()
        temporary.delete()
    }

    public fun exists(): Boolean = file.exists()

    public companion object {
        public const val FILE_NAME: String = "tlc-state.json"
    }
}
