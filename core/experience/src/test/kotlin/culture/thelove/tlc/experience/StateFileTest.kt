package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.Role
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateFileTest {

    private fun snapshot(): AppSnapshot = AppSnapshot(
        profile = Profile("Ada", "Circle", Role.SINGLE, "0123456789abcdef0123456789abcdef", 1),
        preferences = Preferences(onboarded = true),
        stewards = listOf(Steward("0123456789abcdef0123456789abcdef", "Ada", StewardRole.PRIMARY)),
    )

    @Test
    fun `missing state is a first launch, not an error`() {
        val dir = Files.createTempDirectory("tlc-state").toFile()
        assertNull(StateFile(dir).read())
    }

    @Test
    fun `write then read survives and leaves no temporary file`() {
        val dir = Files.createTempDirectory("tlc-state").toFile()
        val file = StateFile(dir)
        file.write(snapshot())
        assertTrue(file.exists())
        val read = file.read()
        assertTrue(read is StateResult.Ok)
        assertEquals("Ada", read.snapshot.profile.displayName)
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun `a corrupt state file is reported instead of silently replaced`() {
        val dir = Files.createTempDirectory("tlc-state").toFile()
        java.io.File(dir, StateFile.FILE_NAME).writeText("{ not json")
        val read = StateFile(dir).read()
        assertTrue(read is StateResult.Invalid)
    }

    @Test
    fun `delete returns the app to first launch`() {
        val dir = Files.createTempDirectory("tlc-state").toFile()
        val file = StateFile(dir)
        file.write(snapshot())
        file.delete()
        assertNull(file.read())
    }
}
