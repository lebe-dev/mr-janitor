package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.util.Defaults
import java.io.File

class ReadConfigFromFileTest: StringSpec({

    fun getResourceFile(path: String): File = File(javaClass.getResource("/$path").toURI())

    val useCase = ReadConfigFromFile()

    val validConfigFile = getResourceFile("valid.conf")

    "Read method should return app config" {
        val result = useCase.read(validConfigFile)
        result.isRight() shouldBe true

        when (result) {
            is Either.Right -> {
                result.b.profiles.size shouldBe 2

                val firstProfile = result.b.profiles.first()

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

                val lastProfile = result.b.profiles.last()

                lastProfile.name shouldBe "nginx-logs"
                lastProfile.path shouldBe "."
                lastProfile.storageUnit shouldBe StorageUnit.FILE
                lastProfile.fileNameFilter.pattern shouldBe ".*\\.sql$"
                lastProfile.directoryNameFilter.pattern shouldBe "\\d{4}"
                lastProfile.keepItemsQuantity shouldBe 14
                lastProfile.cleanAction shouldBe CleanAction.COMPRESS

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
            is Either.Left -> throw Exception("assert error")
        }
    }

    "Return failure if config file doesn't exist" {
        useCase.read(File("unknown-file")) shouldBe Either.left(OperationResult.ERROR)
    }

    "Return failure if at least one profile is absent" {
        val configFile = getResourceFile("missing-specified-profile.conf")
        useCase.read(configFile).isLeft() shouldBe true
    }

    "App config should contain complete default profile" {
        val result = useCase.read(validConfigFile)

        result.isRight() shouldBe true

        when (result) {
            is Either.Right -> {
                result.b.defaultProfile.name shouldBe Defaults.PROFILE_NAME
                result.b.defaultProfile.path.isBlank() shouldBe true
                result.b.defaultProfile.keepItemsQuantity shouldBe 7
                result.b.defaultProfile.storageUnit shouldBe StorageUnit.DIRECTORY

                val directoryItemValidationConfig = result.b.defaultProfile.directoryItemValidationConfig
                directoryItemValidationConfig.filesQtyAtLeastAsInPrevious shouldBe true
                directoryItemValidationConfig.sizeAtLeastAsPrevious shouldBe true
                directoryItemValidationConfig.fileSizeAtLeastAsInPrevious shouldBe true

                val fileItemValidationConfig = result.b.defaultProfile.fileItemValidationConfig
                fileItemValidationConfig.sizeAtLeastAsPrevious shouldBe true
                fileItemValidationConfig.md5FileCheck shouldBe true
                fileItemValidationConfig.logFileExists shouldBe true
                fileItemValidationConfig.zipTest shouldBe true

                val cleanUpPolicy = result.b.defaultProfile.cleanUpPolicy

                cleanUpPolicy.invalidItemsBeyondOfKeepQuantity shouldBe true
                cleanUpPolicy.allInvalidItems shouldBe false
            }
            is Either.Left -> throw Exception("assert error")
        }
    }

    "Skip profile if path doesn't exist" {
        val configFile = getResourceFile("profile-path-does-not-exist.conf")
        val result = useCase.read(configFile)

        result.isRight() shouldBe true

        when (result) {
            is Either.Right -> result.b.profiles.size shouldBe 1
            is Either.Left -> throw Exception("assert error")
        }
    }

    "Missing profile properties should be fulfilled from defaults" {
        val configFile = getResourceFile("fulfill-from-defaults.conf")
        val result = useCase.read(configFile)

        result.isRight() shouldBe true

        when (result) {
            is Either.Right -> {
                val firstProfile = result.b.profiles.first()
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
            is Either.Left -> throw Exception("assert error")
        }
    }
})
