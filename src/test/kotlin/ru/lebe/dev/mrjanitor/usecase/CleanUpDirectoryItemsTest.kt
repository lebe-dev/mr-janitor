package ru.lebe.dev.mrjanitor.usecase

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

internal class CleanUpDirectoryItemsTest {

    private lateinit var temporaryDirectory: Path

    private lateinit var useCase: CleanUpDirectoryItems

    @BeforeEach
    fun setUp() {
        temporaryDirectory = Files.createTempDirectory("")

        useCase = CleanUpDirectoryItems()
    }

    @AfterEach
    fun tearDown() {
        temporaryDirectory.toFile().deleteRecursively()
    }

    @Test
    fun `Directories should not exist after cleanup`() {
        val directoryItems = arrayListOf<DirectoryItem>()

        listOf("2019-07-25", "2019-07-26", "2019-07-28").forEach {
            val path = createDirectory(it)
            assertTrue(path.toFile().exists())

            directoryItems.add(
                DirectoryItem(
                    path = path, name = path.toFile().name, size = 238423, fileItems = listOf(), valid = true
                )
            )
        }

        val result = useCase.cleanUp(directoryItems)

        assertTrue(result.isSuccess())

        listOf("2019-07-25", "2019-07-26", "2019-07-28").forEach {
            assertFalse(Paths.get(temporaryDirectory.toString(), it).toFile().exists())
        }
    }

    @Test
    fun `Ignore if directory item does not exist`() {
        val dir = createDirectory("2019-07-25")
        dir.toFile().deleteRecursively()

        val item = DirectoryItem(
            path = dir, name = dir.toFile().name,
            size = 12345, fileItems = listOf(), valid = false
        )

        assertFalse(dir.toFile().exists())

        val result = useCase.cleanUp(listOf(item))

        assertTrue(result.isSuccess())
    }

    private fun createDirectory(name: String): Path {
        val path = Paths.get(temporaryDirectory.toString(), name).apply { toFile().mkdirs() }

        createFile(path, getRandomFileName())
        createFile(path, getRandomFileName())
        createFile(path, getRandomFileName())
        createFile(path, getRandomFileName())
        createFile(path, getRandomFileName())

        return path
    }

    private fun createFile(path: Path, name: String): File =
            Paths.get(path.toString(), name).toFile().apply { writeText(getRandomFileContent()) }

    private fun getRandomFileName() = UUID.randomUUID().toString()

    private fun getRandomFileContent() = UUID.randomUUID().toString()
}