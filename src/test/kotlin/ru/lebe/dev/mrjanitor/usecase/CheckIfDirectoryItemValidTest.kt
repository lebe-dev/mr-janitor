package ru.lebe.dev.mrjanitor.usecase

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

internal class CheckIfDirectoryItemValidTest {

    private lateinit var indexPath: Path

    private lateinit var useCase: CheckIfDirectoryItemValid

    private val validationConfig = DirectoryItemValidationConfig(
        qtyAtLeastAsInPreviousItem = false
    )

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        useCase = CheckIfDirectoryItemValid()
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().delete()
    }

    @Test
    fun `Return false if path doesn't exist`() {
        val directoryItem = DirectoryItem(
            path = File("path-does-not-exist").toPath(),
            name = "whatever",
            size = 59235,
            fileItems = listOf()
        )

        assertFalse(useCase.isValid(directoryItem, getPreviousItem(), validationConfig))
    }

    @Test
    fun `Return false if size equals zero`() {
        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 0,
            fileItems = listOf()
        )

        assertFalse(useCase.isValid(directoryItem, getPreviousItem(), validationConfig))
    }

    @Test
    fun `Return false if there are no file-items`() {
        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf()
        )

        assertFalse(useCase.isValid(directoryItem, getPreviousItem(), validationConfig))
    }

    @Test
    fun `Return false if file-items count is lesser than in previous item`() {
        val fileItem1 = FileItem(
            path = Paths.get("."), name = "whehehee", size = 123, hash = UUID.randomUUID().toString()
        )

        val fileItem2 = FileItem(
            path = Paths.get("."), name = "bugaga", size = 73457, hash = UUID.randomUUID().toString()
        )

        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf(fileItem1)
        )

        assertFalse(
            useCase.isValid(
                directoryItem, getPreviousItem(fileItems = listOf(fileItem2, fileItem1)),
                validationConfig.copy(qtyAtLeastAsInPreviousItem = true)
            )
        )
    }

    @Test
    fun `Return true if no special validation methods were selected`() {
        val fileItem1 = FileItem(
            path = Paths.get("."), name = "whehehee", size = 123, hash = UUID.randomUUID().toString()
        )

        val fileItem2 = FileItem(
            path = Paths.get("."), name = "bugaga", size = 73457, hash = UUID.randomUUID().toString()
        )

        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf(fileItem1)
        )

        assertTrue(
            useCase.isValid(
                directoryItem, getPreviousItem(fileItems = listOf(fileItem2, fileItem1)),
                validationConfig.copy(qtyAtLeastAsInPreviousItem = false)
            )
        )
    }

    @Test
    fun `Return true if all validation methods were passed`() {
        val fileItem1 = FileItem(
            path = Paths.get("."), name = "whehehee", size = 123, hash = UUID.randomUUID().toString()
        )

        val fileItem2 = FileItem(
            path = Paths.get("."), name = "bugaga", size = 73457, hash = UUID.randomUUID().toString()
        )

        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf(fileItem1, fileItem2)
        )

        assertTrue(
            useCase.isValid(
                directoryItem, getPreviousItem(fileItems = listOf(fileItem1)),
                validationConfig.copy(qtyAtLeastAsInPreviousItem = true)
            )
        )
    }

    private fun getPreviousItem(path: Path = indexPath, size: Long = 12345L,
                                fileItems: List<FileItem> = listOf()) = DirectoryItem(
        path = path,
        name = "whatever",
        size = size,
        fileItems = fileItems
    )
}
