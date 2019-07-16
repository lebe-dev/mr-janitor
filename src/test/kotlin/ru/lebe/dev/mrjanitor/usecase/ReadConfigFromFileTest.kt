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
                firstProfile.keepCopies shouldBe 31
                firstProfile.cleanAction shouldBe CleanAction.REMOVE

                val fileItemValidationConfig1 = firstProfile.fileItemValidationConfig
                fileItemValidationConfig1.md5FileCheck shouldBe false
                fileItemValidationConfig1.zipTest shouldBe true
                fileItemValidationConfig1.logFileExists shouldBe true

                firstProfile.directoryItemValidationConfig.qtyAtLeastAsInPreviousItem shouldBe true

                val lastProfile = result.b.profiles.last()

                lastProfile.name shouldBe "nginx-logs"
                lastProfile.path shouldBe "."
                lastProfile.storageUnit shouldBe StorageUnit.FILE
                lastProfile.keepCopies shouldBe 14
                lastProfile.cleanAction shouldBe CleanAction.COMPRESS

                val fileItemValidationConfig2 = lastProfile.fileItemValidationConfig
                fileItemValidationConfig2.md5FileCheck shouldBe true
                fileItemValidationConfig2.zipTest shouldBe false
                fileItemValidationConfig2.logFileExists shouldBe false

                lastProfile.directoryItemValidationConfig.qtyAtLeastAsInPreviousItem shouldBe false
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
                result.b.defaultProfile.keepCopies shouldBe 7
                result.b.defaultProfile.storageUnit shouldBe StorageUnit.DIRECTORY

                result.b.defaultProfile.directoryItemValidationConfig
                                       .qtyAtLeastAsInPreviousItem shouldBe true
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
                firstProfile.keepCopies shouldBe 7
                firstProfile.cleanAction shouldBe CleanAction.JUST_NOTIFY

                val fileItemValidationConfig = firstProfile.fileItemValidationConfig
                fileItemValidationConfig.md5FileCheck shouldBe true
                fileItemValidationConfig.zipTest shouldBe false
                fileItemValidationConfig.logFileExists shouldBe false

                firstProfile.directoryItemValidationConfig.qtyAtLeastAsInPreviousItem shouldBe true
            }
            is Either.Left -> throw Exception("assert error")
        }
    }
})
