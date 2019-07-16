package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.*
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.getInt
import ru.lebe.dev.mrjanitor.util.getString
import java.io.File
import java.nio.file.Paths

class ReadConfigFromFile {
    private val log = LoggerFactory.getLogger(javaClass)

    fun read(file: File): Either<OperationResult, AppConfig> {
        log.info("read configuration from file '${file.absolutePath}'")

        return if (file.exists()) {

            try {
                val config = ConfigFactory.parseFile(file).getConfig("config")

                val internalDefaultProfile = Profile(
                    name = "defaults",
                    path = ".", storageUnit = Defaults.DEFAULT_STORAGE_UNIT,
                    keepCopies = Defaults.DEFAULT_KEEP_COPIES,
                    fileItemValidationConfig = FileItemValidationConfig(
                        md5FileCheck = true, zipTest = true, logFileExists = true
                    ),
                    directoryItemValidationConfig = DirectoryItemValidationConfig(
                        qtyAtLeastAsPreviousValid = true
                    ),
                    cleanAction = CleanAction.JUST_NOTIFY
                )

                when(val defaultProfile = loadProfile(config, Defaults.PROFILE_NAME, internalDefaultProfile)) {
                    is Either.Right -> {

                        when(val profiles = loadProfiles(config, defaultProfile.b)) {
                            is Either.Right -> {
                                Either.right(
                                    AppConfig(
                                        defaultProfile = defaultProfile.b,
                                        profiles = profiles.b
                                    )
                                )
                            }
                            is Either.Left -> {
                                log.error("unable to load profiles")
                                Either.left(OperationResult.ERROR)
                            }
                        }
                    }
                    is Either.Left -> {
                        log.error("unable to load default profile")
                        defaultProfile
                    }

                }

            } catch (e: ConfigException.Missing) {
                log.error("missing property: ${e.message}", e)
                Either.left(OperationResult.ERROR)
            }

        } else {
            log.error("config file wasn't found")
            Either.left(OperationResult.ERROR)
        }
    }

    private fun loadProfiles(config: Config, defaultProfile: Profile): Either<OperationResult, List<Profile>> =
        if (config.hasPath("profiles")) {

            val profiles = arrayListOf<Profile>()

            var profileLoadError = false

            config.getStringList("profiles").forEach { profileName ->
                when(val profile = loadProfile(config, profileName, defaultProfile)) {
                    is Either.Right -> {

                        if (isProfileValid(profile.b)) {
                            profiles += profile.b

                        } else {
                            log.error("invalid profile configuration: '$profileName'")
                        }
                    }
                    is Either.Left -> {
                        log.error("unable to load profile '$profileName'")
                        profileLoadError = true
                    }
                }
            }

            if (!profileLoadError) {
                Either.right(profiles)

            } else {
                Either.left(OperationResult.ERROR)
            }

        } else {
            log.error("'profiles' property hasn't found")
            Either.left(OperationResult.ERROR)
        }

    private fun isProfileValid(profile: Profile): Boolean {
        var result = true

        if (profile.keepCopies < 1) {
            log.error("invalid keep-copies value '${profile.keepCopies}', should be > 0")
            result = false
        }

        if (!Paths.get(profile.path).toFile().exists()) {
            log.error("path '${profile.path}' doesn't exist")
            result = false
        }

        return result
    }

    private fun loadProfile(config: Config, profileName: String,
                            defaultProfile: Profile): Either<OperationResult, Profile> =

        if (config.hasPath(profileName)) {
            Either.right(
                Profile(
                    name = profileName,
                    path = config.getString("$profileName.path", ""),
                    storageUnit = getStorageUnit(config, profileName, defaultProfile.storageUnit),
                    keepCopies = config.getInt("$profileName.keep-copies", defaultProfile.keepCopies),
                    fileItemValidationConfig = getFileItemValidationConfig(
                        config, "$profileName.item-validation", defaultProfile.fileItemValidationConfig
                    ),
                    directoryItemValidationConfig = getDirectoryItemValidationConfig(
                        config, "$profileName.item-validation", defaultProfile.directoryItemValidationConfig
                    ),
                    cleanAction = getCleanAction(config, profileName, CleanAction.JUST_NOTIFY)
                )
            )

        } else {
            log.error("profile '$profileName' wasn't found at path '$profileName'")
            Either.left(OperationResult.ERROR)
        }

    private fun getFileItemValidationConfig(config: Config, sectionPath: String,
                                            defaultValidationConfig: FileItemValidationConfig): FileItemValidationConfig {

        return if (config.hasPath(sectionPath)) {

            FileItemValidationConfig(
                md5FileCheck = getBooleanPropertyValue(
                    config, "$sectionPath.md5-file-check", defaultValidationConfig.md5FileCheck
                ),
                zipTest = getBooleanPropertyValue(
                    config, "$sectionPath.zip-test", defaultValidationConfig.zipTest
                ),
                logFileExists = getBooleanPropertyValue(
                    config, "$sectionPath.log-file-exists", defaultValidationConfig.logFileExists
                )
            )

        } else {
            defaultValidationConfig
        }
    }

    private fun getDirectoryItemValidationConfig(config: Config, sectionPath: String,
                                defaultValidationConfig: DirectoryItemValidationConfig): DirectoryItemValidationConfig {

        return if (config.hasPath(sectionPath)) {

            DirectoryItemValidationConfig(
                qtyAtLeastAsPreviousValid = getBooleanPropertyValue(
                    config, "$sectionPath.qty-at-least-as-previous-valid",
                    defaultValidationConfig.qtyAtLeastAsPreviousValid
                )
            )

        } else {
            defaultValidationConfig
        }
    }

    private fun getBooleanPropertyValue(config: Config, propertyPath: String, defaultValue: Boolean): Boolean =
        if (config.hasPath(propertyPath)) {
            config.getBoolean(propertyPath)

        } else {
            defaultValue
        }

    private fun getStorageUnit(config: Config, profilePath: String, defaultValue: StorageUnit): StorageUnit =
        when(config.getString("$profilePath.unit", "")) {
            "directory" -> StorageUnit.DIRECTORY
            "file" -> StorageUnit.FILE
            else -> defaultValue
        }

    private fun getCleanAction(config: Config, profilePath: String, defaultValue: CleanAction): CleanAction =
        if (config.hasPath("$profilePath.action")) {
            when(config.getString("$profilePath.action")) {
                "compress" -> CleanAction.COMPRESS
                "remove" -> CleanAction.REMOVE
                else -> defaultValue
            }

        } else {
            defaultValue
        }

}
