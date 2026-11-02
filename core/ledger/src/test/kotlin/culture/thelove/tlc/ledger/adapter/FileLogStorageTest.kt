package culture.thelove.tlc.ledger.adapter

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The adapter is the only code that touches a disk; it is tested against a real temporary one. */
class FileLogStorageTest {

    private val dir: File = Files.createTempDirectory("tlc-file-storage").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `an unwritten log reads as null, not as an empty string`() {
        val storage = FileLogStorage(dir)
        assertNull(storage.readLog())
        assertNull(storage.readSeal())
    }

    @Test
    fun `appends are visible to a second reader`() {
        FileLogStorage(dir).apply {
            appendLine("one")
            appendLine("two")
        }
        val text = FileLogStorage(dir).readLog()
        assertEquals("one\ntwo\n", text)
    }

    @Test
    fun `replacement is whole-file and leaves no temporary behind`() {
        val storage = FileLogStorage(dir)
        storage.appendLine("old")
        storage.replaceLog("new\n")
        assertEquals("new\n", storage.readLog())
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun `the seal round trips`() {
        FileLogStorage(dir).writeSeal("3:abc:def")
        assertEquals("3:abc:def", FileLogStorage(dir).readSeal())
    }

    @Test
    fun `a newline inside a line is refused by the adapter`() {
        assertFailsWith<IllegalArgumentException> {
            FileLogStorage(dir).appendLine("one\ntwo")
        }
    }

    @Test
    fun `the directory is created when missing`() {
        val nested = File(dir, "a/b/c")
        FileLogStorage(nested).appendLine("x")
        assertEquals("x\n", FileLogStorage(nested).readLog())
    }

    @Test
    fun `non-ascii text is written as utf-8`() {
        val storage = FileLogStorage(dir)
        storage.appendLine("Ada \uD83D\uDC9B")
        assertEquals("Ada \uD83D\uDC9B\n", storage.readLog())
    }

    @Test
    fun `a missing directory path that is a file is refused early`() {
        val file = File(dir, "not-a-dir")
        file.writeText("x")
        assertFailsWith<java.io.IOException> { FileLogStorage(file) }
    }
}
