package player.music.ancient.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry

class PathTraversalTest {

    // Helper to mock ZipEntry since we can't easily instantiate it without a file
    class MockZipEntry(private val entryName: String) : ZipEntry(entryName) {
        override fun getName(): String = entryName
    }

    @Test
    fun testFix() {
        // Create a temporary directory for the test
        val baseDir = File.createTempFile("testDir", "")
        baseDir.delete()
        baseDir.mkdir()

        try {
            // Safe entry
            val safeEntry = MockZipEntry("userImages/photo.jpg")
            val safeFile = invokeIsPathSafe(baseDir, safeEntry)
            assertNotNull(safeFile)
            assertEquals(File(baseDir, "photo.jpg").canonicalPath, safeFile?.canonicalPath)

            // Traversal entry
            val traversalEntry = MockZipEntry("userImages/../../../../etc/passwd")
            val traversalFile = invokeIsPathSafe(baseDir, traversalEntry)
            // It should resolve to baseDir/passwd which is safe (within baseDir)
            assertNotNull(traversalFile)
            assertEquals(File(baseDir, "passwd").canonicalPath, traversalFile?.canonicalPath)

            // Direct traversal
            val directEntry = MockZipEntry("..")
            val directFile = invokeIsPathSafe(baseDir, directEntry)
            assertNull(directFile)

        } finally {
            baseDir.deleteRecursively()
        }
    }

    private fun invokeIsPathSafe(targetDir: File, zipEntry: ZipEntry): File? {
        val method = BackupHelper::class.java.getDeclaredMethod("isPathSafe", File::class.java, ZipEntry::class.java)
        method.isAccessible = true
        return method.invoke(BackupHelper, targetDir, zipEntry) as File?
    }
}
