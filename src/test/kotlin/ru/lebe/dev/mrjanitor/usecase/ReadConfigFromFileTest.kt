package ru.lebe.dev.mrjanitor.usecase

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.assertErrorResult
import ru.lebe.dev.mrjanitor.util.assertRightResult
import java.io.File

class ReadConfigFromFileTest: StringSpec({

    fun getResourceFile(path: String): File = File(javaClass.getResource("/$path").toURI())

    val useCase = ReadConfigFromFile()

    val validConfigFile = getResourceFile("valid.conf")

    "Read method should return app config" {
        assertRightResult(useCase.read(validConfigFile)) { result ->
            result.profiles.size shouldBe 2

            val firstProfile = result.profiles.first()

            firstProfile.name shouldBe "mysite"
            firstProfile.path shouldBe "."
            firstProfile.storageUnit shouldBe StorageUnit.DIRECTORY
            firstProfile.fileNameFilter.pattern shouldBe ".*\\.ZIP$"
            firstProfile.directoryNameFilter.pattern shouldBe "\\d{10}-\\d{2}"
            firstProfile.keepItemsQuantity shouldBe 31
            firstProfile.cleanAction shouldBe CleanAction.REMOVE

            val fileItemValidationConfig1 = firstProfile.fileItemValidationConfig
            fileItemValidationConfig1.sizeAtLeastAsPrevious shouldBe true
            fileItemValidationConfig1.md5FileCheck shouldBe true
            fileItemValidationConfig1.zipTest shouldBe false
            fileItemValidationConfig1.logFileExists shouldBe false
            fileItemValidationConfig1.useCustomValidator shouldBe true
            fileItemValidationConfig1.customValidatorCommand shouldBe "zip -t \${filename}"

            firstProfile.directoryItemValidationConfig.filesQtyAtLeastAsInPrevious shouldBe true

            val directoryItemValidationConfig = firstProfile.directoryItemValidationConfig
            directoryItemValidationConfig.filesQtyAtLeastAsInPrevious shouldBe true
            directoryItemValidationConfig.sizeAtLeastAsPrevious shouldBe false
            directoryItemValidationConfig.fileSizeAtLeastAsInPrevious shouldBe false

            firstProfile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity shouldBe false
            firstProfile.cleanUpPolicy.allInvalidItems shouldBe false

            //

            val lastProfile = result.profiles.last()

            lastProfile.name shouldBe "nginx-logs"
            lastProfile.path shouldBe "."
            lastProfile.storageUnit shouldBe StorageUnit.FILE
            lastProfile.fileNameFilter.pattern shouldBe ".*\\.sql$"
            lastProfile.directoryNameFilter.pattern shouldBe "\\d{4}"
            lastProfile.keepItemsQuantity shouldBe 14
            lastProfile.cleanAction shouldBe CleanAction.JUST_NOTIFY

            val fileItemValidationConfig2 = lastProfile.fileItemValidationConfig
            fileItemValidationConfig2.sizeAtLeastAsPrevious shouldBe false
            fileItemValidationConfig2.md5FileCheck shouldBe true
            fileItemValidationConfig2.zipTest shouldBe true
            fileItemValidationConfig2.logFileExists shouldBe false
            fileItemValidationConfig2.useCustomValidator shouldBe false
            fileItemValidationConfig2.customValidatorCommand shouldBe "gzip -t \${filename}"

            val directoryItemValidationConfig2 = lastProfile.directoryItemValidationConfig
            directoryItemValidationConfig2.filesQtyAtLeastAsInPrevious shouldBe false
            directoryItemValidationConfig2.sizeAtLeastAsPrevious shouldBe true
            directoryItemValidationConfig2.fileSizeAtLeastAsInPrevious shouldBe true

            lastProfile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity shouldBe true
            lastProfile.cleanUpPolicy.allInvalidItems shouldBe true
        }
    }

    "Return failure if config file doesn't exist" {
        assertErrorResult(useCase.read(File("unknown-file")))
    }

    "Return failure if at least one profile is absent" {
        val configFile = getResourceFile("missing-specified-profile.conf")
        assertErrorResult(useCase.read(configFile))
    }

    "App config should contain complete default profile" {
        assertRightResult(useCase.read(validConfigFile)) { result ->
            result.defaultProfile.name shouldBe Defaults.PROFILE_NAME
            result.defaultProfile.path.isBlank() shouldBe true
            result.defaultProfile.keepItemsQuantity shouldBe 7
            result.defaultProfile.storageUnit shouldBe StorageUnit.DIRECTORY

            val directoryItemValidationConfig = result.defaultProfile.directoryItemValidationConfig
            directoryItemValidationConfig.filesQtyAtLeastAsInPrevious shouldBe true
            directoryItemValidationConfig.sizeAtLeastAsPrevious shouldBe true
            directoryItemValidationConfig.fileSizeAtLeastAsInPrevious shouldBe true

            val fileItemValidationConfig = result.defaultProfile.fileItemValidationConfig
            fileItemValidationConfig.sizeAtLeastAsPrevious shouldBe true
            fileItemValidationConfig.md5FileCheck shouldBe true
            fileItemValidationConfig.logFileExists shouldBe true
            fileItemValidationConfig.zipTest shouldBe true

            val cleanUpPolicy = result.defaultProfile.cleanUpPolicy

            cleanUpPolicy.invalidItemsBeyondOfKeepQuantity shouldBe true
            cleanUpPolicy.allInvalidItems shouldBe false
        }
    }

    "Skip profile if path doesn't exist" {
        val configFile = getResourceFile("profile-path-does-not-exist.conf")
        assertRightResult(useCase.read(configFile)) {
            it.profiles.size shouldBe 1
        }
    }

    "Missing profile properties should be fulfilled from defaults" {
        val configFile = getResourceFile("fulfill-from-defaults.conf")

        assertRightResult(useCase.read(configFile)) { result ->
            val firstProfile = result.profiles.first()
            firstProfile.storageUnit shouldBe StorageUnit.DIRECTORY
            firstProfile.keepItemsQuantity shouldBe 7
            firstProfile.cleanAction shouldBe CleanAction.JUST_NOTIFY
            firstProfile.fileNameFilter.pattern shouldBe ".*\\.tar.gz$"
            firstProfile.directoryNameFilter.pattern shouldBe "\\d{3}-\\d{6}"

            val fileItemValidationConfig = firstProfile.fileItemValidationConfig
            fileItemValidationConfig.sizeAtLeastAsPrevious shouldBe false
            fileItemValidationConfig.md5FileCheck shouldBe true
            fileItemValidationConfig.zipTest shouldBe true
            fileItemValidationConfig.logFileExists shouldBe false
            fileItemValidationConfig.useCustomValidator shouldBe true
            fileItemValidationConfig.customValidatorCommand shouldBe "blablabla \${filename}"

            val directoryItemValidationConfig = firstProfile.directoryItemValidationConfig
            directoryItemValidationConfig.filesQtyAtLeastAsInPrevious shouldBe true
            directoryItemValidationConfig.sizeAtLeastAsPrevious shouldBe false
            directoryItemValidationConfig.fileSizeAtLeastAsInPrevious shouldBe true

            firstProfile.cleanUpPolicy.allInvalidItems shouldBe true
            firstProfile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity shouldBe true
        }
    }
})
